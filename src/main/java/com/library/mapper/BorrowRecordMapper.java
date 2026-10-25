package com.library.mapper;

import com.library.dto.BorrowQuery;
import com.library.entity.BorrowRecord;
import com.library.vo.BorrowRecordVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BorrowRecordMapper {
    long count(BorrowQuery query);

    List<BorrowRecordVO> selectPage(BorrowQuery query);

    BorrowRecord selectById(Long id);

    /** 某读者当前未还的数量：借书前判断是否超过 max_borrow */
    long countUnreturnedByReader(Long readerId);

    /** 某读者当前逾期未还的数量：有逾期就不许再借 */
    long countOverdueByReader(Long readerId);

    /** 某本书当前未还的数量：删除图书前检查 */
    long countUnreturnedByBook(Long bookId);

    /** 同一读者是否已借着这本书没还：不允许重复借同一本 */
    long countUnreturned(@Param("readerId") Long readerId, @Param("bookId") Long bookId);

    int insert(BorrowRecord record);

    /** 标记归还；WHERE status = 0 保证同一条记录只能还一次，返回 0 表示已经还过了 */
    int markReturned(Long id);
}
