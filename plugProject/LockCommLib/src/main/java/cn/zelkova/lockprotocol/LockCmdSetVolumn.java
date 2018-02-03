package cn.zelkova.lockprotocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 设置音量
 *
 * @author liwenqi
 * Created by liwenqi on 2017/8/21.
 */
public class LockCmdSetVolumn extends LockCmdBase {
    private int volumn;
    /**
     * 启动扫描指纹
     */
    public LockCmdSetVolumn(byte[] targetMac, BriefDate validFrom, BriefDate validTo) {
        super(targetMac, validFrom, validTo);
    }
    /**
     * 1.仅开关声音：0关闭声音，0x7F打开声音
     * 2.设置声音大小：0关闭声音，[0, 0x7F]音量大小
     * @param volumn 音量
     */
    public void setVolumn(int volumn) {
        this.volumn = volumn;
    }

    @Override
    public byte getCmdId() {
        return 0x18;
    }

    @Override
    public String getCmdName() {
        return "LockCmdFpDel";
    }

    @Override
    protected byte[] getPayload() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianOutputStream out = new LittleEndianOutputStream(baos);

        try {
            out.writeByte(this.volumn);

        } catch (IOException e) {
            e.printStackTrace();
            throw new LockFormatException("LockCmdFpDel:" + e.getMessage(), e);
        }

        return baos.toByteArray();
    }
}
