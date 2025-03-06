package org.example.gezhiplatform.entity.archive;

import jakarta.persistence.Embeddable;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.Optional;

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
