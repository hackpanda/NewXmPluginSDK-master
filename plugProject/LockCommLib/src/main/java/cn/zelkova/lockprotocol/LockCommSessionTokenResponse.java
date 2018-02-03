package cn.zelkova.lockprotocol;

import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 获得session Token指令返回
 *
 * @author zp
 * Created by zp on 2016/6/14.
 */
public class LockCommSessionTokenResponse extends LockCommResponse {
    public static final String TAG ="LockCommSessionToken";
    public  static final short CMD_ID =0x01;

    /**
     * 通讯安全方式
     */
    public enum SafetyTypeEnum{
        /**
         * 直接执行锁命令
         */
        Clear,

        /**
         *通过session token方式执行锁命令
         */
        Token
    }

    private Date expireDate;

    @Override
    public String getCmdName() {
        return "ssTokenResp";
    }

    @Override
    void setKLVList(KLVListContainer klvList){
        super.setKLVList(klvList);

        Calendar now =Calendar.getInstance();
        now.add(Calendar.SECOND, getEffectSnd());
        this.expireDate =now.getTime();

        DateFormat sdFormat=SimpleDateFormat.getTimeInstance();
        String msg="加密形式:"+getSafetyType();
        msg +="; Token:" +BitConverter.toHexString(getToken());
        msg +="; 有效秒数:" +getEffectSnd();
        msg +="; 过期时间:" + sdFormat.format(getExpire());
        msg +="; 当前时间:" + sdFormat.format(Calendar.getInstance().getTime());

        Log.i(TAG, msg);
    }

    /**
     * 当前的token是否已经过期，建议用这个判断是否过期，减少了2秒作为保险
     * @return
     */
    public boolean isExpire(){
        DateFormat sdFormat =SimpleDateFormat.getTimeInstance();

        Calendar now =Calendar.getInstance();
        now.add(Calendar.SECOND, 2);

        Log.i(TAG +".ex", sdFormat.format(now.getTime()));
        Log.i(TAG +".ex", sdFormat.format(getExpire()));

        boolean retVal =now.getTime().getTime() > getExpire().getTime();
        return retVal;
    }


    /**
     * 获取当前token过期的绝对时间
     * @return
     */
    public Date getExpire(){
        return this.expireDate;
    }


    /**
     * token的有效秒数
     * @return
     */
    public int getEffectSnd(){
        KLVContainer klv =this.mKLVList.getKLV(0x03);
        int retVal =BitConverter.toUInt16(klv.getVal(), 0);
        return retVal;
    }

    /**
     * 获取token的原始内容
     */
    public byte[] getToken(){
        KLVContainer klv =this.mKLVList.getKLV(0x02);
        byte[] retVal =klv.getVal();
        return retVal;
    }

    /**
     * 获取加密形式
     * @return
     */
    public SafetyTypeEnum getSafetyType(){
        KLVContainer klv =this.mKLVList.getKLV(0x01);
        int retVal = klv.getVal()[0] & 0x03 ;  //as 0b00000011

        if( retVal == 0x02 )
            return SafetyTypeEnum.Token;

        return SafetyTypeEnum.Clear;
    }
}
