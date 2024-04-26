package com.example.mumuchat;

public class ChatContent {


    private String chatContent = "";//"{\"messages\":[{\"role\":\"user\",\"content\":\"你好 \"}]}"
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
        chatContent = "{\"messages\":[" + chatContentBody + "],\"disable_search\":false,\"enable_citation\":false}";
    }
}
