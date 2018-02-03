package cn.zelkova.lockprotocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 交换密钥返回命令
 *
 * @author zp
 */
public class LockCmdExSecurityKeyResponse extends LockCmdResponse {
    /*
     * 这个类要与 LockCmdExSecurityKey保持一致，他们两个只差一个resultCode
     * 但是由于逻辑含义不同，所以分开写了，所以在维护的时候要保持一致
     */

    public  static final byte CMD_ID =0x07;

    public LockCmdExSecurityKeyResponse(
            byte[] targetMac, BriefDate validFrom, BriefDate validTo,
            short resultCode, byte[] secKey, String lockid) {
        super(targetMac, validFrom, validTo, resultCode, lockid);

        this.secKey =secKey;
    }

    LockCmdExSecurityKeyResponse(){}

    private byte[] secKey;

    public byte[] getSecKey() {
        return this.secKey;
    }

    public byte getSecKeyLen() {
        return (byte) this.secKey.length;
    }

    @Override
    protected void parsePayload(LittleEndianInputStream input) {
        try {
            byte secKeyLen =input.readByte();
            this.secKey = new byte[secKeyLen];
            input.read(this.secKey);
        } catch (IOException e) {
            e.printStackTrace();//EOF会么?其他我觉得这个地方不应该有什么错误
            throw new LockFormatException("解析Comm时异常:" + e.getMessage(), e);
        }
    }

    @Override
    public byte getCmdId() { return CMD_ID; }

    @Override
    public String getCmdName() { return "exSecKeyResp"; }

    @Override
    protected byte[] getPayload() {

        ByteArrayOutputStream baos =new ByteArrayOutputStream();
        LittleEndianOutputStream out =new LittleEndianOutputStream(baos);

        try {
            out.write(super.getPayload());  //resultCode

            out.writeByte(getSecKeyLen());
            out.write(getSecKey());
        } catch (IOException e) {
            e.printStackTrace();
            throw new LockFormatException("LockCmdSetTime:"+e.getMessage(), e);
        }

        return baos.toByteArray();
    }
}
