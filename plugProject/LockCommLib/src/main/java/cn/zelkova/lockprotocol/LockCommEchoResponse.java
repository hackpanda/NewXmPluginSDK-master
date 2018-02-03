package cn.zelkova.lockprotocol;

import java.io.UnsupportedEncodingException;

/**
 * Echoe指令返回
 *
 * @author zp
 * Created by zp on 2016/6/14.
 */
public class LockCommEchoResponse extends LockCommResponse {

    public  static final short CMD_ID =0x012;

    @Override
    public String getCmdName() {
        return "echoResp";
    }

    /**
     * 获得返回的内容，应该与发送的内用一致
     */
    public String getMsg(){
        KLVContainer klv = this.mKLVList.getKLV(0x01);
        String result;
        try {
            result = new String(klv.getVal(),"utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return result;
    }

}
