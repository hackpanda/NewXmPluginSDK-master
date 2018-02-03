package cn.zelkova.lockprotocol;

import java.util.Date;

/**
 * 设置或仅获取锁内时间指令
 *
 * @author lwq
 * Created by liwenqi on 2016/6/25.
 */
public class LockCommSyncTime extends LockCommBase {
    private boolean wantToSetTime;
    /**
     *仅获取锁内时间
     */
    public LockCommSyncTime() {
        this(new byte[0]);

        this.wantToSetTime =false;
    }
    /**
     *设置时间，并获取锁内时间
     * @param cmd 从服务器获得的设置时间命令
     */
    public LockCommSyncTime(byte[] cmd) {
        this(BriefDate.fromNature(new Date()), cmd);
    }

    /**
     *设置时间，并获取锁内时间
     * @param clientTime 控制设备的本地时间
     * @param cmd 从服务器获得的设置时间命令
     */
    public LockCommSyncTime(BriefDate clientTime, byte[] cmd) {
        super.mKLVList.add((byte) 0x01, clientTime.toProtocolByte());//time 4字节，授时时间，见日期表示法
        super.mKLVList.add((byte) 0x02, cmd);

        this.wantToSetTime =true;
    }

    @Override
    public short getCmdId() {
        return 0x0E;
    }

    @Override
    public String getCmdName() {
        return "SyncTime";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }

}

