package com.library.dto;

import com.library.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 读者分页查询条件：姓名模糊 + 借书证号精确 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReaderQuery extends PageQuery {
    private String name;
    private String cardNo;
}
