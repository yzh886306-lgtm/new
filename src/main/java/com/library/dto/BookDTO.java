package com.library.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增/修改图书的参数。注意没有 availableStock：可借数量由系统根据借还自动维护，
 * 前端只能改馆藏总量 totalStock，否则管理员可以随手把"可借数"改乱。
 */
@Data
public class BookDTO {
    @NotBlank(message = "ISBN 不能为空")
    @Size(max = 20, message = "ISBN 最多 20 位")
    private String isbn;

    @NotBlank(message = "书名不能为空")
    @Size(max = 100, message = "书名最多 100 个字")
    private String title;

    @Size(max = 50, message = "作者最多 50 个字")
    private String author;

    @NotNull(message = "请选择分类")
    private Long categoryId;

    @NotNull(message = "馆藏总量不能为空")
    @Min(value = 0, message = "馆藏总量不能为负数")
    private Integer totalStock;
}
