package org.example.gezhiplatform;

import jakarta.transaction.Transactional;
import org.example.gezhiplatform.DTO.PageResult;
import org.example.gezhiplatform.DTO.user.UserDetailResponse;
import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.User;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.example.gezhiplatform.entity.role.*;
import org.example.gezhiplatform.repository.UserRepository;
import org.example.gezhiplatform.service.user.UserManagementService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class UserManagementServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserManagementService userManagementService;

    private static Map<String, User> makeUsers() {
        return Map.ofEntries(
            Map.entry("ethan", new User("ethan", "ethan", null, List.of(new SuperAdmin()))),
            Map.entry("school-leader", new User("school-leader", "school-leader", null, List.of(new Principal()))),
            Map.entry("dean-27", new User("dean-27", "dean-27", null, List.of(new GradeDean(2027)))),
            Map.entry("class-2701",
                      new User("class-2701", "class-2701", null, List.of(new ClassAdviser(new GradeClass(2027, 1))))
            ),
            Map.entry("class-2702",
                      new User("class-2702", "class-2702", null, List.of(new ClassAdviser(new GradeClass(2027, 2))))
            ),
            Map.entry("mco-2801,2802", new User(
                          "mco-2801,2802", "mco-2801,2802", null,
                          List.of(new MultipleClassObserver(List.of(new GradeClass(2028, 1), new GradeClass(2028, 2))))
                      )
            ),
            Map.entry("cu-28xx01", new User("cu-28xx01", "cu-28xx01", null,
                                            List.of(new CollaborativeUser(List.of("280101", "280201", "280301")))
                      )
            ),
            Map.entry("multirole", new User("multirole", "multirole", null, List.of(
                          new GradeDean(2028),
                          new ClassAdviser(new GradeClass(2027, 3)),
                          new CollaborativeUser(List.of("260101", "260102"))
                      )
                      )
            ),
            // 家长用户 - 可以查看多个孩子(260101, 260102, 270201)
            Map.entry("parent-multi", new User("parent-multi", "parent-multi", null,
                                               List.of(new ParentUser(List.of("260101", "260102", "270201")))
                      )
            ),
            // 家长用户 - 只能查看一个孩子(280101)
            Map.entry("parent-single", new User("parent-single", "parent-single", null,
                                                List.of(new ParentUser(List.of("280101")))
                      )
            ),
            // 学生用户 - 只能查看自己的信息(260103)
            Map.entry("student-260103", new User("student-260103", "student-260103", null,
                                                 List.of(new StudentUser("260103"))
                      )
            ),
            // 学生用户 - 只能查看自己的信息(270101)
            Map.entry("student-270101", new User("student-270101", "student-270101", null,
                                                 List.of(new StudentUser("270101"))
                      )
            )
        );
    }

    @BeforeEach
    void addUsers() {
        userRepository.deleteAll();
        userRepository.flush();
        userRepository.saveAll(makeUsers().values());
    }

    /**
     * 测试 searchUsers 方法的单条件搜索功能
     * 包括：ID精确匹配、姓名模糊匹配、用户名模糊匹配、锁定状态筛选、启用状态筛选、角色类型筛选
     */
    @Test
    void testSearchUsers_SingleCondition() {
        Map<String, User> users = makeUsers();
        Pageable pageable = PageRequest.of(0, users.size());

        // 2. 测试姓名模糊匹配 - 搜索姓名包含特定字符的用户
        PageResult<UserDetailResponse> nameResult = userManagementService.searchUsers(
            "ethan", null, null, null, pageable);
        assertThat(nameResult.content()).hasSize(1);
        assertThat(nameResult.content().getFirst().username()).isEqualTo("ethan");

        // 3. 测试用户名模糊匹配 - 搜索用户名包含"class"的用户
        PageResult<UserDetailResponse> usernameResult = userManagementService.searchUsers(
            "class", null, null, null, pageable);
        assertThat(usernameResult.content()).hasSizeGreaterThanOrEqualTo(2); // class-2701, class-2702
        assertThat(usernameResult.content())
            .extracting(UserDetailResponse::username)
            .allMatch(username -> {
                Assertions.assertNotNull(username);
                return username.contains("class");
            });

        // 4. 测试锁定状态筛选 - 搜索未锁定的用户（所有测试用户默认未锁定）
        PageResult<UserDetailResponse> unlockedResult = userManagementService.searchUsers(
            null, false, null, null, pageable);
        assertThat(unlockedResult.content()).hasSize(users.size());
        assertThat(unlockedResult.content())
            .extracting(UserDetailResponse::isLocked)
            .containsOnly(false);

        // 测试锁定状态筛选 - 搜索已锁定的用户（应该为空）
        PageResult<UserDetailResponse> lockedResult = userManagementService.searchUsers(
            null, true, null, null, pageable);
        assertThat(lockedResult.content()).isEmpty();

        // 5. 测试启用状态筛选 - 搜索未启用的用户（所有测试用户默认未启用）
        PageResult<UserDetailResponse> enabledResult = userManagementService.searchUsers(
            null, null, false, null, pageable);
        assertThat(enabledResult.content()).hasSize(users.size());
        assertThat(enabledResult.content())
            .extracting(UserDetailResponse::isEnabled)
            .containsOnly(false);

        // 测试启用状态筛选 - 搜索启用的用户（应该为空）
        PageResult<UserDetailResponse> disabledResult = userManagementService.searchUsers(
            null, null, true, null, pageable);
        assertThat(disabledResult.content()).isEmpty();

        // 6. 测试角色类型筛选 - 搜索超级管理员
        PageResult<UserDetailResponse> superAdminResult = userManagementService.searchUsers(
            null, null, null, RoleType.SUPER_ADMIN, pageable);
        assertThat(superAdminResult.content()).hasSize(1);
        assertThat(superAdminResult.content().getFirst().username()).isEqualTo("ethan");

        // 测试角色类型筛选 - 搜索班主任
        PageResult<UserDetailResponse> classAdvisorResult = userManagementService.searchUsers(
            null, null, null, RoleType.CLASS_ADVISOR, pageable);
        assertThat(classAdvisorResult.content()).hasSize(3); // class-2701, class-2702, multirole
        assertThat(classAdvisorResult.content())
            .extracting(UserDetailResponse::username)
            .containsExactlyInAnyOrder("class-2701", "class-2702", "multirole");

        // 测试角色类型筛选 - 搜索年级组长
        PageResult<UserDetailResponse> gradeDeanResult = userManagementService.searchUsers(
            null, null, null, RoleType.GRADE_DEAN, pageable);
        assertThat(gradeDeanResult.content()).hasSize(2); // dean-27, multirole
        assertThat(gradeDeanResult.content())
            .extracting(UserDetailResponse::username)
            .containsExactlyInAnyOrder("dean-27", "multirole");

        // 测试角色类型筛选 - 搜索家长用户
        PageResult<UserDetailResponse> parentResult = userManagementService.searchUsers(
            null, null, null, RoleType.PARENT_USER, pageable);
        assertThat(parentResult.content()).hasSize(2); // parent-multi, parent-single
        assertThat(parentResult.content())
            .extracting(UserDetailResponse::username)
            .containsExactlyInAnyOrder("parent-multi", "parent-single");

        // 测试角色类型筛选 - 搜索学生用户
        PageResult<UserDetailResponse> studentResult = userManagementService.searchUsers(
            null, null, null, RoleType.STUDENT_USER, pageable);
        assertThat(studentResult.content()).hasSize(2); // student-260103, student-270101
        assertThat(studentResult.content())
            .extracting(UserDetailResponse::username)
            .containsExactlyInAnyOrder("student-260103", "student-270101");
    }

    /**
     * 测试 searchUsers 方法的多条件组合搜索功能
     * 测试多个查询条件的AND逻辑组合
     */
    @Test
    void testSearchUsers_MultipleConditions() {
        Map<String, User> users = makeUsers();
        Pageable pageable = PageRequest.of(0, users.size());

        // 1. 关键词 + 角色类型：搜索用户名包含"class"且角色为班主任的用户
        PageResult<UserDetailResponse> keywordAndRoleResult = userManagementService.searchUsers(
            "class", null, null, RoleType.CLASS_ADVISOR, pageable);
        assertThat(keywordAndRoleResult.content()).hasSize(2); // class-2701, class-2702
        assertThat(keywordAndRoleResult.content())
            .extracting(UserDetailResponse::username)
            .containsExactlyInAnyOrder("class-2701", "class-2702");

        // 2. 启用状态 + 角色类型：搜索已启用且角色为超级管理员的用户
        PageResult<UserDetailResponse> enabledAndRoleResult = userManagementService.searchUsers(
            null, null, false, RoleType.SUPER_ADMIN, pageable);
        assertThat(enabledAndRoleResult.content()).hasSize(1);
        assertThat(enabledAndRoleResult.content().getFirst().username()).isEqualTo("ethan");

        // 3. 锁定状态 + 启用状态：搜索未锁定且已启用的用户（应该是所有用户）
        PageResult<UserDetailResponse> lockedAndEnabledResult = userManagementService.searchUsers(
            null, false, false, null, pageable);
        assertThat(lockedAndEnabledResult.content()).hasSize(users.size());

        // 4. 关键词 + 锁定状态 + 启用状态：搜索姓名包含"parent"、未锁定且已启用的用户
        PageResult<UserDetailResponse> keywordLockedEnabledResult = userManagementService.searchUsers(
            "parent", false, false, null, pageable);
        assertThat(keywordLockedEnabledResult.content()).hasSize(2); // parent-multi, parent-single
        assertThat(keywordLockedEnabledResult.content())
            .extracting(UserDetailResponse::username)
            .containsExactlyInAnyOrder("parent-multi", "parent-single");

        // 5. 测试无匹配结果的组合：搜索用户名包含"nonexistent"且角色为超级管理员的用户
        PageResult<UserDetailResponse> noMatchResult = userManagementService.searchUsers(
            "nonexistent", null, null, RoleType.SUPER_ADMIN, pageable);
        assertThat(noMatchResult.content()).isEmpty();

        // 6. 测试冲突条件：搜索已锁定的用户（所有测试用户都未锁定，应该为空）
        PageResult<UserDetailResponse> conflictResult = userManagementService.searchUsers(
            null, true, true, null, pageable);
        assertThat(conflictResult.content()).isEmpty();
    }

    /**
     * 测试 searchUsers 方法的分页功能
     * 包括：排序字段、升序降序、页大小、页码参数、分页边界条件
     */
    @Test
    void testSearchUsers_Pagination() {
        Map<String, User> users = makeUsers();
        int totalUsers = users.size();

        // 1. 测试基本分页 - 第一页，每页5条
        Pageable firstPage = PageRequest.of(0, 5);
        PageResult<UserDetailResponse> firstPageResult = userManagementService.searchUsers(
            null, null, null, null, firstPage);
        assertThat(firstPageResult.content()).hasSize(5);
        assertThat(firstPageResult.totalElements()).isEqualTo(totalUsers);
        assertThat(firstPageResult.totalPages()).isEqualTo((int) Math.ceil((double) totalUsers / 5));
        assertThat(firstPageResult.isFirst()).isTrue();
        assertThat(firstPageResult.isLast()).isFalse();

        // 2. 测试第二页
        Pageable secondPage = PageRequest.of(1, 5);
        PageResult<UserDetailResponse> secondPageResult = userManagementService.searchUsers(
            null, null, null, null, secondPage);
        assertThat(secondPageResult.content()).hasSize(Math.min(5, totalUsers - 5));
        assertThat(secondPageResult.totalElements()).isEqualTo(totalUsers);
        assertThat(secondPageResult.isFirst()).isFalse();

        // 3. 测试按用户名升序排序
        Pageable sortByUsernameAsc = PageRequest.of(0, totalUsers, Sort.by(Sort.Direction.ASC, "username"));
        PageResult<UserDetailResponse> sortedAscResult = userManagementService.searchUsers(
            null, null, null, null, sortByUsernameAsc);
        assertThat(sortedAscResult.content()).hasSize(totalUsers);
        List<String> ascUsernames = sortedAscResult.content().stream()
            .map(UserDetailResponse::username)
            .toList();
        System.out.println(ascUsernames);
        assertThat(ascUsernames).isSorted();

        // 4. 测试按用户名降序排序
        Pageable sortByUsernameDesc = PageRequest.of(0, totalUsers, Sort.by(Sort.Direction.DESC, "username"));
        PageResult<UserDetailResponse> sortedDescResult = userManagementService.searchUsers(
            null, null, null, null, sortByUsernameDesc);
        assertThat(sortedDescResult.content()).hasSize(totalUsers);
        List<String> descUsernames = sortedDescResult.content().stream()
            .map(UserDetailResponse::username)
            .toList();
        // 验证降序排序
        for (int i = 0; i < descUsernames.size() - 1; i++) {
            assertThat(descUsernames.get(i)).isGreaterThanOrEqualTo(descUsernames.get(i + 1));
        }

        // 5. 测试按ID排序
        Pageable sortByIdAsc = PageRequest.of(0, totalUsers, Sort.by(Sort.Direction.ASC, "id"));
        PageResult<UserDetailResponse> sortedByIdResult = userManagementService.searchUsers(
            null, null, null, null, sortByIdAsc);
        assertThat(sortedByIdResult.content()).hasSize(totalUsers);
        List<Long> sortedIds = sortedByIdResult.content().stream()
            .map(UserDetailResponse::id)
            .toList();
        assertThat(sortedIds).isSorted();

        // 6. 测试多字段排序：先按启用状态降序，再按用户名升序
        Pageable multiSort = PageRequest.of(0, totalUsers, 
            Sort.by(Sort.Direction.DESC, "isEnabled")
                .and(Sort.by(Sort.Direction.ASC, "username")));
        PageResult<UserDetailResponse> multiSortResult = userManagementService.searchUsers(
            null, null, null, null, multiSort);
        assertThat(multiSortResult.content()).hasSize(totalUsers);

        // 7. 测试页大小为1的极端情况
        Pageable onePerPage = PageRequest.of(0, 1);
        PageResult<UserDetailResponse> onePerPageResult = userManagementService.searchUsers(
            null, null, null, null, onePerPage);
        assertThat(onePerPageResult.content()).hasSize(1);
        assertThat(onePerPageResult.totalElements()).isEqualTo(totalUsers);
        assertThat(onePerPageResult.totalPages()).isEqualTo(totalUsers);

        // 8. 测试超大页大小（但在1000限制内）
        Pageable largePage = PageRequest.of(0, 100);
        PageResult<UserDetailResponse> largePageResult = userManagementService.searchUsers(
            null, null, null, null, largePage);
        assertThat(largePageResult.content()).hasSize(totalUsers); // 实际数据量小于页大小
        assertThat(largePageResult.isFirst()).isTrue();
        assertThat(largePageResult.isLast()).isTrue();

        // 9. 测试超出范围的页码
        Pageable outOfRangePage = PageRequest.of(999, 10);
        PageResult<UserDetailResponse> outOfRangeResult = userManagementService.searchUsers(
            null, null, null, null, outOfRangePage);
        assertThat(outOfRangeResult.content()).isEmpty();
        assertThat(outOfRangeResult.totalElements()).isEqualTo(totalUsers);

        // 10. 测试分页大小超限异常
        Pageable oversizedPage = PageRequest.of(0, 1001);
        assertThatThrownBy(() -> userManagementService.searchUsers(
            null, null, null, null, oversizedPage))
            .isInstanceOf(Exception.class)
            .hasMessageContaining("分页的最大页大小为1000条记录");

        // 11. 测试无效排序字段异常
        Pageable invalidSort = PageRequest.of(0, 10, Sort.by("invalidField"));
        assertThatThrownBy(() -> userManagementService.searchUsers(
            null, null, null, null, invalidSort))
            .isInstanceOf(Exception.class)
            .hasMessageContaining("分页排序参数中包含无效的字段");
    }

}
