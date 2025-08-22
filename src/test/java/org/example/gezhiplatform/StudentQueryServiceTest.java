package org.example.gezhiplatform;

import jakarta.transaction.Transactional;
import org.example.gezhiplatform.DTO.response.PageResult;
import org.example.gezhiplatform.DTO.response.StudentCoverResponse;
import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.User;
import org.example.gezhiplatform.entity.archive.Archive;
import org.example.gezhiplatform.entity.enums.Campus;
import org.example.gezhiplatform.entity.user_role.*;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.repository.StudentRepository;
import org.example.gezhiplatform.repository.UserRepository;
import org.example.gezhiplatform.seed.ArchiveFaker;
import org.example.gezhiplatform.seed.PersonalInfoFaker;
import org.example.gezhiplatform.seed.StudentFaker;
import org.example.gezhiplatform.service.StudentQueryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
public class StudentQueryServiceTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentQueryService studentQueryService;

    @Autowired
    private UserRepository userRepository;

    private static Map<String, User> makeUsers() {
        return Map.ofEntries(
            Map.entry("ethan", new User("ethan", new SuperAdmin())),
            Map.entry("school-leader", new User("school-leader", new Principal())),
            Map.entry("dean-27", new User("dean-27", new GradeDean(2027))),
            Map.entry("class-2701", new User("class-2701", new ClassAdviser(new GradeClass(2027, 1)))),
            Map.entry("class-2702", new User("class-2702", new ClassAdviser(new GradeClass(2027, 2)))),
            Map.entry("mco-2801,2802", new User("mco-2801,2802",
                                                new MultipleClassObserver(List.of(new GradeClass(2028, 1), new GradeClass(2028, 2))))),
            Map.entry("cu-28xx01", new User("cu-28xx01",
                                            new CollaborativeUser(List.of("280101", "280201", "280301")))),
            Map.entry("multirole", new User("multirole", List.of(
                new GradeDean(2028),
                new ClassAdviser(new GradeClass(2027, 3)),
                new CollaborativeUser(List.of("260101", "260102"))
            ))),
            // 家长用户 - 可以查看多个孩子(260101, 260102, 270201)
            Map.entry("parent-multi", new User("parent-multi", 
                                               new ParentUser(List.of("260101", "260102", "270201")))),
            // 家长用户 - 只能查看一个孩子(280101)
            Map.entry("parent-single", new User("parent-single", 
                                                new ParentUser(List.of("280101")))),
            // 学生用户 - 只能查看自己的信息(260103)
            Map.entry("student-260103", new User("student-260103", 
                                                 new StudentUser("260103"))),
            // 学生用户 - 只能查看自己的信息(270101)
            Map.entry("student-270101", new User("student-270101", 
                                                 new StudentUser("270101")))
        );
    }

    @BeforeEach
    void addUsers() {
        userRepository.deleteAll();
        userRepository.flush();
        userRepository.saveAll(makeUsers().values());
    }

    @BeforeEach
    void addStudents() {
        // 准备学生数据 2026～2028届 每个年级1-3班 每班5人
        studentRepository.deleteAll();
        studentRepository.flush();
        PersonalInfoFaker faker = PersonalInfoFaker.builder().build();
        for (int grade = 2026; grade <= 2028; grade++) {
            for (int classNo = 1; classNo <= 3; classNo++) {
                for (int seatNo = 1; seatNo <= 5; seatNo++) {
                    studentRepository.save(StudentFaker.of(faker, Campus.HUANGPU, grade, classNo, seatNo));
                }
            }
        }
    }

    private Set<GradeClass> allClasses() {
        Set<GradeClass> allClasses = new HashSet<>();
        for (int grade = 2026; grade <= 2028; grade++) {
            for (int classNo = 1; classNo <= 3; classNo++) {
                allClasses.add(new GradeClass(grade, classNo));
            }
        }
        return allClasses;
    }

    private Set<GradeClass> allClassesOfGrade(int gradeNo) {
        Set<GradeClass> allClasses = new HashSet<>();
        for (int classNo = 1; classNo <= 3; classNo++) {
            allClasses.add(new GradeClass(gradeNo, classNo));
        }
        return allClasses;
    }

    private Set<String> stuNosFromClasses(GradeClass gradeClass) {
        return IntStream.range(1, 6)
                        .mapToObj(seatNo -> String.format(
                                      "%s%02d%02d",
                                      String.valueOf(gradeClass.getGradeNo()).substring(2),
                                      gradeClass.getClassNo(),
                                      seatNo
                                  )
                        )
                        .collect(Collectors.toSet());
    }

    @Test
    void testGetAllAccessibleClasses() {
        // 测试校级领导、超级管理员可以访问所有班级
        var testUsers = makeUsers();
        User schoolLeader = testUsers.get("school-leader");
        User superAdmin = testUsers.get("ethan");
        List<GradeClass> leaderClasses = studentQueryService.getAllAccessibleClasses(superAdmin);
        System.out.println("leader:" + leaderClasses);
        assertEquals(allClasses(), new HashSet<>(leaderClasses));
        List<GradeClass> superAdminClasses = studentQueryService.getAllAccessibleClasses(schoolLeader);
        assertEquals(allClasses(), new HashSet<>(superAdminClasses));

        // 测试年级主任只能访问指定年级的班级
        User dean27 = testUsers.get("dean-27");
        List<GradeClass> dean27Classes = studentQueryService.getAllAccessibleClasses(dean27);
        System.out.println("dean-27:" + dean27Classes);
        assertEquals(allClassesOfGrade(2027), new HashSet<>(dean27Classes));

        // 测试班主任只能访问指定班级
        User class2701 = testUsers.get("class-2701");
        List<GradeClass> class2701Classes = studentQueryService.getAllAccessibleClasses(class2701);
        System.out.println("class-2701:" + class2701Classes);
        assertEquals(Set.of(new GradeClass(2027, 1)), new HashSet<>(class2701Classes));

        // 测试多班观察员可以访问多个班级
        User mco2801_2802 = testUsers.get("mco-2801,2802");
        List<GradeClass> mcoClasses = studentQueryService.getAllAccessibleClasses(mco2801_2802);
        System.out.println("mco-2801,2802:" + mcoClasses);
        assertEquals(Set.of(new GradeClass(2028, 1), new GradeClass(2028, 2)), new HashSet<>(mcoClasses));

        // 测试协同用户可以访问指定学号的班级
        User cu28xx01 = testUsers.get("cu-28xx01");
        List<GradeClass> cuClasses = studentQueryService.getAllAccessibleClasses(cu28xx01);
        System.out.println("cu-28xx01:" + cuClasses);
        Set<GradeClass> expectedCuClasses = allClassesOfGrade(2028);
        assertEquals(expectedCuClasses, new HashSet<>(cuClasses));

        // 测试多角色用户可以访问2028届所有班级、2027届3班、2026届1班
        User multiRoleUser = testUsers.get("multirole");
        List<GradeClass> multiRoleClasses = studentQueryService.getAllAccessibleClasses(multiRoleUser);
        System.out.println("multirole:" + multiRoleClasses);
        Set<GradeClass> expected = new HashSet<>(allClassesOfGrade(2028));
        expected.add(new GradeClass(2027, 3));
        expected.add(new GradeClass(2026, 1));
        assertEquals(expected, new HashSet<>(multiRoleClasses));

        // 测试家长用户可以访问其孩子所在的班级(多个孩子)
        User parentMulti = testUsers.get("parent-multi");
        List<GradeClass> parentMultiClasses = studentQueryService.getAllAccessibleClasses(parentMulti);
        System.out.println("parent-multi:" + parentMultiClasses);
        Set<GradeClass> expectedParentMultiClasses = Set.of(
            new GradeClass(2026, 1), // 260101, 260102 (同班级，会去重)
            new GradeClass(2027, 2)  // 270201
        );
        assertEquals(expectedParentMultiClasses, new HashSet<>(parentMultiClasses));

        // 测试家长用户可以访问其孩子所在的班级(单个孩子)
        User parentSingle = testUsers.get("parent-single");
        List<GradeClass> parentSingleClasses = studentQueryService.getAllAccessibleClasses(parentSingle);
        System.out.println("parent-single:" + parentSingleClasses);
        assertEquals(Set.of(new GradeClass(2028, 1)), new HashSet<>(parentSingleClasses));

        // 测试学生用户只能访问自己所在的班级
        User student260103 = testUsers.get("student-260103");
        List<GradeClass> student260103Classes = studentQueryService.getAllAccessibleClasses(student260103);
        System.out.println("student-260103:" + student260103Classes);
        assertEquals(Set.of(new GradeClass(2026, 1)), new HashSet<>(student260103Classes));

        User student270101 = testUsers.get("student-270101");
        List<GradeClass> student270101Classes = studentQueryService.getAllAccessibleClasses(student270101);
        System.out.println("student-270101:" + student270101Classes);
        assertEquals(Set.of(new GradeClass(2027, 1)), new HashSet<>(student270101Classes));
    }

    private Set<String> getStuNosFromDTO(PageResult<StudentCoverResponse> response) {
        return response.content().stream()
                       .map(StudentCoverResponse::stuNo)
                       .collect(Collectors.toSet());
    }

    @Test
    void testSearchStudentsByGradeOnly() {
        Pageable pageable = Pageable.ofSize(20);
        var testUsers = makeUsers();

        // 超级管理员按年级搜索 - 可以找到指定年级的所有学生
        User superAdmin = testUsers.get("ethan");
        Set<String> actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2026, null, null, superAdmin, pageable)
        );
        System.out.println("超级管理员搜索2026届所有学生: " + actualStuNos);
        Set<String> expected2026 = new HashSet<>();
        for (int classNo = 1; classNo <= 3; classNo++) {
            expected2026.addAll(stuNosFromClasses(new GradeClass(2026, classNo)));
        }
        assertEquals(expected2026, actualStuNos);

        // 年级组长按自己年级搜索 - 可以找到
        User dean27 = testUsers.get("dean-27");
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2027, null, null, dean27, pageable)
        );
        System.out.println("27届年级组长搜索2027届所有学生: " + actualStuNos);
        Set<String> expected2027 = new HashSet<>();
        for (int classNo = 1; classNo <= 3; classNo++) {
            expected2027.addAll(stuNosFromClasses(new GradeClass(2027, classNo)));
        }
        assertEquals(expected2027, actualStuNos);

        // 年级组长按其他年级搜索 - 找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2026, null, null, dean27, pageable)
        );
        System.out.println("27届年级组长搜索2026届学生: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos);

        // 班主任按自己年级搜索 - 只能找到自己班级的学生
        User class2701 = testUsers.get("class-2701");
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2027, null, null, class2701, pageable)
        );
        System.out.println("2701班主任搜索2027届学生: " + actualStuNos);
        assertEquals(stuNosFromClasses(new GradeClass(2027, 1)), actualStuNos);
    }

    @Test
    void testSearchStudentsByGradeAndClass() {
        Pageable pageable = Pageable.ofSize(20);
        var testUsers = makeUsers();

        // 超级管理员按年级班级搜索
        User superAdmin = testUsers.get("ethan");
        Set<String> actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2026, 1, null, superAdmin, pageable)
        );
        System.out.println("超级管理员搜索2026届1班学生: " + actualStuNos);
        assertEquals(stuNosFromClasses(new GradeClass(2026, 1)), actualStuNos);

        // 班主任按自己班级搜索 - 可以找到
        User class2701 = testUsers.get("class-2701");
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2027, 1, null, class2701, pageable)
        );
        System.out.println("2701班主任搜索2027届1班学生: " + actualStuNos);
        assertEquals(stuNosFromClasses(new GradeClass(2027, 1)), actualStuNos);

        // 班主任按其他班级搜索 - 找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2027, 2, null, class2701, pageable)
        );
        System.out.println("2701班主任搜索2027届2班学生: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos);

        // 年级组长按本年级任意班级搜索 - 可以找到
        User dean27 = testUsers.get("dean-27");
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2027, 2, null, dean27, pageable)
        );
        System.out.println("27届年级组长搜索2027届2班学生: " + actualStuNos);
        assertEquals(stuNosFromClasses(new GradeClass(2027, 2)), actualStuNos);

        // 协同用户按特定班级搜索 - 只能找到有权限的学生
        User cu28xx01 = testUsers.get("cu-28xx01");
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2028, 1, null, cu28xx01, pageable)
        );
        System.out.println("cu-28xx01搜索2028届1班学生: " + actualStuNos);
        assertEquals(Set.of("280101"), actualStuNos);

        // 多角色用户按班级搜索
        User multiRoleUser = testUsers.get("multirole");
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2026, 1, null, multiRoleUser, pageable)
        );
        System.out.println("multirole搜索2026届1班学生: " + actualStuNos);
        assertEquals(Set.of("260101", "260102"), actualStuNos);
    }

    @Test
    void testSearchStudentsByKeywordOnly() {
        PersonalInfoFaker faker = PersonalInfoFaker.builder().build();
        Pageable pageable = Pageable.ofSize(20);
        var testUsers = makeUsers();

        // 准备测试数据：设置特定学生的手机号和姓名
        // 260101的学生拥有指定手机号
        Student student260101 = studentRepository.findByStuNo("260101")
                                                 .orElseThrow(() -> new NotFoundException(
                                                     "Student with stuNo 260101 not found"));
        Archive archive = ArchiveFaker.of(faker);
        Assertions.assertNotNull(archive.getPersonalPart());
        archive.getPersonalPart().setMobile("13800134617");
        student260101.setArchive(archive);
        studentRepository.save(student260101);

        // 260102的学生拥有指定姓名
        Student student260102 = studentRepository.findByStuNo("260102")
                                                 .orElseThrow(() -> new NotFoundException(
                                                     "Student with stuNo 260102 not found"));
        student260102.setStuName("格小智");
        studentRepository.save(student260102);

        // 280201的学生设置另一个手机号
        Student student280201 = studentRepository.findByStuNo("280201")
                                                 .orElseThrow(() -> new NotFoundException(
                                                     "Student with stuNo 280201 not found"));
        Archive archive280201 = ArchiveFaker.of(faker);
        Assertions.assertNotNull(archive280201.getPersonalPart());
        archive280201.getPersonalPart().setMobile("13900123456");
        student280201.setArchive(archive280201);
        studentRepository.save(student280201);

        // 260103的学生设置父母手机号和姓名
        Student student260103 = studentRepository.findByStuNo("260103")
                                                 .orElseThrow(() -> new NotFoundException(
                                                     "Student with stuNo 260103 not found"));
        Archive archive260103 = ArchiveFaker.of(faker);
        Assertions.assertNotNull(archive260103.getFamilyPart());
        Assertions.assertNotNull(archive260103.getFamilyPart().getFather());
        archive260103.getFamilyPart().getFather().setMobile("13700137000");
        archive260103.getFamilyPart().getFather().setName("张爸爸");
        Assertions.assertNotNull(archive260103.getFamilyPart().getMother());
        archive260103.getFamilyPart().getMother().setMobile("13800138888");
        archive260103.getFamilyPart().getMother().setName("李妈妈");
        student260103.setArchive(archive260103);
        studentRepository.save(student260103);

        // 260104的学生设置不同的父母姓名用于测试
        Student student260104 = studentRepository.findByStuNo("260104")
                                                 .orElseThrow(() -> new NotFoundException(
                                                     "Student with stuNo 260104 not found"));
        Archive archive260104 = ArchiveFaker.of(faker);
        Assertions.assertNotNull(archive260104.getFamilyPart());
        Assertions.assertNotNull(archive260104.getFamilyPart().getFather());
        archive260104.getFamilyPart().getFather().setName("王爸爸");
        Assertions.assertNotNull(archive260104.getFamilyPart().getMother());
        archive260104.getFamilyPart().getMother().setName("赵妈妈");
        student260104.setArchive(archive260104);
        studentRepository.save(student260104);

        // ========== 测试超级管理员的关键词搜索 ==========
        User superAdmin = testUsers.get("ethan");
        
        // 1. 按手机号精确搜索
        Set<String> actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "13800134617", superAdmin, pageable)
        );
        System.out.println("超级管理员搜索手机号为'13800134617'的学生: " + actualStuNos);
        assertEquals(Set.of("260101"), actualStuNos);

        // 2. 按姓名搜索
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "格小智", superAdmin, pageable)
        );
        System.out.println("超级管理员搜索姓名包含'格小智'的学生: " + actualStuNos);
        assertEquals(Set.of("260102"), actualStuNos);

        // 3. 按学号搜索 - 部分匹配
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "2601", superAdmin, pageable)
        );
        System.out.println("超级管理员搜索学号包含'2601'的学生: " + actualStuNos);
        assertEquals(stuNosFromClasses(new GradeClass(2026, 1)), actualStuNos);

        // 4. 按学号搜索 - 完整匹配
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "260101", superAdmin, pageable)
        );
        System.out.println("超级管理员搜索学号'260101'的学生: " + actualStuNos);
        assertEquals(Set.of("260101"), actualStuNos);

        // 5. 按父亲手机号精确搜索
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "13700137000", superAdmin, pageable)
        );
        System.out.println("超级管理员搜索父亲手机号为'13700137000'的学生: " + actualStuNos);
        assertEquals(Set.of("260103"), actualStuNos);

        // 6. 按母亲手机号精确搜索
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "13800138888", superAdmin, pageable)
        );
        System.out.println("超级管理员搜索母亲手机号为'13800138888'的学生: " + actualStuNos);
        assertEquals(Set.of("260103"), actualStuNos);

        // 7. 按父亲姓名精确搜索
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "张爸爸", superAdmin, pageable)
        );
        System.out.println("超级管理员搜索父亲姓名为'张爸爸'的学生: " + actualStuNos);
        assertEquals(Set.of("260103"), actualStuNos);

        // 8. 按母亲姓名精确搜索
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "李妈妈", superAdmin, pageable)
        );
        System.out.println("超级管理员搜索母亲姓名为'李妈妈'的学生: " + actualStuNos);
        assertEquals(Set.of("260103"), actualStuNos);

        // 9. 按另一个父亲姓名精确搜索
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "王爸爸", superAdmin, pageable)
        );
        System.out.println("超级管理员搜索父亲姓名为'王爸爸'的学生: " + actualStuNos);
        assertEquals(Set.of("260104"), actualStuNos);

        // 10. 按另一个母亲姓名精确搜索
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "赵妈妈", superAdmin, pageable)
        );
        System.out.println("超级管理员搜索母亲姓名为'赵妈妈'的学生: " + actualStuNos);
        assertEquals(Set.of("260104"), actualStuNos);

        // 11. 按不存在的父母姓名精确搜索 - 找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "不存在的姓名", superAdmin, pageable)
        );
        System.out.println("超级管理员搜索不存在姓名'不存在的姓名'的学生: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos);

        // 12. 按不存在的手机号精确搜索 - 找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "13999999999", superAdmin, pageable)
        );
        System.out.println("超级管理员搜索不存在手机号'13999999999'的学生: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos);

        // ========== 测试不同角色的关键词搜索权限 ==========
        
        // 班主任搜索自己班级的学生
        User class2701 = testUsers.get("class-2701");
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "2701", class2701, pageable)
        );
        System.out.println("2701班主任搜索学号包含'2701'的学生: " + actualStuNos);
        assertEquals(stuNosFromClasses(new GradeClass(2027, 1)), actualStuNos);

        // 班主任搜索其他班级的学生 - 找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "2602", class2701, pageable)
        );
        System.out.println("2701班主任搜索学号包含'2602'的学生: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos);

        // 协同用户搜索有权限的学生
        User cu28xx01 = testUsers.get("cu-28xx01");
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "280101", cu28xx01, pageable)
        );
        System.out.println("cu-28xx01搜索学号'280101'的学生: " + actualStuNos);
        assertEquals(Set.of("280101"), actualStuNos);

        // 协同用户搜索有权限学生的手机号
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "13900123456", cu28xx01, pageable)
        );
        System.out.println("cu-28xx01搜索手机号为'13900123456'的学生: " + actualStuNos);
        assertEquals(Set.of("280201"), actualStuNos);

        // 多角色用户搜索
        User multiRoleUser = testUsers.get("multirole");
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "格小智", multiRoleUser, pageable)
        );
        System.out.println("multirole搜索姓名'格小智'的学生: " + actualStuNos);
        assertEquals(Set.of("260102"), actualStuNos);

        // ========== 测试父母手机号权限控制 ==========
        
        // 班主任搜索其他班级学生的父母手机号 - 找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "13700137000", class2701, pageable)
        );
        System.out.println("2701班主任搜索父亲手机号'13700137000'的学生: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos); // 260103在2601班，2701班主任无权限

        // 多角色用户通过父母手机号搜索有权限的学生
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "13800138888", multiRoleUser, pageable)
        );
        System.out.println("multirole搜索母亲手机号'13800138888'的学生: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos); // multirole对260103无权限

        // ========== 测试父母姓名权限控制 ==========
        
        // 班主任搜索其他班级学生的父亲姓名 - 找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "张爸爸", class2701, pageable)
        );
        System.out.println("2701班主任搜索父亲姓名'张爸爸'的学生: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos); // 260103在2601班，2701班主任无权限

        // 班主任搜索其他班级学生的母亲姓名 - 找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "李妈妈", class2701, pageable)
        );
        System.out.println("2701班主任搜索母亲姓名'李妈妈'的学生: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos); // 260103在2601班，2701班主任无权限

        // 多角色用户通过父亲姓名搜索有权限的学生
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "张爸爸", multiRoleUser, pageable)
        );
        System.out.println("multirole搜索父亲姓名'张爸爸'的学生: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos); // multirole对260103无权限

        // 多角色用户通过母亲姓名搜索有权限的学生
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "李妈妈", multiRoleUser, pageable)
        );
        System.out.println("multirole搜索母亲姓名'李妈妈'的学生: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos); // multirole对260103无权限

        // 协同用户搜索无权限学生的父母姓名 - 找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "王爸爸", cu28xx01, pageable)
        );
        System.out.println("cu-28xx01搜索父亲姓名'王爸爸'的学生: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos); // 260104在2601班，cu28xx01无权限

        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "赵妈妈", cu28xx01, pageable)
        );
        System.out.println("cu-28xx01搜索母亲姓名'赵妈妈'的学生: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos); // 260104在2601班，cu28xx01无权限
    }

    @Test
    void testSearchStudentsCombinedConditions() {
        Pageable pageable = Pageable.ofSize(20);
        var testUsers = makeUsers();

        // 准备测试数据
        Student student260101 = studentRepository.findByStuNo("260101")
                                                 .orElseThrow(() -> new NotFoundException(
                                                     "Student with stuNo 260101 not found"));
        student260101.setStuName("张三");
        studentRepository.save(student260101);

        Student student260102 = studentRepository.findByStuNo("260102")
                                                 .orElseThrow(() -> new NotFoundException(
                                                     "Student with stuNo 260102 not found"));
        student260102.setStuName("李四");
        studentRepository.save(student260102);

        User superAdmin = testUsers.get("ethan");

        // ========== 测试组合条件：年级 + 关键词 ==========
        
        // 在2026届中搜索姓名包含"张"的学生
        Set<String> actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2026, null, "张", superAdmin, pageable)
        );
        System.out.println("在2026届中搜索姓名包含'张'的学生: " + actualStuNos);
        assertEquals(Set.of("260101"), actualStuNos);

        // 在2026届中搜索学号包含"01"的学生
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2026, null, "01", superAdmin, pageable)
        );
        System.out.println("在2026届中搜索学号包含'01'的学生: " + actualStuNos);
        HashSet<String> expectedStuNos = new HashSet<>(stuNosFromClasses(new GradeClass(2026, 1)));
        expectedStuNos.addAll(Set.of("260201", "260301"));
        assertEquals(expectedStuNos, actualStuNos);

        // 在2027届中搜索姓名包含"张"的学生 - 应该找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2027, null, "张", superAdmin, pageable)
        );
        System.out.println("在2027届中搜索姓名包含'张'的学生: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos);

        // ========== 测试组合条件：年级 + 班级 + 关键词 ==========
        
        // 在2026届1班中搜索姓名包含"李"的学生
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2026, 1, "李", superAdmin, pageable)
        );
        System.out.println("在2026届1班中搜索姓名包含'李'的学生: " + actualStuNos);
        assertEquals(Set.of("260102"), actualStuNos);

        // 在2026届1班中搜索学号包含"02"的学生
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2026, 1, "02", superAdmin, pageable)
        );
        System.out.println("在2026届1班中搜索学号包含'02'的学生: " + actualStuNos);
        assertEquals(Set.of("260102"), actualStuNos);

        // 在2026届2班中搜索姓名包含"李"的学生 - 应该找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2026, 2, "李", superAdmin, pageable)
        );
        System.out.println("在2026届2班中搜索姓名包含'李'的学生: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos);

        // ========== 测试权限控制下的组合条件 ==========
        
        // 班主任在自己班级中搜索
        User class2701 = testUsers.get("class-2701");
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2027, 1, "270101", class2701, pageable)
        );
        System.out.println("2701班主任在自己班级中搜索'270101': " + actualStuNos);
        assertEquals(Set.of("270101"), actualStuNos);

        // 班主任在其他班级中搜索 - 找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2027, 2, "270201", class2701, pageable)
        );
        System.out.println("2701班主任在2027届2班中搜索'270201': " + actualStuNos);
        assertEquals(Set.of(), actualStuNos);
    }

    @Test
    void testSearchStudentsEdgeCases() {
        Pageable pageable = Pageable.ofSize(20);
        var testUsers = makeUsers();
        User superAdmin = testUsers.get("ethan");

        // ========== 测试边界条件 ==========

        // 1. 无条件搜索 - 返回用户权限范围内的所有学生
        PageResult<StudentCoverResponse> allResult = studentQueryService.searchStudents(null, null, null, superAdmin, pageable);
        System.out.println("超级管理员无条件搜索结果数量: " + allResult.totalElements());
        assertEquals(45, allResult.totalElements()); // 3年级 * 3班级 * 5学生 = 45

        // 2. 空关键词搜索 - 等同于无关键词
        PageResult<StudentCoverResponse> emptyKeywordResult = studentQueryService.searchStudents(null, null, "", superAdmin, pageable);
        assertEquals(45, emptyKeywordResult.totalElements());
        System.out.println("空关键词搜索结果数量: " + emptyKeywordResult.totalElements());

        // 3. 空白关键词搜索 - 等同于无关键词
        PageResult<StudentCoverResponse> blankKeywordResult = studentQueryService.searchStudents(null, null, "   ", superAdmin, pageable);
        assertEquals(45, blankKeywordResult.totalElements());
        System.out.println("空白关键词搜索结果数量: " + blankKeywordResult.totalElements());

        // 4. 不存在的年级
        Set<String> actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2030, null, null, superAdmin, pageable)
        );
        assertEquals(Set.of(), actualStuNos);
        System.out.println("搜索不存在年级2030的结果: " + actualStuNos);

        // 5. 不存在的班级
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2026, 10, null, superAdmin, pageable)
        );
        assertEquals(Set.of(), actualStuNos);
        System.out.println("搜索不存在班级2026届10班的结果: " + actualStuNos);

        // 6. 不存在的关键词
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "不存在的关键词", superAdmin, pageable)
        );
        assertEquals(Set.of(), actualStuNos);
        System.out.println("搜索不存在关键词的结果: " + actualStuNos);

        // ========== 测试不同用户的无条件搜索 ==========

        // 班主任无条件搜索 - 只返回自己班级的学生
        User class2701 = testUsers.get("class-2701");
        PageResult<StudentCoverResponse> classResult = studentQueryService.searchStudents(null, null, null, class2701, pageable);
        assertEquals(5, classResult.totalElements()); // 只有1个班级的5个学生
        System.out.println("2701班主任无条件搜索结果数量: " + classResult.totalElements());

        // 年级组长无条件搜索 - 只返回自己年级的学生
        User dean27 = testUsers.get("dean-27");
        PageResult<StudentCoverResponse> gradeResult = studentQueryService.searchStudents(null, null, null, dean27, pageable);
        assertEquals(15, gradeResult.totalElements()); // 3个班级 * 5个学生 = 15
        System.out.println("27届年级组长无条件搜索结果数量: " + gradeResult.totalElements());

        // 协同用户无条件搜索 - 只返回有权限的学生
        User cu28xx01 = testUsers.get("cu-28xx01");
        PageResult<StudentCoverResponse> cuResult = studentQueryService.searchStudents(null, null, null, cu28xx01, pageable);
        assertEquals(3, cuResult.totalElements()); // 只有3个指定学号的学生
        System.out.println("cu-28xx01无条件搜索结果数量: " + cuResult.totalElements());
    }

    @Test
    void testParentUserPermissions() {
        Pageable pageable = Pageable.ofSize(20);
        var testUsers = makeUsers();

        // ========== 测试多孩子家长用户的权限 ==========
        User parentMulti = testUsers.get("parent-multi");
        
        // 1. 无条件搜索 - 只能看到自己的孩子们
        Set<String> actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, null, parentMulti, pageable)
        );
        System.out.println("多孩子家长无条件搜索结果: " + actualStuNos);
        assertEquals(Set.of("260101", "260102", "270201"), actualStuNos);

        // 2. 按年级搜索 - 只能看到该年级自己的孩子
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2026, null, null, parentMulti, pageable)
        );
        System.out.println("多孩子家长搜索2026届结果: " + actualStuNos);
        assertEquals(Set.of("260101", "260102"), actualStuNos);

        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2027, null, null, parentMulti, pageable)
        );
        System.out.println("多孩子家长搜索2027届结果: " + actualStuNos);
        assertEquals(Set.of("270201"), actualStuNos);

        // 3. 按年级班级搜索 - 只能看到该班级自己的孩子
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2026, 1, null, parentMulti, pageable)
        );
        System.out.println("多孩子家长搜索2026届1班结果: " + actualStuNos);
        assertEquals(Set.of("260101", "260102"), actualStuNos);

        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2027, 2, null, parentMulti, pageable)
        );
        System.out.println("多孩子家长搜索2027届2班结果: " + actualStuNos);
        assertEquals(Set.of("270201"), actualStuNos);

        // 4. 搜索无权限班级 - 找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2028, null, null, parentMulti, pageable)
        );
        System.out.println("多孩子家长搜索无权限年级2028届结果: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos);

        // 5. 按关键词搜索自己的孩子学号
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "260101", parentMulti, pageable)
        );
        System.out.println("多孩子家长搜索孩子学号260101结果: " + actualStuNos);
        assertEquals(Set.of("260101"), actualStuNos);

        // 6. 按关键词搜索其他学生学号 - 找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "260103", parentMulti, pageable)
        );
        System.out.println("多孩子家长搜索其他学生学号260103结果: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos);

        // ========== 测试单孩子家长用户的权限 ==========
        User parentSingle = testUsers.get("parent-single");
        
        // 1. 无条件搜索 - 只能看到自己的唯一孩子
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, null, parentSingle, pageable)
        );
        System.out.println("单孩子家长无条件搜索结果: " + actualStuNos);
        assertEquals(Set.of("280101"), actualStuNos);

        // 2. 按年级搜索 - 正确年级能找到，错误年级找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2028, null, null, parentSingle, pageable)
        );
        System.out.println("单孩子家长搜索正确年级2028届结果: " + actualStuNos);
        assertEquals(Set.of("280101"), actualStuNos);

        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2026, null, null, parentSingle, pageable)
        );
        System.out.println("单孩子家长搜索错误年级2026届结果: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos);

        // 3. 按年级班级搜索 - 正确班级能找到，错误班级找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2028, 1, null, parentSingle, pageable)
        );
        System.out.println("单孩子家长搜索正确班级2028届1班结果: " + actualStuNos);
        assertEquals(Set.of("280101"), actualStuNos);

        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2028, 2, null, parentSingle, pageable)
        );
        System.out.println("单孩子家长搜索错误班级2028届2班结果: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos);

        // 4. 按关键词搜索自己的孩子
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "280101", parentSingle, pageable)
        );
        System.out.println("单孩子家长搜索孩子学号280101结果: " + actualStuNos);
        assertEquals(Set.of("280101"), actualStuNos);

        // 5. 组合条件搜索
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2028, 1, "280101", parentSingle, pageable)
        );
        System.out.println("单孩子家长组合条件搜索结果: " + actualStuNos);
        assertEquals(Set.of("280101"), actualStuNos);
    }

    @Test
    void testStudentUserPermissions() {
        Pageable pageable = Pageable.ofSize(20);
        var testUsers = makeUsers();

        // ========== 测试学生用户260103的权限 ==========
        User student260103 = testUsers.get("student-260103");
        
        // 1. 无条件搜索 - 只能看到自己
        Set<String> actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, null, student260103, pageable)
        );
        System.out.println("学生260103无条件搜索结果: " + actualStuNos);
        assertEquals(Set.of("260103"), actualStuNos);

        // 2. 按年级搜索 - 正确年级能找到自己，错误年级找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2026, null, null, student260103, pageable)
        );
        System.out.println("学生260103搜索正确年级2026届结果: " + actualStuNos);
        assertEquals(Set.of("260103"), actualStuNos);

        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2027, null, null, student260103, pageable)
        );
        System.out.println("学生260103搜索错误年级2027届结果: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos);

        // 3. 按年级班级搜索 - 正确班级能找到自己，错误班级找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2026, 1, null, student260103, pageable)
        );
        System.out.println("学生260103搜索正确班级2026届1班结果: " + actualStuNos);
        assertEquals(Set.of("260103"), actualStuNos);

        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2026, 2, null, student260103, pageable)
        );
        System.out.println("学生260103搜索错误班级2026届2班结果: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos);

        // 4. 按关键词搜索自己的学号
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "260103", student260103, pageable)
        );
        System.out.println("学生260103搜索自己学号结果: " + actualStuNos);
        assertEquals(Set.of("260103"), actualStuNos);

        // 5. 按关键词搜索其他学生学号 - 找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "260101", student260103, pageable)
        );
        System.out.println("学生260103搜索其他学生学号260101结果: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos);

        // 6. 按关键词搜索同班其他学生 - 找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, "260104", student260103, pageable)
        );
        System.out.println("学生260103搜索同班其他学生260104结果: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos);

        // ========== 测试学生用户270101的权限 ==========
        User student270101 = testUsers.get("student-270101");
        
        // 1. 无条件搜索 - 只能看到自己
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(null, null, null, student270101, pageable)
        );
        System.out.println("学生270101无条件搜索结果: " + actualStuNos);
        assertEquals(Set.of("270101"), actualStuNos);

        // 2. 按年级搜索 - 正确年级能找到自己
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2027, null, null, student270101, pageable)
        );
        System.out.println("学生270101搜索正确年级2027届结果: " + actualStuNos);
        assertEquals(Set.of("270101"), actualStuNos);

        // 3. 按年级班级搜索 - 正确班级能找到自己
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2027, 1, null, student270101, pageable)
        );
        System.out.println("学生270101搜索正确班级2027届1班结果: " + actualStuNos);
        assertEquals(Set.of("270101"), actualStuNos);

        // 4. 组合条件搜索 - 所有条件都正确时能找到自己
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2027, 1, "270101", student270101, pageable)
        );
        System.out.println("学生270101组合条件搜索结果: " + actualStuNos);
        assertEquals(Set.of("270101"), actualStuNos);

        // 5. 组合条件搜索 - 任何条件不匹配都找不到
        actualStuNos = getStuNosFromDTO(
            studentQueryService.searchStudents(2027, 1, "270102", student270101, pageable)
        );
        System.out.println("学生270101错误关键词组合搜索结果: " + actualStuNos);
        assertEquals(Set.of(), actualStuNos);
    }


}
