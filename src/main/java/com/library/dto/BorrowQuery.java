package com.library.dto;

import com.library.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 借阅记录分页查询条件：按读者、图书、状态筛选，都可不传 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BorrowQuery extends PageQuery {
    private Long readerId;
    private Long bookId;
    /** 0 借出中 1 已归还 */
    private Integer status;
}
