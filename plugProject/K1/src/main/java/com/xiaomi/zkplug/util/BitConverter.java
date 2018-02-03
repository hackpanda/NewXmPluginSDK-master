package com.xiaomi.zkplug.util;

import java.util.ArrayList;
import java.util.Collections;

/**
 * 基本类型与byte[]的转换，按照little endian方式
 *
 * @author zp
 */
public class BitConverter {

    public static short toInt16(byte[] bytes, int offset) {
        short result = (short) ((int) bytes[offset] & 0xff);
        result |= ((int) bytes[offset + 1] & 0xff) << 8;

        return (short) (result & 0xffff);
    }

    public static int toUInt16(byte[] bytes, int offset) {
        int result = (int) bytes[offset] & 0xff;
        result |= ((int) bytes[offset + 1] & 0xff) << 8;

        return result & 0xffff;
    }

    public static int toInt32(byte[] bytes, int offset) {
        int result = (int) bytes[offset] & 0xff;
        result |= ((int) bytes[offset + 1] & 0xff) << 8;
        result |= ((int) bytes[offset + 2] & 0xff) << 16;
        result |= ((int) bytes[offset + 3] & 0xff) << 24;

        return result;
    }

    public static long toUInt32(byte[] bytes, int offset) {
        long result = (int) bytes[offset] & 0xff;
        result |= ((int) bytes[offset + 1] & 0xff) << 8;
        result |= ((int) bytes[offset + 2] & 0xff) << 16;
        result |= ((int) bytes[offset + 3] & 0xff) << 24;

        return result & 0xFFFFFFFFL;
    }

    public static long toInt64(byte[] bytes, int offset) {
        long result = (long) bytes[offset] & 0xff;
        result |= ((long) bytes[offset + 1] & 0xff) << 8;
        result |= ((long) bytes[offset + 2] & 0xff) << 16;
        result |= ((long) bytes[offset + 3] & 0xff) << 24;
        result |= ((long) bytes[offset + 4] & 0xff) << 32;
        result |= ((long) bytes[offset + 5] & 0xff) << 40;
        result |= ((long) bytes[offset + 6] & 0xff) << 48;
        result |= ((long) bytes[offset + 7] & 0xff) << 56;

        return result & 0xFFFFFFFFFFFFFFFFL;
    }

    public static byte[] getBytes(short value) {
        byte[] bytes = new byte[2];

        bytes[0] = (byte) (value);
        bytes[1] = (byte) (value >> 8);

        return bytes;
    }

    public static byte[] getBytes(int value) {
        byte[] bytes = new byte[4];

        bytes[0] = (byte) (value);
        bytes[1] = (byte) (value >> 8);
        bytes[2] = (byte) (value >> 16);
        bytes[3] = (byte) (value >> 24);

        return bytes;
    }

    public static byte[] getBytes(long value) {
        byte[] bytes = new byte[8];

        bytes[0] = (byte) (value);
        bytes[1] = (byte) (value >> 8);
        bytes[2] = (byte) (value >> 16);
        bytes[3] = (byte) (value >> 24);

        bytes[4] = (byte) (value >> 32);
        bytes[5] = (byte) (value >> 40);
        bytes[6] = (byte) (value >> 48);
        bytes[7] = (byte) (value >> 56);

        return bytes;
    }

    /**
     * 将字节转化为16进制字符串输出，以逗号分隔
     *
     * @param value
     * @return
     */
    public static String toHexString(byte[] value) {
        return toHexString(value, ",");
    }


    /**
     * 将字节转化为16进制字符串输出
     *
     * @param value
     * @return
     */
    public static String toHexString(byte[] value, String splitter) {
        StringBuilder sb = new StringBuilder();
        for (Byte cur : value) {
            String val = String.format("%02x", cur);
            if (sb.length() <= 0)
                sb.append(val);
            else
                sb.append(splitter + val);
        }
        return sb.toString();
    }

    /**
     * Convert hex string to byte[]   把为字符串转化为字节数组
     * @param hexString the hex string
     * @return byte[]
     */
    public static byte[] fromHexString(String hexString){
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    /**
     * Convert hex string to byte[]   把为字符串转化为字节数组
     * @param hexString the hex string
     * @return byte[]
     */
    public static byte[] fromHexString(String hexString, String splitter){
        return fromHexString(hexString.replace(splitter,""));
    }


    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }


    /**
     * Convert hex string to byte[]   把为字符串转化为字节数组
     * @param hexString the hex string
     * @return byte[]
     */
    @Deprecated
    public static byte[] hexStringToBytes(String hexString) {
        return fromHexString(hexString);
    }


    /**
     * 将包装的类型转化为原生类型
     * @param value
     * @return
     */
    public static byte[] toPrimitive(Byte[] value){
        byte[] result=new byte[value.length];
        for (int i = 0; i < value.length; i++) result[i] = value[i];

        return result;
    }


    /**
     * 将原生类型转化为包装类型
     * @param value
     * @return
     */
    public static Byte[] toPackaged(final byte[] value){
        Byte[] result=new Byte[value.length];
        for (int i = 0; i < value.length; i++) result[i] = value[i];

        return result;
    }

    /**
     * 将mac地址字符与字节格式转换，如果不够8byges，则高位补0x00
     * @param mac 格式为: b[7]:b[6]:b[5]:b[4]:b[3]:b[2]:b[1]:b[0]
     */
    public static byte[] convertMacAdd(String mac){
        byte[] macByte =fromHexString(mac,":");
        if(macByte.length >8) throw new IllegalArgumentException("mac length must be less 8 bytes");

        ArrayList<Byte> temp =new ArrayList<>();
        for(Byte cur:macByte) temp.add(cur);
        Collections.reverse(temp);
        while(temp.size()<8)  temp.add((byte) 0x00);

        Byte[] mac8 =temp.toArray(new Byte[0]);
        return BitConverter.toPrimitive(mac8);
    }
    /**
     * 将mac地址字符与字节格式转换，如果不够8byges，则高位不补0x00
     * @param mac 格式为: b[7]:b[6]:b[5]:b[4]:b[3]:b[2]:b[1]:b[0]
     *
     */
    public static byte[] convertMacAddSix(String mac){
        byte[] macByte =fromHexString(mac,":");
        if(macByte.length >8) throw new IllegalArgumentException("mac length must be less 8 bytes");

        ArrayList<Byte> temp =new ArrayList<>();
        for(Byte cur:macByte) temp.add(cur);
        Collections.reverse(temp);
        Byte[] mac8 =temp.toArray(new Byte[0]);
        return BitConverter.toPrimitive(mac8);
    }
    /**
     * 将mac地址字符与字节格式转换: b[7]:b[6]:b[5]:b[4]:b[3]:b[2]:b[1]:b[0]
     */
    public static String convertMacAdd(final byte[] mac){
        if(mac.length >8) throw new IllegalArgumentException("mac length must be less 8 bytes");

        ArrayList<Byte> temp =new ArrayList<>();
        for(Byte cur:mac) temp.add(cur);
        Collections.reverse(temp);
        while(temp.size()>0 && temp.get(0) ==0x00)
            temp.remove(0);

        Byte[] macByte =temp.toArray(new Byte[0]);
        return toHexString(BitConverter.toPrimitive(macByte),":");
    }

}
