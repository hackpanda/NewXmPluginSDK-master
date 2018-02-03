package cn.zelkova.lockprotocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * 内部使用的数据结构，表示通讯用的KLV结构
 *
 * @author zp
 */
class KLVContainer {

    /**
     * 已重载，返回具体内容
     *
     * @return
     */
    @Override
    public String toString() {
        String result = "[";
        result += Integer.toHexString(this.mKey);
        result += "." + Integer.toString(this.mVal.length);
        result += "." + Arrays.toString(this.mVal);
        result += "]";

        return result;
    }

    private int mKey = 0;
    private byte[] mVal = null;

    public KLVContainer(int key, byte[] val) {
        this.mKey = key;
        this.mVal = val;
    }

    public int getKey(){
        return this.mKey;
    }


    /**
     * 返回值部分
     */
    public byte[] getVal(){
        return this.mVal;
    }

    /**
     * 获得此结构的字节数组格式
     */
    public byte[] getBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianOutputStream out = new LittleEndianOutputStream(baos);

        try {
            out.writeByte(this.mKey);
            out.writeShort(this.mVal.length);
            out.write(this.mVal);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return baos.toByteArray();
    }

}
