package com.library.service;

import com.library.common.BusinessException;
import com.library.common.PageResult;
import com.library.dto.ReaderDTO;
import com.library.dto.ReaderQuery;
import com.library.entity.Reader;
import com.library.mapper.BorrowRecordMapper;
import com.library.mapper.ReaderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReaderService {

    private final ReaderMapper readerMapper;
    private final BorrowRecordMapper borrowRecordMapper;

    public PageResult<Reader> page(ReaderQuery query) {
        long total = readerMapper.count(query);
        if (total == 0) {
            return new PageResult<>(0, List.of());
        }
        return new PageResult<>(total, readerMapper.selectPage(query));
    }

    public Reader getOrThrow(Long id) {
        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new BusinessException("读者不存在");
        }
        return reader;
    }

    public Long create(ReaderDTO dto) {
        if (readerMapper.selectByCardNo(dto.getCardNo()) != null) {
            throw new BusinessException("借书证号已存在");
        }
        Reader reader = toEntity(dto);
        readerMapper.insert(reader);
        return reader.getId();
    }

    public void update(Long id, ReaderDTO dto) {
        getOrThrow(id);
        Reader sameCard = readerMapper.selectByCardNo(dto.getCardNo());
        if (sameCard != null && !sameCard.getId().equals(id)) {
            throw new BusinessException("借书证号已存在");
        }
        Reader reader = toEntity(dto);
        reader.setId(id);
        readerMapper.update(reader);
    }

    /** 冻结 / 解冻：status 只允许 0 或 1 */
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态只能是 0（冻结）或 1（正常）");
        }
        getOrThrow(id);
        readerMapper.updateStatus(id, status);
    }

    public void delete(Long id) {
        getOrThrow(id);
        // 读者手里还有书就删掉，这些书就再也追不回来了
        if (borrowRecordMapper.countUnreturnedByReader(id) > 0) {
            throw new BusinessException("该读者还有未归还的图书，不能删除");
        }
        readerMapper.deleteById(id);
    }

    private Reader toEntity(ReaderDTO dto) {
        Reader reader = new Reader();
        reader.setCardNo(dto.getCardNo());
        reader.setName(dto.getName());
        reader.setPhone(dto.getPhone());
        reader.setMaxBorrow(dto.getMaxBorrow());
        return reader;
    }
}
