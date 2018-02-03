package cn.zelkova.lockprotocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 更新开门密码有效周期
 *
 * @author liwenqi
 * Created by liwenqi on 2016/12/05.
 */
public class LockCmdSyncPwdPeriod extends LockCmdBase {


    int pwdIdx;
    BriefDate vFromNew;
    BriefDate vToNew;
    /**
     * 更新开门密码有效周期
     */
    public LockCmdSyncPwdPeriod(byte[] targetMac, BriefDate validFrom, BriefDate validTo, String lockid) {
        super(targetMac, validFrom, validTo, lockid);
    }
    @Override
    public byte getCmdId() {
        return 0x0B;
    }

    @Override
    public String getCmdName() {
        return "LockCmdSyncPwdPeriod";
    }

    /*
    *
    * 设置参数
    * */
    public void setPwdPeriod(int pwdIdx, BriefDate vFromNew, BriefDate vToNew){
        this.pwdIdx = pwdIdx;
        this.vFromNew = vFromNew;
        this.vToNew = vToNew;
    }
    @Override
    protected byte[] getPayload() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianOutputStream out = new LittleEndianOutputStream(baos);

        try {
            out.writeShort(this.pwdIdx);

            out.write(this.vFromNew.toProtocolByte());
            out.write(this.vToNew.toProtocolByte());
        } catch (IOException e) {
            e.printStackTrace();
            throw new LockFormatException("LockCmdSetZotpPeriod:" + e.getMessage(), e);
        }

        return baos.toByteArray();
    }
}
