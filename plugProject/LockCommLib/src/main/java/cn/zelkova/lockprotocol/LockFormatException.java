package cn.zelkova.lockprotocol;

/**
 * 表示协议格式的异常
 *
 * @author zp
 * Created by zp on 2016/6/13.
 */
public class LockFormatException extends LockException {
    public LockFormatException(String detailMessage) {
        super(detailMessage);
    }

    public LockFormatException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
