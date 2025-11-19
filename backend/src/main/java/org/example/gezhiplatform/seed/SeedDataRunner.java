package org.example.gezhiplatform.seed;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.User;
import org.example.gezhiplatform.entity.archive.Archive;
import org.example.gezhiplatform.entity.archive.PermissionGroup;
import org.example.gezhiplatform.entity.archive.ValidationExpr;
import org.example.gezhiplatform.entity.enums.Campus;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.example.gezhiplatform.entity.role.*;
import org.example.gezhiplatform.repository.PermissionGroupRepository;
import org.example.gezhiplatform.repository.StudentRepository;
import org.example.gezhiplatform.repository.UserRepository;
import org.example.gezhiplatform.service.archive.ArchiveMetadataService;
import org.example.gezhiplatform.service.archive.ArchivePermissionGroupService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.IntStream;

import static org.example.gezhiplatform.seed.PermissionGroupFaker.*;

@Slf4j
@Component
@Profile("seed")
@RequiredArgsConstructor
public class SeedDataRunner implements ApplicationRunner {

    private final ApplicationContext ctx;
    private final Scanner scanner = new Scanner(System.in);
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ArchiveMetadataService archiveMetadataService;
    private final ArchivePermissionGroupService archivePermissionGroupService;
    private final PermissionGroupRepository permissionGroupRepository;

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


    // ================ 模拟权限组数据 ================
    private List<PermissionGroup> makePermissionGroupRequests() {
        var faker = new PermissionGroupFaker(archiveMetadataService.getFieldMetadata());
        return List.of(
            createPermissionGroup(
                "个人信息(Level-0)", null, true, whichLevelGe(0),
                faker.pathsBeginWith(PERSONAL_PART).except(PERSONAL_PART + ".rin").get(),
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of()
            ),
            createPermissionGroup(
                "入学信息(Level-5)", null, true, whichLevelGe(5),
                faker.pathsBeginWith(ADMISSION_PART).get(),
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of()
            ),
            createPermissionGroup(
                "家庭信息(Level-5)", null, true, whichLevelGe(5),
                Set.of(FAMILY_PART,
                       FAMILY_PART + ".father", FAMILY_PART + ".mother",
                       FAMILY_PART + ".father.name", FAMILY_PART + ".mother.name",
                       FAMILY_PART + ".father.mobile", FAMILY_PART + ".mother.mobile"
                ),
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of()
            ),
            createPermissionGroup(
                "地址信息(Level-9)", null, true, whichLevelGe(9),
                faker.pathsBeginWith(ADDRESS_PART).get(),
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of()
            ),
            createPermissionGroup(
                "健康信息(Level-9)", null, true, whichLevelGe(9),
                faker.pathsBeginWith(HEALTH_PART).get(),
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of()
            ),
            createPermissionGroup(
                "敏感信息(Level-9)", null, true, whichLevelGe(9),
                faker.pathsBeginWith(FAMILY_PART + ".other")
                     .and(PERSONAL_PART).and(FAMILY_PART)
                     .and(PERSONAL_PART + ".rin")
                     .and(FAMILY_PART + ".father").and(FAMILY_PART + ".mother")
                     .and(FAMILY_PART + ".father.workUnit")
                     .and(FAMILY_PART + ".mother.workUnit").get(),
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of()
            ),
            createPermissionGroup(
                "管理员(AllAccess)", null, true, whichLevelGe(10),
                faker.pathsBeginWith("$").get(),
                faker.pathsBeginWith("$").get(), Set.of(), Set.of(), Set.of(), Set.of()
            )
        );
    }

    private PermissionGroup createPermissionGroup(
        String name, String description, Boolean enabled, Set<RoleType> roleTypes,
        Set<String> readableJsonPaths, Set<String> writableJsonPaths,
        Set<String> addArrayJsonPaths, Set<String> editArrayJsonPaths,
        Set<String> deleteArrayJsonPaths, Set<ValidationExpr> validationSpELs
    ) {
        PermissionGroup pg = new PermissionGroup();
        pg.setName(name);
        pg.setDescription(description);
        pg.setEnabled(enabled);
        pg.setRoleTypes(roleTypes);
        pg.setAllowedReadableJsonPaths(readableJsonPaths);
        pg.setAllowedWritableJsonPaths(writableJsonPaths);
        pg.setAllowedAddArrayJsonPaths(addArrayJsonPaths);
        pg.setAllowedEditArrayJsonPaths(editArrayJsonPaths);
        pg.setAllowedDeleteArrayJsonPaths(deleteArrayJsonPaths);
        pg.setValidations(validationSpELs);
        return pg;
    }

    private void generatePermissionGroups() {
        List<PermissionGroup> permissionGroups = makePermissionGroupRequests();
        permissionGroups.forEach(archivePermissionGroupService::addPermissionGroup);
    }

    @Override
    public void run(ApplicationArguments args) {
        log.warn("你正在准备进行模拟数据生成...");

        boolean isGenerateStudents = studentRepository.count() == 0;
        boolean isGenerateUsers = userRepository.count() == 0;
        boolean isGeneratePermissionGroup = permissionGroupRepository.count() == 0;
        log.warn("1. 生成以下学生及档案");
        log.warn(isGenerateStudents ? seedStudents.toString() : "跳过(已存在学生数据)");
        log.warn("2. 生成以下用户(初始密码均为: {} )", UserFaker.defaultPassword);
        if (isGenerateUsers)
            seedUsers.forEach(
                su -> log.warn("{} -> {}", su.name(), su.roles().stream().map(Role::getRoleAndScope).toList()));
        else
            log.warn("跳过(已存在用户数据)");
        log.warn("3. 生成以下权限组");
        if (isGeneratePermissionGroup)
            makePermissionGroupRequests().forEach(pg -> log.warn(pg.toString()));
        else
            log.warn("跳过(已存在权限组数据)");

        System.out.println("请确认要生成的数据是否正确, 并输入[Y/y]继续...");
        if (!scanner.nextLine().matches("[Yy]")) {
            log.warn("已取消数据生成。");
            return;
        }

        if (isGenerateStudents) generateStudentAndArchive();
        if (isGenerateUsers) generateUsers();
        if (isGeneratePermissionGroup) generatePermissionGroups();

        System.exit(SpringApplication.exit(ctx));
    }


}
