<h1 align="center">MuMuVChat</h1>
<div align="center">
<a href="https://github.com/mixiaojiediy/MuMuVChat/stargazers"><img src="https://img.shields.io/github/stars/mixiaojiediy/MuMuVChat" alt="Stars Badge"/></a>
<a href="https://github.com/mixiaojiediy/MuMuVChat/network/members"><img src="https://img.shields.io/github/forks/mixiaojiediy/MuMuVChat" alt="Forks Badge"/></a>
<a href="https://github.com/mixiaojiediy/MuMuVChat/pulls"><img src="https://img.shields.io/github/issues-pr/mixiaojiediy/MuMuVChat" alt="Pull Requests Badge"/></a>
<a href="https://github.com/mixiaojiediy/MuMuVChat/issues"><img src="https://img.shields.io/github/issues/mixiaojiediy/MuMuVChat" alt="Issues Badge"/></a>
<a href="https://github.com/mixiaojiediy/MuMuVChat/graphs/contributors"><img alt="GitHub contributors" src="https://img.shields.io/github/contributors/mixiaojiediy/MuMuVChat?color=2b9348"></a>
<a><img src="https://img.shields.io/github/license/mixiaojiediy/MuMuVChat?color=2b9348" alt="License Badge"/></a>
</div>
<div align="center">
<i>喜欢这个项目吗？请考虑给 Star ⭐️ 以帮助改进！</i>

</div>

---

>**项目1**：app  
>**项目1简介**：科大讯飞Demo  

>**项目2**：MuMuChat  
>**项目2简介**：做个app测试百度千帆大模型平台，多种大模型API可以使用，还是挺方便的  
>**项目2视频介绍**：[做个app测试百度千帆大模型平台](https://www.bilibili.com/video/BV1WC41137ND/)  

>**项目3**：MuMuVChat  
>**项目3简介**：科大讯飞语音配合百度千帆大模型做的智能语音助手app  
>**项目3视频介绍**：[科大讯飞语音遇上百度千帆大模型](https://www.bilibili.com/video/BV1ht421w7MS/)  

>**项目4**：MuMu  
>**项目4简介**：MuMu聊天人工智能机器人，使用百度文心一言大模型和科大讯飞语音，支持打断对话  
>**项目4视频介绍**：[对话丝滑的MuMu聊天机器人](https://www.bilibili.com/video/BV1kZ421j7FV/)  

## 项目说明 

### 项目1:app

/res/values/strings.xml中

添加app_id；

```xml
<string name="app_id">xxxxxxxx</string>
```

app_id获取地址：https://console.xfyun.cn/services/sparkapiCenter
![image-20240421154437816](https://cdn.jsdelivr.net/gh/mixiaojiediy/MDPicBed@main//img202404211544849.png)



### 项目2:MuMuChat

/res/values/strings.xml中

添加api_path和access_token；

access_token获取地址：https://console.bce.baidu.com/tools/#/index
![image-20240421154137487](https://cdn.jsdelivr.net/gh/mixiaojiediy/MDPicBed@main//img202404211541581.png)

api_path也要付费购买一下或者选择免费的大模型api_path替换一下：https://console.bce.baidu.com/qianfan/ais/console/onlineService

![image-20240421162739172](https://cdn.jsdelivr.net/gh/mixiaojiediy/MDPicBed@main//img202404211627249.png)

![image-20240421162818468](https://cdn.jsdelivr.net/gh/mixiaojiediy/MDPicBed@main//img202404211628510.png)

### 项目3:MuMuVChat

/res/values/strings.xml中

添加app_id、api_path和access_token；

```XML
<string name="api_path">xxxxxx</string>
<string name="access_token">xxxxxx</string>
<string name="app_id">xxxxxxxx</string>
```



### 项目4:MuMu

/res/values/strings.xml中

添加app_id、api_path和access_token；

```XML
<string name="api_path">xxxxxx</string>
<string name="access_token">xxxxxx</string>
<string name="app_id">xxxxxxxx</string>
```










