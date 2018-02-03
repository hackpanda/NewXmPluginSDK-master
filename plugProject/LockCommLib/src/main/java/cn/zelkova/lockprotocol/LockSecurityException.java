package cn.zelkova.lockprotocol;

/**
 * 表示安全性问题，如加解密失败
 *
 * @author zp
 * Created by zp on 2016/6/28.
 */
public class LockSecurityException extends LockException {
    public LockSecurityException(String detailMessage) {
        super(detailMessage);
    }

    public LockSecurityException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
