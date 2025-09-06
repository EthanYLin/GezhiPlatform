package org.example.gezhiplatform.service.archive;

import cn.idev.excel.FastExcel;
import cn.idev.excel.exception.ExcelRuntimeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.transaction.Transactional;
import org.example.gezhiplatform.DTO.archive.ArchiveExportResponse;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.User;
import org.example.gezhiplatform.entity.archive.Archive;
import org.example.gezhiplatform.entity.enums.AuditOperationType;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.repository.StudentRepository;
import org.example.gezhiplatform.repository.UserRepository;
import org.example.gezhiplatform.service.audit.AuditService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS;

/**
 * 档案查询服务
 *
 * <p><b>职责：</b></p>
 * 提供档案查询接口，根据用户权限返回过滤后的档案数据，屏蔽掉用户无权访问的字段。
 *
 * <p><b>架构说明：</b></p>
 * <p>
 * 档案查询服务（{@link ArchiveQueryService}）与档案更新服务（{@link ArchiveUpdateService}）
 * 在处理请求时，会调用访问控制服务（{@link ArchiveAccessControlService}）获取用户对于该学生的读写权限。<br/>
 * 访问控制服务在进行权限判断时，需要依赖：<br/>
 * - 档案元字段服务（{@link ArchiveMetadataService}），提供档案字段及类型信息；<br/>
 * - 权限组配置服务（{@link ArchivePermissionGroupService}），提供用户角色所在的权限组及其读写权限。<br/>
 * 最终，{@link ArchiveQueryService} 与 {@link ArchiveUpdateService}
 * 会根据 {@link ArchiveAccessControlService} 返回的权限信息，过滤或限制数据访问。
 * </p>
 */
@Service
public class ArchiveQueryService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ArchiveMetadataService archiveMetadataService;
    private final ArchiveAccessControlService archiveAccessControlService;
    private final AuditService auditService;

    private final ParseContext jsonParser = JsonPath.using(defaultConfiguration().setOptions(SUPPRESS_EXCEPTIONS));
    private final ObjectMapper objectMapper;

    @Value("classpath:templates/archiveExportTemplate.xlsx")
    private Resource archiveExportTemplate;

    public ArchiveQueryService(
        StudentRepository studentRepository,
        ArchiveMetadataService archiveMetadataService,
        ObjectMapper objectMapper,
        ArchiveAccessControlService archiveAccessControlService,
        AuditService auditService,
        UserRepository userRepository
    ) {
        this.studentRepository = studentRepository;
        this.archiveMetadataService = archiveMetadataService;
        this.objectMapper = objectMapper;
        this.archiveAccessControlService = archiveAccessControlService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    /**
     * 查询指定学号学生的档案（基于权限过滤）
     * <p>
     * 根据当前用户的权限返回过滤后的学生档案数据。
     * 只有用户具备读取权限的字段才会在返回结果中包含，无权访问的字段将被移除。
     * </p>
     * <p>
     * 查询和权限过滤流程：
     * <ol>
     *   <li>根据学号查找学生档案数据</li>
     *   <li>调用{@link #filterArchivedData(Archive, User, Student)}方法过滤档案数据</li>
     *   <li>返回过滤后的档案JSON数据</li>
     * </ol>
     * </p>
     *
     * @param currentUserId 当前用户ID
     * @param stuNoForQuery 要查询的学生学号
     * @return 经过权限过滤的学生档案JSON字符串
     * @throws BadRequestException 当档案序列化失败时抛出
     * @throws NotFoundException   当学生不存在时抛出
     */
    @Transactional
    public String queryByStuNo(
        @NotNull Long currentUserId,
        @NotNull String stuNoForQuery
    ) throws BadRequestException {
        // 查询学生档案
        User user = userRepository.findById(currentUserId).orElseThrow(
            () -> new NotFoundException("当前操作用户不存在 (ID:" + currentUserId + ")")
        );
        Student student = studentRepository.findByStuNo(stuNoForQuery).orElseThrow(
            () -> new NotFoundException("要查询的学生(学号:" + stuNoForQuery + ")不存在")
        );
        Archive archive = Optional.ofNullable(student.getArchive()).orElseThrow(
            () -> new NotFoundException("要查询的学生(学号:" + stuNoForQuery + ")尚无档案")
        );

        var archiveJayway = filterArchivedData(archive, user, student);

        // 在审计日志中记录
        auditService.log(
            user, AuditOperationType.ARCHIVE_QUERY,
            String.format("查询学生档案(学号:%s, 姓名: %s)", student.getStuNo(), student.getStuName())
        );

        return archiveJayway.jsonString();
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
     *   <li>根据学号查找学生档案数据</li>
     *   <li>记录审计日志</li>
     *   <li>调用{@link #filterArchivedData(Archive, User, Student)}方法过滤档案数据</li>
     *   <li>将过滤后的数据转换为ArchiveExportResponse对象</li>
     *   <li>使用Excel模板生成文件并输出到指定流</li>
     * </ol>
     * </p>
     *
     * @param currentUserId 当前用户ID
     * @param stuNoForQuery 要导出的学生学号
     * @param outputStream 用于输出Excel文件的输出流
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
        User user = userRepository.findById(currentUserId).orElseThrow(
            () -> new NotFoundException("当前操作用户不存在 (ID:" + currentUserId + ")")
        );
        Student student = studentRepository.findByStuNo(stuNoForQuery).orElseThrow(
            () -> new NotFoundException("要查询的学生(学号:" + stuNoForQuery + ")不存在")
        );
        Archive archive = Optional.ofNullable(student.getArchive()).orElseThrow(
            () -> new NotFoundException("要查询的学生(学号:" + stuNoForQuery + ")尚无档案")
        );

        // 在审计日志中记录
        auditService.log(
            user, AuditOperationType.ARCHIVE_EXPORT,
            String.format("导出学生档案(学号:%s, 姓名: %s)", student.getStuNo(), student.getStuName())
        );


        String trimmedArchiveJson = filterArchivedData(archive, user, student).jsonString();
        Archive trimmedArchive;

        try {
            trimmedArchive = objectMapper.readValue(trimmedArchiveJson, Archive.class);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("无法反序列化档案数据(追踪点:AQS02) :" + e.getMessage());
        }

        ArchiveExportResponse data = ArchiveExportResponse.of(trimmedArchive, student, user.toString());
        Map<?, ?> dataMap = objectMapper.convertValue(data, Map.class);
        try (InputStream templateStream = archiveExportTemplate.getInputStream()) {
            FastExcel.write(outputStream)
                     .withTemplate(templateStream)
                     .sheet()
                     .doFill(dataMap);
        } catch (IOException e) {
            throw new BadRequestException("无法导出档案数据, 发生IO错误(追踪点:AQS03): " + e.getMessage());
        } catch (ExcelRuntimeException e) {
            throw new BadRequestException("无法导出档案数据, 发生Excel错误(追踪点:AQS04): " + e.getMessage());
        }

    }

    /**
     * 生成导出文件的文件名
     * <p>
     * 根据学生信息生成格式化的导出文件名，包含学生姓名、学号和当前时间戳。
     * 文件名格式为：{学生姓名}({学号})学生档案-{时间戳}
     * </p>
     *
     * @param stuNoForQuery 要导出的学生学号
     * @return 格式化的导出文件名
     * @throws NotFoundException 当学生不存在时抛出
     */
    public String getExportFileName(@NotNull String stuNoForQuery) {
        Student student = studentRepository.findByStuNo(stuNoForQuery).orElseThrow(
            () -> new NotFoundException("要查询的学生(学号:" + stuNoForQuery + ")不存在")
        );
        return String.format(
            "%s(%s)学生档案-%s",
            student.getStuName(),
            student.getStuNo(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        );
    }

    /**
     * 根据用户权限过滤档案数据
     * <p>
     * 该私有方法负责根据当前用户对指定学生的访问权限来过滤档案数据。
     * 它会获取用户的可读权限路径，计算出不允许访问的路径，然后从档案JSON中删除这些路径对应的数据。
     * </p>
     * <p>
     * 过滤流程：
     * <ol>
     *   <li>将Archive对象序列化为JSON字符串</li>
     *   <li>解析JSON为DocumentContext对象以便进行JsonPath操作</li>
     *   <li>获取用户对该学生的可读权限路径</li>
     *   <li>计算不允许访问的路径（补集）</li>
     *   <li>从DocumentContext中删除不允许访问的路径</li>
     *   <li>返回过滤后的DocumentContext对象</li>
     * </ol>
     * </p>
     *
     * @param archive 要过滤的档案对象
     * @param user 当前用户
     * @param student 目标学生
     * @return 经过权限过滤的DocumentContext对象，可调用jsonString()方法获取JSON字符串
     * @throws BadRequestException 当档案序列化失败时抛出
     */
    private @NotNull DocumentContext filterArchivedData(@NotNull Archive archive, @NotNull User user, @NotNull Student student) {
        // 将档案数据经由String转换为JaywayDocument
        String archiveJson;
        try {
            archiveJson = objectMapper.writeValueAsString(archive);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("无法序列化档案数据(追踪点:AQS01) :" + e.getMessage());
        }
        var archiveJayway = jsonParser.parse(archiveJson);

        // 获取用户具有的可读权限
        var allowedReadablePaths = archiveAccessControlService
            .getMergedPermissions(user, student)
            .allowedJsonPaths().readableJsonPaths();
        var deniedReadablePaths = archiveMetadataService.getComplementSet(allowedReadablePaths);

        // 根据可读权限裁剪档案数据并返回
        deniedReadablePaths.forEach(archiveJayway::delete);
        return archiveJayway;
    }


}
