package com.library.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Book {
    private Long id;
    private String isbn;
    private String title;
    private String author;
    private Long categoryId;
    private Integer totalStock;
    private Integer availableStock;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
