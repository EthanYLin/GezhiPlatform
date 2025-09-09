package org.example.gezhiplatform.service.student;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.example.gezhiplatform.DTO.PageResult;
import org.example.gezhiplatform.DTO.student.StudentCoverResponse;
import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.User;
import org.example.gezhiplatform.entity.archive.Archive;
import org.example.gezhiplatform.entity.archive.family_part.FamilyPart;
import org.example.gezhiplatform.entity.archive.family_part.Parent;
import org.example.gezhiplatform.entity.archive.personal_part.PersonalPart;
import org.example.gezhiplatform.entity.role.*;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.FieldNotFoundException;
import org.example.gezhiplatform.repository.StudentRepository;
import org.example.gezhiplatform.service.archive.ArchiveQueryService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.example.gezhiplatform.utils.ReflectionUtils.getField;
import static org.example.gezhiplatform.utils.ReflectionUtils.getIllegalSortProperties;

/**
 * 学生查询服务类
 * 1. 按照年级/班级查询学生名单，名单内只包括学号、姓名等基本信息
 * 2. 按照学号、姓名或手机号查找学生，查找结果只包括学号、姓名等基本信息
 * 该服务类面向普通用户，查询结果受到该用户的权限控制。
 * 系统管理员维护学生信息请使用 {@link StudentManagementService}
 * 查询学生具体档案信息请使用 {@link ArchiveQueryService}
 */
@Service
public class StudentQueryService {

    private final GradeClassService gradeClassService;
    private final StudentRepository studentRepository;

    public StudentQueryService(GradeClassService gradeClassService, StudentRepository studentRepository) {
        this.gradeClassService = gradeClassService;
        this.studentRepository = studentRepository;
    }

    @PostConstruct
    private void checkFields() {
        getField(Student.class, "gradeClass", GradeClass.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "学生查询服务(StudentQueryService)需要依照班级(gradeClass)进行筛选, 但未在Student类中找到GradeClass类型的gradeClass字段。"));

        getField(GradeClass.class, "gradeNo", Integer.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "学生查询服务(StudentQueryService)需要依照年级(gradeNo)进行筛选 但未在GradeClass类中找到Integer类型的gradeNo字段。"));

        getField(GradeClass.class, "classNo", Integer.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "学生查询服务(StudentQueryService)需要依照班级(classNo)进行筛选 但未在GradeClass类中找到Integer类型的classNo字段。"));

        getField(Student.class, "stuNo", String.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "学生查询服务(StudentQueryService)需要依照学号(stuNo)进行搜索, 但未在Student类中找到String类型的stuNo字段。"));

        getField(Student.class, "stuName", String.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "学生查询服务(StudentQueryService)需要依照姓名(stuName)进行搜索, 但未在Student类中找到String类型的stuName字段。"));

        // 检查手机号字段路径：Student -> Archive -> PersonalPart -> mobile
        getField(Student.class, "archive", Archive.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "学生查询服务(StudentQueryService)需要依照手机号(mobile)进行搜索并访问学生档案(archive), 但未在Student类中找到Archive类型的archive字段。"));

        getField(Archive.class, "personalPart", PersonalPart.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "学生查询服务(StudentQueryService)需要依照手机号(mobile)进行搜索并访问个人信息(personalPart), " +
                "但未在Archive类中找到PersonalPart类型的personalPart字段。"));

        getField(PersonalPart.class, "mobile", String.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "学生查询服务(StudentQueryService)需要依照手机号(mobile)进行搜索, 但未在PersonalPart类中找到String类型的mobile字段。"));

        // 检查父母手机号和姓名字段路径：Student -> Archive -> FamilyPart -> Father/Mother -> mobile/name
        getField(Archive.class, "familyPart", FamilyPart.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "学生查询服务(StudentQueryService)需要依照父母手机号和姓名进行搜索并访问家庭信息(familyPart), " +
                "但未在Archive类中找到FamilyPart类型的familyPart字段。"));

        getField(FamilyPart.class, "father", Parent.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "学生查询服务(StudentQueryService)需要依照父亲手机号和姓名进行搜索并访问父亲信息(father), " +
                "但未在FamilyPart类中找到Parent类型的father字段。"));

        getField(FamilyPart.class, "mother", Parent.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "学生查询服务(StudentQueryService)需要依照母亲手机号和姓名进行搜索并访问母亲信息(mother), " +
                "但未在FamilyPart类中找到Parent类型的mother字段。"));

        getField(Parent.class, "mobile", String.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "学生查询服务(StudentQueryService)需要依照父母手机号进行搜索, 但未在Parent类中找到String类型的mobile字段。"));

        getField(Parent.class, "name", String.class)
            .orElseThrow(() -> new FieldNotFoundException(
                "学生查询服务(StudentQueryService)需要依照父母姓名进行搜索, 但未在Parent类中找到String类型的name字段。"));
    }

    /**
     * <p>获取用户可访问的所有班级。</p>
     * <p>根据用户的角色权限，返回该用户可以访问的所有班级列表：</p>
     * <ul>
     *   <li>班主任：添加管理的班级</li>
     *   <li>年级组长：添加该年级的所有班级</li>
     *   <li>多班级观察员：添加所有关联的班级</li>
     *   <li>校级领导及超级管理员：添加学校中的所有班级</li>
     *   <li>协同用户：添加所有关联的学生所在班级</li>
     *   <li>学生本人及家长：返回学生所在班级</li>
     *   <li>其他角色：不添加任何班级</li>
     * </ul>
     * <p>如果一个用户有多个角色，取所有角色权限范围内班级的并集，并去重。</p>
     * <p>返回的班级列表按年级和班级升序。</p>
     *
     * @param user 用户对象
     * @return 该用户可访问的班级列表，按年级和班级升序。即使只能访问班级里的部分学生，仍然会返回该班级。
     */
    public List<GradeClass> getAllAccessibleClasses(User user) {
        Set<GradeClass> result = new HashSet<>();
        user.getRoles().forEach(role -> {
            switch (role) {
                case ClassAdviser ca -> result.add(ca.getGradeClass());
                case MultipleClassObserver mco -> result.addAll(mco.getGradeClasses());
                case GradeDean dean -> {
                    Integer gradeNo = dean.getGradeNo();
                    if (gradeNo == null) return;
                    result.addAll(gradeClassService.getGradeClassesByGrade(gradeNo));
                }
                case SuperAdmin _, Principal _ -> result.addAll(gradeClassService.getAllGradeClasses());
                case CollaborativeUser cu -> result.addAll(gradeClassService.getGradeClassesByStuNos(cu.getStuNos()));
                case StudentUser su -> result.addAll(gradeClassService.getGradeClassesByStuNos(
                    Optional.ofNullable(su.getStuNo()).map(List::of).orElse(List.of())
                ));
                case ParentUser pu -> result.addAll(gradeClassService.getGradeClassesByStuNos(pu.getStuNos()));
                case null, default -> {}
            }
        });
        return result.stream().sorted().toList();
    }

    /**
     * 根据条件和用户权限搜索学生
     * <p>
     * 该方法是学生查询的核心方法，使用<b>AND逻辑</b>将<b>外部传入的查询条件</b>与<b>权限条件</b>结合。
     * 确保用户只能查看其权限范围内的学生数据。
     * </p>
     * <p><b>分页的最大页大小为1000条记录，超过将会抛出 {@link BadRequestException}</b></p>
     * <p>
     * 权限控制机制：
     * <ul>
     *   <li>将用户的所有角色权限使用OR逻辑合并，获得用户可访问的学生范围</li>
     *   <li>将查询条件与用户权限范围使用AND逻辑结合</li>
     *   <li>最终结果 = 查询条件 AND (角色权限1 OR 角色权限2 OR ...)</li>
     * </ul>
     * </p>
     *
     * @param condition 查询条件，用于过滤学生的JPA Specification
     * @param user      当前用户，用于应用权限过滤
     * @param pageable  分页参数
     * @return 符合条件且在用户权限范围内的学生基本信息分页结果
     */
    public PageResult<StudentCoverResponse> searchStudents(
        @NotNull Specification<Student> condition,
        @NotNull User user,
        @NotNull Pageable pageable
    ) throws BadRequestException {
        if (pageable.getPageSize() > 1000) {
            throw new BadRequestException("分页的最大页大小为1000条记录。");
        }
        Set<String> illegalSortProperties = getIllegalSortProperties(Student.class, pageable);
        if (!illegalSortProperties.isEmpty()) {
            throw new BadRequestException("分页排序参数中包含无效的字段: " + String.join(", ", illegalSortProperties));
        }
        Specification<Student> userSpec = user.getSpec();
        Specification<Student> combinedSpec = Specification.where(condition).and(userSpec);
        return new PageResult<>(
            studentRepository
                .findBy(combinedSpec, q -> q.sortBy(pageable.getSort()).page(pageable))
                .map(StudentCoverResponse::new)
        );
    }

    /**
     * 根据多种条件和用户权限搜索学生
     * <p>
     * 该方法是{@link #searchStudents(Specification, User, Pageable)}的封装，
     * 支持<b>年级、班级、关键词</b>的组合查询。所有查询条件使用<b>AND逻辑</b>连接，
     * 关键词内部使用<b>OR逻辑</b>在学号、姓名、手机号、父母手机号、父母姓名之间进行匹配。
     * </p>
     * <p><b>分页的最大页大小为1000条记录，超过将会抛出 {@link BadRequestException}</b></p>
     * <p>
     * 查询条件组合方式：
     * <ul>
     *   <li><b>年级过滤</b>：当gradeNo不为null时，只返回指定年级的学生</li>
     *   <li><b>班级过滤</b>：当classNo不为null时，只返回指定班级的学生</li>
     *   <li><b>关键词搜索</b>：当keyword不为null且非空时，在学号、姓名进行模糊匹配，在手机号、父母手机号、父母姓名进行精确匹配</li>
     *   <li><b>组合查询</b>：多个条件同时存在时，使用AND逻辑连接</li>
     *   <li><b>无条件查询</b>：所有参数为null时，返回用户权限范围内的所有学生</li>
     * </ul>
     * </p>
     * <p>
     * 使用场景举例：
     * <ul>
     *   <li>查找2026届所有学生：gradeNo=2026, classNo=null, keyword=null</li>
     *   <li>查找2026届1班所有学生：gradeNo=2026, classNo=1, keyword=null</li>
     *   <li>在2026届中搜索姓名包含"张"的学生：gradeNo=2026, classNo=null, keyword="张"</li>
     *   <li>在2026届1班中搜索学号包含"01"的学生：gradeNo=2026, classNo=1, keyword="01"</li>
     *   <li>全局搜索自己或父母手机号为"13800138000"的学生：gradeNo=null, classNo=null, keyword="13800138000"</li>
     *   <li>全局搜索父母姓名为"张三"的学生：gradeNo=null, classNo=null, keyword="张三"</li>
     * </ul>
     * </p>
     * <p>
     * 权限控制：
     * 所有查询结果都会受到用户权限的限制，只返回用户有权查看的学生。
     * 即使指定了具体的年级或班级，如果用户没有相应权限，结果也会为空或部分显示。
     * </p>
     *
     * @param gradeNo  年级过滤条件，为null时不进行年级过滤，例如：2026
     * @param classNo  班级过滤条件，为null时不进行班级过滤，例如：1
     * @param keyword  关键词搜索条件，为null或空白时不进行关键词搜索，支持学号、姓名模糊匹配，手机号、父母手机号、父母姓名精确匹配
     * @param user     当前用户，用于应用权限过滤
     * @param pageable 分页参数(最大页大小为1000)
     * @return 符合所有条件且在用户权限范围内的学生基本信息分页结果
     * @throws BadRequestException 当分页大小超过1000时抛出
     * @see #searchStudents(Specification, User, Pageable) 底层通用搜索方法
     */
    public PageResult<StudentCoverResponse> searchStudents(
        @Nullable Integer gradeNo,
        @Nullable Integer classNo,
        @Nullable String keyword,
        @NotNull User user,
        @NotNull Pageable pageable
    ) throws BadRequestException {
        Specification<Student> spec = (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 添加年级筛选
            if (gradeNo != null) {
                predicates.add(cb.equal(root.get("gradeClass").get("gradeNo"), gradeNo));
            }
            // 添加班级筛选
            if (classNo != null) {
                predicates.add(cb.equal(root.get("gradeClass").get("classNo"), classNo));
            }
            // 添加关键词筛选
            if (keyword != null && !keyword.isBlank()) {
                Join<Student, Archive> archiveJoin = root.join("archive", JoinType.LEFT);
                Join<Archive, PersonalPart> personalJoin = archiveJoin.join("personalPart", JoinType.LEFT);
                Join<Archive, FamilyPart> familyJoin = archiveJoin.join("familyPart", JoinType.LEFT);
                Join<FamilyPart, Parent> fatherJoin = familyJoin.join("father", JoinType.LEFT);
                Join<FamilyPart, Parent> motherJoin = familyJoin.join("mother", JoinType.LEFT);

                predicates.add(cb.or(
                    cb.like(root.get("stuNo"), "%" + keyword + "%"), // 学号模糊匹配
                    cb.like(root.get("stuName"), "%" + keyword + "%"), // 姓名模糊匹配
                    cb.equal(motherJoin.get("name"), keyword), // 母亲姓名精确匹配
                    cb.equal(fatherJoin.get("name"), keyword), // 父亲姓名精确匹配
                    cb.equal(personalJoin.get("mobile"), keyword), // 手机号精确匹配
                    cb.equal(motherJoin.get("mobile"), keyword), // 母亲手机号精确匹配
                    cb.equal(fatherJoin.get("mobile"), keyword) // 父亲手机号精确匹配
                ));
            }
            // 如果没有任何筛选条件，返回所有学生；否则AND合并所有条件
            if (predicates.isEmpty()) {
                return cb.conjunction();
            } else {
                return cb.and(predicates.toArray(new Predicate[0]));
            }
        };

        // 调用通用搜索方法
        return searchStudents(spec, user, pageable);
    }


}
