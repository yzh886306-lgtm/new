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

设计顺序：**先走一遍业务流程 → 每一步要数据库回答什么问题 → 问题决定表和列 → SQL 决定索引 → 不变量决定约束**。
建表脚本：[schema.sql](src/main/resources/schema.sql)，初始数据：[data.sql](src/main/resources/data.sql)（启动时自动执行，可重复执行）。

### 3.1 从业务流程推出"要存什么"

图书馆一天的完整流程，每一步都会向数据库提一个问题：

| 流程步骤 | 数据库要回答的问题 | 落到哪里 |
|---|---|---|
| ① 管理员建分类 | 有哪些分类？名字有没有重复？ | `category.name` + 唯一索引 |
| ② 新书入库 | 这本书是哪本（全国唯一编号）？属于哪类？馆里有几本？ | `book.isbn`、`book.category_id`、`book.total_stock` |
| ③ 办借书证 | 这个人是谁？证号有没有重复？最多能借几本？ | `reader.card_no`、`reader.max_borrow` |
| ④ 读者来借书 | 证有没有被冻结？ | `reader.status` |
|  | 他手上现在有几本没还？有没有已过期没还的？ | `borrow_record` 按 `reader_id + status (+ due_time)` 统计 |
|  | 这本书现在还能借出去几本？ | `book.available_stock` |
| ⑤ 登记借出 | 谁、哪本、什么时候借、哪天前要还？ | `borrow_record` 新增一行：`reader_id, book_id, borrow_time, due_time` |
| ⑥ 还书 | 这条借阅还了没？哪天还的？ | `borrow_record.status`、`return_time` |
| ⑦ 管理员查账 | 一本书被借了几次？谁逾期了？ | `borrow_record` 联表 `book`、`reader` |

从这张表能直接看出 4 个"名词"：分类、图书、读者是**长期存在的东西**；借阅是**人和书之间发生的一次事件**，有开始（借）有结束（还）。

### 3.2 表之间的关系

```
category 1 ──< N book 1 ──< N borrow_record N >── 1 reader
```

- 一个分类下有多本书，一本书只属于一个分类 → `book` 里存 `category_id`（"多"的一方存"一"的一方的 id）。
- 读者和图书是多对多：一个人借多本书，一本书被多个人借过 → 需要中间表 `borrow_record`。
- **`borrow_record` 不是普通中间表**：普通中间表（如"用户-角色"）一对组合只存一行，可以建 `UNIQUE(reader_id, book_id)`；
  但同一个人可以借同一本书很多次（借了还、还了再借），每次都是一条独立历史，所以**不能**建这个唯一索引，
  它有自己的主键和生命周期（借出中 → 已归还）。

### 3.3 逐表字段设计

**category 图书分类**

| 列 | 类型 | 约束 | 为什么 |
|---|---|---|---|
| id | BIGINT 自增 | 主键 | 统一用 BIGINT，不用担心 INT 21 亿上限；自增保证按插入顺序追加到 B+ 树末尾，插入不引起页分裂 |
| name | VARCHAR(50) | NOT NULL，UNIQUE | 分类名不能重复；唯一索引是并发下的最后防线（见 3.5） |
| create_time | DATETIME | 默认 CURRENT_TIMESTAMP | 由数据库填，Java 不用管 |

**book 图书**

| 列 | 类型 | 约束 | 为什么 |
|---|---|---|---|
| isbn | VARCHAR(20) | NOT NULL，UNIQUE | 用业务编号判断"是不是同一本书"；不用 BIGINT 是因为 ISBN 可能带 `-`、`X`，且不参与计算 |
| title | VARCHAR(100) | NOT NULL | 列表要按书名模糊搜 |
| author | VARCHAR(50) | 可空 | 有的书作者不详，允许为空 |
| category_id | BIGINT | NOT NULL，普通索引 | 指向分类；必须有分类 |
| total_stock | INT | NOT NULL 默认 0 | 馆藏总量，只在管理员修改图书时变 |
| available_stock | INT | NOT NULL 默认 0 | 可借数量，只在借书（-1）还书（+1）时变 |
| create_time / update_time | DATETIME | update_time 带 ON UPDATE | 每次 UPDATE 由数据库自动刷新修改时间 |

库存为什么存**两个数**，而不是存一个"已借出数"或三个数都存：
- 只存 total：每次借书都要 `COUNT(*)` 借阅表算已借出几本，慢，且无法用一条 UPDATE 原子地"判断+扣减"。
- 三个数都存（总量、可借、已借出）：任何一个漏改就三个数对不上。
- 存 total + available：已借出 = total − available 现算；借书只改 available 一列，一条条件 UPDATE 就能防超借。
  需要永远成立的规则只有一条：**0 ≤ available_stock ≤ total_stock**（3.5 讲谁来保证）。

**reader 读者**

| 列 | 类型 | 约束 | 为什么 |
|---|---|---|---|
| card_no | VARCHAR(20) | NOT NULL，UNIQUE | 借书证号，业务上的唯一身份 |
| name | VARCHAR(50) | NOT NULL | 允许重名，所以**不**建唯一索引 |
| phone | VARCHAR(11) | 可空 | 手机号用字符串：不做加减运算，且数字类型会丢掉开头的 0 |
| max_borrow | INT | NOT NULL 默认 5 | 每个读者的借书上限可以不同（如老师 10 本、学生 5 本），所以放在读者身上而不是写死在代码里 |
| status | TINYINT | NOT NULL 默认 1 | 1 正常 0 冻结；TINYINT 占 1 字节，加新状态只改代码不改表（ENUM 加值要 ALTER TABLE） |

**borrow_record 借阅记录**

| 列 | 类型 | 约束 | 为什么 |
|---|---|---|---|
| book_id / reader_id | BIGINT | NOT NULL | 谁借了哪本 |
| borrow_time | DATETIME | NOT NULL | 借出时间，由 Java 传入（和 due_time 用同一个 now 计算，保证正好相差 N 天） |
| due_time | DATETIME | NOT NULL | 应还时间；借书时就算好存下，因为每次借的天数可以不同 |
| return_time | DATETIME | 可空 | 没还就是 NULL；还书时填 NOW() |
| status | TINYINT | NOT NULL 默认 0 | 0 借出中 1 已归还 |

- 有了 return_time 为什么还要 status？`return_time IS NULL` 也能表示没还，但 `IS NULL` 不如 `status = 0` 直观，
  且以后加"丢失""损坏"等状态时 return_time 表达不了。
- **"是否逾期"不存列**：逾期取决于"现在几点"，今天没逾期明天就逾期了，存下来就会过时，要靠定时任务刷新。
  所以查询时当场算：`status = 0 AND due_time < NOW()`。
- 借阅记录**只增不删**，还书只是改状态，所有历史都保留。

公共约定：时间统一用 DATETIME（TIMESTAMP 只到 2038 年，且会按会话时区转换）；字符集 utf8mb4（utf8 存不了 emoji 和部分生僻字）；引擎 InnoDB（支持事务和行锁，借书的事务和条件 UPDATE 都依赖它）。

### 3.4 索引设计：从 SQL 反推

索引不是凭感觉加的，而是把 Mapper 里每条 SQL 的 WHERE 列出来，看谁需要：

| SQL（所在方法） | WHERE 条件 | 用到的索引 |
|---|---|---|
| 按名字查分类 `selectByName` | `name = ?` | `uk_category_name` |
| 按 ISBN 查书 `selectByIsbn` | `isbn = ?` | `uk_book_isbn` |
| 删分类前数书 `countByCategoryId` | `category_id = ?` | `idx_book_category`（COUNT 只读索引树，不回表） |
| 图书分页按分类筛 `selectPage` | `category_id = ?` | `idx_book_category` |
| 图书分页按书名搜 `selectPage` | `title LIKE '%x%'` | **用不上索引**（前面有 `%`，B+ 树不知道从哪开始找），数据量小时全表扫描可接受，量大要换全文索引或 ES |
| 按证号查读者 `selectByCardNo` | `card_no = ?` | `uk_reader_card_no` |
| 读者未还几本 `countUnreturnedByReader` | `reader_id = ? AND status = 0` | `idx_borrow_reader_status` 两列全用上 |
| 读者有无逾期 `countOverdueByReader` | `reader_id = ? AND status = 0 AND due_time < NOW()` | 同上，先用索引定位到该读者未还的几行，再逐行判断 due_time |
| 是否已借同一本 `countUnreturned` | `reader_id = ? AND status = 0 AND book_id = ?` | 同上 |
| 图书有无未还 `countUnreturnedByBook` | `book_id = ? AND status = 0` | `idx_borrow_book` |
| 借阅记录按读者筛 `selectPage` | `reader_id = ?` | `idx_borrow_reader_status`（最左前缀：只用第一列也能命中） |

为什么是 `(reader_id, status)` 联合索引而不是两个单列索引：
- 借书时这三条统计 SQL 每次都要跑，条件都是"某读者 + 未还"。联合索引按 reader_id 排好序，同一读者内再按 status 排序，能直接定位到"张三的未还记录"这一小段。
- 顺序不能反过来：`status` 只有 0/1 两个值，放前面区分度太低；而且 `(status, reader_id)` 无法服务"只按读者查"的分页筛选。
- 单独给 status 建索引没有意义：一半数据是 0、一半是 1，走索引还要回表，优化器通常直接选全表扫描。

### 3.5 数据一致性：每条规则由谁保证

不建物理外键后，"数据不出错"由四层分担，越往下越可靠：

| 必须成立的规则 | Service 校验（友好提示） | 条件 UPDATE（原子判断） | 事务（同成同败） | 数据库约束（最后防线） |
|---|---|---|---|---|
| 分类名 / ISBN / 证号不重复 | 先查有没有同名 | | | UNIQUE 索引 |
| 可借数量不小于 0（不超借） | | `WHERE available_stock > 0` | | |
| 可借数量不超过总量（不重复加） | | `WHERE available_stock < total_stock` | | |
| 改总量后可借不为负 | | `WHERE available_stock + (新总量 - total_stock) >= 0` | | |
| 一条借阅只能还一次 | | `WHERE status = 0` | | |
| 扣库存和写借阅记录同时成功 | | | `@Transactional` | |
| 书必须属于存在的分类 | `categoryService.getOrThrow` | | | （外键可保证，本项目不建） |
| 有书的分类、有未还的书和读者不能删 | 先 COUNT 再删 | | | （外键可保证，本项目不建） |

为什么"先查再判断"挡不住并发，必须靠后两列：两个请求同时查，都看到"没重复 / 有库存"，都往下执行，结果都写进去了。
条件 UPDATE 把"判断"和"修改"合成一条语句，InnoDB 给这一行加锁，第二个请求要等第一个提交后重新判断；唯一索引则让第二条 INSERT 直接失败。

为什么不建物理外键：外键在每次插入/删除时都要去另一张表检查并加锁，高并发下锁冲突多；分库分表后两张表不在同一个库，外键根本建不了。
代价就是上表最后两行只能靠 Service 校验，存在并发缺口（见 3.6）。

### 3.6 已知缺口（思考题，暂不修复）

这些是当前设计**挡不住**的情况，留给你分析原因和改法：

1. **同一读者并发借书可能超过上限**：读者还差 1 本到上限，同时发两个借书请求，两个都 `COUNT` 到"未达上限"，都借成功。
   提示：事务开始时 `SELECT ... FROM reader WHERE id = ? FOR UPDATE` 锁住读者行，让同一读者的借书请求排队。
2. **同一读者并发借同一本书**：原因同上。另一种思路：加一列"借出中标记"（借出中为 1、归还后改为 NULL），
   建 `UNIQUE(reader_id, book_id, 标记)`——唯一索引允许多个 NULL，所以历史记录不冲突，借出中的只能有一条。
3. **删分类时恰好有人往这个分类加书**：`countByCategoryId` 查到 0，删除前另一个请求插入了一本该分类的书，结果书指向了不存在的分类。
4. **删除图书后历史记录书名变空**：图书是物理删除，借阅记录 `LEFT JOIN book` 查不到书名。
   改法：图书改为逻辑删除（加 `deleted` 列），或在借阅记录里冗余一份借书时的书名。
5. **深分页变慢**：`LIMIT 10 OFFSET 100000` 要先扫过前 10 万行再丢掉。改法：按上一页最后一条的 id 继续查 `WHERE id < ? ORDER BY id DESC LIMIT 10`。

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
