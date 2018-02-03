package cn.zelkova.lockprotocol;

import java.util.Date;

/**
 * 获得锁内密钥设置信息指令返回
 *
 * @author lwq
 * Created by liwenqi on 2016/6/22.
 */
public class LockCommGetStatusResponse extends LockCommResponse {
    public final static short CMD_ID =0x03;
    /**
     * 满电量指数
     */
    public final static int  MAX_Level =670;

    /**
     * 低电量指数
     */
    public final static int MIN_Level =430;

    /**
     * 报警电量指数
     */
    public final static int Warn_Level =441;
    /**
     * 密钥交换方式
     */
    public enum KeyExType{
        /**
         * 不支持加密
         */
        None,

        /**
         * 明文交换
         */
        Clear,

        /**
         * ecdh方式交换
         */
        ECDH
    }

    @Override
    public String getCmdName() {
        return "getStatusResp";
    }
    /**
     * 结果码，0为成功执行，否则可能返回：0x07、0x0E、0x0D、0x03、0x0C
     */
    public int getResultCode(){
        return getVal(0x03);
    }

    /**
     * 当前安全状态<br>0:从未设置过密钥<br>1:已设置
     */
    public int getStatusCode(){return getVal(0x01); }

    /**
     * 支持的密钥交换方式<br>0:不支持密钥<br>1:明文设置密钥<br>2:通过ECDH交换（当前未支持20160623）
     */
    public KeyExType getExchangeType(){
        byte exType = (byte) getVal(0x02);
        if(exType ==0x00)
            return KeyExType.None;
        else if (exType ==0x01)
            return KeyExType.Clear;
        else if(exType ==0x02)
            return KeyExType.ECDH;
        else
            throw new LockException("不能识别的密钥交换类型：" +exType);
    }

    /**
     * 锁内时间
     */
    /**
     * 获得的锁内时钟的标准时间
     */
    public Date getLockTime(){
        return getLockBriefTime().toNature();
    }

    /**
     * 获得的锁内时钟的协议时间
     */
    public BriefDate getLockBriefTime(){
        KLVContainer klv =this.mKLVList.getKLV(0x04);

        BriefDate briefDate = BriefDate.fromProtocol(BitConverter.toInt32(klv.getVal(), 0));
        return briefDate;
    }
    /**
     * 获取与本地时间相差秒数， lockTime -local
     */
    public long getDiffSnd(){
        Date localTime =new Date();
        long diff = getLockTime().getTime() -localTime.getTime();  // it is ms
        diff =diff /1000;  // it is snd
        return diff;
    }
    /**
     * 获得zotp有效期
     */
    public int getZotpPeriod(){
        KLVContainer klv =this.mKLVList.getKLV(0x05);
        return BitConverter.toUInt16(klv.getVal(),0);
    }

    /**
     * 获得键盘锁定时间
     */
    public int getFreezeTime(){
        return getVal(0x06);
    }
    /**
     * 获得密码输错次数
     */
    public int getErrCount(){
        return getVal(0x07);
    }
    /**
     * 获得安全级别
     */
    public int getSecurityLevel(){
        return getVal(0x08);
    }
    /**
     * pwrSaveDelay: 没有外界触发N分钟后，进入休眠模式
     */
    public int getPwrSaveDelay(){
        KLVContainer klv =this.mKLVList.getKLV(0x09);
        return BitConverter.toUInt16(klv.getVal(),0);
    }

    /**
     * 根据电量绝对值，获得剩余电量百分比
     */
    public static int getPower(int pwrLevel){
        if(pwrLevel <0) return 0;
        if(pwrLevel < MIN_Level) return 0;
        if(pwrLevel >MAX_Level) return 100;

        int p = 100 * (pwrLevel - MIN_Level) /(MAX_Level - MIN_Level);

        return p;
    }

    /**
     * 剩余电量，百分比，精度到整数，这是由锁中获得的。
     */
    public int getPower(){
        //todo 为了兼容ROMv1版本，以后可去掉
        if(getPowerLevel() ==-1)
            return  getVal(0x01);

        return getPowerPercentage();
    }

    /**
     * 获取剩余电量的百分比，精确到整数
     */
    private int getPowerPercentage(){
        int curLvl =getPowerLevel();
        if(curLvl==-1) return 0;
        return getPower(curLvl);
    }


    /**
     *获取锁内电量绝对值，如果是老版本ROM，没有这个值，返回-1
     */
    public int getPowerLevel(){
        KLVContainer klv =this.mKLVList.getKLV(0x11);
        if(klv ==null) return -1;
        return BitConverter.toUInt16(klv.getVal(),0);
    }


    /**
     * 是否达到了电量报警水平
     * 依赖getPowerLevel()，请见注释，无效时，总返回false
     */
    public boolean isPowerWarn(){
        int pwdLvl =getPowerLevel();
        if(pwdLvl <0) return true;
        return pwdLvl <=Warn_Level;
    }

    /**
     *获取锁内电量范围最小值
     */
    public int getPowerMin(){
        KLVContainer klv =this.mKLVList.getKLV(0x12);
        return BitConverter.toUInt16(klv.getVal(),0);
    }
    /**
     *获取锁内电量范围最大值
     */
    public int getPowerMax(){
        KLVContainer klv =this.mKLVList.getKLV(0x13);
        return BitConverter.toUInt16(klv.getVal(),0);
    }
    /**
     *预留PIN空间个数
     */
    public int getPinCapacity(){
        return getVal(0x21);
    }
    /**
     *已有PIN数量
     */
    public int getPinStock(){
        return getVal(0x22);
    }
    /**
     *已绑定设备大PIN数量
     */
    public int getPinBind(){
        return getVal(0x23);
    }
    /**
     *可存开门密码数量
     */
    public int getPwdCapacity(){
        return getVal(0x24);
    }
    /**
     *已存开门密码数量
     */
    public int getPwdStock(){
        return getVal(0x25);
    }
    /**
     *支持的最大密码长度
     */
    public int getPwdMaxlen(){
        return getVal(0x26);
    }
    /**
     *指纹容量
     */
    public int getFpCapacity(){
        return getVal(0x27);
    }
    /**
     *已存指纹数量
     */
    public int getFpStock(){
        return getVal(0x28);
    }
    /**
     *已存批次号数量
     */
    public int getFpBatchAmount(){
        return getVal(0x29);
    }
    /**
     *volunmn
     */
    public int getVolumn(){
        return getVal(0x14);
    }
    /**
     *固件版本:rom
     */
    public String getVerFrameware(){
        KLVContainer klv =this.mKLVList.getKLV(0x31);

        //return (klv.getVal()[0]+"." + klv.getVal()[1]+"." +klv.getVal()[2]);
        return String.valueOf(BitConverter.toInt32(klv.getVal(), 0));
    }
    /**
     *DFU版本
     */
    public int getVerDfu(){
        return getVal(0x32);
    }
    /**
     *键盘固件版本
     */
    public int getVerKeyboard(){
        return getVal(0x33);
    }
    /**
     * C锁full，C锁lite
     */
    public int getProductClass(){
        return getVal(0x34);
    }
    private int getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        int retVal = (int)klv.getVal()[0];
        return retVal;
    }
}
