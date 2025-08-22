package org.example.gezhiplatform.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

/**
 * 年级-班级值对象。
 * <p>
 * 提供年级与班级的绝对/相对表达能力：
 * <ul>
 *   <li>{@link #toAbsoluteExpr()} 示例：{@code 2024届1班}</li>
 *   <li>{@link #toRelativeExpr()} 示例：{@code 高一(01)班}（默认以当前时间所在的学年为准）</li>
 *   <li>{@link #toRelativeExpr(int)} 示例：{@code 高一(01)班}（按指定学年计算）</li>
 * </ul>
 */
@Embeddable
@Data
public class GradeClass implements Comparable<GradeClass> {

    @Nullable
    private Integer gradeNo; // 届(Integer)

    @Nullable
    private Integer classNo; // 班级(Integer)

    public GradeClass() {
    }

    /**
     * 使用给定届别与班级号创建实例。
     *
     * @param gradeNo 届别，允许为 {@code null}
     * @param classNo 班级号，允许为 {@code null}
     */
    public GradeClass(@Nullable Integer gradeNo, @Nullable Integer classNo) {
        this.gradeNo = gradeNo;
        this.classNo = classNo;
    }

    /**
     * 返回绝对表达形式（入学届 + 班级）。
     * 当任一字段缺失时以 {@code ?} 占位。
     *
     * @return 例如：{@code 2024届1班}
     */
    public @NotNull String toAbsoluteExpr() {
        return (gradeNo != null ? gradeNo.toString() : "?") + "届" +
               (classNo != null ? classNo.toString() : "?") + "班";
    }

    /**
     * 返回相对表达形式（年级段 + 班级），相对指定学年计算。
     * 当无法映射为「高一/高二/高三」时，年级部分回退为「{届}届」。
     *
     * @param currentAcademicYear 指定的学年（通常为「九月起算」的年份）
     * @return 例如：{@code 高一(01)班}
     */
    public @NotNull String toRelativeExpr(int currentAcademicYear) {
        String gradeStr;
        String classStr;

        if (gradeNo == null) {
            gradeStr = "?届";
        } else {
            gradeStr = switch (gradeNo - currentAcademicYear) {
                case 3 -> "高一";
                case 2 -> "高二";
                case 1 -> "高三";
                default -> gradeNo + "届";
            };
        }

        if (classNo == null) {
            classStr = "(?)班";
        } else {
            classStr = "(" + String.format("%02d", classNo) + ")班";
        }

        return gradeStr + classStr;
    }

    /**
     * 返回相对表达形式（年级段 + 班级），相对当前学年计算。
     * 九月及之后视为新学年，否则为上一学年。
     *
     * @return 例如：{@code 高一(01)班}
     */
    public @NotNull String toRelativeExpr() {
        var currentAcademicYear = LocalDateTime.now().getMonthValue() >= 9 ?
            LocalDateTime.now().getYear() :
            LocalDateTime.now().getYear() - 1;
        return toRelativeExpr(currentAcademicYear);
    }

    /**
     * 等同于 {@link #toAbsoluteExpr()}。
     *
     * @return 绝对表达形式字符串
     */
    @Override
    public String toString() {
        return this.toAbsoluteExpr();
    }

    @Override
    public int compareTo(@NotNull GradeClass o) {
        int gradeComparison = Integer.compare(
            o.gradeNo != null ? o.gradeNo : 0,
            this.gradeNo != null ? this.gradeNo : 0
        );
        if (gradeComparison != 0) {
            return gradeComparison;
        }
        return Integer.compare(
            this.classNo != null ? this.classNo : 0,
            o.classNo != null ? o.classNo : 0
        );
    }
}

