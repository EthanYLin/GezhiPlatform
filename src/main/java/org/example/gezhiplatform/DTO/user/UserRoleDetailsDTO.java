package org.example.gezhiplatform.DTO.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import org.example.gezhiplatform.entity.enums.RoleType;
import org.example.gezhiplatform.entity.user_role.Role;
import org.example.gezhiplatform.exception.CustomInvalidArgException;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * 用户角色详情响应体/请求体
 * <p>
 *     作为响应体时: 返回用户角色的类型和详细信息，用于之后前端发送更新用户角色的请求。<br/>
 *     作为请求体时: 接收用户角色的类型和详细信息，用于后台更新用户角色。<br/>
 *     <b>注意: 该DTO既可作为响应体, 同时又能作为请求体, 两者格式一致。</b>
 * </p>
 * 例如:
 * <ul>
 *     <li>roleType: "超级管理员/校级领导", details: {}</li>
 *     <li>roleType: "多班级观察员", details: { "gradeClasses": [{"gradeNo": 2027, "classNo": 3}, {"gradeNo": 2028, "classNo": 4}] }</li>
 *     <li>roleType: "班主任", details: { "gradeClass": {"gradeNo": 2027, "classNo": 3} }</li>
 *     <li>roleType: "年级组长", details: {"gradeNo": 2027}</li>
 *     <li>roleType: "协作用户/家长用户", details: { "stuNos": ["270101", "270102"] }</li>
 *     <li>roleType: "学生用户", details: { "stuNo": "270101" }</li>
 * </ul>
 * @param roleType 用户角色类型(中文名, 如: 班主任 {@link RoleType#getDesc()})
 * @param details 用户角色的详细信息(如: 班级号、年级号、学生号等)
 */
public record UserRoleDetailsDTO(
    @NotNull String roleType,
    @NotNull Map<?, ?> details
) {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将一个Role实体转换为对应的UserRoleDetailsDTO。
     *
     * @param role 角色对象
     * @return UserRoleDetailsDTO对象，包括角色类型及该角色详细信息(如gradeNo, classNo)
     */
    public static UserRoleDetailsDTO of(Role role) {
        Map<?, ?> details = objectMapper.convertValue(role, Map.class);
        return new UserRoleDetailsDTO(role.getRoleType().getDesc(), details);
    }

    /**
     * 将多个Role实体转换为对应的UserRoleDetailsDTO列表。
     *
     * @param roles 角色列表
     * @return 包含转换后所有用户角色详情的列表，每个元素为{@link UserRoleDetailsDTO}实例。
     */
    public static List<UserRoleDetailsDTO> of(Collection<Role> roles) {
        return roles.stream().map(UserRoleDetailsDTO::of).toList();
    }

    /**
     * 将UserRoleDetailsDTO转换为对应的Role实体。
     * 依据roleType字段找到对应的RoleType枚举值，
     * 然后使用details字段中的信息，使用{@link RoleType#fromJson}构造具体的Role实体。
     *
     * @return 一个具体的Role实例对象，如班主任、年级组长等。
     *         不同的Role类型实例会根据roleType和details字段的数据动态创建。
     * @throws CustomInvalidArgException 如果roleType字段对应的角色类型无效，
     *         或details字段的信息格式不符合构造该Role实例的要求。
     */
    public Role toRole() throws CustomInvalidArgException {
        return RoleType.fromDesc(roleType).fromJson(details);
    }
}
