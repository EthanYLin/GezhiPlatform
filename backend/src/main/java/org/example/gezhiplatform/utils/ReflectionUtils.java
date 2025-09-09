package org.example.gezhiplatform.utils;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ReflectionUtils {
    /**
     * 按名称和类型从指定的类检索字段。
     *
     * @param clazz     要搜索字段的类
     * @param fieldName 要查找的字段的名称
     * @param fieldType 要查找的字段类型
     * @return 如果找到该字段，则返回Optional<该字段>，否则返回空Optional
     */
    public static Optional<Field> getField(Class<?> clazz, String fieldName, Class<?> fieldType) {
        return Arrays.stream(clazz.getDeclaredFields())
                     .filter(field -> field.getName().equals(fieldName) && field.getType().equals(fieldType))
                     .findFirst();
    }

    /**
     * 按名称从指定的类检索字段。
     *
     * @param clazz     要搜索字段的类
     * @param fieldName 要查找的字段的名称
     * @return 如果找到该字段，则返回Optional<该字段>，否则返回空Optional
     */
    public static Optional<Field> getField(Class<?> clazz, String fieldName) {
        return Arrays.stream(clazz.getDeclaredFields())
                     .filter(field -> field.getName().equals(fieldName))
                     .findFirst();
    }

    /**
     * 获取指定类中不存在的字段名称集合。
     *
     * @param clazz      要检查的类
     * @param fieldNames 要检查的字段名称集合
     * @return 不存在于类中的字段名称集合
     */
    public static Set<String> getAllNotExistingFieldNames(Class<?> clazz, Set<String> fieldNames) {
        Set<String> allFieldNames = Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        return fieldNames.stream().filter(field -> !allFieldNames.contains(field)).collect(Collectors.toSet());
    }

    /**
     * 获取分页请求中排序属性中不存在于指定类的字段名称集合。
     *
     * @param clazz    要检查的类
     * @param pageable 分页请求对象
     * @return 分页请求中排序属性中不存在于类中的字段名称集合
     */
    public static Set<String> getIllegalSortProperties(Class<?> clazz, Pageable pageable) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return Set.of();
        }
        return getAllNotExistingFieldNames(
            clazz,
            pageable.getSort().stream().map(Sort.Order::getProperty).collect(Collectors.toSet())
        );
    }
}
