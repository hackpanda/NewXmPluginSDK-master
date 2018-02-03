package cn.zelkova.lockprotocol;


/**
 * Echoe指令
 *
 * @author zp
 */
public class LockCommEcho extends LockCommBase {

    public  static final short CMD_ID =0x012;

    /**
     * @param msg 要发送的内容
     */
    public LockCommEcho(String msg) {
        super.mKLVList.add((byte) 0x01, msg);
    }

    /**
     * @param value 要发送的内容
     */
    public LockCommEcho(byte[] value) {
        super.mKLVList.add((byte) 0x01, value);
    }

    @Override
    public short getCmdId() {
        return CMD_ID;
    }

    @Override
    public String getCmdName() {
        return "Echo Req&Resp";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }

}
