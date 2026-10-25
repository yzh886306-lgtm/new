package com.library.service;

import com.library.common.BusinessException;
import com.library.common.PageResult;
import com.library.dto.BookDTO;
import com.library.dto.BookQuery;
import com.library.entity.Book;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowRecordMapper;
import com.library.vo.BookVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final CategoryService categoryService;

    public PageResult<BookVO> page(BookQuery query) {
        long total = bookMapper.count(query);
        // 总数为 0 时第二条 SQL 没必要发
        if (total == 0) {
            return new PageResult<>(0, List.of());
        }
        return new PageResult<>(total, bookMapper.selectPage(query));
    }

    public Book getOrThrow(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        return book;
    }

    public Long create(BookDTO dto) {
        // 没有物理外键，"分类必须存在"由 Service 校验
        categoryService.getOrThrow(dto.getCategoryId());
        if (bookMapper.selectByIsbn(dto.getIsbn()) != null) {
            throw new BusinessException("ISBN 已存在");
        }
        Book book = toEntity(dto);
        bookMapper.insert(book);
        return book.getId();
    }

    public void update(Long id, BookDTO dto) {
        getOrThrow(id);
        categoryService.getOrThrow(dto.getCategoryId());
        Book sameIsbn = bookMapper.selectByIsbn(dto.getIsbn());
        if (sameIsbn != null && !sameIsbn.getId().equals(id)) {
            throw new BusinessException("ISBN 已存在");
        }
        Book book = toEntity(dto);
        book.setId(id);
        // 影响 0 行：图书存在（上面查过），只可能是 WHERE 里的库存条件不满足
        if (bookMapper.update(book) == 0) {
            throw new BusinessException("馆藏总量不能少于当前借出数量");
        }
    }

    public void delete(Long id) {
        getOrThrow(id);
        // 还有书没还就删掉图书，还书时就找不到这本书了
        if (borrowRecordMapper.countUnreturnedByBook(id) > 0) {
            throw new BusinessException("该图书还有未归还的借阅，不能删除");
        }
        bookMapper.deleteById(id);
    }

    private Book toEntity(BookDTO dto) {
        Book book = new Book();
        book.setIsbn(dto.getIsbn());
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setCategoryId(dto.getCategoryId());
        book.setTotalStock(dto.getTotalStock());
        return book;
    }
}
