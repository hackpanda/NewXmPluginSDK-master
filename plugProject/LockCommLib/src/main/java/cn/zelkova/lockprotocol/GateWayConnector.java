/**
 *
 */
package cn.zelkova.lockprotocol;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 负责与网关设备通讯，发送指令和接收设备返回的指令
 *
 * @author liwenqi
 */
public class GateWayConnector extends BleLockConnector {

    private static Map<String, GateWayConnector> ConnectorPool = new HashMap<>();

    private GateWayConnector(Context ctx, String mac) {
        super(ctx, mac);
        super.RX_SERVICE_UUID = UUID.fromString("0000fe90-0000-1000-8000-00805f9b34fb");
        super.RX_CHAR_UUID = UUID.fromString("0000fe92-0000-1000-8000-00805f9b34fb");
        super.TX_CHAR_UUID = UUID.fromString("0000fe91-0000-1000-8000-00805f9b34fb");
        super.CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    }

    /**
     * 创建或返回与指定mac相关联的连接对象
     * @param mac 要连接的BLE设备地址
     */
    public static GateWayConnector create(Context ctx, String mac) {

        if (ConnectorPool.containsKey(mac)) return ConnectorPool.get(mac);

        GateWayConnector retVal = new GateWayConnector(ctx, mac);
        ConnectorPool.put(mac, retVal);

        return retVal;
    }
}
