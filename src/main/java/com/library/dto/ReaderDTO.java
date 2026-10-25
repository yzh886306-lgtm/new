package com.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增/修改读者的参数。没有 status：冻结/解冻走单独接口，避免"改个手机号顺手把冻结解了" */
@Data
public class ReaderDTO {
    @NotBlank(message = "借书证号不能为空")
    @Size(max = 20, message = "借书证号最多 20 位")
    private String cardNo;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名最多 50 个字")
    private String name;

    /** 允许不填；填了就必须是 11 位手机号 */
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @NotNull(message = "最大可借数量不能为空")
    @Min(value = 1, message = "最大可借数量至少为 1")
    @Max(value = 20, message = "最大可借数量最多为 20")
    private Integer maxBorrow;
}
