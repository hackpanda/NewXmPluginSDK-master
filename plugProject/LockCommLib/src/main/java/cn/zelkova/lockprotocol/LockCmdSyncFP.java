package cn.zelkova.lockprotocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 启动扫描或者删除指纹命令
 *
 * @author zp
 *         Created by zp on 2016/8/26.
 */
public class LockCmdSyncFP extends LockCmdBase {

    /**
     * 启动扫描指纹
     */
    private LockCmdSyncFP(byte[] targetMac, BriefDate validFrom, BriefDate validTo) {
        this(targetMac, validFrom, validTo, null);
    }

    /**
     * 删除指定的指纹
     *
     * @param fpIdxToDel 要删除的指纹
     */
    private LockCmdSyncFP(byte[] targetMac, BriefDate validFrom, BriefDate validTo, String lockid, int... fpIdxToDel) {
        super(targetMac, validFrom, validTo, lockid);

        this.fpIdxToDelList = fpIdxToDel;
    }

    private int[] fpIdxToDelList;
    private boolean encryption=true;

    /**
     * 确定当前是启动扫描指纹子命令
     */
    public boolean isStartCmd() {
        return (this.fpIdxToDelList == null || this.fpIdxToDelList.length <= 0);
    }


    /**
     * 确定当前是删除指纹子命令
     *
     * @return
     */
    public boolean isDelFPCmd() {
        if (this.fpIdxToDelList == null) return false;
        return this.fpIdxToDelList.length > 0;
    }

    @Override
    public byte getCmdId() {
        return 0x13;
    }

    @Override
    public String getCmdName() {
        return "syncFP";
    }

    @Override
    protected byte[] getPayload() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianOutputStream out = new LittleEndianOutputStream(baos);

        try {
            if (this.isStartCmd()) {
                out.writeByte(0x01);    //sub cmd
                out.writeShort(0x00);   //delFPNum

            } else if (this.isDelFPCmd()) {
                out.writeByte(0x02);
                out.writeShort(this.fpIdxToDelList.length);
                for (Integer curIdx:this.fpIdxToDelList){
                    out.writeInt(curIdx);
                }

            } else {
                throw new IllegalArgumentException("无法确定subCmd");
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            throw new LockFormatException("LockCmdSyncFP:" + ex.getMessage(), ex);
        }

        return baos.toByteArray();
    }

    @Override
    public boolean needEncryption() {
        return this.encryption;
    }

    public void setE(boolean val){
        this.encryption =val;
    }


    /**
     * 创建启动扫描命令
     */
    public static LockCmdSyncFP createStart(byte[] targetMac, BriefDate validFrom, BriefDate validTo){
        return new LockCmdSyncFP(targetMac, validFrom,validTo);
    }

    /**
     * 创建清空指纹命令
     */
    public static LockCmdSyncFP createClearFP(byte[] targetMac, BriefDate validFrom, BriefDate validTo, String lockid){
        return new LockCmdSyncFP(targetMac, validFrom,validTo, lockid, 0xffffffff);
    }

    /**
     * 创建删除指定指纹命令
     */
    public static LockCmdSyncFP createDelFP(byte[] targetMac, BriefDate validFrom, BriefDate validTo, String lockid, int... fpIdx){
        return new LockCmdSyncFP(targetMac, validFrom,validTo, lockid, fpIdx);
    }
}
