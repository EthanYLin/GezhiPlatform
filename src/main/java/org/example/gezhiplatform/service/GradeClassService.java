package org.example.gezhiplatform.service;

import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.repository.StudentRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


/**
 * GradeClassService类用于提供年级和班级相关的业务功能。
 * 包括:
 * <ul>
 *     <li>获取所有年级</li>
 *     <li>获取所有年级-班级</li>
 *     <li>根据年级获取该年级的所有班级</li>
 * </ul>
 */
@Service
public class GradeClassService {

    private final StudentRepository studentRepository;

    public GradeClassService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * 获取数据库中的所有年级
     * @return 年级列表
     */
    public List<Integer> getAllGrades() {
        return studentRepository.findAllGrades();
    }

    /**
     * 获取数据库中的所有年级-班级
     * @return 年级-班级列表
     */
    public List<GradeClass> getAllGradeClasses() {
        return studentRepository.findAllGradeClasses();
    }

    /**
     * 根据年级获取该年级的所有班级
     * @param gradeNo 年级
     * @return 年级班级列表(GradeClass对象)
     */
    public List<GradeClass> getGradeClassesByGrade(@NotNull Integer gradeNo) {
        return studentRepository.findGradeClassesByGrade(gradeNo);
    }

    /**
     * 根据学生学号列表获取对应的年级班级列表
     * 如传入学号列表 ["260101", "260102", "270201"]
     * 返回年级班级列表 [GradeClass(2026, 1), GradeClass(2027, 2)]
     * @param stuNos 学生学号列表
     * @return 传入学号所在的年级班级列表(GradeClass对象)
     */
    public List<GradeClass> getGradeClassesByStuNos(@NotNull Collection<String> stuNos) {
        return studentRepository.findStudentsByStuNoIn(stuNos).stream()
                                .map(Student::getGradeClass)
                                .flatMap(Optional::stream)
                                .distinct().sorted().toList();
    }

}
