package cn.zelkova.lockprotocol;

/**
 * 表示真个执行过程中的异常信息
 *
 * @author zp
 * Created by zp on 2016/6/21.
 */

public class LockException extends RuntimeException {
    public LockException(String detailMessage) {
        super(detailMessage);
    }

    public LockException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}