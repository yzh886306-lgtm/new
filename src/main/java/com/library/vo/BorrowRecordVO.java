package com.library.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 借阅记录列表项：联表带出书名、读者姓名，并由 SQL 算出是否逾期 */
@Data
public class BorrowRecordVO {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private Long readerId;
    private String readerName;
    private String cardNo;
    private LocalDateTime borrowTime;
    private LocalDateTime dueTime;
    private LocalDateTime returnTime;
    /** 0 借出中 1 已归还 */
    private Integer status;
    /** 借出中且已过应还时间 */
    private Boolean overdue;
}
