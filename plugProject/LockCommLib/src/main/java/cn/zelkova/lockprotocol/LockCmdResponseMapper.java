package cn.zelkova.lockprotocol;

import android.util.Log;

/**
 * 根据锁命令创建对应的返回对象
 *
 * @author zp
 */
class LockCmdResponseMapper {
    public static LockCmdResponse create(short cmdId){

        LockCmdResponse retVal;

        switch (cmdId){
            case LockCmdExSecurityKeyResponse.CMD_ID:
                Log.w("LockCmdResponseMapper","[cmdId:"+cmdId+"]找到了特定类LockCmdExSecurityKeyResponse");
                retVal=new LockCmdExSecurityKeyResponse();
                break;
            default:
                retVal =new LockCmdResponse();
        }

        return retVal;
    }
}
