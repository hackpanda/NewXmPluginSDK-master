package cn.zelkova.lockprotocol;

/**
 * BLE广播数据，仅解析出关心的数据
 *
 * @author zp
 * Created by zp on 2016/8/23.
 */


import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/***
 * BLE广播数据，仅解析出关心的数据
 */
public class BLEAdData {

    private class AdStruct {
        public byte length;
        public byte adType;
        public byte[] adData;

        /**
         * 已重载，返回内部完整数据
         */
        @Override
        public String toString() {
            return "["+length+","+String.format("0x%02x",adType) +"]" + BitConverter.toHexString(adData);
        }
    }


    /**
     * 根据传感器判断出的门状态
     */
    public enum DoorStatusEnum {

        /**
         * 无传感器数据
         */
        NoData,

        /**
         * 敞开状态
         */
        Opened,

        /**
         * 关闭状态
         */
        Closed,

        /**
         * 虚掩
         */
        BadClosed,

        /**
         * 内门反锁状态
         */
        DbLocked
    }

    private List<AdStruct> mAdDataList =new ArrayList<>();
    private String mId;


    /**
     * 将字节流解析为结构
     * @param id 用来唯一标识一个设备的，可以用系统返回的Mac来标记
     */
    public static BLEAdData parse(String id, byte[]  scanRecord) {
        BLEAdData bleAdData = new BLEAdData();
        bleAdData.mId =id;

        ByteBuffer buffer = ByteBuffer.wrap(scanRecord).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0)
                break;

            byte adType = buffer.get();
            byte[] adData =new byte[length-1];
            buffer.get(adData, 0, length-1);  // len-1 for ad data length
            bleAdData.add(length, adType, adData);
        }

        return bleAdData;
    }


    /**
     * 添加解析内容
     */
    private AdStruct add(byte length, byte adType, byte[] adData){
        AdStruct ad =new AdStruct();
        ad.length =length;
        ad.adType =adType;
        ad.adData =adData;
        this.mAdDataList.add(ad);

        return ad;
    }


    /**
     * 返回解析后的内容列表
     */
    public AdStruct[] getADList(){
        return this.mAdDataList.toArray(new AdStruct[0]);
    }


    public String getId(){
        return  this.mId;
    }


    /**
     * 获得工厂段数据， type=0xFF
     */
    public byte[] getManufacturerData(){
        byte[] mData =getData((byte) 0xff);
        if(mData !=null && mData.length !=10) {
//            Log.e(this.getClass().getSimpleName(),"ManufacturerData不是预期的长度[10]："+mData.length);
            return null;  //不是预期的长度
        }
        return mData;
    }


    /**
     * 返回ROM当前的重置状态，如果没有对应数据，则返回false
     */
    public boolean isReseting(){
        byte[] mData =getManufacturerData();
        if(mData==null) return false;

        return (mData[8] & 0b10000000)>0;
    }


    /**
     * 根据传感器返回当前门的状态
     */
    public DoorStatusEnum judgeDoorStatus(){
        byte[] mData =getManufacturerData();
        if(mData==null) return DoorStatusEnum.NoData;

/* 详见文档
传感器	门开闭  主锁舌	呆舌   门状态
状态值	bit0	bit1	bit2
0		0		0		0	    门开启opened
4		0		0		1		门开启opened
2		0		1		0		门开启opened
6		0		1		1		门开启opened
1		1		0		0		虚掩bad closed
5		1		0		1		虚掩bad closed
3		1		1		0		关闭closed
7		1		1		1		双锁定double locked
*/

        byte status = (byte) (mData[8] & 0b00000111);
        switch (status){
            case 0:
            case 2:
            case 4:
            case 6:
                return DoorStatusEnum.Opened;
            case 1:
            case 5:
                return DoorStatusEnum.BadClosed;
            case 3:
                return DoorStatusEnum.Closed;
            case 7:
                return DoorStatusEnum.DbLocked;
            default:
                return DoorStatusEnum.NoData;
        }
    }


    /**
     *获得门开闭状态，1闭合，0打开
     */
    public boolean getSensorDoor(){
        byte[] mData =getManufacturerData();
        if(mData==null) return false;

        return (mData[8] & 0b00000001)>0;
    }


    /**
     *获得主锁状态，1锁舌出，0收回
     */
    public boolean getSensorMasterLock(){
        byte[] mData =getManufacturerData();
        if(mData==null) return false;

        return (mData[8] & 0b00000010)>0;
    }


    /**
     * 获得呆舌状态，1锁定，0打开
     */
    public boolean getInnerLockSensor(){
        byte[] mData =getManufacturerData();
        if(mData==null) return false;

        return (mData[8] & 0b00000100)>0;
    }


    /**
     * 返回广播的电量，如果没有数据，则返回 -1
     */
    public int getPwrLevel(){
        byte[] mData =getManufacturerData();
        if(mData==null) return -1;
        byte lvl =mData[9];
        return lvl + LockCommGetStatusResponse.MIN_Level;
    }

    /**
     * 获得广播数据中的MAC，没有对应数据，就返回null
     */
    public byte[] getMac(){
        byte[] mData =getManufacturerData();
        if(mData==null) return null;
        return Arrays.copyOfRange(mData,2,8);
    }


    /**
     * 获得广播数据中的MAC文本，没有对应数据，就返回“”零长字符串
     */
    public String getMacText(){
        byte[] mac =getMac();
        if(mac==null) return "";
        return BitConverter.convertMacAdd(mac);
    }

    /**
     * 获得ManufacturerData中的manuId，如果没有则返回0
     * @return
     */
    public int getManuId(){
        byte[] mData =getManufacturerData();
        if(mData==null) return 0;
        try {
            return BitConverter.toUInt16(Arrays.copyOfRange(mData, 0, 2), 0);
        }catch ( Exception ex){
            ex.printStackTrace();
            return 0;
        }
    }


    /**
     * 获得广播的设备名，先取fullName，没有就取shortName，否则返回“”
     */
    public String getDeviceName(){
        String deviceName ="";

        byte[] dName=getData((byte) 0x08); //full name
        if(dName!=null){
            try {
                deviceName =new String(dName, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        dName=getData((byte) 0x09); //short name
        if(dName!=null){
            try {
                deviceName =new String(dName, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return deviceName;
    }


    /**
     * 最大日志索引
     * 锁协议v02v 10. BLE Broadcast
     */
    public long getMaxLogIndex(){

        byte[] result =getData((byte) 0x16); //广播数据
        return BitConverter.toUInt32(Arrays.copyOfRange(result, 2, 6), 0);

    }

    /**
     * 获得的锁内时钟的协议时间
     * 锁协议v02v 10. BLE Broadcast
     */
    public BriefDate getLockBriefTime(){
        byte[] result =getData((byte) 0x16); //广播数据
        BriefDate briefDate = BriefDate.fromProtocol(Arrays.copyOfRange(result, 6, 10));
        return briefDate;
    }
    /**
     * 根据电量绝对值，获得剩余电量百分比: 例：80%
     * 锁协议v02v 10. BLE Broadcast
     */
    public int getPower(int pwrLevel){
        byte[] result = getData((byte) 0x16); //广播数据
        int powerLevel = BitConverter.toUInt16(Arrays.copyOfRange(result, 10, 11), 0);//电量 ＝ 当前电量 － 最低电量(430)
        int p = 100 * powerLevel /(LockCommGetStatusResponse.MAX_Level - LockCommGetStatusResponse.MIN_Level);

        return p;
    }

    private byte[] getData(byte type){
        byte[] result =null;

        for (AdStruct cur : this.mAdDataList) {
            if(cur.adType == type) {
                if(result ==null) {
                    result =cur.adData;
                }else{
                    Log.w(this.getClass().getSimpleName(),"发现了重复的AdType:" +type);
                }
            }
        }

        return result;
    }
}
