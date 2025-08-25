package org.example.gezhiplatform.service;

import org.example.gezhiplatform.DTO.PageResult;
import org.example.gezhiplatform.DTO.student_query.NewStudentRequest;
import org.example.gezhiplatform.DTO.student_query.StudentCoverResponse;
import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.CustomInvalidArgException;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.repository.StudentRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.example.gezhiplatform.utils.ReflectionUtils.getIllegalSortProperties;

/**
 * 学生Service类
 * 只用于对基本信息(学号、姓名、校区、年级班级)进行增删改查
 * 该服务类【只面向管理员权限】
 * 用户查询学生信息请使用 {@link StudentQueryService}
 */
@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * 按全量、年级、年级班级获取学生列表
     * - 不提供gradeNo和classNo时, 返回全量学生列表
     * - 只提供gradeNo时, 返回该年级的学生列表
     * - 提供gradeNo和classNo时, 返回该年级班级的学生列表
     * @param gradeNo 年级(可选)
     * @param classNo 班级(可选)
     * @param pageable 分页信息
     * @return 分页列表(学生基本信息DTO)
     * @throws CustomInvalidArgException 当classNo不为空时, gradeNo为空
     */
    public PageResult<StudentCoverResponse> getAllStudents (
        @Nullable Integer gradeNo,
        @Nullable Integer classNo,
        @NotNull Pageable pageable
    ) throws CustomInvalidArgException {
        if (gradeNo == null && classNo != null) {
            throw new CustomInvalidArgException("在班级不为空时, 年级不能为空。");
        }
        Set<String> illegalSortProperties = getIllegalSortProperties(Student.class, pageable);
        if (!illegalSortProperties.isEmpty()) {
            throw new BadRequestException("分页排序参数中包含无效的字段: " + String.join(", ", illegalSortProperties));
        }
        Page<StudentCoverResponse> result;
        if (gradeNo == null) {
            // 全量
            result = studentRepository.findAll(pageable).map(StudentCoverResponse::new);
        } else if (classNo == null) {
            // 按年级
            result = studentRepository.findByGradeClass_GradeNo(gradeNo, pageable).map(StudentCoverResponse::new);
        } else {
            // 按年级班级
            result = studentRepository.findByGradeClass(new GradeClass(gradeNo, classNo), pageable).map(StudentCoverResponse::new);
        }
        return new PageResult<>(result);
    }

    /**
     * 根据学号, 获取该学号的学生信息
     * @param stuNo 学号
     * @return 学生基本信息DTO
     * @throws NotFoundException 当学号不存在时
     */
    public StudentCoverResponse getStudentByStuNo(@NotNull String stuNo) throws NotFoundException {
        Student student = studentRepository.findByStuNo(stuNo).orElseThrow(
            () -> new NotFoundException("学号为: " + stuNo + " 的学生不存在")
        );
        return new StudentCoverResponse(student);
    }

    /**
     * 获取数据库中的所有年级
     * @return 年级列表
     */
    public List<Integer> getAllGrades() {
        return studentRepository.findAllGrades();
    }

    /**
     * 获取指定年级的所有班级
     * @param gradeNo 年级
     * @return 班级列表(整数形式, 如: 1, 2, 3)
     */
    public List<Integer> getClassesByGrade(@NotNull Integer gradeNo) {
        return studentRepository.findClassesByGrade(gradeNo);
    }

    /**
     * 根据年级获取该年级的所有班级
     * @param gradeNo 年级
     * @return 年级班级列表(GradeClass对象)
     */
    public List<GradeClass> getGradeClassesByGrade(@NotNull Integer gradeNo) {
        return this.getClassesByGrade(gradeNo).stream().map(classNo -> new GradeClass(gradeNo, classNo)).toList();
    }

    /**
     * 根据学生学号列表获取对应的年级班级列表
     * 如传入学号列表 ["260101", "260102", "270201"]
     * 返回年级班级列表 [GradeClass(2026, 1), GradeClass(2027, 2)]
     * @param stuNos 学生学号列表
     * @return 传入学号所在的年级班级列表(GradeClass对象)
     */
    public List<GradeClass> getGradeClassesByStuNos(@NotNull List<String> stuNos) {
        return studentRepository.findStudentsByStuNoIn(stuNos).stream()
                                .map(Student::getGradeClass)
                                .flatMap(Optional::stream)
                                .distinct().sorted().toList();
    }

    /**
     * 向数据库添加多个学生
     * @param requests 新增学生请求列表
     * @return 新增的学生基本信息DTO列表
     * @throws BadRequestException 当学号重复时
     */
    public List<StudentCoverResponse> addStudents(@NotNull List<NewStudentRequest> requests) throws BadRequestException {
        List<String> duplicateStuNos = studentRepository.findByStuNoIn(
            requests.stream().map(NewStudentRequest::stuNo).toList()
        ).stream().map(StudentRepository.StuNoOnly::getStuNo).toList();

        if (!duplicateStuNos.isEmpty()) {
            throw new BadRequestException("由于学号为: " + duplicateStuNos + " 的学生已存在，本次新增操作全部取消。");
        }
        List<Student> students = requests.stream().map(NewStudentRequest::toStudent).toList();
        List<Student> results = studentRepository.saveAll(students);
        return results.stream().map(StudentCoverResponse::new).toList();
    }

    /**
     * 向数据库添加一个学生
     * @param request 新增学生请求
     * @return 新增的学生基本信息DTO
     * @throws BadRequestException 当学号重复时
     */
    public StudentCoverResponse addStudent(@NotNull NewStudentRequest request) throws BadRequestException {
        if (studentRepository.existsByStuNo(request.stuNo())) {
            throw new BadRequestException("学号为: " + request.stuNo() + " 的学生已存在");
        }
        Student result = studentRepository.save(request.toStudent());
        return new StudentCoverResponse(result);
    }

    /**
     * 删除多个学生
     * @param studentNos 学生学号列表
     * @return 删除的学生基本信息DTO列表
     */
    public List<StudentCoverResponse> deleteStudents(@NotNull List<String> studentNos) {
        List<Student> students = studentRepository.findStudentsByStuNoIn(studentNos);
        studentRepository.deleteAll(students);
        return students.stream().map(StudentCoverResponse::new).toList();
    }

    /**
     * 根据学生学号, 更新该学号的学生信息
     * @param stuNo 要修改的原学生学号
     * @param request 新学生信息请求(可以修改学号)
     * @return 更新后的学生基本信息DTO
     * @throws NotFoundException 当原学号不存在时
     * @throws BadRequestException 当新学号与已有学生重复时
     */
    public StudentCoverResponse updateStudent(
        @NotNull String stuNo,
        @NotNull NewStudentRequest request
    ) throws BadRequestException {
        Student oldStudent = studentRepository.findByStuNo(stuNo).orElseThrow(
            () -> new NotFoundException("学号为: " + stuNo + " 的学生不存在")
        );
        if (studentRepository.existsByStuNo(request.stuNo()) && !request.stuNo().equals(stuNo)) {
            throw new BadRequestException("不能将学生的学号从 " + stuNo + " 改为: " + request.stuNo() + " , 因为该学号的学生已存在。");
        }
        Student newStudent = request.toStudent();
        newStudent.setId(oldStudent.getId());
        Student result = studentRepository.save(newStudent);
        return new StudentCoverResponse(result);
    }


}
