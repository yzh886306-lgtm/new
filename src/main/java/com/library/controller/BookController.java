package com.library.controller;

import com.library.common.PageResult;
import com.library.common.Result;
import com.library.dto.BookDTO;
import com.library.dto.BookQuery;
import com.library.entity.Book;
import com.library.service.BookService;
import com.library.vo.BookVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /** 查询参数对象不加注解：Spring 把 ?page=1&size=10&title=Java 按属性名逐个绑定进 BookQuery */
    @GetMapping
    public Result<PageResult<BookVO>> page(BookQuery query) {
        return Result.ok(bookService.page(query));
    }

    @GetMapping("/{id}")
    public Result<Book> get(@PathVariable Long id) {
        return Result.ok(bookService.getOrThrow(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody @Valid BookDTO dto) {
        return Result.ok(bookService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid BookDTO dto) {
        bookService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return Result.ok();
    }
}
