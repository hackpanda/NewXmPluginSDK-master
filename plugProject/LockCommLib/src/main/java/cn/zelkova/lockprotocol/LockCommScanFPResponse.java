package cn.zelkova.lockprotocol;

/**
 * 启动指纹扫描，返回结果
 *
 * @author zp
 * Created by zp on 2016/8/15.
 */
public class LockCommScanFPResponse  extends LockCommResponse {

    public final static short CMD_ID =0x13;

    @Override
    public String getCmdName() {
        return "scanFPResp";
    }

    public int getIsReady(){
        KLVContainer klv = super.getKLVList().getKLV(0x01);
        if (klv == null) {
            return -1;
        } else {
            return klv.getVal()[0];
        }
    }
}
