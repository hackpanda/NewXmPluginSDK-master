package cn.zelkova.lockprotocol;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;
/**
 * 交换密钥指令
 *
 * @author lwq
 * Created by liwenqi on 2016/6/23.
 */
public class LockCommExchangeSecurityKey extends LockCommBase {

    /**
     * @param cmd 从服务器获得的交换密钥锁命令
     */
    public LockCommExchangeSecurityKey(byte[] cmd) {
        this((byte) 0x01,cmd);
    }

    /**
     * @param secKeyExType 密钥交换方式，0x01:AES密钥  0x02:ECDH
     * @param cmd 锁命令
     */
    public LockCommExchangeSecurityKey(byte secKeyExType, byte[] cmd) {
        BriefDate briefDate = BriefDate.fromNature(new Date());
        super.mKLVList.add((byte) 0x01, briefDate.toProtocolByte());//time 4字节，授时时间，见日期表示法
        super.mKLVList.add((byte) 0x02, secKeyExType);
        super.mKLVList.add((byte) 0x03, cmd);
    }

    @Override
    public short getCmdId() {
        return 0x05;
    }

    @Override
    public String getCmdName() {
        return "ExchangeSecutityKey";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }

}

