package com.library.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 实体类：字段与 category 表的列一一对应，只用于和数据库之间搬运数据 */
@Data
public class Category {
    private Long id;
    private String name;
    private LocalDateTime createTime;
}
