package cn.zelkova.lockprotocol;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 由于通讯限值不能用标准的时间类，所以用较短的字节表示时间。<br>
 * 此类用来将标准时间与协议日期互相转换。基准时间2010-1-1 0:0:0
 *
 * @author zp
 */
public class BriefDate {

    /**
     * 基准时间
     */
    public final static String BASE_DATE_TIME = "2010-01-01 00:00:00";

    /**
     * 时间格式
     */
    public final static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final Date theDate;

    /**
     * 获取协议的基准日期
     */
    public static Date getBaseTime() {
        SimpleDateFormat sdFormat = new SimpleDateFormat(DATE_FORMAT);
        Date theBaseDate = null;
        try {
            theBaseDate = sdFormat.parse(BASE_DATE_TIME);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return theBaseDate;
    }

    private BriefDate(Date val) {
        if (getBaseTime().getTime() > val.getTime()) {
            SimpleDateFormat sdFormat = new SimpleDateFormat(DATE_FORMAT);
            throw new IllegalArgumentException("应大于协议基准日期:" +sdFormat.format(val));
        }
//        Log.w(this.getClass().getSimpleName(), "b:"+getBaseTime().getTime() +", v:"+val.getTime());

        //忽略秒以下的精度
        long theTime =val.getTime() / 1000;
        theTime =theTime * 1000;
        this.theDate = new Date(theTime);
    }


    /**
     * 用自然时间初始化
     */
    public static BriefDate fromNature(Date val) {
        return new BriefDate(val);
    }


    /**
     * 将协议时间转化为正常时间
     */
    public static BriefDate fromProtocol(byte[] val) {
        long theDateSnd = BitConverter.toUInt32(val, 0);
        return fromProtocol(theDateSnd);
    }


    /**
     * 将协议时间转化为正常时间
     * @param dateSnd 协议时间的格式
     */
    public static BriefDate fromProtocol(long dateSnd) {
        if(dateSnd<0)
            throw new IllegalArgumentException("不能小于0");

//        Log.i(BriefDate.class.getSimpleName() +".fromProtocol", "dateSnd:" + dateSnd +", " +getBaseTime().getTime() +dateSnd *1000);

        Calendar cld = Calendar.getInstance();
        cld.setTimeInMillis(getBaseTime().getTime() +dateSnd *1000 );
        //用下面的方法，在类型转换时会变为负数，在此提醒不能用  by zp
        //cld.setTime(getBaseTime());
        //cld.add(Calendar.SECOND, (int) dateSnd);
        return new BriefDate(cld.getTime());
    }


    /**
     * 将协议中的时间转换为自然时间
     * @return
     */
    public Date toNature() {
        return this.theDate;
    }


    /**
     * 将当前时间转化为协议时间表示法的秒数字节方式
     */
    public byte[] toProtocolByte() {
        int diffSnd = (int) toProtocol();
        byte[] retVal = BitConverter.getBytes(diffSnd);
//        Log.i(this.getClass().getSimpleName()+".toProtocolByte", "diffSndBytes["+diffSnd+"]:" + BitConverter.toHexString(retVal));
        return retVal;
    }


    /**
     * 转换为协议时间表示法，秒
     */
    public long toProtocol() {
        long diffMS = this.theDate.getTime() - getBaseTime().getTime();
        long diffSnd = diffMS / 1000;
//        Log.i(this.getClass().getSimpleName()+".toProtocol", "diffSnd:" + String.valueOf(diffSnd));
        return diffSnd;
    }

    /**
     * 已重载，显示当前时间
     */
    @Override
    public String toString() {
        SimpleDateFormat sdFormat =new SimpleDateFormat(DATE_FORMAT);
        String retVal =sdFormat.format(this.theDate);
        return retVal;
    }
}
