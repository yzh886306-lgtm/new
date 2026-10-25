package com.library.common;

import lombok.Data;

/**
 * 分页查询参数基类：前端传 page（第几页，从 1 开始）和 size（每页几条）。
 * 数据库要的是 OFFSET（跳过几条），换算公式 offset = (page - 1) * size，
 * MyBatis 在 XML 里写 #{offset} 时会调用 getOffset()，所以不需要真的有 offset 字段。
 */
@Data
public class PageQuery {
    private int page = 1;
    private int size = 10;

    public int getOffset() {
        return (Math.max(page, 1) - 1) * getSize();
    }

    /** 限制每页最多 100 条，防止前端传 size=100000 一次拖垮数据库 */
    public int getSize() {
        return Math.min(Math.max(size, 1), 100);
    }
}
