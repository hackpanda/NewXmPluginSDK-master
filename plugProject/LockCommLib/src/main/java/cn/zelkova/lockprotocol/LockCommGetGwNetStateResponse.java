package cn.zelkova.lockprotocol;

/**
 * 获取网关联网状态返回值
 *
 * @author lwq
 * Created by liwenqi on 2017/7/11.
 */
public class LockCommGetGwNetStateResponse extends LockCommResponse {
    public final static short CMD_ID =0x36;

    @Override
    public String getCmdName() {
        return "getGWNetState";
    }


    private int getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        int retVal =BitConverter.toUInt16(klv.getVal(), 0);
        return retVal;
    }

    /**
     * 错误码
     * 0x00：成功
     * 0x??：“需要定义可能的错误”
     */
    public int getResultCode(){
        return getVal(0x01);
    }

    /**
     * 0x00：未设置wifi参数
     * 0x01：到服务器连接正常
     * 0x11：wifi阶段连接异常
     * 0x12：服务器阶段连接异常，但是wifi连接正常

     * ROM内仅有预设的ssid时，未连接到wifi时不认为是设置过wifi参数（0x00），当连接上预设的wifi时认为是连接正常（0x01）。
     */
    public int getNetState(){return getVal(0x02); }


    /**
     * 是否在重试中
     * 0x00：未重试
     * 0x01：正在重试连接中。
     * 当已经连接到服务器时，总是为0x00，请求方也可忽略此属性
     */
    public int isTrying(){
        return getVal(0x03);
    }

    /**
     * 当前设置的 SSID
     * 如果未设置过，则返回0长度，
     * 预设的ssid不作为已设置过
     */
    public String getSsid(){
        KLVContainer klv =this.mKLVList.getKLV(0x04);
        if(klv.getVal().length == 0){
            return "";
        }
        return BitConverter.toHexString(klv.getVal());
    }
}
