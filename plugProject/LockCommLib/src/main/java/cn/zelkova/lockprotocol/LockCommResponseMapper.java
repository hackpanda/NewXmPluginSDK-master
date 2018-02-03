package cn.zelkova.lockprotocol;

import android.util.Log;

/**
 * 根据返回的cmdId创建相应的类，用来解析返回的内容
 *
 * @author zp
 * Created by zp on 2016/6/14.
 */
class LockCommResponseMapper {

    public static LockCommResponse create(short cmdId){

        LockCommResponse retVal;

        switch (cmdId){
            case LockCommEchoResponse.CMD_ID:
                retVal=new LockCommEchoResponse();
                break;

            case LockCommSessionTokenResponse.CMD_ID:
                retVal =new LockCommSessionTokenResponse();
                break;

            case LockCommGetVerResponse.CMD_ID:
                retVal =new LockCommGetVerResponse();
                break;
            case LockCommGetStatusResponse.CMD_ID:
                retVal =new LockCommGetStatusResponse();
                break;
            case LockCommGetBleMacResponse.CMD_ID:
                retVal =new LockCommGetBleMacResponse();
                break;
            case LockCommExchangeKeyResponse.CMD_ID:
                retVal =new LockCommExchangeKeyResponse();
                break;
            case LockCommGetPinInfoResponse.CMD_ID:
                retVal =new LockCommGetPinInfoResponse();
                break;
            case LockCommSyncPINsResponse.CMD_ID:
                retVal =new LockCommSyncPINsResponse();
                break;
            case LockCommGetPwdInfoResponse.CMD_ID:
                retVal =new LockCommGetPwdInfoResponse();
                break;
            case LockCommSyncPwdResponse.CMD_ID:
                retVal =new LockCommSyncPwdResponse();
                break;
            case LockCommSyncTimeResponse.CMD_ID:
                retVal =new LockCommSyncTimeResponse();
                break;
            case LockCommGetLogResponse.CMD_ID:
                retVal =new LockCommGetLogResponse();
                break;
            case LockCommFpAddResponse.CMD_ID:
                retVal =new LockCommFpAddResponse();
                break;
            case LockCommFpDelResponse.CMD_ID:
                retVal =new LockCommFpDelResponse();
                break;
            case LockCommFpConfirmResponse.CMD_ID:
                retVal =new LockCommFpConfirmResponse();
                break;
            case LockCommGetDeviceNameResponse.CMD_ID:
                retVal =new LockCommGetDeviceNameResponse();
                break;
            case LockCommSetProfileForIotResponse.CMD_ID:
                retVal =new LockCommSetProfileForIotResponse();
                break;
            case LockCommSetProfileForWifiResponse.CMD_ID:
                retVal =new LockCommSetProfileForWifiResponse();
                break;
            case LockCommReBootGateWayResponse.CMD_ID:
                retVal =new LockCommReBootGateWayResponse();
                break;
            case LockCommGetGwNetStateResponse.CMD_ID:
                retVal =new LockCommGetGwNetStateResponse();
                break;
            case LockCommSetVolumnResponse.CMD_ID:
                retVal =new LockCommSetVolumnResponse();
                break;
            default:
                Log.w("LockCommResponseMapper","未找到指定的Response类，使用通用类，cmdId：" +String.format("0x%02x", cmdId));
                retVal =new LockCommResponse();
        }


        return retVal;
    }

}
