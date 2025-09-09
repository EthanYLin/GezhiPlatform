package org.example.gezhiplatform.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * <p>用于标记字段或参数是否为有效的中国居民身份证号码（18 位）。</p>
 *
 * <p>身份证号码的校验包括以下部分：</p>
 * <ol>
 *   <li>格式校验：必须为 18 位数字或者 17 位数字加 'X' 字符（大写）。</li>
 *   <li>出生日期校验：身份证中包含的出生日期必须在 1900 年 1 月 1 日之后且不得晚于当前日期。</li>
 *   <li>校验码校验：根据身份证号码的前 17 位计算出校验码，并与第 18 位进行比较。</li>
 * </ol>
 *
 * <p>默认错误消息为：18位身份证号格式或校验码不正确(若最后一位为X需大写)。</p>
 *
 * <p>使用时需要搭配 {@link RINValidator} 进行验证，验证逻辑位于 RINValidator 类中。</p>
 */
@Documented
@Constraint(validatedBy = RINValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface RIN {

    String message() default "18位身份证号格式或校验码不正确(若最后一位为X需大写)";

    Class<?>[] groups() default { };
    Class<? extends Payload>[] payload() default { };
}
