package com.mumuavchat.voicedemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.mumuavchat.speech.util.JsonParser;
import com.mumuavchat.voicedemo.msg.ChatBody;
import com.mumuavchat.voicedemo.msg.ChatContent;
import com.mumuavchat.voicedemo.msg.Msg;
import com.mumuavchat.voicedemo.msg.MsgAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings("all")
public class MuMuAVChat extends AppCompatActivity {
    private static final String LOG_TAG = "MuMuAVChat";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private static final String AUDIO_DIRECTORY = "MuMuAVChat";
    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
    );
    private static final int SILENCE_TIMEOUT_MS = 4000; // 4秒无声超时
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private AudioManager audioManager;

    private SpeechSynthesizer mTts; // 语音合成对象
    private SpeechRecognizer mIat; // 语音听写对象
    private boolean ttsSpeaking = false;
    private boolean userSpeaking = false;
    private Timer silenceTimer;
    private long lastSpokenTime;

    private final List<Msg> msgList = new ArrayList<>();
    private RecyclerView msgRecyclerView;
    private MsgAdapter adapter;

    private HashMap<String, String> mIatResults = new LinkedHashMap<>(); // 用HashMap存储听写结果
    private String content;
    File audioFile;

    private Toast mToast;

    private Response myResponse;
    static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder().build();
    private final ChatContent chatContent = new ChatContent();

    private Handler mainHandler;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.mumuavchat);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        initView();
        initIat();
        initTTS();
        mainHandler = new Handler(Looper.getMainLooper());
        startContinuousRecording();
    }

    private void initView() {
        msgRecyclerView = findViewById(R.id.msg_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        msgRecyclerView.setLayoutManager(layoutManager);
        adapter = new MsgAdapter(msgList);
        msgRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();
    }

    private void initIat() {
        mIat = SpeechRecognizer.createRecognizer(this, null); //创建一个语音识别器
        mIat.setParameter(SpeechConstant.PARAMS, null); // 清空参数
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); // 设置听写引擎
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json"); // 设置返回结果格式
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, "4000");
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, "1000");
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "1");
    }

    private void initTTS() {
        mTts = SpeechSynthesizer.createSynthesizer(this, null);// 初始化合成对象
        mTts.setParameter(SpeechConstant.PARAMS, null); // 清空参数
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); // 设置听写引擎
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        mTts.setParameter(SpeechConstant.SPEED, "50"); //设置合成语速
        mTts.setParameter(SpeechConstant.PITCH, "50"); //设置合成音调
        mTts.setParameter(SpeechConstant.VOLUME, "80"); //设置合成音量
    }

    //初始化监听器
    private InitListener mInitListener = code -> {
        Log.d(LOG_TAG, "init() code = " + code);
        if (code != ErrorCode.SUCCESS) {
            showTip("初始化失败，错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
        }
    };

    private void showTip(final String str) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT);
        mToast.show();
    }

    private void startContinuousRecording() {
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE
        );
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);

        isRecording = true;
        lastSpokenTime = System.currentTimeMillis();
        silenceTimer = new Timer();

        audioRecord.startRecording();

        // Start silence detection timer
        silenceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkSilenceTimeout();
            }
        }, 100, 100); // 每1秒检查一次

        // Start recording and saving in segments
        new Thread(new Runnable() {
            @Override
            public void run() {
                detectAndSaveAudioSegments();
            }
        }).start();
    }

    private void detectAndSaveAudioSegments() {
        byte[] audioBuffer = new byte[BUFFER_SIZE];
        DataOutputStream dos = null;
        audioFile = null;

        while (isRecording) {
            int readBytes = audioRecord.read(audioBuffer, 0, audioBuffer.length);
            if (readBytes > 0) {
                boolean speaking = isUserSpeaking(audioBuffer, readBytes);
                if (speaking) {
                    userSpeaking = true;
                    lastSpokenTime = System.currentTimeMillis();
                    if (dos == null) {
                        // 开始新的音频段
                        audioFile = startNewAudioSegmentFile();
                        dos = startNewAudioSegment(audioFile);
                    }
                    try {
                        dos.write(audioBuffer, 0, readBytes);
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error writing audio data", e);
                    }
                } else {
                    userSpeaking = false;
                    if (System.currentTimeMillis() - lastSpokenTime >= SILENCE_TIMEOUT_MS && dos != null) {
                        // 超时无声，保存音频文件
                        try {
                            dos.close();
                            dos = null;
                            if (audioFile != null) {
                                // 确保在主线程中执行语音识别或大模型API调用
                                final File finalAudioFile = audioFile;
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        executeStream(finalAudioFile);
                                    }
                                });
                            }
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Error closing audio file", e);
                        }
                    }
                }
            }
        }
        // Clean up when stopped
        if (dos != null) {
            try {
                dos.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error closing audio file", e);
            }
        }
        audioRecord.stop();
        audioRecord.release();
    }

    private File startNewAudioSegmentFile() {
        try {
            File audioDir = new File(getFilesDir(), AUDIO_DIRECTORY);
            if (!audioDir.exists()) {
                audioDir.mkdir();
            }
            String fileName = "audio_" + System.currentTimeMillis() + ".pcm";
            return new File(audioDir, fileName);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error creating audio file", e);
            return null;
        }
    }

    private DataOutputStream startNewAudioSegment(File audioFile) {
        try {
            return new DataOutputStream(new FileOutputStream(audioFile));
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error creating audio file", e);
            return null;
        }
    }

    private boolean isUserSpeaking(byte[] audioBuffer, int length) {
        long sumAmplitude = 0;
        for (int i = 0; i < length; i += 2) {
            short amplitude = (short) ((audioBuffer[i] & 0xff) | (audioBuffer[i + 1] << 8));
            sumAmplitude += Math.abs(amplitude);
        }
        return sumAmplitude / (length / 2) > 1000; // 阈值1000可以调整
    }

    private void checkSilenceTimeout() {
        if (System.currentTimeMillis() - lastSpokenTime >= SILENCE_TIMEOUT_MS) {
            lastSpokenTime = System.currentTimeMillis(); // 更新最后一次讲话时间以防多次检测
        }
        // 如果用户开始讲话且TTS正在播报，则停止TTS
        if (userSpeaking && ttsSpeaking) {
            stopTTS();
        }
    }

    /**
     * 执行音频流识别操作
     */
    private void executeStream(File file) {
        mIatResults.clear();
        // 设置音频来源为外部文件
        mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
        // 也可以像以下这样直接设置音频文件路径识别（要求设置文件在sdcard上的全路径）：
        // mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-2");
        // mIat.setParameter(SpeechConstant.ASR_SOURCE_PATH, "sdcard/XXX/XXX.pcm");
        int ret = mIat.startListening(mRecognizerListener);
        if (ret != ErrorCode.SUCCESS) {
            showTip("识别失败,错误码：" + ret + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            return;
        }
        try {
            InputStream open = new FileInputStream(file.getAbsolutePath().toString());
            byte[] buff = new byte[1280];
            while (open.available() > 0) {
                int read = open.read(buff);
                mIat.writeAudio(buff, 0, read);
            }
            mIat.stopListening();
        } catch (IOException e) {
            mIat.cancel();
            showTip("读取音频流失败");
        }
    }

    /**
     * 听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
//            showTip("开始说话");
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            Log.d(LOG_TAG, "onError " + error.getPlainDescription(true));
            showTip(error.getPlainDescription(true));
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
//            showTip("结束说话");
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(LOG_TAG, results.getResultString());
            printResult(results);
            if (isLast) {
                Log.d(LOG_TAG, "onResult 结束");
                if (!content.isEmpty()) {
                    Msg msg = new Msg(content, Msg.TYPE_SENT);
                    chatContent.add(content, Msg.TYPE_SENT);
                    msgList.add(msg);
//                    adapter.notifyItemInserted(msgList.size() - 1); // 当有新消息时，刷新ListView中的显示
//                    msgRecyclerView.scrollToPosition(msgList.size() - 1); // 将ListView定位到最后一行
                    Log.d("mumuchat", chatContent.getChatContent());
                    sendRequest(chatContent.getChatContent());
                }
            }

        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
//            showTip("当前正在说话，音量大小 = " + volume + " 返回音频数据 = " + data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }
    };

    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());
        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
        content = resultBuffer.toString();
    }

    private void showResponse(final String result) {
        runOnUiThread(() -> {
            Msg msg = new Msg(result, Msg.TYPE_RECEIVED);
            chatContent.add(result, Msg.TYPE_RECEIVED);
            Log.d("mumuchat1", chatContent.getChatContent());
            msgList.add(msg);
            adapter.notifyItemInserted(msgList.size() - 1); // 当有新消息时，刷新ListView中的显示
            msgRecyclerView.scrollToPosition(msgList.size() - 1); // 将ListView定位到最后一行
            ttsPlay(result);
        });
    }

    private String parseJSONWithGSON(String jsonData) {
        Log.d("mumuchat2", jsonData);
        Gson gson = new Gson();
        ChatBody chatBody = gson.fromJson(jsonData, ChatBody.class);
        return chatBody.getResult();
    }

    private void sendRequest(String chatContent) {
        new Thread(() -> {
            try {
                MediaType mediaType = MediaType.parse("application/json");
                Log.d("mumuchat3", chatContent);
                RequestBody body = RequestBody.create(mediaType, chatContent);
                Request request = new Request.Builder()
                        .url(MuMuAVChat.this.getString(R.string.api_path) + "access_token=" + MuMuAVChat.this.getString(R.string.access_token))//如何获取看README
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

    public void ttsPlay(String texts) {
        if (null == mTts) {
            // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
            this.showTip("创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化");
            return;
        }
        // 合成并播放
        int code = mTts.startSpeaking(texts, mTtsListener);
        if (code != ErrorCode.SUCCESS) {
            showTip("语音合成失败,错误码: " + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
        }
    }

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
        }

        @Override
        public void onSpeakPaused() {
        }

        @Override
        public void onSpeakResumed() {
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
        }

        @Override
        public void onCompleted(SpeechError error) {
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }
    };

    private void stopTTS() {
        if (mTts != null && ttsSpeaking) {
            mTts.stopSpeaking();
            ttsSpeaking = false;
        }
    }

    @Override
    protected void onDestroy() {
        if (audioRecord != null) {
            isRecording = false;
        }
        if (mTts != null) {
            mTts.stopSpeaking();
            mTts.destroy();
        }
        if (silenceTimer != null) {
            silenceTimer.cancel();
        }
        super.onDestroy();
    }


}
