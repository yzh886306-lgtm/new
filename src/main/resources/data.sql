-- 初始数据：显式指定主键 + INSERT IGNORE，主键已存在就跳过，所以每次启动重复执行也不会重复插入

INSERT IGNORE INTO category (id, name) VALUES
    (1, '计算机'),
    (2, '文学'),
    (3, '历史');

INSERT IGNORE INTO book (id, isbn, title, author, category_id, total_stock, available_stock) VALUES
    (1, '9787111213826', 'Java编程思想', 'Bruce Eckel', 1, 3, 3),
    (2, '9787115545381', 'MySQL是怎样运行的', '小孩子4919', 1, 2, 2),
    (3, '9787121360978', '深入理解Java虚拟机', '周志明', 1, 1, 1),
    (4, '9787020002207', '红楼梦', '曹雪芹', 2, 2, 2),
    (5, '9787101003048', '史记', '司马迁', 3, 1, 1);

INSERT IGNORE INTO reader (id, card_no, name, phone, max_borrow, status) VALUES
    (1, 'R0001', '张三', '13800000001', 5, 1),
    (2, 'R0002', '李四', '13800000002', 2, 1),
    (3, 'R0003', '王五', '13800000003', 5, 0);
