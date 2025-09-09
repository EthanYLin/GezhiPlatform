package org.example.gezhiplatform.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * <p>居民身份证号码验证器，用于验证18位身份证号码的合法性。</p>
 * <ol>
 *   <li>格式校验：必须为 18 位数字或者 17 位数字加 'X' 字符（大写）。</li>
 *   <li>出生日期校验：身份证中包含的出生日期必须在 1900 年 1 月 1 日之后且不得晚于当前日期。</li>
 *   <li>校验码校验：根据身份证号码的前 17 位计算出校验码，并与第 18 位进行比较。</li>
 * </ol>
 */
public class RINValidator implements ConstraintValidator<RIN, String> {

    /**
     * 身份证号码各位的权重值
     */
    private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

    /**
     * 校验码对应值
     */
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    /**
     * 验证身份证号码是否有效
     * 核验内容包括: 格式校验、出生日期校验和校验码校验。
     *
     * @param value   待验证的身份证号码
     * @param context 约束验证上下文
     * @return 身份证号码是否有效
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 允许身份证号为null
        if (value == null) return true;
        // 不允许空字符串
        if (value.isBlank()) return false;
        // 位数及格式校验
        if (!value.matches("^\\d{17}[0-9X]$")) return false;
        // 生日校验
        String birth = value.substring(6, 14);
        if (!isValidBirthDate(birth)) return false;
        // 校验码校验
        return verifyChecksum(value);
    }

    /**
     * 验证出生日期的有效性（在1900-01-01之后, 在当前日期之前）
     *
     * @param birthdate 出生日期字符串（格式：yyyyMMdd）
     * @return 出生日期是否有效
     */
    private boolean isValidBirthDate(String birthdate) {
        try {
            LocalDate date = LocalDate.parse(birthdate, DateTimeFormatter.BASIC_ISO_DATE);
            LocalDate today = LocalDate.now();
            return !date.isAfter(today) && !date.isBefore(LocalDate.of(1900, 1, 1));
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * 验证身份证号码的校验码
     *
     * @param id18 18位身份证号码
     * @return 校验码是否正确
     */
    private boolean verifyChecksum(String id18) {
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            int ai = id18.charAt(i) - '0';
            sum += ai * WEIGHTS[i];
        }
        int mod = sum % 11;
        char expect = CHECK_CODES[mod];
        return expect == id18.charAt(17);
    }
}