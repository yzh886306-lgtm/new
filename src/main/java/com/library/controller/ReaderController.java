package com.library.controller;

import com.library.common.PageResult;
import com.library.common.Result;
import com.library.dto.ReaderDTO;
import com.library.dto.ReaderQuery;
import com.library.entity.Reader;
import com.library.service.ReaderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/readers")
@RequiredArgsConstructor
public class ReaderController {

    private final ReaderService readerService;

    @GetMapping
    public Result<PageResult<Reader>> page(ReaderQuery query) {
        return Result.ok(readerService.page(query));
    }

    @GetMapping("/{id}")
    public Result<Reader> get(@PathVariable Long id) {
        return Result.ok(readerService.getOrThrow(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody @Valid ReaderDTO dto) {
        return Result.ok(readerService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid ReaderDTO dto) {
        readerService.update(id, dto);
        return Result.ok();
    }

    /**
     * 冻结/解冻只改一个字段，用 PATCH（局部更新）区别于 PUT（整体替换）。
     * @RequestParam：取 ?status=0 这种查询参数
     */
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        readerService.updateStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        readerService.delete(id);
        return Result.ok();
    }
}
