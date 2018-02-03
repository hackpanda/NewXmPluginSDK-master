package cn.zelkova.lockprotocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


/**
 * 密钥交换锁命令
 *
 * @author zp
 */
public class LockCmdExSecurityKey extends LockCmdBase {

    public  static final byte CMD_ID =0x05;

    /**
     * @param securityKey 要交换的密钥
     */
    public LockCmdExSecurityKey(
            byte[] targetMac, BriefDate validFrom, BriefDate validTo,
            byte[] securityKey, String lockid)
    {
        super(targetMac, validFrom, validTo, lockid);

        if( securityKey.length<=0)
            throw new IllegalArgumentException("密钥长度不能为0");
        if(securityKey.length > keyLen )
            throw new IllegalArgumentException("密钥长度需要["+keyLen+"]但有"+securityKey.length);

        this.secKey =securityKey;
    }

    public static final int keyLen =16; //密钥长度
    private byte[] secKey;

    @Override
    public byte getCmdId() {
        return CMD_ID;
    }

    @Override
    public String getCmdName() {
        return "exSecKey";
    }

    @Override
    protected byte[] getPayload() {

        ByteArrayOutputStream baos =new ByteArrayOutputStream();
        LittleEndianOutputStream out =new LittleEndianOutputStream(baos);

        try {
            out.writeByte(this.secKey.length);
            out.write(this.secKey);
        } catch (IOException e) {
            e.printStackTrace();
            throw new LockFormatException("LockCmdSetTime:"+e.getMessage(), e);
        }

        return baos.toByteArray();
    }

    @Override
    public boolean needEncryption() {
        return false;
    }
}
