package org.example.gezhiplatform;

import jakarta.transaction.Transactional;
import org.example.gezhiplatform.DTO.student.NewStudentRequest;
import org.example.gezhiplatform.DTO.student.StudentCoverResponse;
import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.enums.Campus;
import org.example.gezhiplatform.exception.BadRequestException;
import org.example.gezhiplatform.exception.CustomInvalidArgException;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.repository.StudentRepository;
import org.example.gezhiplatform.service.student.GradeClassService;
import org.example.gezhiplatform.service.student.StudentManagementService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
public class StudentManagementServiceTest {

    @Autowired
    private StudentManagementService studentManagementService;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private GradeClassService gradeClassService;

    @BeforeEach
    void addStudents() {
        List<NewStudentRequest> requests = List.of(
            // 2017级 1班
            new NewStudentRequest("170101", "Alice", Campus.HUANGPU, 2017, 1),
            new NewStudentRequest("170102", "Bob", Campus.HUANGPU, 2017, 1),
            new NewStudentRequest("170103", "Charlie", Campus.HUANGPU, 2017, 1),
            new NewStudentRequest("170104", "David", Campus.HUANGPU, 2017, 1),
            new NewStudentRequest("170105", "Eve", Campus.HUANGPU, 2017, 1),

            // 2017级 2班
            new NewStudentRequest("170201", "Frank", Campus.HUANGPU, 2017, 2),
            new NewStudentRequest("170202", "Grace", Campus.HUANGPU, 2017, 2),
            new NewStudentRequest("170203", "Hank", Campus.HUANGPU, 2017, 2),
            new NewStudentRequest("170204", "Ivy", Campus.HUANGPU, 2017, 2),
            new NewStudentRequest("170205", "Jack", Campus.HUANGPU, 2017, 2),

            // 2017级 3班
            new NewStudentRequest("170301", "Karen", Campus.HUANGPU, 2017, 3),
            new NewStudentRequest("170302", "Leo", Campus.HUANGPU, 2017, 3),
            new NewStudentRequest("170303", "Mia", Campus.HUANGPU, 2017, 3),
            new NewStudentRequest("170304", "Nick", Campus.HUANGPU, 2017, 3),
            new NewStudentRequest("170305", "Olivia", Campus.HUANGPU, 2017, 3),

            // 2018级 1班
            new NewStudentRequest("180101", "Peter", Campus.HUANGPU, 2018, 1),
            new NewStudentRequest("180102", "Quinn", Campus.HUANGPU, 2018, 1),
            new NewStudentRequest("180103", "Rachel", Campus.HUANGPU, 2018, 1),
            new NewStudentRequest("180104", "Sam", Campus.HUANGPU, 2018, 1),
            new NewStudentRequest("180105", "Tom", Campus.HUANGPU, 2018, 1),

            // 2018级 2班
            new NewStudentRequest("180201", "Uma", Campus.HUANGPU, 2018, 2),
            new NewStudentRequest("180202", "Victor", Campus.HUANGPU, 2018, 2),
            new NewStudentRequest("180203", "Wendy", Campus.HUANGPU, 2018, 2),
            new NewStudentRequest("180204", "Xander", Campus.HUANGPU, 2018, 2),
            new NewStudentRequest("180205", "Yvonne", Campus.HUANGPU, 2018, 2),

            // 2018级 3班
            new NewStudentRequest("180301", "Zack", Campus.HUANGPU, 2018, 3),
            new NewStudentRequest("180302", "Aaron", Campus.HUANGPU, 2018, 3),
            new NewStudentRequest("180303", "Bella", Campus.HUANGPU, 2018, 3),
            new NewStudentRequest("180304", "Chris", Campus.HUANGPU, 2018, 3),
            new NewStudentRequest("180305", "Diana", Campus.HUANGPU, 2018, 3)
                                                  );
        studentRepository.deleteAll();
        studentRepository.flush();
        studentManagementService.addStudents(requests);
    }

    @Test
    void getAllStudents() {
        // 全量查询(不分页)
        var result = studentManagementService.getAllStudents(null, null, PageRequest.of(0, 500));
        result.forEach(System.out::println);
        assertEquals(30, result.content().size());
        // 全量查询(分2页 每页10个)
        System.out.println("Page 0:");
        var resultPage0 = studentManagementService.getAllStudents(null, null, PageRequest.of(0, 10));
        resultPage0.forEach(System.out::println);
        System.out.println("Page 1:");
        var resultPage1 = studentManagementService.getAllStudents(null, null, PageRequest.of(1, 10));
        resultPage1.forEach(System.out::println);
        assertEquals(10, resultPage0.content().size());
        assertEquals(10, resultPage1.content().size());
    }

    @Test
    void getStudentByStuNo() {
        // 通过学号查询
        var result = studentManagementService.getStudentByStuNo("170101");
        System.out.println(result);
        assertEquals("Alice", result.stuName());
    }

    @Test
    void getStudentsByGrade() {
        // 按年级查询(不分页)
        var result = studentManagementService.getAllStudents(2017, null, PageRequest.ofSize(500));
        result.forEach(System.out::println);
        assertEquals(15, result.content().size());
    }

    @Test
    void getStudentsByClasses() {
        // 按班级查询 2017-1 班
        var result = studentManagementService.getAllStudents(2017, 1, PageRequest.of(0, 500));
        result.forEach(System.out::println);
        assertEquals(5, result.content().size());
    }

    @Test
    void getStudentsWrong() {
        // 年级不为空, 班级为空查询报错
        Assertions.assertThrows(
            CustomInvalidArgException.class,
            () -> studentManagementService.getAllStudents(
                null, 1, PageRequest.of(0, 500)
           )
       );
    }

    @Test
    void getAllGradesAndClasses() {
        // 获取所有年级
        var result = gradeClassService.getAllGrades();
        System.out.println(result);
        assertEquals(Set.of(2017, 2018), Set.copyOf(result));
        // 获取2017年级的所有班级(1~3)
        var resultClasses = gradeClassService.getGradeClassesByGrade(2017);
        System.out.println(resultClasses);
        var expectedGradeClasses = List.of(
            new GradeClass(2017, 1),
            new GradeClass(2017, 2),
            new GradeClass(2017, 3)
        );
        assertEquals(expectedGradeClasses, resultClasses);
        // 获取2020年级的所有班级(无)
        var resultClasses2020 = gradeClassService.getGradeClassesByGrade(2020);
        System.out.println(resultClasses2020);
        assertEquals(0, resultClasses2020.size());
        // 获取所有班级
        var resultAllClasses = gradeClassService.getAllGradeClasses();
        System.out.println(resultAllClasses);
        expectedGradeClasses = List.of(
            new GradeClass(2017, 1),
            new GradeClass(2017, 2),
            new GradeClass(2017, 3),
            new GradeClass(2018, 1),
            new GradeClass(2018, 2),
            new GradeClass(2018, 3)
        );
        assertEquals(expectedGradeClasses, resultAllClasses);
    }

    @Test
    void addOneStudent() {
        // 学号重复
        Assertions.assertThrows(
            BadRequestException.class,
            () -> studentManagementService.addStudent(new NewStudentRequest("170101", "Alice", Campus.HUANGPU, 2017, 1))
        );
        assertEquals(30, studentManagementService.getAllStudents(null, null, PageRequest.of(0, 500)).totalElements());
        // 合法添加
        var response = studentManagementService.addStudent(new NewStudentRequest("190101", "Alice", Campus.HUANGPU, 2019, 1));
        var result = studentManagementService.getAllStudents(2019, null, PageRequest.of(0, 500));
        System.out.println(result);
        assertEquals(1, result.content().size());
        assertEquals("Alice", response.stuName());
        assertEquals(31, studentManagementService.getAllStudents(null, null, PageRequest.of(0, 500)).totalElements());
    }

    @Test
    void addBatchStudents() {
        // 部分学号重复
        Assertions.assertThrows(
            BadRequestException.class,
            () -> studentManagementService.addStudents(List.of(
                new NewStudentRequest("180101", "Alice", Campus.HUANGPU, 2018, 1),
                new NewStudentRequest("190102", "Bob", Campus.HUANGPU, 2019, 1),
                new NewStudentRequest("190103", "Charlie", Campus.HUANGPU, 2019, 1)
            ))
        );
        assertEquals(30, studentManagementService.getAllStudents(null, null, PageRequest.of(0, 500)).totalElements());
        // 合法添加
        var response = studentManagementService.addStudents(List.of(
            new NewStudentRequest("190101", "Alice", Campus.HUANGPU, 2019, 1),
            new NewStudentRequest("190102", "Bob", Campus.HUANGPU, 2019, 1),
            new NewStudentRequest("190103", "Charlie", Campus.HUANGPU, 2019, 1)
        ));
        var result = studentManagementService.getAllStudents(2019, null, PageRequest.of(0, 500));
        System.out.println(result);
        assertEquals(3, result.content().size());
        assertEquals(response, result.content());
        assertEquals(33, studentManagementService.getAllStudents(null, null, PageRequest.of(0, 500)).totalElements());
    }

    @Test
    void deleteStudents() {
        // 删除的学生中 190101, 190102 不存在; 170101, 180101 存在
        var result = studentManagementService.deleteStudents(List.of("190101", "190102", "170101", "180101"));
        System.out.println(result);
        assertEquals(2, result.size());
        assertEquals(Set.of("170101", "180101"), result.stream().map(StudentCoverResponse::stuNo).collect(Collectors.toSet()));
        // 数据库中只剩下 28 条数据
        assertEquals(28, studentManagementService.getAllStudents(null, null, PageRequest.of(0, 500)).totalElements());
    }

    @Test
    void updateStudentWrongly() {
        NewStudentRequest request = new NewStudentRequest(
            "170101", "Alice", Campus.HUANGPU, 2017, 1
        );
        // 学号不存在
        Assertions.assertThrows(
            NotFoundException.class,
            () -> studentManagementService.updateStudent("200101", request)
        );
        // 学号重复
        Assertions.assertThrows(
            BadRequestException.class,
            () -> studentManagementService.updateStudent("170102", request)
        );
    }

    @Test
    void updateStudent() {
        NewStudentRequest request = new NewStudentRequest(
            "200101", "Alice", Campus.HUANGPU, 2017, 1
        );
        // 更新学号为 170101 的学生信息
        var result = studentManagementService.updateStudent("170101", request);
        System.out.println(result);
        assertEquals("200101", result.stuNo());
        // 数据库中仍然只有 30 条数据
        assertEquals(30, studentManagementService.getAllStudents(null, null, PageRequest.of(0, 500)).totalElements());
        // 170101 不存在
        assertThrows(
            NotFoundException.class,
            () -> studentManagementService.getStudentByStuNo("170101")
        );
        // 200101 存在
        var result200101 = studentManagementService.getStudentByStuNo("200101");
        assertEquals("Alice", result200101.stuName());
    }

}
