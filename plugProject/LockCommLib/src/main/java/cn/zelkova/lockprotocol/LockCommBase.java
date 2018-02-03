package cn.zelkova.lockprotocol;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * 与BLE设备通讯的基础类，负责封装通用的功能，通过继承此类完成具体的功能
 *
 * @author zp
 */
public abstract class LockCommBase {

    private static final String TAG = "LockCommBase";

    /**
     * 协议头标记
     */
    private static final byte headMark = (byte) 0xAB;

    /**
     * L1协议版本号
     */
    private static final byte versionL1 = (byte) 0x00;

    /**
     * 保留
     */
    private static final byte reserveL1 = (byte) 0x00;
    /**
     * 全局的事件通讯流水号
     */
    private static int globalSequId = 0;

    /**
     * session token 在klv中的Key
     */
    public static final byte SESSION_TOKEN_KEY = (byte) 0xF0;

    /**
     * 本条消息的流水号
     */
    private int sequId = 0;
    /**
     * 用来容纳L2中Payload的内容
     */
    protected final KLVListContainer mKLVList = new KLVListContainer();

    public LockCommBase() {
        this.sequId = LockCommBase.getGlobalSequId();
    }


    /**
     * 确认指定的字节可以解析出完整的协议内容
     */
    private static boolean hasWholeBody(byte[] buffer) {
        byte[] headL1 = Arrays.copyOf(buffer, 8);  //L1头8个字节

        if (headL1[0] != LockCommBase.headMark) {
            Log.e("LockCommBase", "无效的Comm协议头内容");
            throw new LockFormatException("无效的Comm协议头内容");
        }

        int payloadLen = BitConverter.toUInt16(buffer, 2);

        Log.i(TAG, "缓存长度：" + buffer.length + "；payloadLen：" + payloadLen + "；协议包长度：" + (payloadLen + 8));

        //fixed len: +L1head
        return buffer.length >= payloadLen + 8;
    }

    /**
     * 从输入的字节流中解析出协议对象，如果不能解析出来则返回空，可以通过类型转换到指定的类型，
     * 并消耗使用的字节。
     *
     * @param value 要进行解析的字节内容，会被消耗里面的内容
     */
    public static LockCommResponse parse(ArrayDeque<Byte> value) {
        //有些浪费性能了
        byte[] buffer = BitConverter.toPrimitive(value.toArray(new Byte[0]));
        if (!hasWholeBody(buffer)) return null;

        //消耗掉缓存中一个完整协议包的内容，剩下的内容还回去
        int packageLen = BitConverter.toUInt16(buffer, 2) + 8;  //找到payload长度，再加上L1 Head Len
        buffer = new byte[packageLen];
        for (int i = 0; i < packageLen; i++) buffer[i] = value.pop();
        Log.i(TAG, "缓存消耗后长度：" + value.size());

        //开始解析数据包到对象里
        LockCommResponse response;
        try {
            ByteArrayInputStream inStream = new ByteArrayInputStream(buffer);
            LittleEndianInputStream input = new LittleEndianInputStream(inStream);

            byte tempByte;

            //*****************  L1
            //head mark
            tempByte = input.readByte();
            if (tempByte != LockCommBase.headMark) {
                String msg ="无效的Comm协议头内容:" +tempByte;
                Log.e(TAG, msg);
                throw new LockFormatException(msg);
            }

            //ver, reserve, ackFlag, errFlag, all in one byte
            //todo 处理ackFlag errFlag
            input.readByte();
            //payload len, 2 bytes
            int payloadLen = input.readUnsignedShort();
            //crc16, 2 bytes
            input.readUnsignedShort();
            //SequId, 2 bytes;
            int sequId = input.readUnsignedShort();

            //******************** L2
            short cmdId = (short) input.readUnsignedByte();
            //cmd ver, reserve
            input.readByte();
            //reserve 2 bytes
            input.readByte();
            input.readByte();
            //klv
            byte[] klvBuffer = new byte[payloadLen - 4];  // -L2head
            input.read(klvBuffer);
            KLVListContainer klvList = KLVListContainer.parse(klvBuffer);

            response = LockCommResponseMapper.create(cmdId);
            response.setCmdId(cmdId);
            response.setSequId(sequId);
            response.setKLVList(klvList);

        } catch (IOException e) {
            //我觉得这个地方不应该有什么错误
            e.printStackTrace();
            throw new LockFormatException("解析Comm时异常:" + e.getMessage(), e);
        }

        return response;
    }

    private boolean getErrFlag() {
        return false;
    }

    private boolean getAckFlag() {
        return false;
    }


    /**
     * 序列化，获取协议的字节内容，不需要ssToken
     */
    public byte[] getBytes(){
        return getBytes(null);
    }

    /**
     * 序列化，获取协议的字节内容
     * @param ssToken session Token
     */
    public byte[] getBytes(byte[] ssToken) {

        if(needSessionToken()){
            if(ssToken ==null || ssToken.length<=0)
                throw new LockException("未给命令提供ssToken,cmdId:["+ this.getCmdId() +"]"+this.getCmdName());

            this.mKLVList.add(SESSION_TOKEN_KEY, ssToken);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianOutputStream out = new LittleEndianOutputStream(baos);

        int temp = 0;
//		L1
        try {
            out.writeByte(LockCommBase.headMark);

            //			L1头部的四个字段
            temp |= LockCommBase.versionL1 << 4;
            temp |= LockCommBase.reserveL1 & 0x0C;  //保留字段 0x0C:[00001100]
            temp |= getAckFlag() ? 0x02 : 0x00;
            temp |= getErrFlag() ? 0x01 : 0x00;
            out.writeByte(temp);
            out.writeShort(this.mKLVList.getBytes().length + 4);  //L2头的固定长度
            out.writeShort(getCRC16());
            out.writeShort(this.sequId);

//		L2
            temp = 0;
            out.writeByte(this.getCmdId());
            temp |= (getCmdVer() & 0x0F) << 4;
            temp |= 0x00; // L2 reserve 4bits
            out.writeByte(temp);
            out.write(0x00); //L2 reserve 1/2 byte
            out.write(0x00); //L2 reserve 2/2 byte
            out.write(this.mKLVList.getBytes());

        } catch (IOException e) {
            //我觉得这个地方不应该有什么错误
            e.printStackTrace();
            throw new LockException("序列化Comm时异常:"+e.getMessage(), e);
        }
        return baos.toByteArray();
    }

    private int getCRC16() {
        return 0;
    }

    /**
     * 指令代码
     */
    public abstract short getCmdId();

    /**
     * 指令名称
     */
    public abstract String getCmdName();

    /**
     * 标识此命令需要sessionToken
     */
    public abstract boolean needSessionToken();

    /**
     * 获得当前指令的执行流水序号
     */
    public int getSequId() {
        return this.sequId;
    }

    void setSequId(int value) {
        this.sequId = value;
    }

    void setKLVList(KLVListContainer value) {
        if (value == null) throw new NullPointerException();
        this.mKLVList.clear();
        this.mKLVList.addAll(value);
    }


    /**
     * 获取Payload的原始内容
     */
    protected KLVListContainer getKLVList() {
        return this.mKLVList;
    }

    /**
     * 通讯协议中，cmd的版本号
     */
    protected byte getCmdVer() {
        return 0;
    }

    private static int getGlobalSequId() {
        LockCommBase.globalSequId++;
        return LockCommBase.globalSequId;
    }

    /**
     * 已重载，返回命令Id和name
     */
    @Override
    public String toString() {
        return "["+getCmdId()+"]"+getCmdName();
    }
}
