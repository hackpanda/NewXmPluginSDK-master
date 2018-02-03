package cn.zelkova.lockprotocol;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;

/**
 * 锁命令返回内容通用类
 *
 * @author zp
 */
public class LockCmdResponse extends LockCmdBase {


    public LockCmdResponse(byte[] targetMac, BriefDate validFrom, BriefDate validTo, short resultCode, String lockid) {
        super(targetMac, validFrom, validTo, lockid);

        this.resultCode =resultCode;
    }

    LockCmdResponse(){
        super();
    }

    public final static byte HEAD_MARK_RESPONSE =(byte) 0xbb;

    private byte cmdId = (byte) 0xff;  //将会被解析出的值替换掉

    private short resultCode =(byte)0xff;   //将会被解析出的值替换掉

    /**
     * 返回码，0为正确执行
     */
    public short getResultCode() {
        return this.resultCode;
    }


    /**
     * 客户端的执行时间
     */
    public Date getExeTime(){ return super.validFrom.toNature(); }


    protected  void parsePayload(LittleEndianInputStream input){
        //do nothing
    }


    @Override
    protected byte getHeadMark(){ return LockCmdResponse.HEAD_MARK_RESPONSE; }

    @Override
    public byte getCmdId() {
        return this.cmdId;
    }

    @Override
    public String getCmdName() {

        return "genericCmdResp";
    }

    @Override
    public boolean needEncryption() {
        return false;
    }

    @Override
    protected byte[] getPayload() {
        return BitConverter.getBytes(this.resultCode);
    }


    /**
     * 解析锁命令，通过给定内容的第一个字节是否为协议头来判断是否需要解密操作
     * @param cmdCipher 锁命令密文（也可能是明文）
     */
    public static LockCmdResponse parse(String targetMacAdd, byte[] cmdCipher){
        byte[] macByte =BitConverter.convertMacAdd(targetMacAdd);
        return parse(macByte, cmdCipher);
    }


    /**
     * 解析锁命令，通过给定内容的第一个字节是否为协议头来判断是否需要解密操作
     *
     * @param cmdCipher 锁命令密文（也可能是明文）
     */
    public static LockCmdResponse parse(byte[] macAdd, byte[] cmdCipher) {
        if(cmdCipher.length<=0) return null;
        //todo 对锁命令是否需要解密的判断是否可靠？需要关注
        boolean needDecryption = cmdCipher[0] !=HEAD_MARK_RESPONSE;
        return parse(macAdd, cmdCipher, needDecryption);
    }

    /**
     * 解析锁命令
     * @param cmdCipher 锁命令密文（也可能是明文）
     * @param  needDecryption 确定给定的内容是否需要解密
     */
    public static LockCmdResponse parse(byte[] macAdd, byte[] cmdCipher, boolean needDecryption){
        if(cmdCipher.length<=0) return null;

        byte[] inputVal =cmdCipher;
        if(needDecryption) {
            try {
                inputVal = AESService.decryptByMac(macAdd, cmdCipher);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                Log.e("密文解密失败：", BitConverter.toHexString(cmdCipher));
                throw new LockSecurityException("锁命令解密失败", e);
            }
        }

        ByteArrayInputStream inStream = new ByteArrayInputStream(inputVal);
        LittleEndianInputStream input = new LittleEndianInputStream(inStream);

        byte tempByte;
        byte[] tempBArray;
        try {
            tempByte =input.readByte();
            if(tempByte !=LockCmdResponse.HEAD_MARK_RESPONSE)
                throw new LockFormatException("LockCmdResponse head mark error");

            byte version =input.readByte(); //version

            byte[] targetMac =new byte[8];
            input.read(targetMac);

            int trackId =input.readInt();

            tempBArray=new byte[4];
            input.read(tempBArray);
            BriefDate validFrom =BriefDate.fromProtocol(tempBArray);

            tempBArray=new byte[4];
            input.read(tempBArray);
            BriefDate validTo =BriefDate.fromProtocol(tempBArray);

            byte cmdId =input.readByte();

            input.readByte(); //reserve;

            short resultCode =input.readShort();

            LockCmdResponse result =LockCmdResponseMapper.create(cmdId);
            result.targetMac =targetMac;
            result.traceId =trackId;
            result.validFrom =validFrom;
            result.validTo =validTo;
            result.cmdId =cmdId;
            result.resultCode =resultCode;
            result.parsePayload(input);

            Log.i("LockCmdResponse.parse", result.getClass().getSimpleName()+ "："+ result.toString());

            return result;
        } catch (IOException e) {
            e.printStackTrace();//EOF会么?其他我觉得这个地方不应该有什么错误
            throw new LockFormatException("解析Comm时异常:" + e.getMessage(), e);
        }
    }
}
