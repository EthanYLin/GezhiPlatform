package org.example.gezhiplatform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.example.gezhiplatform.DTO.user.UserRoleDetailsDTO;
import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.User;
import org.example.gezhiplatform.entity.role.*;
import org.example.gezhiplatform.exception.CustomInvalidArgException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RoleCreatorTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Validator validator;

    private static Map<String, User> makeUsers() {
        return Map.ofEntries(
            Map.entry("ethan", new User("ethan", new SuperAdmin())),
            Map.entry("school-leader", new User("school-leader", new Principal())),
            Map.entry("dean-27", new User("dean-27", new GradeDean(2027))),
            Map.entry("class-2701", new User("class-2701", new ClassAdviser(new GradeClass(2027, 1)))),
            Map.entry("class-2702", new User("class-2702", new ClassAdviser(new GradeClass(2027, 2)))),
            Map.entry("mco-2801,2802", new User("mco-2801,2802",
                                                new MultipleClassObserver(
                                                    List.of(new GradeClass(2028, 1), new GradeClass(2028, 2)))
                      )
            ),
            Map.entry("cu-28xx01", new User("cu-28xx01",
                                            new CollaborativeUser(List.of("280101", "280201", "280301"))
                      )
            ),
            Map.entry("multirole", new User("multirole", List.of(
                          new GradeDean(2028),
                          new ClassAdviser(new GradeClass(2027, 3)),
                          new CollaborativeUser(List.of("260101", "260102"))
                      )
                      )
            ),
            // 家长用户 - 可以查看多个孩子(260101, 260102, 270201)
            Map.entry("parent-multi", new User("parent-multi",
                                               new ParentUser(List.of("260101", "260102", "270201"))
                      )
            ),
            // 家长用户 - 只能查看一个孩子(280101)
            Map.entry("parent-single", new User("parent-single",
                                                new ParentUser(List.of("280101"))
                      )
            ),
            // 学生用户 - 只能查看自己的信息(260103)
            Map.entry("student-260103", new User("student-260103",
                                                 new StudentUser("260103")
                      )
            ),
            // 学生用户 - 只能查看自己的信息(270101)
            Map.entry("student-270101", new User("student-270101",
                                                 new StudentUser("270101")
                      )
            )
        );
    }

    // ========================  UserRoleDetailsResponse Test  ====================

    /**
     * 打印出所有角色的详细信息
     */
    @Test
    public void showUserRoleDetailsResponse() {
        makeUsers().values().stream()
                   .flatMap(user -> user.getRoles().stream())
                   .map(UserRoleDetailsDTO::of)
                   .forEach(System.out::println);
    }

    /**
     * 先将 Role 实体类转化为 UserRoleDetailsResponse, 然后再从中创建出 Role 实体类
     * 验证两者是否相等
     */
    @Test
    public void createRoles() {
        makeUsers().values().stream()
                   .map(User::getRoles)
                   .forEach(roles -> {
                       // 先将 Role 实体类转化为 UserRoleDetailsResponse
                       List<UserRoleDetailsDTO> roleDetailsResponses = UserRoleDetailsDTO.of(roles);
                       // 再从 UserRoleDetailsResponse 中创建出 Role 实体类
                       List<Role> createdRoles = roleDetailsResponses.stream().map(UserRoleDetailsDTO::toRole).toList();
                       // 验证两者是否相等
                       assertEquals(roles, createdRoles);
                   });
    }

    public UserRoleDetailsDTO parseRequest(String json) throws JsonProcessingException, CustomInvalidArgException {
        UserRoleDetailsDTO dto = objectMapper.readValue(json, UserRoleDetailsDTO.class);
        var violations = validator.validate(dto);
        if (!violations.isEmpty()) throw new CustomInvalidArgException(violations.toString());
        return dto;
    }

    /**
     * 验证一些非法的创建情况
     * 例如: 非法JSON字符串、无details字段、details字段为null、roleType字段为null、roleType字段为非法值等
     */
    @Test
    public void testParse() {
        // 非法JSON字符串
        String json1 = "{ \"details\": }";
        var thrown1 = assertThrows(JsonProcessingException.class, () -> parseRequest(json1).toRole());
        System.out.println(thrown1.getMessage());

        // 无details字段
        String json2 =
            """
                { "roleType": "年级组长" }
                """;
        var thrown = assertThrows(CustomInvalidArgException.class, () -> parseRequest(json2).toRole());
        System.out.println(thrown.getMessage());

        // details字段为null
        String json3 =
            """
                { "roleType": "年级组长", "details": null }
                """;
        thrown = assertThrows(CustomInvalidArgException.class, () -> parseRequest(json3).toRole());
        System.out.println(thrown.getMessage());

        // roleType字段为null
        String json5 =
            """
                { "details": { "classNo": 1, "gradeNo": 2027 } }
                """;
        thrown = assertThrows(CustomInvalidArgException.class, () -> parseRequest(json5).toRole());
        System.out.println(thrown.getMessage());

        // roleType字段为非法值
        String json6 =
            """
                { "roleType": "GD", "details": { "classNo": 1, "gradeNo": 2027 } }
                """;
        thrown = assertThrows(CustomInvalidArgException.class, () -> parseRequest(json6).toRole());
        System.out.println(thrown.getMessage());
    }

    @Test
    public void testParseDetails() throws JsonProcessingException {
        // 年级组长 (正常情况)
        String json1 =
            """
                { "roleType": "年级组长", "details": { "gradeNo": 2027 } }
                """;
        Role role = parseRequest(json1).toRole();
        if (role instanceof GradeDean gradeDean) {
            assertEquals(2027, gradeDean.getGradeNo());
            System.out.println(gradeDean.getRoleAndScope());
        } else fail("Expected GradeDean");

        // 年级组长 (无gradeNo字段)
        String json2 =
            """
                { "roleType": "年级组长", "details": { } }
                """;
        role = parseRequest(json2).toRole();
        if (role instanceof GradeDean gradeDean) {
            assertNull(gradeDean.getGradeNo());
            System.out.println(gradeDean.getRoleAndScope());
        } else fail("Expected GradeDean");

        // 年级组长 (gradeNo字段为null)
        String json3 =
            """
                { "roleType": "年级组长", "details": { "gradeNo": null } }
                """;
        role = parseRequest(json3).toRole();
        if (role instanceof GradeDean gradeDean) {
            assertNull(gradeDean.getGradeNo());
            System.out.println(gradeDean.getRoleAndScope());
        } else fail("Expected GradeDean");

        // 年级组长 (gradeNo字段为字符串数字)
        String json4 =
            """
                { "roleType": "年级组长", "details": { "gradeNo": "2027" } }
                """;
        role = parseRequest(json4).toRole();
        if (role instanceof GradeDean gradeDean) {
            assertEquals(2027, gradeDean.getGradeNo());
            System.out.println(gradeDean.getRoleAndScope());
        } else fail("Expected GradeDean");

        // 年级组长 (gradeNo字段为不合法字符串)
        String json5 =
            """
                { "roleType": "年级组长", "details": { "gradeNo": "abc" } }
                """;
        var thrown = assertThrows(CustomInvalidArgException.class, () -> parseRequest(json5).toRole());
        System.out.println(thrown.getMessage());
    }
}
