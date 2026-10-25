-- =====================================================================
-- 图书借阅管理系统 建表脚本（每次启动执行，IF NOT EXISTS 保证可重复执行）
-- 设计约定：
--   1. 不建物理外键：关联完整性由 Service 层校验（互联网项目常见做法，
--      避免外键带来的级联锁和分库困难）；但关联列都建了索引。
--   2. 唯一性（ISBN、借书证号、分类名）交给数据库唯一索引兜底，
--      并发下两个请求同时通过了 Service 的检查，最终也只有一个能插入成功。
--   3. 库存拆成 total_stock（馆藏总量）和 available_stock（可借数量），
--      已借出数量 = total_stock - available_stock，不单独存，避免三个数不一致。
-- =====================================================================

CREATE TABLE IF NOT EXISTS category (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(50)  NOT NULL COMMENT '分类名',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '图书分类';

CREATE TABLE IF NOT EXISTS book (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    isbn            VARCHAR(20)  NOT NULL COMMENT 'ISBN 书号',
    title           VARCHAR(100) NOT NULL COMMENT '书名',
    author          VARCHAR(50)  COMMENT '作者',
    category_id     BIGINT       NOT NULL COMMENT '所属分类 id',
    total_stock     INT          NOT NULL DEFAULT 0 COMMENT '馆藏总量',
    available_stock INT          NOT NULL DEFAULT 0 COMMENT '可借数量',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_book_isbn (isbn),
    KEY idx_book_category (category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '图书';

CREATE TABLE IF NOT EXISTS reader (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    card_no     VARCHAR(20) NOT NULL COMMENT '借书证号',
    name        VARCHAR(50) NOT NULL COMMENT '姓名',
    phone       VARCHAR(11) COMMENT '手机号',
    max_borrow  INT         NOT NULL DEFAULT 5 COMMENT '最多同时借几本',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1 正常 0 冻结',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reader_card_no (card_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '读者';

CREATE TABLE IF NOT EXISTS borrow_record (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    book_id     BIGINT   NOT NULL COMMENT '图书 id',
    reader_id   BIGINT   NOT NULL COMMENT '读者 id',
    borrow_time DATETIME NOT NULL COMMENT '借出时间',
    due_time    DATETIME NOT NULL COMMENT '应还时间',
    return_time DATETIME COMMENT '实际归还时间，未还为 NULL',
    status      TINYINT  NOT NULL DEFAULT 0 COMMENT '0 借出中 1 已归还',
    PRIMARY KEY (id),
    -- 借书前要查"该读者未还几本、有没有逾期"，按 (reader_id, status) 建联合索引
    KEY idx_borrow_reader_status (reader_id, status),
    KEY idx_borrow_book (book_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '借阅记录';
