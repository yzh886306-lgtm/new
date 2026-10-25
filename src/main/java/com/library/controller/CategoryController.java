package com.library.controller;

import com.library.common.Result;
import com.library.dto.CategoryDTO;
import com.library.entity.Category;
import com.library.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RESTful 风格：同一个资源路径 /api/categories，用 HTTP 方法区分动作
 *   GET 查  POST 增  PUT 改  DELETE 删
 * Controller 只做三件事：收参数（+校验）→ 调 Service → 包成 Result 返回。
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public Result<List<Category>> list() {
        return Result.ok(categoryService.list());
    }

    /** @RequestBody：从请求体 JSON 反序列化；@Valid：触发 DTO 上的 @NotBlank 等校验，失败抛异常交给全局处理器 */
    @PostMapping
    public Result<Long> create(@RequestBody @Valid CategoryDTO dto) {
        return Result.ok(categoryService.create(dto));
    }

    /** @PathVariable：取 URL 路径里的 {id} */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid CategoryDTO dto) {
        categoryService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}
