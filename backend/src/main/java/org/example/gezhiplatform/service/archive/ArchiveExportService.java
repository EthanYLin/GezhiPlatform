package org.example.gezhiplatform.service.archive;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.exception.ExcelRuntimeException;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.idev.excel.write.metadata.fill.FillWrapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.TypeRef;
import jakarta.servlet.ServletOutputStream;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.User;
import org.example.gezhiplatform.entity.enums.AuditOperationType;
import org.example.gezhiplatform.entity.enums.Campus;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.service.audit.AuditService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;


/**
 * 档案导出服务
 *
 * <p><b>职责：</b></p>
 * 提供档案导出接口，根据用户权限返回过滤后的档案数据，屏蔽掉用户无权访问的字段，并根据模板导出为Excel格式。
 *
 * <p><b>架构说明：</b></p>
 * <p>
 * 档案查询（{@link ArchiveQueryService}）、档案导出（{@link ArchiveExportService}）
 * 与档案更新服务（{@link ArchiveUpdateService}）在处理请求时，会调用访问控制服务
 * （{@link ArchiveAccessControlService}）获取用户对于该学生的读写权限。
 * </p>
 * <p>
 * 访问控制服务（{@link ArchiveAccessControlService}）将根据用户的权限过滤档案数据：
 * <ul>
 *   <li>在<b>读取操作</b>中，只返回档案中用户有权限访问的字段。</li>
 *   <li>在<b>更新操作</b>中，去除请求体中用户不可写的字段数据。</li>
 * </ul>
 * </p>
 * <p>
 * 在进行权限判断时，依赖以下服务：
 * <ul>
 *   <li>档案元字段服务（{@link ArchiveMetadataService}）- 提供档案字段及类型信息。</li>
 *   <li>权限组配置服务（{@link ArchivePermissionGroupService}）- 提供用户角色所在的权限组及其读写权限。</li>
 * </ul>
 * </p>
 */
@RequiredArgsConstructor
@Service
public class ArchiveExportService {

    private final AuditService auditService;
    private final ArchiveAccessControlService archiveAccessControlService;

    @Value("classpath:templates/archiveExportTemplate.xlsx")
    private Resource archiveExportTemplate;

    /**
     * 拼接地址字符串
     */
    private static String buildAddressString(
        @Nullable String province,
        @Nullable String city,
        @Nullable String district,
        @Nullable String detail
    ) {
        StringBuilder sb = new StringBuilder();
        if (province != null && !province.isEmpty()) sb.append(province);
        if (city != null && !city.isEmpty() && !java.util.Objects.equals(province, city)) sb.append(city);
        if (district != null && !district.isEmpty()) sb.append(district);
        if (detail != null && !detail.isEmpty()) sb.append(detail);
        return !sb.isEmpty() ? sb.toString() : null;
    }

    private static LocalDateTime parseDateTime(@Nullable String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 导出指定学号学生的档案为Excel文件
     * <p>
     * 根据当前用户的权限导出经过过滤的学生档案数据到Excel文件。
     * 该方法会使用预定义的Excel模板，将过滤后的档案数据填充到模板中并输出到指定的输出流。
     * </p>
     * <p>
     * 导出和权限过滤流程：
     * <ol>
     *   <li>通过 {@link ArchiveAccessControlService.UserStudentArchive} 查询学生档案并进行权限过滤</li>
     *   <li>调用 {@link AuditService#log} 记录档案导出操作的审计日志</li>
     *   <li>调用 {@link #write} 方法将过滤后的档案数据填充到Excel模板并输出</li>
     * </ol>
     * </p>
     *
     * @param currentUserId 当前用户ID
     * @param stuNoForQuery 要导出的学生学号
     * @param outputStream  用于输出Excel文件的输出流
     * @throws BadRequestException 当档案序列化失败、IO错误或Excel处理错误时抛出
     * @throws NotFoundException   当用户或学生不存在，或学生无档案时抛出
     */
    @Transactional
    public void exportByStuNo(
        @NotNull Long currentUserId,
        @NotNull String stuNoForQuery,
        @NotNull ServletOutputStream outputStream
    ) throws BadRequestException {
        // 查询学生档案
        var context = archiveAccessControlService.new UserStudentArchive(currentUserId, stuNoForQuery);
        // 在审计日志中记录
        auditService.log(
            context.user(), AuditOperationType.ARCHIVE_EXPORT,
            String.format("导出学生档案(学号:%s, 姓名: %s )", context.student().getStuNo(), context.student().getStuName())
        );
        // 调用 write() 进行导出
        this.write(outputStream, context.getReadableArchive(), context.student(), context.user());
    }

    /**
     * 生成导出文件的文件名
     * <p>
     * 根据学生信息生成格式化的导出文件名，包含学生姓名、学号和当前时间戳。
     * 文件名格式为：{学生姓名}({学号})学生档案-{时间戳(yyyyMMdd-HHmmss)}
     * </p>
     *
     * @param currentUserId 当前用户ID
     * @param stuNoForQuery 要导出的学生学号
     * @return 格式化的导出文件名
     * @throws NotFoundException 当学生不存在时抛出
     */
    public String getExportFileName(@NotNull Long currentUserId, @NotNull String stuNoForQuery) throws BadRequestException{
        Student student = archiveAccessControlService.new UserStudentArchive(currentUserId, stuNoForQuery).student();
        return String.format(
            "%s(%s)学生档案-%s",
            student.getStuName(),
            student.getStuNo(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        );
    }

    /**
     * 将档案数据写入Excel模板并输出
     * <p>
     * 使用预定义的Excel模板，将过滤后的档案数据填充到"学生信息登记表"和"健康声明表"两个工作表中。
     * </p>
     *
     * @param outputStream 输出流
     * @param filteredArchiveJson 经过权限过滤的档案JSON数据
     * @param student 学生信息
     * @param exporter 导出操作的用户
     * @throws RuntimeException 当IO错误或Excel处理错误时抛出
     */
    private void write(
        @NotNull ServletOutputStream outputStream,
        @NotNull DocumentContext filteredArchiveJson,
        @NotNull Student student,
        @NotNull User exporter
    ) throws RuntimeException {
        try (
            InputStream templateStream = archiveExportTemplate.getInputStream();
            ExcelWriter writer = FastExcel.write(outputStream).withTemplate(templateStream).build()
        ) {
            WriteSheet baseSheet = FastExcel.writerSheet("学生信息登记表").build();
            writer.fill(buildBaseInfo(student, exporter), baseSheet);
            writer.fill(buildArchiveMap(filteredArchiveJson), baseSheet);
            WriteSheet healthSheet = FastExcel.writerSheet("健康声明表").build();
            writer.fill(buildBaseInfo(student, exporter), healthSheet);
            writer.fill(buildHealthPartTips(filteredArchiveJson), healthSheet);
            writer.fill(new FillWrapper("healthPart", buildHealthPartList(filteredArchiveJson)), healthSheet);
        } catch (IOException e) {
            throw new RuntimeException("无法导出档案数据, 发生IO错误(追踪点:AES01): " + e.getMessage());
        } catch (ExcelRuntimeException e) {
            throw new RuntimeException("无法导出档案数据, 发生Excel错误(追踪点:AES02): " + e.getMessage());
        }
    }

    /**
     * 构建基础信息（导出人、导出时间、学号、姓名、班级、校区）的 Map，供 Excel 填充。
     *
     * @param student 学生信息
     * @param user 导出操作的用户
     * @return 基础信息的键值对映射
     */
    private Map<String, String> buildBaseInfo(@NotNull Student student, @NotNull User user) {
        Map<String, String> result = new HashMap<>();
        // 基础信息部分(导出人、导出时间、学号、姓名、班级、校区)
        result.put("exporter", user.toString());
        result.put("exportTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        result.put("stuNo", student.getStuNo());
        result.put("stuName", student.getStuName());
        result.put("stuClass", student.getGradeClass().map(GradeClass::toRelativeExpr).orElse(null));
        result.put("campus", student.getCampus().map(Campus::getName).orElse(null));
        return result;
    }

    /**
     * 构建档案信息的 Map，供 Excel 填充。
     * <p>
     * 包括个人信息、录取信息、地址信息（居住地址、户籍地址）、家庭信息（父母、其他直系亲属）等部分。
     * </p>
     *
     * @param document 档案JSON文档上下文
     * @return 档案各字段的键值对映射
     */
    private Map<String, String> buildArchiveMap(@NotNull DocumentContext document) {
        Map<String, String> result = new HashMap<>();

        // 个人信息部分
        result.put("gender", document.read("$.personalPart.gender", String.class));
        result.put("birthDate", document.read("$.personalPart.birthDate", String.class));
        result.put("nation", document.read("$.personalPart.nation", String.class));
        result.put("politicalStatus", document.read("$.personalPart.politicalStatus", String.class));
        result.put("mobile", document.read("$.personalPart.mobile", String.class));
        result.put("rin", document.read("$.personalPart.rin", String.class));

        // 录取信息部分
        result.put("juniorHighSchoolDistrict", document.read("$.admissionPart.juniorHighSchoolDistrict", String.class));
        result.put("admissionPath", document.read("$.admissionPart.admissionPath", String.class));
        result.put("juniorHighSchoolName", document.read("$.admissionPart.juniorHighSchoolName", String.class));

        // 地址信息部分 - 居住地址
        String currentAddressProvince = document.read("$.addressPart.currentAddress.province", String.class);
        String currentAddressCity = document.read("$.addressPart.currentAddress.city", String.class);
        String currentAddressDistrict = document.read("$.addressPart.currentAddress.district", String.class);
        String currentAddressDetail = document.read("$.addressPart.currentAddress.detail", String.class);
        result.put("currentAddress", buildAddressString(currentAddressProvince, currentAddressCity, currentAddressDistrict, currentAddressDetail));
        result.put("street", document.read("$.addressPart.currentAddress.street", String.class));
        result.put("committee", document.read("$.addressPart.currentAddress.committee", String.class));

        // 地址信息部分 - 户籍地址
        String domicileAddressProvince = document.read("$.addressPart.domicileAddress.province", String.class);
        String domicileAddressCity = document.read("$.addressPart.domicileAddress.city", String.class);
        String domicileAddressDistrict = document.read("$.addressPart.domicileAddress.district", String.class);
        String domicileAddressDetail = document.read("$.addressPart.domicileAddress.detail", String.class);
        result.put("domicileAddress", buildAddressString(domicileAddressProvince, domicileAddressCity, domicileAddressDistrict, domicileAddressDetail));

        // 家庭信息部分 - 父母
        result.put("fatherName", document.read("$.familyPart.father.name", String.class));
        result.put("motherName", document.read("$.familyPart.mother.name", String.class));
        result.put("fatherMobile", document.read("$.familyPart.father.mobile", String.class));
        result.put("motherMobile", document.read("$.familyPart.mother.mobile", String.class));
        result.put("fatherWorkUnit", document.read("$.familyPart.father.workUnit", String.class));
        result.put("motherWorkUnit", document.read("$.familyPart.mother.workUnit", String.class));

        // 家庭信息部分 - 其他直系亲属
        result.put("rel1Name", document.read("$.familyPart.otherRelatives[0].name", String.class));
        Integer rel1Age = document.read("$.familyPart.otherRelatives[0].age", Integer.class);
        result.put("rel1Age", rel1Age == null ? null : String.valueOf(rel1Age));
        result.put("rel2Name", document.read("$.familyPart.otherRelatives[1].name", String.class));
        Integer rel2Age = document.read("$.familyPart.otherRelatives[1].age", Integer.class);
        result.put("rel2Age", rel2Age == null ? null : String.valueOf(rel2Age));
        result.put("rel1Info", document.read("$.familyPart.otherRelatives[0].info", String.class));
        result.put("rel2Info", document.read("$.familyPart.otherRelatives[1].info", String.class));

        // 其他直系亲属提示
        List<?> otherRelatives = document.read("$.familyPart.otherRelatives", List.class);
        String remainRelativeTips = otherRelatives != null && otherRelatives.size() > 2
            ? "剩余 " + (otherRelatives.size() - 2) + " 位直系亲属未显示，请登录系统查看。"
            : null;
        result.put("remainRelativeTips", remainRelativeTips);

        return result;
    }

    /**
     * 构建健康声明列表，供 Excel 填充。
     * <p>
     * 从档案中读取身体情况和心理情况两个列表，为每条记录添加类别标识，然后按创建时间排序后合并返回。
     * </p>
     *
     * @param document 档案JSON文档上下文
     * @return 合并排序后的健康声明列表
     */
    private List<Map<String, String>> buildHealthPartList(@NotNull DocumentContext document) {
        var physicals = Optional.ofNullable(
            document.<List<Map<String, String>>>read("$.healthPart.physicalCondition", new TypeRef<>() {})
        ).orElse(new ArrayList<>());

        var mentals = Optional.ofNullable(
            document.<List<Map<String, String>>>read("$.healthPart.mentalCondition", new TypeRef<>() {})
        ).orElse(new ArrayList<>());

        // 为每条记录添加类别标识
        physicals.forEach(condition -> condition.put("category", "身体情况"));
        mentals.forEach(condition -> condition.put("category", "心理情况"));

        // 合并并按创建时间排序
        return Stream.concat(physicals.stream(), mentals.stream())
            .sorted(Comparator.comparing(
                condition -> parseDateTime(condition.get("createdAt")),
                Comparator.nullsLast(Comparator.naturalOrder())
            ))
            .toList();
    }

    /**
     * 构建健康声明提示信息
     * <p>
     * 根据用户权限和健康记录数量生成提示文本。
     * </p>
     *
     * @param document 档案JSON文档上下文
     * @return 包含提示信息的键值对映射
     */
    private Map<String, String> buildHealthPartTips(@NotNull DocumentContext document) {
        Map<String, String> result = new HashMap<>();
        // 健康声明部分 - 提示信息
        @Nullable List<?> physicalConditions = document.read("$.healthPart.physicalCondition", List.class);
        @Nullable List<?> mentalConditions = document.read("$.healthPart.mentalCondition", List.class);
        if (physicalConditions == null || mentalConditions == null) {
            result.put("healthPartTips", "您没有权限访问该学生的健康声明信息");
        } else {
            int totalConditions = physicalConditions.size() + mentalConditions.size();
            result.put("healthPartTips", totalConditions == 0
                ? "该学生当前无健康声明记录"
                : "共有 " + totalConditions + " 条身体或心理关注情况"
            );
        }
        return result;
    }

}
