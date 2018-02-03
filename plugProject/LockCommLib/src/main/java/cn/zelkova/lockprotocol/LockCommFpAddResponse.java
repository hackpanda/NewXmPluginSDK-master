package cn.zelkova.lockprotocol;

import android.util.Log;

/**
 * 执行AddNewFp操作指令返回
 *
 * @author lwq
 * Created by liwenqi on 2016/11/23.
 */
public class LockCommFpAddResponse extends LockCommResponse {
    public final static short CMD_ID =0x13;
    @Override
    public String getCmdName() {
        return "LockCommAddNewFpResponse";
    }

    /**
     * 执行结果，0为成功执行，否则可能返回： 0x01、0x02、0x03、0x06、0x04、0x0D
     */
    public byte getResultCode(){
        return getVal(0x03);
    }

    /**
     * 当前是采集第几次
     */
    public byte getLuruNum(){
        return getVal(0x04);
    }

    /**
     * 指纹特征编号
     */
    public int getFeatures(){
        KLVContainer klv =this.mKLVList.getKLV(0x05);
        int retVal = (int)klv.getVal()[0];
        return retVal;
    }
    /**
     * 批次编号
     */
    public int getBatchId(){
        KLVContainer klv =this.mKLVList.getKLV(0x06);
        int retVal = (int)klv.getVal()[0];
        Log.d("batchId", retVal+"-----");
        Log.d("batchId", BitConverter.toInt32(klv.getVal(), 0)+"-----");
        return BitConverter.toInt32(klv.getVal(), 0);

    }
    /**
     * 下次返回指纹特征编码的超时sec。
     */
    public byte getOverdueSec(){
        return getVal(0x07);
    }
    private byte getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        byte retVal = klv.getVal()[0];
        return retVal;
    }

    public String toFpString(){
        String result = "录入次数:"+getLuruNum()+",批次号："+getBatchId()+",下次返回指纹特征码的超时时间:"+getOverdueSec();
        return result;
    }
}
