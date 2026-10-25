package com.library.common;

import lombok.Data;

/**
 * 统一响应体：所有接口都返回 {code, msg, data}，前端只需判断 code 一处。
 * code 约定：200 成功；400 业务/参数错误（用户能看懂并改正）；500 系统异常（要查日志）。
 */
@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}
