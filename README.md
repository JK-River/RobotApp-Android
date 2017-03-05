# RobotApp-Android
儿童机器人Android APP相关的框架、组件、关键技术

## 视频通话
### 能力
* 提供类似qq和微信的呼叫和双人视频通话能力
* 支持录制视频（待添加），只支持录制调用端的画面，如果需要录制对方的画面，可以通过im消息让对方调用录制接口。录制需要开通腾讯云点播，按存储和流量付费

### 适用场景
* 因为机器人的屏幕和摄像头的方向固定，本例仅支持双方横屏通话，画面不随设备的旋转而变化

### 底层实现
* 基于腾讯的[云视频](https://github.com/zhaoyang21cn/Android_Suixinbo)（现在腾讯的云视频demo是直播用的，没有双人通话的demo）

### Getting Started
* VideoConstants类中的sdkAppId和accountType需要改成业务方自己的。
* MultiQavService类中的mSelfIdentifier、mSelfSig、mReceiveIdentifier需要改成业务方自己的账号

### TODO
* 之后会加上腾讯的账号注册体系，优化demo的体验流程
* 视频录制
* 优化接口和调用方法

### References
- [腾讯随心播](https://github.com/zhaoyang21cn/Android_Suixinbo)
- [腾讯云视频](https://www.qcloud.com/solution/video)



