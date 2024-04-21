<h1 align="center">MuMuVChat</h1>
<div align="center">
<a href="https://github.com/mixiaojiediy/MuMuVChat/stargazers"><img src="https://img.shields.io/github/stars/mixiaojiediy/MuMuVChat" alt="Stars Badge"/></a>
<a href="https://github.com/mixiaojiediy/MuMuVChat/network/members"><img src="https://img.shields.io/github/forks/mixiaojiediy/MuMuVChat" alt="Forks Badge"/></a>
<a href="https://github.com/mixiaojiediy/MuMuVChat/pulls"><img src="https://img.shields.io/github/issues-pr/mixiaojiediy/MuMuVChat" alt="Pull Requests Badge"/></a>
<a href="https://github.com/mixiaojiediy/MuMuVChat/issues"><img src="https://img.shields.io/github/issues/mixiaojiediy/MuMuVChat" alt="Issues Badge"/></a>
<a href="https://github.com/mixiaojiediy/MuMuVChat/graphs/contributors"><img alt="GitHub contributors" src="https://img.shields.io/github/contributors/mixiaojiediy/MuMuVChat?color=2b9348"></a>
<a><img src="https://img.shields.io/github/license/mixiaojiediy/MuMuVChat?color=2b9348" alt="License Badge"/></a>

<i>喜欢这个项目吗？请考虑给 Star ⭐️ 以帮助改进！</i>

---

>**项目1**：app
>**项目1简介**：科大讯飞Demo

>**项目2**：MuMuChat
>**项目2简介**：做个app测试百度千帆大模型平台，多种大模型API可以使用，还是挺方便的
>**视频2介绍**：[做个app测试百度千帆大模型平台](https://www.bilibili.com/video/BV1WC41137ND/)
>
>**项目3**：MuMuVChat
>**项目3简介**：科大讯飞语音配合百度千帆大模型做的智能语音助手app
>**视频3介绍**：[科大讯飞语音遇上百度千帆大模型](https://www.bilibili.com/video/BV1ht421w7MS/)



## 项目说明 

### 项目1:app

MuMuVoiceChat/app/src/main/res/values/strings.xml中请注意替换app_id

```xml
<!-- 请替换成在语音云官网申请的appid -->
<string name="app_id">xxxxxxxx</string>
```

app_id获取地址：https://console.xfyun.cn/services/sparkapiCenter

![image-20240421154437816](https://cdn.jsdelivr.net/gh/mixiaojiediy/MDPicBed@main//img202404211544849.png)

### 项目2:MuMuChat

MuMuVoiceChat/mumuchat/src/main/java/com/example/mumuchat/MainActivity.java中注意替换access_token

```java
private void sendRequest(String chatContent) {
    new Thread(() -> {
        try {
            MediaType mediaType = MediaType.parse("application/json");
            Log.d("mumuchat3",chatContent);
            RequestBody body = RequestBody.create(mediaType, chatContent);
            Request request = new Request.Builder()
                    .url("https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/ernie_speed?access_token=<百度千帆大模型平台申请一下,然后替换>")
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            myResponse = HTTP_CLIENT.newCall(request).execute();
            String result = parseJSONWithGSON(myResponse.body().string());
            showResponse(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }).start();
}
```

access_token获取地址：https://console.bce.baidu.com/tools/#/index

![image-20240421154137487](https://cdn.jsdelivr.net/gh/mixiaojiediy/MDPicBed@main//img202404211541581.png)

### 项目3:MuMuVChat

MuMuVoiceChat/mumuvchat/src/main/java/com/iflytek/voicedemo/MuMuVChat.java中注意替换access_token

```java
private void sendRequest(String chatContent) {
    new Thread(() -> {
        try {
            MediaType mediaType = MediaType.parse("application/json");
            Log.d("mumuchat3", chatContent);
            RequestBody body = RequestBody.create(mediaType, chatContent);
            Request request = new Request.Builder()
                    .url("https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/ernie-speed-128k?access_token=<百度千帆大模型平台申请一下,然后替换>")
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            myResponse = HTTP_CLIENT.newCall(request).execute();
            String result = parseJSONWithGSON(myResponse.body().string());
            showResponse(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }).start();
}
```

MuMuVoiceChat/mumuvchat/src/main/res/values/strings.xml中注意替换app_id

```XML
<!-- 请替换成在语音云官网申请的appid -->
<string name="app_id">xxxxxxxx</string>
```












