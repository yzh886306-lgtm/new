package com.library.controller;

import com.library.common.PageResult;
import com.library.common.Result;
import com.library.dto.BorrowDTO;
import com.library.dto.BorrowQuery;
import com.library.service.BorrowService;
import com.library.vo.BorrowRecordVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 借还书不是简单的增删改：借书 = 新建一条借阅记录（POST），
 * 还书 = 把某条记录的状态改为已归还（PUT /{id}/return），记录本身永不删除，留作历史。
 */
@RestController
@RequestMapping("/api/borrows")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    @GetMapping
    public Result<PageResult<BorrowRecordVO>> page(BorrowQuery query) {
        return Result.ok(borrowService.page(query));
    }

    @PostMapping
    public Result<Long> borrow(@RequestBody @Valid BorrowDTO dto) {
        return Result.ok(borrowService.borrow(dto));
    }

    @PutMapping("/{id}/return")
    public Result<Void> returnBook(@PathVariable Long id) {
        borrowService.returnBook(id);
        return Result.ok();
    }
}
