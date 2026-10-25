package com.library.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：Controller/Service 只管抛异常，这里统一翻译成 Result。
 * 没有它，每个 Controller 方法都得 try-catch，且异常会以 Spring 默认的 HTML/JSON 错误页返回给前端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务规则不满足：消息原样给用户看 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.fail(400, e.getMessage());
    }

    /**
     * 参数校验失败：@Valid 校验 @RequestBody 抛 MethodArgumentNotValidException，
     * 校验查询参数对象抛 BindException；前者是后者的子类，所以这里一个处理器全接住。
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleValid(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().isEmpty()
                ? "参数错误"
                : e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return Result.fail(400, msg);
    }

    /** 唯一索引冲突：并发下两个请求都通过了 Service 的"是否已存在"检查，数据库兜底拦住第二个 */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicate(DuplicateKeyException e) {
        return Result.fail(400, "数据已存在，请勿重复提交");
    }

    /** 兜底：未预料的异常，打完整堆栈方便排查，但不把堆栈暴露给前端 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统繁忙，请稍后再试");
    }
}
