package org.example.gezhiplatform.DTO.archive;

import java.util.Set;

/**
 * 学生档案中允许访问的JSON Path响应体
 * <p>
 * 用于返回用户对档案数据的读写权限信息，包含可读和可写的JSON Path集合。
 * </p>
 * @param readableJsonPaths 可读的JSON Path集合
 * @param writableJsonPaths 可写的JSON Path集合
 */
public record AllowedJsonPathsResponse(
    Set<String> readableJsonPaths,
    Set<String> writableJsonPaths
) {}
