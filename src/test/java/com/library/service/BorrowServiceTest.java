package com.library.service;

import com.library.common.BusinessException;
import com.library.dto.BookDTO;
import com.library.dto.BorrowDTO;
import com.library.entity.Book;
import com.library.mapper.BookMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 借还书业务测试（H2 内存库，初始数据见 data.sql）。
 * 类上 @Transactional：每个测试方法结束自动回滚，测试之间互不污染。
 */
@SpringBootTest
@Transactional
class BorrowServiceTest {

    @Autowired
    BorrowService borrowService;
    @Autowired
    BookService bookService;
    @Autowired
    BookMapper bookMapper;

    private BorrowDTO dto(long bookId, long readerId) {
        BorrowDTO dto = new BorrowDTO();
        dto.setBookId(bookId);
        dto.setReaderId(readerId);
        return dto;
    }

    @Test
    void 借书扣库存_还书加库存() {
        Long recordId = borrowService.borrow(dto(1, 1));
        assertEquals(2, bookMapper.selectById(1L).getAvailableStock());

        borrowService.returnBook(recordId);
        assertEquals(3, bookMapper.selectById(1L).getAvailableStock());
    }

    @Test
    void 重复还书被拦住() {
        Long recordId = borrowService.borrow(dto(1, 1));
        borrowService.returnBook(recordId);
        BusinessException e = assertThrows(BusinessException.class, () -> borrowService.returnBook(recordId));
        assertEquals("该记录已归还，请勿重复操作", e.getMessage());
        assertEquals(3, bookMapper.selectById(1L).getAvailableStock());
    }

    @Test
    void 库存为0不能借() {
        // 图书 3 只有 1 本
        borrowService.borrow(dto(3, 1));
        BusinessException e = assertThrows(BusinessException.class, () -> borrowService.borrow(dto(3, 2)));
        assertEquals("库存不足", e.getMessage());
    }

    @Test
    void 冻结读者不能借() {
        BusinessException e = assertThrows(BusinessException.class, () -> borrowService.borrow(dto(1, 3)));
        assertEquals("借书证已冻结", e.getMessage());
    }

    @Test
    void 超过最大借阅数不能借() {
        // 读者 2 最多借 2 本
        borrowService.borrow(dto(1, 2));
        borrowService.borrow(dto(2, 2));
        BusinessException e = assertThrows(BusinessException.class, () -> borrowService.borrow(dto(4, 2)));
        assertEquals("已达最大借阅数量 2 本", e.getMessage());
    }

    @Test
    void 同一本书未还不能重复借() {
        borrowService.borrow(dto(1, 1));
        BusinessException e = assertThrows(BusinessException.class, () -> borrowService.borrow(dto(1, 1)));
        assertEquals("该读者已借阅此书且未归还", e.getMessage());
    }

    @Test
    void 馆藏总量不能改到少于借出数量() {
        // 图书 1 总量 3，借出 2 本后改总量为 1 → 拒绝；改为 2 → 可借变 0
        borrowService.borrow(dto(1, 1));
        borrowService.borrow(dto(1, 2));
        Book old = bookMapper.selectById(1L);
        BookDTO update = new BookDTO();
        update.setIsbn(old.getIsbn());
        update.setTitle(old.getTitle());
        update.setCategoryId(old.getCategoryId());

        update.setTotalStock(1);
        assertThrows(BusinessException.class, () -> bookService.update(1L, update));

        update.setTotalStock(2);
        bookService.update(1L, update);
        Book now = bookMapper.selectById(1L);
        assertEquals(2, now.getTotalStock());
        assertEquals(0, now.getAvailableStock());
    }

    @Test
    void 有未还借阅的图书不能删() {
        borrowService.borrow(dto(1, 1));
        assertThrows(BusinessException.class, () -> bookService.delete(1L));
    }
}

/**
 * 并发扣库存测试：单独一个类且不加 @Transactional，
 * 因为多线程各自开事务、各自提交，才能真正模拟"多人同时借最后一本"。
 */
@SpringBootTest
class ConcurrentStockTest {

    @Autowired
    BookMapper bookMapper;

    @Test
    void 十个线程抢1本书_只有一个成功() throws Exception {
        // 图书 5 库存 1
        int threads = 10;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return bookMapper.decreaseStock(5L);
            }));
        }
        start.countDown();
        int success = 0;
        for (Future<Integer> f : results) {
            success += f.get();
        }
        pool.shutdown();
        try {
            assertEquals(1, success);
            assertEquals(0, bookMapper.selectById(5L).getAvailableStock());
        } finally {
            bookMapper.increaseStock(5L); // 恢复数据，避免影响其他测试
        }
    }
}
