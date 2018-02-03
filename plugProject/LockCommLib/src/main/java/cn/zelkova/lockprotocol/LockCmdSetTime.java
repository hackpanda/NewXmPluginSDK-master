package cn.zelkova.lockprotocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 授时命令
 *
 * @author zp
 * Created by zp on 2016/6/25.
 */
public class LockCmdSetTime extends LockCmdBase {

    BriefDate clientDateAtReq;
    /**
     * @param clientDateAtReq  客户端向服务器请求时，客户端的时间
     */
    public LockCmdSetTime(byte[] targetMac, BriefDate validFrom, BriefDate validTo, BriefDate clientDateAtReq) {
        super(targetMac, validFrom, validTo);
        this.clientDateAtReq = clientDateAtReq;
    }

    @Override
    public byte getCmdId() {
        return 0x0e;
    }

    @Override
    public String getCmdName() {
        return "setTime";
    }

    @Override
    protected byte[] getPayload() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianOutputStream out = new LittleEndianOutputStream(baos);

        try {
            out.write(this.clientDateAtReq.toProtocolByte());

        } catch (IOException e) {
            e.printStackTrace();
            throw new LockFormatException("LockCmdSetZotpPeriod:" + e.getMessage(), e);
        }

        return baos.toByteArray();
    }
}
