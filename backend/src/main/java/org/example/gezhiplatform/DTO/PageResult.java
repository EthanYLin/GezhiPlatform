package org.example.gezhiplatform.DTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;

import java.util.Iterator;
import java.util.List;

/**
 * PageResult 是一个泛型类，用于封装分页查询的结果。
 * @param content 查询结果
 * @param pageNo 当前页码(从0开始)
 * @param pageSize 每页大小(用户请求的每页大小, 而非实际返回的每页大小)
 * @param totalPages 总页数
 * @param totalElements 总元素数
 * @param <T> 查询结果的类型
 */
public record PageResult<T> (
    List<T> content,
    Integer pageNo,
    Integer pageSize,
    Integer totalPages,
    Long totalElements
) implements Iterable<T> {
    public PageResult(Page<T> page) {
        this(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalPages(),
            page.getTotalElements()
            );
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return content.iterator();
    }

    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(page);
    }

    @JsonIgnore
    public boolean isFirst() {
        return pageNo == 0;
    }

    @JsonIgnore
    public boolean isLast() {
        return pageNo == totalPages - 1;
    }
}
