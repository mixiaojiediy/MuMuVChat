package com.mumu.voicedemo.msg;

public class ChatContent {


    private String chatContent = "";
    private String chatContentBody = "";

    public String getChatContent() {
        return chatContent;
    }

    public void add(String content, int type) {
        content = content.replaceAll("\n", "");
        if (type == Msg.TYPE_SENT) {
            if (chatContentBody.isEmpty()) {
                chatContentBody = chatContentBody + "{\"role\":\"user\",\"content\":\"" + content + "\"}";
            } else {
                chatContentBody = chatContentBody + ",{\"role\":\"user\",\"content\":\"" + content + "\"}";
            }
        } else {
            if (chatContentBody.isEmpty()) {
                chatContentBody = chatContentBody + "{\"role\":\"assistant\",\"content\":\"" + content + "\"}";
            } else {
                chatContentBody = chatContentBody + ",{\"role\":\"assistant\",\"content\":\"" + content + "\"}";
            }
        }
        chatContent = "{\"messages\":[" + chatContentBody + "],\"system\":\"你是一个中国江苏南京五年级的小学生，名字叫木木，年龄：13岁，各个学科的成绩都不错，班级排名第一，很听话，说话不多\"}";
    }
}
