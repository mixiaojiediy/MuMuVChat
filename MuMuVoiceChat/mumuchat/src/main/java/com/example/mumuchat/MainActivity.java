package com.example.mumuchat;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private final List<Msg> msgList = new ArrayList<>();
    private EditText inputText;
    private RecyclerView msgRecyclerView;
    private MsgAdapter adapter;
    private Response myResponse;
    static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder().build();
    private final ChatContent chatContent = new ChatContent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inputText = findViewById(R.id.input_text);
        Button send = findViewById(R.id.send);
        msgRecyclerView = findViewById(R.id.msg_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        msgRecyclerView.setLayoutManager(layoutManager);
        adapter = new MsgAdapter(msgList);
        msgRecyclerView.setAdapter(adapter);
        send.setOnClickListener(v -> {
            String content = inputText.getText().toString();
            if (!content.isEmpty()) {
                Msg msg = new Msg(content, Msg.TYPE_SENT);
                chatContent.add(content, Msg.TYPE_SENT);
                msgList.add(msg);
                adapter.notifyItemInserted(msgList.size() - 1); // 当有新消息时，刷新ListView中的显示
                msgRecyclerView.scrollToPosition(msgList.size() - 1); // 将ListView定位到最后一行
                inputText.setText(""); // 清空输入框中的内容
                Log.d("mumuchat",chatContent.getChatContent());
                sendRequest(chatContent.getChatContent());
            }
        });
    }

    private void showResponse(final String result) {
        runOnUiThread(() -> {
            Msg msg = new Msg(result, Msg.TYPE_RECEIVED);
            chatContent.add(result, Msg.TYPE_RECEIVED);
            Log.d("mumuchat1",chatContent.getChatContent());
            msgList.add(msg);
            adapter.notifyItemInserted(msgList.size() - 1); // 当有新消息时，刷新ListView中的显示
            msgRecyclerView.scrollToPosition(msgList.size() - 1); // 将ListView定位到最后一行
        });
    }

    private String parseJSONWithGSON(String jsonData) {
        Log.d("mumuchat2",jsonData);
        Gson gson = new Gson();
        ChatBody chatBody = gson.fromJson(jsonData, ChatBody.class);
        return chatBody.getResult();
    }

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

}