package cn.zelkova.lockprotocol;

/**
 * 作为锁通讯协议返回内容的通用容器
 *
 * @author zp
 * Created by zp on 2016/6/14.
 */
public class LockCommResponse extends LockCommBase {

    private short mCmdId;

    LockCommResponse(){}

    void setCmdId(short value){
        this.mCmdId=value;
    }

    @Override
    public short getCmdId() {
        return this.mCmdId;
    }

    @Override
    public String getCmdName() {
        return "CommonResp";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
