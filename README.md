# 图书借阅管理系统（library-crud）

目标：用一个完整的小系统把 Java 后端 CRUD 的每一环走通——建表 → 写 SQL → 业务校验 → 接口 → 页面 → 测试 → Git。

技术栈：Spring Boot 3.2 + MyBatis（手写 XML SQL）+ MySQL 8；前端原生 HTML + fetch；测试用 H2 内存库。

---

## 一、工作流程主线：一次请求从头到尾怎么走

以"借书"为例，所有接口都走同一条线：

```
页面点"借出"
  → fetch POST /api/borrows  {bookId, readerId, days}          (index.html 的 api())
  → Controller：JSON 反序列化成 BorrowDTO + @Valid 校验参数      (BorrowController)
  → Service：业务规则校验 → 改库（一个事务）                      (BorrowService)
  → Mapper：接口方法 → 找到 XML 里同名 SQL → 执行                 (BorrowRecordMapper.xml)
  → 返回 Result{code, msg, data}；任何一步抛异常 → GlobalExceptionHandler 翻译成 Result
  → 页面判断 code：200 刷新列表，否则弹出 msg
```

每一层只做一件事：Controller 收参数/回结果，Service 判断"能不能做"，Mapper 只执行 SQL。

---

## 二、系统功能模块

| 模块 | 功能 | 接口 |
|---|---|---|
| 分类管理 | 列表、新增、修改、删除（有书不能删） | `GET/POST /api/categories`，`PUT/DELETE /api/categories/{id}` |
| 图书管理 | 分页+书名模糊+分类筛选、详情、新增、修改（总量不能少于借出数）、删除（有未还不能删） | `GET/POST /api/books`，`GET/PUT/DELETE /api/books/{id}` |
| 读者管理 | 分页+姓名/证号筛选、新增、修改、冻结/解冻、删除（有未还不能删） | `GET/POST /api/readers`，`GET/PUT/DELETE /api/readers/{id}`，`PATCH /api/readers/{id}/status?status=0` |
| 借阅管理 | 借书、还书、借阅记录分页（显示是否逾期） | `GET/POST /api/borrows`，`PUT /api/borrows/{id}/return` |

---

## 三、数据库设计

需求推出表：要管"书"，书要分门别类 → `book` + `category`；要知道"谁借了"→ `reader`；
"谁在什么时候借了哪本、还没还"是人和书之间的一次事件 → `borrow_record`（多对多的中间表，同时记录过程数据）。

```
category 1 ──< book 1 ──< borrow_record >── 1 reader
```

| 表 | 关键列 | 设计理由 |
|---|---|---|
| category | name 唯一 | 防重名，并发时由唯一索引兜底 |
| book | isbn 唯一；total_stock 馆藏总量；available_stock 可借数量 | 借出数 = total - available，不单独存，避免三个数对不上 |
| reader | card_no 唯一；max_borrow；status（1 正常 0 冻结） | 借书前的规则校验都依赖这几列 |
| borrow_record | status（0 借出中 1 已归还）；due_time；return_time | 联合索引 (reader_id, status) 支撑"该读者未还几本/有无逾期" |

约定：不建物理外键，关联完整性在 Service 校验（见 `CategoryService.delete`、`BookService.delete`）；
"是否逾期"不存表，查询时 `status = 0 AND due_time < NOW()` 当场算，因为它随时间变化。

建表脚本：[schema.sql](src/main/resources/schema.sql)，初始数据：[data.sql](src/main/resources/data.sql)（启动时自动执行，可重复执行）。

---

## 四、核心业务流程

### 借书（改两张表，一个事务）

```
读者存在？→ 未冻结？→ 无逾期？→ 未还数 < max_borrow？
  → 图书存在？→ 这本没在借着？
  → 扣库存  UPDATE book SET available_stock = available_stock - 1 WHERE id = ? AND available_stock > 0
        影响 0 行 → "库存不足"
  → 插入借阅记录
任何一步抛 BusinessException → @Transactional 回滚已扣的库存
```

关键点：扣库存不能"先 SELECT 判断再 UPDATE"，两个人同时借最后一本会超借；
把判断写进 UPDATE 的 WHERE，靠 InnoDB 行锁串行化（`ConcurrentStockTest` 用 10 个线程验证只成功 1 次）。

### 还书

```
记录存在？→ 标记归还 UPDATE ... SET status = 1 WHERE id = ? AND status = 0   影响 0 行 → "请勿重复操作"
          → 加库存   UPDATE ... WHERE id = ? AND available_stock < total_stock
```

先标记再加库存：标记这一步只有一个请求能成功，保证库存只被加一次。

### 修改馆藏总量

新可借 = 旧可借 + (新总量 − 旧总量)，结果 < 0 说明还有书没还回来，拒绝。计算与判断放在同一条 UPDATE 里（见 `BookMapper.xml` 的 `update` 注释，含 SET 顺序的坑）。

---

## 五、运行

1. 本机 MySQL 8 已启动；数据库 `library_crud` 会自动创建。
2. 设置 MySQL 密码环境变量（未设置时默认 `root`）：
   ```bash
   export MYSQL_PASSWORD=你的密码      # Windows PowerShell: $env:MYSQL_PASSWORD="你的密码"
   ```
3. 启动：`mvn spring-boot:run`，浏览器打开 http://localhost:8080
4. 测试（不需要 MySQL）：`mvn test`

## 六、代码阅读顺序

`schema.sql` → `entity` → `mapper/*.xml`（重点 `BookMapper.xml`、`BorrowRecordMapper.xml`）→ `service`（重点 `BorrowService`）→ `controller` → `common/GlobalExceptionHandler` → `static/index.html` → `BorrowServiceTest`
