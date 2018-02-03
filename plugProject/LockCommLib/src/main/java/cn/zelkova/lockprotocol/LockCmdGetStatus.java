package cn.zelkova.lockprotocol;

/**
 * 获取锁状态
 *
 * @author zp
 */
public class LockCmdGetStatus extends LockCmdBase {

    private byte[] payloadBuffer = new byte[]{};
    /**
     * 获取锁状态
     */
    public LockCmdGetStatus(byte[] targetMac, BriefDate validFrom, BriefDate validTo) {
        super(targetMac, validFrom, validTo);
    }
    @Override
    public byte getCmdId() {
        return 0x03;
    }

    @Override
    public String getCmdName() {
        return "cmdGetStatus";
    }

    @Override
    protected byte[] getPayload() {
        return this.payloadBuffer;
    }

}
