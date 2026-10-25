package com.library.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * VO（返回给前端的对象）和实体分开的原因：列表页要显示"分类名"，
 * 而 book 表里只有 category_id —— 需要联表查出来的字段放在 VO 里，实体保持和表一一对应。
 */
@Data
public class BookVO {
    private Long id;
    private String isbn;
    private String title;
    private String author;
    private Long categoryId;
    private String categoryName;
    private Integer totalStock;
    private Integer availableStock;
    private LocalDateTime createTime;
}
