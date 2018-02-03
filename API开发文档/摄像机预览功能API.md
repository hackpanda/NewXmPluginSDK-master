# 摄像机预览功能API
## 落地页视频接入
首页顶部第三屏的“我的智能摄像机”。

## 更新MessageReceiver类
处理MSG\_INIT\_CAMERA\_FRAME\_SENDER, MSG\_STAR\_REQUEST\_CAMERA\_FRAME和MSG\_STOP\_REQUEST\_CAMERA\_FRAME和MSG\_DESTROY\_REQUEST\_CAMERA\_FRAME

* MSG_INIT_CAMERA_FRAME_SENDER:初始化发送数据流通道，请调用XmPluginHostApi.initCameraFrameSender
* MSG_REQUEST_CAMERA_FRAME:开始请求视频流  
* MSG_STOP_CAMERA_FRAME:停止请求视频流  
* MSG_DESTROY_REQUEST_CAMERA_FRAME：关闭发送通道，请调用XmPlguinHostApi.closeCameraFrameSender

请求视频格式会在intent中  
int frameRate = intent.getIntExtra("request_frame_rate", 0); //0代表自动，1代表480p，2代表720p，3代表1080p
boolean isMute = intent.getBooleanExtra("mute", false);//true代表mute

开始请求数据之后，音频流由米家扩展程序负责播放，然后将视频流发送给APP。

```
public class MessageReceiver implements IXmPluginMessageReceiver {
    public static final String MODEL = "xiaomi.demo.v1";

    @Override
    public boolean handleMessage(Context context, XmPluginPackage xmPluginPackage, int type,
                                 Intent intent,
                                 DeviceStat deviceStat) {
        switch (type) {
            case LAUNCHER: {// 启动入口
                XmPluginHostApi.instance().startActivity(context, xmPluginPackage, intent,
                        deviceStat.did, MainActivity.class);
                return true;
            }
            case MSG_INIT_CAMERA_FRAME_SENDER: {
                CameraFrameManager.instance().initCameraFrameSender(xmPluginPackage, deviceStat);
                return true;
            }

            case MSG_STAR_REQUEST_CAMERA_FRAME: {
                //开始请求视频流数据，如果视频流已经开启，则需要根据intent调整视频格式和音量
                CameraFrameManager.instance().startRequestData(xmPluginPackage, deviceStat, intent);
                return true;
            }

            case MSG_STOP_REQUEST_CAMERA_FRAME: {
                CameraFrameManager.instance().stopPlay(xmPluginPackage, deviceStat);
                return true;
            }

            case MSG_DESTROY_REQUEST_CAMERA_FRAME: {
                CameraFrameManager.instance().stopRequestData(xmPluginPackage, deviceStat);
                return true;
            }
            default:
                break;
        }
        return false;
    }

    @Override
    public boolean handleMessage(Context context, XmPluginPackage xmPluginPackage, int type,
                                 Intent intent, DeviceStat deviceStat, MessageCallback callback) {
        // TODO Auto-generated method stub
        return false;
    }
}
```
## 实现发送逻辑

调用XmPluginHostApi实现发送逻辑
在发送之前一定要调用initCameraFrameSender。

```
/**
 * ApiLevel: 36
 * 初始化相机发送通道
 */
public abstract void initCameraFrameSender(String did);

/**
 * ApiLevel: 36
 * 摄像机设备发送video接口
 * isIFrame 当前264帧是否为i frame
 */
public abstract void sendCameraFrame(String did, byte[] data, long seq, int frameSize, long timestamp, boolean isIFrame, int width, int height);

/**
 * ApiLevel: 36
 * 关闭发送通道
 */
public abstract void closeCameraFrameSender(String did);
```

## 实现悬浮窗逻辑
使用悬浮窗之前，需要先实现数据流获取。

在米家扩展程序中点击视频悬浮窗之后，首先需要判断是否有悬浮窗权限，6.0之前默认添加，6.0之后用Settings.canDrawOverlays
之后调用XmPluginHostApi.openCameraFloatingWindow开启悬浮窗，最后finish自己的主activity。

当进入米家扩展程序时，需要先调用closeCameraFloatingWindow来关闭悬浮窗。

## app验证方法
把设备model上报给我，我在云端配置之后，本地debug安装的就可以调试了。

米家扩展程序上线时，需要把packageId提供给我，云端配置之后就用户就可以在app上看到。
