package org.example.gezhiplatform.seed;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.User;
import org.example.gezhiplatform.entity.archive.Archive;
import org.example.gezhiplatform.entity.enums.Campus;
import org.example.gezhiplatform.entity.user_role.*;
import org.example.gezhiplatform.repository.StudentRepository;
import org.example.gezhiplatform.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.IntStream;

@Slf4j
@Component
@Profile("seed")
@RequiredArgsConstructor
public class SeedDataRunner implements ApplicationRunner {

    private final ApplicationContext ctx;
    private final Scanner scanner = new Scanner(System.in);
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    // ================ 模拟学生及档案数据 ================
    // 2026级-2028级, 黄浦校区1-3班, 奉贤校区9班, 每班01~05号学生
    private record SeedStudents(int gradeNo, Campus campus, int classNo, int seatNoFrom, int seatNoTo) {}

    private final List<SeedStudents> seedStudents = new ArrayList<>();

    @PostConstruct
    private void setSeedStudents() {
        for (int grade = 2026; grade <= 2028; grade++) {
            seedStudents.addAll(List.of(
                new SeedStudents(grade, Campus.HUANGPU, 1, 1, 5),
                new SeedStudents(grade, Campus.HUANGPU, 2, 1, 5),
                new SeedStudents(grade, Campus.HUANGPU, 3, 1, 5),
                new SeedStudents(grade, Campus.FENGXIAN, 9, 1, 5)
            ));
        }
    }

    private void generateStudentAndArchive() {
        seedStudents.forEach(seedStudents -> IntStream
            .range(seedStudents.seatNoFrom(), seedStudents.seatNoTo() + 1)
            .forEach(seatNo -> {
                int age = 2025 - seedStudents.gradeNo() + 18;
                var faker = PersonalInfoFaker.builder().age(age).build();
                Student student = StudentFaker.of(
                    faker, seedStudents.campus(), seedStudents.gradeNo(),
                    seedStudents.classNo(), seatNo
                );
                Archive archive = ArchiveFaker.of(faker);
                student.setArchive(archive);
                studentRepository.save(student);
            }));
    }


    // ================ 模拟用户数据 ================
    // 1. 超级管理员 admin
    // 2. 校级领导 school-leader
    // 3. 年级组长 dean-2027 (2027届)
    // 4. 班主任 class-2701 (2027届1班)
    // 5. 班主任 class-2702 (2027届2班)
    // 6. 多角色用户 multi-role (2028届年级组长、2027届3班班主任、260101及260102的协作用户)
    // 7. 多班级观察员 mco (2028届1班、2028届2班)
    // 8. 协作用户 cu-28xx01 (280101、280201、280301)
    // 9. 年级组长+班主任 dual-2026-1 (2026届年级组长+2026届1班班主任)
    private record SeedUser(String name, List<Role> roles) {}

    private final List<SeedUser> seedUsers = new ArrayList<>();

    @PostConstruct
    private void setSeedUsers() {
        seedUsers.add(new SeedUser("admin", List.of(new SuperAdmin())));
        seedUsers.add(new SeedUser("school-leader", List.of(new Principal())));
        seedUsers.add(new SeedUser("dean-2027", List.of(new GradeDean(2027))));
        seedUsers.add(new SeedUser("class-2701", List.of(new ClassAdviser(new GradeClass(2027, 1)))));
        seedUsers.add(new SeedUser("class-2702", List.of(new ClassAdviser(new GradeClass(2027, 2)))));
        seedUsers.add(new SeedUser("multi-role", List.of(
            new GradeDean(2028),
            new ClassAdviser(new GradeClass(2027, 3)),
            new CollaborativeUser(List.of("260101", "260102"))
        )
        ));
        seedUsers.add(new SeedUser("mco", List.of(
            new MultipleClassObserver(List.of(new GradeClass(2028, 1), new GradeClass(2028, 2)))
        )
        ));
        seedUsers.add(new SeedUser("cu-28xx01", List.of(new CollaborativeUser(List.of("280101", "280201", "280301")))));
        seedUsers.add(new SeedUser("dual-2026-1", List.of(
            new GradeDean(2026),
            new ClassAdviser(new GradeClass(2026, 1))
        )
        ));
    }

    private void generateUsers() {
        List<User> seedUsers = this.seedUsers.stream().map(
            seedUser ->
                new UserFaker()
                    .setName(seedUser.name())
                    .setRoles(seedUser.roles())
                    .sameUsernameAndName()
                    .useDefaultPassword()
                    .normalActiveAccount()
                    .toUser()
        ).toList();
        userRepository.saveAll(seedUsers);
    }

    @Override
    public void run(ApplicationArguments args) {
        log.warn("你正在准备进行模拟数据生成...");

        boolean isGenerateStudents = studentRepository.count() == 0;
        boolean isGenerateUsers = userRepository.count() == 0;
        log.warn("1. 生成以下学生及档案");
        log.warn(isGenerateStudents ? seedStudents.toString() : "跳过(已存在学生数据)");
        log.warn("2. 生成以下用户(初始密码均为: {} )", UserFaker.defaultPassword);
        if (isGenerateUsers)
            seedUsers.forEach(
                su -> log.warn("{} -> {}", su.name(), su.roles().stream().map(Role::getRoleAndScope).toList()));
        else
            log.warn("跳过(已存在用户数据)");

        System.out.println("请确认要生成的数据是否正确, 并输入[Y/y]继续...");
        if (!scanner.nextLine().matches("[Yy]")) {
            log.warn("已取消数据生成。");
            return;
        }

        if (isGenerateStudents) generateStudentAndArchive();
        if (isGenerateUsers) generateUsers();

        System.exit(SpringApplication.exit(ctx));
    }


}
