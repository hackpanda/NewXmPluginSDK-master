package cn.zelkova.lockprotocol;

import java.util.Date;

/**
 * 设置或仅获取锁内时间指令返回
 *
 * @author lwq
 * Created by liwenqi on 2016/6/23.
 */
public class LockCommSyncTimeResponse extends LockCommResponse {

    public final static short CMD_ID =0x0E;
    @Override
    public String getCmdName() {
        return "syncTimeResp";
    }

    /**
     * 执行结果，成功执行返回0，否则可能的错误码：0x01、0x03、0x06、0x09、0x0E
     */
    public byte getResultCode(){
        return getVal(0x03)[0];
    }

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
        BriefDate briefDate = BriefDate.fromProtocol(getVal(0x04));
        return briefDate;
    }

    /**
     * 获取与本地时间相差秒数， lockTime -local
     */
    public int getDifferenceToLocal(){
        Date localTime =new Date();
        long diff = getLockTime().getTime() -localTime.getTime();  // it is ms
        diff =diff /1000;  // it is snd
        return (int)diff;
    }

    /**
     * 锁命令密文，需要将结果返回给服务器
     */
    public byte[] getCmdCipher(){
        return getVal(0x05);
    }

    private byte[] getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        byte[] retVal = klv.getVal();
        return retVal;
    }
}
