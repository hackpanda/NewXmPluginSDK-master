package cn.zelkova.lockprotocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


/**
 * 锁命令的基类
 *
 * @author zp
 */
public abstract class LockCmdBase {

    /**
     * 协议头起始标志
     */
    public final static byte HEAD_MARK_REQUEST =(byte)0xaa;

    /**
     * 目标设备地址
     */
    protected byte[] targetMac;

    /**
     * 跟踪码
     */
    protected int traceId =Integer.MIN_VALUE;

    /**
     * 命令有效期起始时间
     */
    protected BriefDate validFrom;

    /**
     * 命令有效期终止时间
     */
    protected BriefDate validTo;

    /**
     *
     * @param targetMac 目标锁的mac地址
     * @param validFrom 有效期起始日期
     * @param validTo   有效期终止日期
     */
    public LockCmdBase(byte[] targetMac, BriefDate validFrom, BriefDate validTo){
        if(targetMac.length >8) throw new LockFormatException("mac格式错误");
        this.targetMac =targetMac;
        this.traceId = ProfileProvider.getIns().getTraceId();
        this.validFrom =validFrom;
        this.validTo =validTo;
    }

    LockCmdBase(){}
    public LockCmdBase(byte[] targetMac, BriefDate validFrom, BriefDate validTo, String lockId){
        if(targetMac.length >8) throw new LockFormatException("mac格式错误");
        this.targetMac =targetMac;
        this.traceId = ProfileProvider.getIns().getTraceId();
        this.validFrom =validFrom;
        this.validTo =validTo;
    }

    /**
     * 按定义格式序列化
     */
    public byte[] getBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianOutputStream out = new LittleEndianOutputStream(baos);

        try {
            out.writeByte(getHeadMark());
            out.writeByte(0x00);  //version
            out.write(this.targetMac);
            out.write(BitConverter.getBytes((int)this.traceId));
            out.write(this.validFrom.toProtocolByte());
            out.write(this.validTo.toProtocolByte());
            out.writeByte(getCmdId());
            out.writeByte(0x00);  //reserve

            out.write(getPayload());

        } catch (IOException e) {
            e.printStackTrace();
            throw new LockFormatException("序列化Cmd时异常:" + e.getMessage(), e);
        }

        byte[] buffer = baos.toByteArray();
//        try {
//            if(needEncryption()) {
//                byte[] retVal = AESService.encryptByMac(this.targetMac, buffer);
//                return retVal;
//            } else {
//                return buffer;
//            }
//
//        } catch (GeneralSecurityException e) {
//            e.printStackTrace();
//            throw new LockSecurityException("锁命令加密失败", e);
//        }
        return buffer;//米家去掉加密
    }


    protected byte getHeadMark(){ return LockCmdBase.HEAD_MARK_REQUEST; }

    /**
     * 返回跟踪序号
     */
    public int getTrackId(){
        return this.traceId;
    }

    public abstract byte getCmdId();
    public abstract String getCmdName();
    protected abstract byte[] getPayload();

    /**
     * 锁命令是否需要加密
     */
    public boolean needEncryption(){return true;}

    /**
     * 已重载，返回命令Id和name
     */
    @Override
    public String toString() {
        return "[traceId:"+this.traceId+"][cmdId:" +getCmdId()+ "]" +getCmdName();
    }

}
