package org.example.gezhiplatform.seed;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.example.gezhiplatform.entity.User;
import org.example.gezhiplatform.entity.role.Role;
import org.example.gezhiplatform.utils.PasswordEncryptUtils;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;

@Accessors(chain = true)
public class UserFaker {

    public static String defaultPassword = "123456";

    @Setter @Nullable private Long id;
    @Setter @Nullable private String name;
    @Setter @Nullable private String username;
    @Setter @Nullable private String rawPassword;
    @Setter @Nullable private Boolean locked;
    @Setter @Nullable private Boolean enabled;
    @Setter @Nullable private LocalDateTime lastLoginTime;
    @Setter @Nullable private List<Role> roles;

    @Nullable private Boolean isSameUsernameAndName;
    @Nullable private Boolean isUseDefaultPassword;
    @Nullable private Boolean isNormalActiveAccount;

    public UserFaker sameUsernameAndName() {
        this.isSameUsernameAndName = true;
        return this;
    }

    public UserFaker useDefaultPassword() {
        this.isUseDefaultPassword = true;
        return this;
    }

    public UserFaker normalActiveAccount() {
        this.isNormalActiveAccount = true;
        return this;
    }

    public User toUser() {
        User user = new User();
        user.setId(id);
        if (Boolean.TRUE.equals(isSameUsernameAndName) && name != null) {
            user.setUsername(name);
            user.setName(name);
        } else if (Boolean.TRUE.equals(isSameUsernameAndName) && username != null) {
            user.setUsername(username);
            user.setName(username);
        } else {
            user.setUsername(username);
            user.setName(name);
        }
        if (Boolean.TRUE.equals(isUseDefaultPassword)) {
            user.setEncryptedPassword(PasswordEncryptUtils.encode(defaultPassword));
        } else if (rawPassword != null) {
            user.setEncryptedPassword(PasswordEncryptUtils.encode(rawPassword));
        }
        if (Boolean.TRUE.equals(isNormalActiveAccount)) {
            user.setLocked(false);
            user.setEnabled(true);
        } else {
            if (locked != null) user.setLocked(locked);
            if (enabled != null) user.setEnabled(enabled);
        }
        user.setLastLoginTime(lastLoginTime);
        if (roles != null) user.getRoles().addAll(roles);
        return user;
    }

}
