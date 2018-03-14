package cn.zelkova.lockprotocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * 设置密码锁命令
 *
 * @author zp
 * Created by zp on 2016/6/25.
 */
public class LockCmdSyncPwd extends LockCmdBase {
    public LockCmdSyncPwd(byte[] targetMac, BriefDate validFrom, BriefDate validTo) {
        super(targetMac, validFrom, validTo);

        this.pwdToAdd = new byte[PWD_MAX_LEN];
        //作为初始值与命令一样，以后用到会被设置有意义的值
        this.pwdValidFrom =validFrom;
        this.pwdValidTo =validTo;
    }

    public final static int PWD_MAX_LEN = 8;
    private int pwdIdToDel;//密码别名，2个字节
    private byte[] pwdToAdd;
    private BriefDate pwdValidFrom;
    private BriefDate pwdValidTo;

    /**
     * 设置为清空密码
     */
    public void markForClear() {
        //pwdIdToDel = BitConverter.fromHexString("ffff");
    }

    /**
     * 要删除的开门密码别名
     */
    public void setPwdToDel(int pwdIdDel) {
        this.pwdIdToDel = pwdIdDel;
    }
    /**
     * 要新增的开门密码
     * @param pwd 开门密码
     * //@param pwdIdx 密码别名
     * @param pwdValidFrom 密码有效期起始
     * @param pwdValidTo 密码有效期终止
     */
    /* modify by liwenqi begin */
    public void addNewPwd(String pwd, BriefDate pwdValidFrom, BriefDate pwdValidTo) {
        this.pwdToAdd = convertToASIICByte(pwd);
        this.pwdIdToDel = 0;
        this.pwdValidFrom = pwdValidFrom;
        this.pwdValidTo = pwdValidTo;
    }

    /**
     * 修改开门密码
     * @param pwd 开门密码
     * //@param pwdIdx 要删除的密码别名
     * @param pwdValidFrom 密码有效期起始
     * @param pwdValidTo 密码有效期终止
     */
    public void updatePwd(String pwd, int pwdIdxToDel, BriefDate pwdValidFrom, BriefDate pwdValidTo) {
        this.pwdToAdd = convertToASIICByte(pwd);
        this.pwdIdToDel = pwdIdxToDel;
        this.pwdValidFrom = pwdValidFrom;
        this.pwdValidTo = pwdValidTo;
    }
    /* modify by liwenqi end */

    /**
     * 将符合要求的密码字符串转化为二进制数组
     * 不用翻译，因为界面已经做了限制
     */
    public static byte[] convertToASIICByte(String val) {
        byte[] tempBuffer;
        try {
            tempBuffer = val.getBytes("utf-8");
            if (tempBuffer.length <= 0)
                throw new IllegalArgumentException("Pwd长度不能为0");
            if (tempBuffer.length > PWD_MAX_LEN)
                throw new IllegalArgumentException("Pwd长度不应超过" + PWD_MAX_LEN + "位");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("密码格式有误:" + e.getMessage(), e);
        }

        //检查要在0x30-0x39(0-9)之间
        for (int idx = 0; idx < tempBuffer.length; idx++) {
            if (tempBuffer[idx] > 0x39 || tempBuffer[idx] < 0x30)
                throw new IllegalArgumentException("密码必须为数字");
        }

        byte[] retVal = new byte[PWD_MAX_LEN];
        for (int idx = 0; idx < tempBuffer.length; idx++) retVal[idx] = tempBuffer[idx];

        return retVal;
    }


    @Override
    public byte getCmdId() {
        return 0x0a;
    }

    @Override
    public String getCmdName() {
        return "syncPwd";
    }

    @Override
    protected byte[] getPayload() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianOutputStream out = new LittleEndianOutputStream(baos);

        try {
            out.writeShort(this.pwdIdToDel);
            out.write(this.pwdToAdd);
            out.write(this.pwdValidFrom.toProtocolByte());
            out.write(this.pwdValidTo.toProtocolByte());

        } catch (IOException e) {
            e.printStackTrace();
            throw new LockFormatException("LockCmdSyncPwd:" + e.getMessage(), e);
        }

        return baos.toByteArray();
    }
}
