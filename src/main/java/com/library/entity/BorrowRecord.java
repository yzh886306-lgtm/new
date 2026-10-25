package com.library.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BorrowRecord {
    private Long id;
    private Long bookId;
    private Long readerId;
    private LocalDateTime borrowTime;
    private LocalDateTime dueTime;
    /** 未还为 null */
    private LocalDateTime returnTime;
    /** 0 借出中 1 已归还 */
    private Integer status;
}
