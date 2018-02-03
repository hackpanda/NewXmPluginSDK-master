package cn.zelkova.lockprotocol;

/**
 * 与蓝牙设备连接、发送、接受指令的超时异常
 *
 * @author zp
 * Created by zp on 2016/6/21.
 */
public class LockTimeoutException extends LockException {
    public LockTimeoutException(String detailMessage) {
        super(detailMessage);
    }

    public LockTimeoutException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}