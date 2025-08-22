package org.example.gezhiplatform.utils;

import java.lang.reflect.Field;
import java.util.Optional;

public class ReflectionUtils {
    /**
     * 按名称和类型从指定的类检索字段。
     *
     * @param clazz 要搜索字段的类
     * @param fieldName 要查找的字段的名称
     * @param fieldType 要查找的字段类型
     * @return 如果找到该字段，则返回Optional<该字段>，否则返回空Optional
     */
    public static Optional<Field> getField(Class<?> clazz, String fieldName, Class<?> fieldType) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals(fieldName) && field.getType().equals(fieldType)) {
                return Optional.of(field);
            }
        }
        return Optional.empty();
    }
}
