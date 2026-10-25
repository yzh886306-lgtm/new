package com.library.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 分页结果：前端画分页条需要两个数 —— 总条数 total（算总页数）和当前页数据 list。
 * 所以每次分页查询要发两条 SQL：SELECT COUNT(*) 和 SELECT ... LIMIT。
 */
@Data
@AllArgsConstructor
public class PageResult<T> {
    private long total;
    private List<T> list;
}
