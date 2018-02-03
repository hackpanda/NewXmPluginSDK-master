package cn.zelkova.lockprotocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * 同步PIN命令
 *
 * @author zp
 * Created by zp on 2016/6/25.
 */
public class LockCmdSyncPINs extends LockCmdBase {

    public LockCmdSyncPINs(byte[] targetMac, BriefDate validFrom, BriefDate validTo, String lockid) {
        super(targetMac, validFrom, validTo, lockid);
    }

    private int unitLen = 0x04;  //目前定死长度，以后如果需要就变为最长8字节，或者指定
    private ArrayList<Integer> forNewList = new ArrayList<>();
    private ArrayList<Integer> forRemoveList = new ArrayList<>();
    private boolean forClear = false;


    /**
     * 要新增的PIN
     */
    public void addForNew(int pin) {
        if (this.forNewList.contains(pin))
            return;
        this.forNewList.add(pin);
    }

    /**
     * 要新增的PIN
     */
    public void addForNew(byte[] pin) {
        if (pin.length != this.unitLen)
            throw new IllegalArgumentException("pin超过指定长度");
        int val = BitConverter.toInt32(pin, 0);
        addForNew(val);
    }

    /**
     * 标记为清空PIN的命令，将忽略所有的remove内容
     */
    public void markForClear() {
        this.forClear = true;
    }

    /**
     * 要删除的PIN
     */
    public void addForRemove(int pin) {
        if (this.forRemoveList.contains(pin)) return;
        this.forRemoveList.add(pin);
    }

    /**
     * 要删除的PIN
     */
    public void addForRemove(byte[] pin) {
        if (pin.length != this.unitLen)
            throw new IllegalArgumentException("pin超过指定长度");
        int val = BitConverter.toInt32(pin, 0);
        addForRemove(val);
    }

    @Override
    public byte getCmdId() {
        return 0x08;
    }

    @Override
    public String getCmdName() {
        return "syncPINs";
    }

    @Override
    protected byte[] getPayload() {
        if (this.forClear && this.forRemoveList.size() > 0)
            throw new IllegalArgumentException("不应被标记为清空后还添加removePIN");

        //用完要删掉，否则多次调用，数据就多出来这个内容了
        if (this.forClear) {
            this.forRemoveList.clear();
            this.forRemoveList.add(0xffffffff); //magic num表示全部清空
        }

        if (this.forNewList.size() <= 0 && this.forRemoveList.size() <= 0)
            throw new IllegalArgumentException("添加和删除PIN不能同时为空");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianOutputStream out = new LittleEndianOutputStream(baos);

        try {
            /* del by wenqi begin */
            //out.writeByte(this.unitLen);
            /* del by wenqi end */
            out.writeByte(this.forNewList.size());
            if(this.forClear){
                out.writeByte(0x01);
            }else{
                out.writeByte(this.forRemoveList.size());
            }
            for (Integer curVal : this.forNewList) {
                out.writeInt(curVal);
            }
            for (Integer curVal : this.forRemoveList) {
                out.writeInt(curVal);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new LockFormatException("LockCmdSyncPINs:" + e.getMessage(), e);
        }

        return baos.toByteArray();
    }

}
