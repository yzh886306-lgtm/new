package com.library.common;

/**
 * 业务异常：用户操作不合规（库存不足、借书证冻结……），消息直接展示给用户。
 * 继承 RuntimeException 的原因：@Transactional 默认只在遇到 RuntimeException 时回滚，
 * Service 里一抛这个异常，前面已执行的 SQL（比如已扣的库存）会一起撤销。
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
