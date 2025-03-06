package org.example.gezhiplatform.entity.archive;

import jakarta.persistence.Embeddable;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 其他亲属类(包括姓名、出生年份、工作/就学信息)
 * 用于：用于：学生档案 - 亲属信息部分 - 其他亲属
 */
@Embeddable
public class Relative {

    @Nullable
    private String name; // 亲属姓名

    @Nullable
    private Integer birthYear; // 出生年份

    @Nullable
    private String info; // 工作/就学信息

    public @Nullable Integer getAge() {
        return Optional.ofNullable(birthYear)
                       .map(year -> LocalDateTime.now().getYear() - year)
                       .filter(age -> age >= 0)
                       .orElse(null);
    }

}
