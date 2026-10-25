package com.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 借书参数：谁（readerId）借哪本（bookId）借几天（days） */
@Data
public class BorrowDTO {
    @NotNull(message = "请选择图书")
    private Long bookId;

    @NotNull(message = "请选择读者")
    private Long readerId;

    @Min(value = 1, message = "借阅天数至少 1 天")
    @Max(value = 60, message = "借阅天数最多 60 天")
    private Integer days = 30;
}
