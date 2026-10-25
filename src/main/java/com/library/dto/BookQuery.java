package com.library.dto;

import com.library.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 图书分页查询条件：书名模糊 + 分类精确，两个条件都可不传 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BookQuery extends PageQuery {
    private String title;
    private Long categoryId;
}
