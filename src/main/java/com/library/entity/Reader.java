package com.library.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Reader {
    private Long id;
    private String cardNo;
    private String name;
    private String phone;
    private Integer maxBorrow;
    /** 1 正常 0 冻结 */
    private Integer status;
    private LocalDateTime createTime;
}
