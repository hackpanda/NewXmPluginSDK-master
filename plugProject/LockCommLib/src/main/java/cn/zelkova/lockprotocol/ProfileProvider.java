package cn.zelkova.lockprotocol;

import java.util.Calendar;


/**
 * 需要由外部由外部提供的参数
 *
 * @author zp
 * Created by zp on 2016/6/27.
 */
public class ProfileProvider {

    protected ProfileProvider(){}

    private static ProfileProvider pp=null;

    /**
     * 获取参数提供者实例
     */
    public static ProfileProvider getIns(){
        if(pp ==null)
            pp =new ProfileProvider();
        return pp;
    }

    /**
     * 设置新的参数提供者
     */
    public static void setProvider(ProfileProvider provider){
        pp =provider;
    }


    /**
     * 获取锁命令的跟踪码
     */
    public int getTraceId(){
        return (int) Calendar.getInstance().getTimeInMillis();
    }

    /**
     * 根据mac地址获得对应的密钥，默认16个“0”，
     */
    public byte[] getSecurityKey(String macAddress){
        return new byte[]{
                0x30,0x30,0x30,0x30,
                0x30,0x30,0x30,0x30,
                0x30,0x30,0x30,0x30,
                0x30,0x30,0x30,0x30};
    }


    /**
     * 根据mac地址获得对应的密钥，默认16个“0”，
     */
    byte[] getSecurityKey(byte[] macAddress){
        return getSecurityKey(BitConverter.convertMacAdd(macAddress));
    }

}
