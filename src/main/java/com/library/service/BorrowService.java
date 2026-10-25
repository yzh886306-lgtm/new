package com.library.service;

import com.library.common.BusinessException;
import com.library.common.PageResult;
import com.library.dto.BorrowDTO;
import com.library.dto.BorrowQuery;
import com.library.entity.BorrowRecord;
import com.library.entity.Reader;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowRecordMapper;
import com.library.vo.BorrowRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 借还书：本项目唯一一个"一次请求要改两张表"的业务。
 * 借书 = 扣库存（book） + 插借阅记录（borrow_record），两步必须同成同败，所以加 @Transactional。
 */
@Service
@RequiredArgsConstructor
public class BorrowService {

    private final BorrowRecordMapper borrowRecordMapper;
    private final BookMapper bookMapper;
    private final BookService bookService;
    private final ReaderService readerService;

    public PageResult<BorrowRecordVO> page(BorrowQuery query) {
        long total = borrowRecordMapper.count(query);
        if (total == 0) {
            return new PageResult<>(0, List.of());
        }
        return new PageResult<>(total, borrowRecordMapper.selectPage(query));
    }

    /**
     * 借书流程：校验读者 → 校验图书 → 扣库存 → 写借阅记录。
     * 校验顺序按"便宜的先查"：前面任何一步不通过就直接抛异常，后面的 SQL 都不会执行。
     */
    @Transactional
    public Long borrow(BorrowDTO dto) {
        // 1. 读者：存在、未冻结、无逾期、未超上限
        Reader reader = readerService.getOrThrow(dto.getReaderId());
        if (reader.getStatus() != 1) {
            throw new BusinessException("借书证已冻结");
        }
        if (borrowRecordMapper.countOverdueByReader(reader.getId()) > 0) {
            throw new BusinessException("有逾期未还的图书，请先归还");
        }
        if (borrowRecordMapper.countUnreturnedByReader(reader.getId()) >= reader.getMaxBorrow()) {
            throw new BusinessException("已达最大借阅数量 " + reader.getMaxBorrow() + " 本");
        }
        // 2. 图书：存在、同一本没在借着
        bookService.getOrThrow(dto.getBookId());
        if (borrowRecordMapper.countUnreturned(reader.getId(), dto.getBookId()) > 0) {
            throw new BusinessException("该读者已借阅此书且未归还");
        }
        // 3. 扣库存：判断写在 UPDATE 的 WHERE 里，影响 0 行 = 被别人借光了（不能用先查后改）
        if (bookMapper.decreaseStock(dto.getBookId()) == 0) {
            throw new BusinessException("库存不足");
        }
        // 4. 写借阅记录：这一步如果失败（比如数据库异常），第 3 步扣掉的库存会随事务回滚
        LocalDateTime now = LocalDateTime.now();
        BorrowRecord record = new BorrowRecord();
        record.setBookId(dto.getBookId());
        record.setReaderId(reader.getId());
        record.setBorrowTime(now);
        record.setDueTime(now.plusDays(dto.getDays()));
        borrowRecordMapper.insert(record);
        return record.getId();
    }

    /**
     * 还书流程：查记录 → 标记归还 → 加库存。
     * 先标记归还再加库存：markReturned 的 WHERE status = 0 保证同一条记录只有一个请求能走到加库存，
     * 反过来写的话，两个请求同时还书会把库存加两次。
     */
    @Transactional
    public void returnBook(Long recordId) {
        BorrowRecord record = borrowRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("借阅记录不存在");
        }
        if (borrowRecordMapper.markReturned(recordId) == 0) {
            throw new BusinessException("该记录已归还，请勿重复操作");
        }
        if (bookMapper.increaseStock(record.getBookId()) == 0) {
            // 走到这里说明数据已经不一致（图书被删或库存已满），抛异常让上一步的"标记归还"也回滚
            throw new BusinessException("图书库存异常，归还失败");
        }
    }
}
