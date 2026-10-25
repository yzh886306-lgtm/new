package com.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO（前端提交的参数）和实体分开的原因：前端只该改 name，
 * 如果直接用 Category 接收，前端多传一个 id/createTime 也会被绑定进来。
 */
@Data
public class CategoryDTO {
    @NotBlank(message = "分类名不能为空")
    @Size(max = 50, message = "分类名最多 50 个字")
    private String name;
}
