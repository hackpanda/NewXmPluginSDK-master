package cn.zelkova.lockprotocol;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 获得锁内日志指令
 *
 * @author zp
 * Created by liwenqi on 2016/6/27.还没写完。。
 */
public class LockCommGetLogResponse extends LockCommResponse {
    public final static short CMD_ID =0x16;

    /**
     * 单条日志记录
     *
     * @author zp
     */
    public class LogRecord{

        private final static byte FIXED_HEAD_LEN =11;

        private byte[] logByte;

        public LogRecord(byte[] logByte){
            this.logByte =logByte;
        }

        /**
         * 获得日志原始格式信息
         */
        public byte[] getLogBytes(){
            return this.logByte;
        }

        /**
         * 获得日志类型
         */
        public int getTypeKey(){
            Log.d("kmjl-typekey", BitConverter.toHexString(logByte));
            return Arrays.copyOfRange(this.logByte,0,1)[0];
        }

        /**
         * 获得日志类型名
         */
        public String getTypeName(){
            int key = getTypeKey();
            String result = "unknown";
            switch (key){
                case 0x04://静态电量
                    result="power";
                    break;
                case 0x08://重新上电
                    result="pwrOn";
                    break;
                case 0x09://DFU
                    result="dfu";
                    break;
                case 0x0A://密码开门失败达到设定时，触发锁定时记录
                    result = "freeze";
                    break;
                case 0x0B://PIN开锁成功后记录 锁设备的MAC or UUID，与open指令中的一致
                    result = "open";
                    break;
                case 0x0C://授时：成功实施后记录。“锁内时间”为授时后的时间，日志部分记录的是授时之前的时间。
                    result = "setTm";
                    break;
                case 0x0D://交换密钥，每次成功后记录
                    result = "exKey";
                    break;
                case 0x0E://opType：0删除、1新增. 每删一个PIN记录一条，清空时也逐条记录
                    result = "synPIN";
                    break;
                case 0x0F://同步开门Pwd操作  增删一个记一条
                    result = "synPwd";
                    break;
                case 0x10://一次性密码开门，记录输入的密码全文，ascii:0x30-0x39，如果不足，高位补0
                    result = "OTP";
                    break;
                case 0x11://Pwd开锁成功后记录
                    result = "pwdOpen";
                    break;
                case 0x13: // 指纹开门，指纹特征编号
                    result = "fpOpen";
                    break;
                case 0x14: //addFP
                    result = "addFP";
                    break;
                case 0x15: // delFP
                    result = "delFP";
                    break;
                case 0x16: //设置安全级别成功完成后记录，记录原值，与当前值。格式见getStatus
                    result = "setSecurity";
                    break;
                case 0x17: //设置安全级别成功完成后记录，记录原值，与当前值。格式见getStatus
                    result = "setPwrSave";
                    break;
                case 0x18: //setSoundVolumn
                    result = "setSoundVolumn";
                    break;
                case 0x19: //reset
                    result = "reset";
                    break;
            }
            return result;
        }

        /**
         *获得日志的索引号
         */
        public int getIdx(){
            byte[] temp =Arrays.copyOfRange(this.logByte,3, 3+4);
            return BitConverter.toInt32(temp,0);
        }

        /**
         * 获取日志日期
         */
        public BriefDate getLogDate(){
            byte[] temp = Arrays.copyOfRange(this.logByte, 7, 7+4);
            BriefDate briefDate = BriefDate.fromProtocol(temp);
            return briefDate;
        }

        /**
         * 获得日志负载部分的字节内容，针对每个日志的内容会有不同
         */
        public byte[] getPayload(){
            int payloadLen = this.logByte.length -FIXED_HEAD_LEN;
            //最后一部分的长度
            byte[] payloadBytes = Arrays.copyOfRange(this.logByte, FIXED_HEAD_LEN, FIXED_HEAD_LEN +payloadLen);
            return payloadBytes;
        }


        /**
         * 获取日志中明细的内容，每个日志记录类型有不同的内容
         * @return 返回Key-Val结构
         */
        public Map<String, String> getPayloadDetail(){
            Map<String, String> retVal =new HashMap<>();
            int key = getTypeKey();
            byte[] plBytes = getPayload();
            long trackId =-1;
            String result = "!!! unknown:"+key+" !!!";
            switch (key){
                case 0x04://静态电量，间隔可根据具体算法调整
                    Log.d("plBtyes-电量", BitConverter.toHexString(plBytes));
                    int pwdLvl =BitConverter.toUInt16(plBytes, 0);
                    retVal.put("pwrLv", String.valueOf(pwdLvl));
                    retVal.put("pwrPercent", String.valueOf(LockCommGetStatusResponse.getPower(pwdLvl)));
                    retVal.put("min", String.valueOf(LockCommGetStatusResponse.MIN_Level));
                    retVal.put("max", String.valueOf(LockCommGetStatusResponse.MAX_Level));
                    break;
                case 0x08://重新上电
                    retVal.put("logType","重新上电");
                    break;
                case 0x09://DFU
                    retVal.put("logType","DFU");
                    break;
                case 0x0A://密码开门失败，触发锁定时记录
                    retVal.put("logType","键盘锁定");
                    break;
                case 0x0B: // open v2
                    trackId =BitConverter.toUInt32(Arrays.copyOfRange(plBytes, 0, 0+4 ), 0);
                    long pin2 = BitConverter.toUInt32(Arrays.copyOfRange(plBytes, 4, 4+4 ), 0);
                    byte[] uuid2 =Arrays.copyOfRange(getPayload(), 8, 8+16);
                    retVal.put("logType", "pinOpen");
                    retVal.put("track", String.valueOf(trackId));
                    retVal.put("pin", String.valueOf(pin2));
                    retVal.put("uuid", BitConverter.toHexString(uuid2));
                    break;
                case 0x0C: //setTm v2
                    trackId =BitConverter.toUInt32(Arrays.copyOfRange(plBytes,0 , 0+4 ), 0);
                    BriefDate briefDate2 = BriefDate.fromProtocol(Arrays.copyOfRange(plBytes,4 ,4+4 ));
                    retVal.put("logType", "授时");
                    retVal.put("track", String.valueOf(trackId));
                    retVal.put("orgDate", briefDate2.toString());
                    break;
                case 0x0D: // exKey v2
                    retVal.put("logType", "交换密钥");
                    trackId =BitConverter.toUInt32(Arrays.copyOfRange(plBytes,0 , 0+4 ), 0);
                    retVal.put("track", String.valueOf(trackId));
                    break;
                case 0x0E: //synPIN v2
                    trackId =BitConverter.toUInt32(Arrays.copyOfRange(plBytes,0 , 0+4 ), 0);
                    retVal.put("logType", "syncPIN");
                    retVal.put("track", String.valueOf(trackId));
                    retVal.put("delSum", String.valueOf(plBytes[4]));
                    retVal.put("addSum", String.valueOf(plBytes[5]));
                    break;
                case 0x0F: //synPwd v2
                    trackId =BitConverter.toUInt32(Arrays.copyOfRange(plBytes,0 , 0+4 ), 0);
                    byte[] delIdx2 =Arrays.copyOfRange(plBytes,4,4+2);
                    byte[] addIdx2 =Arrays.copyOfRange(plBytes,6,6+2);
                    retVal.put("logType", "syncPwd");
                    retVal.put("track", String.valueOf(trackId));
                    retVal.put("delIdx", String.valueOf(BitConverter.toUInt16(delIdx2,0)));
                    retVal.put("addIdx", String.valueOf(BitConverter.toUInt16(addIdx2,0)));
                    break;
                case 0x10: // OTP
                    retVal.put("otp", String.valueOf(BitConverter.toHexString(plBytes)));
                    break;

                case 0x11: //Pwd开锁成功后记录
                    byte[] pwdIdx =Arrays.copyOfRange(plBytes, 0, 0+2);
                    byte[] preLogIdxPwd =Arrays.copyOfRange(plBytes, 2, 2+4);
                    retVal.put("logType", "pwdOpen");
                    retVal.put("pwdIdx", String.valueOf(BitConverter.toUInt16(pwdIdx,0)));
                    retVal.put("preLogIdx", String.valueOf(BitConverter.toUInt32(preLogIdxPwd,0)));
                    break;
                case 0x13: // 指纹开门，指纹特征编号
                    long fpBatchNo = BitConverter.toUInt32(Arrays.copyOfRange(plBytes,0 , 0+4 ), 0);
                    byte[] fpIdx =Arrays.copyOfRange(plBytes, 4, 4+4);
                    byte[] preLogIdxFp =Arrays.copyOfRange(plBytes, 8, 8+4);
                    retVal.put("logType", "fpOpen");
                    retVal.put("fpBatchNo", String.valueOf(fpBatchNo));
                    retVal.put("fpIdx", String.valueOf(BitConverter.toUInt32(fpIdx,0)));
                    retVal.put("preLogIdx", String.valueOf(BitConverter.toUInt32(preLogIdxFp,0)));
                    break;
                case 0x14: //数量：即批次号对应的指纹特征编号的数量
                    retVal.put("logType", "addFp");
                    retVal.put("fpBatchNo", String.valueOf(BitConverter.toUInt32(Arrays.copyOfRange(plBytes, 4, 4+4 ), 0)));
                    retVal.put("fpNum", String.valueOf((int)Arrays.copyOfRange(plBytes, 8, 8+1)[0]));
                    break;
                case 0x15: // 数量：即批次号对应的指纹特征编号的数量
                    retVal.put("logType", "delFp");
                    retVal.put("fpBatchNo", String.valueOf(BitConverter.toUInt32(Arrays.copyOfRange(plBytes, 4, 4+4 ), 0)));
                    retVal.put("fpNum", String.valueOf((int)Arrays.copyOfRange(plBytes, 8, 8+1)[0]));
                    break;
                case 0x16: //设置安全级别成功完成后记录，记录原值，与当前值。格式见getStatus
                    retVal.put("logType", "setSecurity");
                    retVal.put("befor",String.valueOf((int)Arrays.copyOfRange(plBytes, 0, 0+1)[0]));
                    retVal.put("now", String.valueOf((int)Arrays.copyOfRange(plBytes, 1, 1+1)[0]));
                    break;
                case 0x17: //命令（setPwrSave）设置成功后，记录设置的新值
                    retVal.put("logType", "setPwrSave");
                    retVal.put("fpBatchNo", String.valueOf(BitConverter.toUInt16(Arrays.copyOfRange(plBytes, 0, 0+2), 0)));
                    break;
                case 0x18: //设置安全级别成功完成后记录，记录原值，与当前值。格式见getStatus
                    retVal.put("logType", "setSoundVolumn");
                    retVal.put("befor",String.valueOf((int)Arrays.copyOfRange(plBytes, 0, 0+1)[0]));
                    retVal.put("now", String.valueOf((int)Arrays.copyOfRange(plBytes, 1, 1+1)[0]));
                    break;
                case 0x19: //reset
                    retVal.put("logType", "reset");
                    break;
                default:
                    Log.d("Log", result);
            }

            return retVal;
        }

        /**
         * 获得锁日志描述
         */
        public String getPayloadDesc(){
            return getPayloadDesc( ":",  "\n");
        }

        /**
         * 获得锁日志描述
         *
         * @param kvSeparate key和value间的分隔符
         * @param lineSeparate 每个kv间的分隔符
         */
        public String getPayloadDesc(String kvSeparate, String lineSeparate){
            String retVal ="";
            Map<String, String> detail =getPayloadDetail();
            for(String curKey: detail.keySet()){
                if(retVal.length()>0) retVal = retVal +lineSeparate;
                retVal += curKey + kvSeparate + detail.get(curKey);
            }

            Log.d("log-payloaddesc", retVal);
            return retVal;
        }


        /**
         * 已重载，显示日志的共有部分的信息
         * @return
         */
        @Override
        public String toString() {
            String retVal ="["+this.getIdx()+"]["+this.getLogDate().toString()+"]";
            retVal +="0x"+Integer.toHexString(this.getTypeKey())+":" +this.getTypeName();
            return retVal;
        }
    }

    @Override
    public String getCmdName() {
        return "getLogResp";
    }

    /**
     * 是否还有更多的日志：0锁体没有日志，大于0还有日志
     * ROM中从指定的开始位置到最后一条日志的条数。只要大于0就认为还有日志。
     */
    public int hasMoreLog(){
        return BitConverter.toUInt16(getVal(0x04),0);
    }

    /**
     * 执行结果，0为成功执行，否则可能返回： 0x01、0x02、0x03、0x06、0x04、0x0D
     */
    public byte getResultCode(){
        return getVal(0x03)[0];
    }
    /**
     * 本次获得日志的饿条数
     */
    public int logCount(){
        int count = 0;
        for (int idx =1; idx <super.mKLVList.size(); idx++){

            byte[] logByte =super.mKLVList.get(idx).getVal();
            Log.d("logByte", "key: "+super.mKLVList.get(idx).getKey()+"  "+BitConverter.toHexString(logByte));
            if(super.mKLVList.get(idx).getKey() == 5){
                count +=1;
            }

        }
        return count;
        //return super.mKLVList.size() -1;
    }

    /**
     * 获得本次查询得到的日志内容
     */
    public LogRecord[] getLogRecord(){
        ArrayList<LogRecord> retVal =new ArrayList<>();
        for (int idx =1; idx <super.mKLVList.size(); idx++){

            byte[] logByte =super.mKLVList.get(idx).getVal();
            Log.d("logByte", "key: "+super.mKLVList.get(idx).getKey()+"  "+BitConverter.toHexString(logByte));
            if(super.mKLVList.get(idx).getKey() == 5){
                LogRecord curRd =new LogRecord(logByte);
                retVal.add(curRd);
            }

        }
        return retVal.toArray(new LogRecord[0]);
    }

    /**
     * 获取返回索引记录中最大的日志索引号
     * @return 最大的日志索引编号
     */
    public long getLogIndexMax(){
        long maxIdx =Long.MIN_VALUE;
        for (LogRecord cur : this.getLogRecord()){
            maxIdx =Math.max(maxIdx, cur.getIdx());
        }
        Log.d("SecureLogRead", "LogIndexMax: "+(maxIdx==Long.MIN_VALUE? 0: maxIdx));
        return maxIdx==Long.MIN_VALUE? 0: maxIdx;
    }

    /**
     * 获取返回索引记录中最小的日志索引号
     * @return 最小的日志索引编号
     */
    public long getLogIndexMin(){
        long minIdx =Long.MAX_VALUE;
        for (LogRecord cur : this.getLogRecord()){
            minIdx =Math.min(minIdx, cur.getIdx());
        }
        return minIdx==Long.MAX_VALUE? 0: minIdx;
    }

    private byte[] getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        byte[] retVal = klv.getVal();
        return retVal;
    }

}
