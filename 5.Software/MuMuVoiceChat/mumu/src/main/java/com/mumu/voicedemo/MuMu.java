package com.mumu.voicedemo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

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
import com.mumu.speech.setting.TtsSettings;
import com.mumu.speech.util.JsonParser;
import com.mumu.voicedemo.msg.ChatBody;
import com.mumu.voicedemo.msg.ChatContent;
import com.mumu.voicedemo.msg.Msg;

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
public class MuMu extends AppCompatActivity {
    private static final String LOG_TAG = "MuMu";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private static final String AUDIO_DIRECTORY = "MuMu";
    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
    );
    private static final int SILENCE_TIMEOUT_MS = 2000; // 2秒无声超时
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

    private HashMap<String, String> mIatResults = new LinkedHashMap<>(); // 用HashMap存储听写结果
    private String content;
    File audioFile;

    private Toast mToast;

    private Response myResponse;
    static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder().build();
    private final ChatContent chatContent = new ChatContent();

    private Handler mainHandler;
    private int speakingThreshold;

    private SharedPreferences mSharedPreferences;
    private AnimationDrawable anim;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable animationControlTask = new Runnable() {
        @Override
        public void run() {
            controlAnimation();
            handler.postDelayed(this, 100); // 每100毫秒检查一次
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mumu);

        mSharedPreferences = getSharedPreferences(TtsSettings.PREFER_NAME, Activity.MODE_PRIVATE);
        boolean privacyConfirm = mSharedPreferences.getBoolean(SpeechApp.PRIVACY_KEY, false);
        if (!privacyConfirm) {
            showPrivacyDialog();
        }
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        SpeechApp.initializeMsc(MuMu.this);
        initView();
        initIat();
        initTTS();
        initAudioRecord();
        speakingThreshold = getDynamicThreshold(3); // 采集3秒的背景噪音,实测安静环境下是503
        mainHandler = new Handler(Looper.getMainLooper());
        startContinuousRecording();
    }

    private void initView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 使用Android 10 (API级别 29) 或更高版本的功能
            // 使用WindowCompat和WindowInsetsController来处理全屏和隐藏系统UI
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            final WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // 使用旧版本的备选方案
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
        ImageView imageView = findViewById(R.id.imageView);
        imageView.setBackgroundResource(R.drawable.anim_emoticon_sequence);
        anim = (AnimationDrawable) imageView.getBackground();
//        imageView.post(anim::start);
//        anim.stop();
        // 启动任务
        handler.post(animationControlTask);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) {
            finish();
        }
    }

    private void initIat() {
        mIat = SpeechRecognizer.createRecognizer(this, mInitListener); //创建一个语音识别器
        mIat.setParameter(SpeechConstant.PARAMS, null); // 清空参数
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); // 设置听写引擎
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json"); // 设置返回结果格式
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, "4000");
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, "4000");
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "1");
    }

    private void initTTS() {
        mTts = SpeechSynthesizer.createSynthesizer(this, mInitListener);// 初始化合成对象
        mTts.setParameter(SpeechConstant.PARAMS, null); // 清空参数
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); // 设置听写引擎
        mTts.setParameter(SpeechConstant.VOICE_NAME, "x4_ningning");
//        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
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

    private void initAudioRecord() {
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
    }

    private int getDynamicThreshold(int seconds) {
        byte[] audioBuffer = new byte[BUFFER_SIZE];
        long sumAmplitude = 0;
        int totalSamples = 0;

        audioRecord.startRecording();
        long endTime = System.currentTimeMillis() + seconds * 1000;
        while (System.currentTimeMillis() < endTime) {
            int readBytes = audioRecord.read(audioBuffer, 0, audioBuffer.length);
            if (readBytes > 0) {
                for (int i = 0; i < readBytes; i += 2) {
                    short amplitude = (short) ((audioBuffer[i] & 0xff) | (audioBuffer[i + 1] << 8));
                    sumAmplitude += Math.abs(amplitude);
                }
                totalSamples += readBytes / 2;
            }
        }
        audioRecord.stop();
        return (int) (sumAmplitude / totalSamples) + 2000; // 动态阈值加500的偏移
    }


    private void showTip(final String str) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT);
        mToast.show();
    }

    private void startContinuousRecording() {
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
            boolean speaking = isUserSpeaking(audioBuffer, readBytes);
            if (speaking) {
                userSpeaking = true;
                lastSpokenTime = System.currentTimeMillis();
            } else {
                if (System.currentTimeMillis() - lastSpokenTime >= SILENCE_TIMEOUT_MS && dos != null) {
                    userSpeaking = false;
                    // 超时无声，保存音频文件
                    try {
                        dos.close();
                        dos = null;
                        if (audioFile != null) {
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
            if (userSpeaking) {
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
        return sumAmplitude / (length / 2) > 3000; // 阈值1000可以调整
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
                    Log.d("mumuchat", chatContent.getChatContent());
                    sendRequest(chatContent.getChatContent());
                }
            }

        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
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
                        .url(MuMu.this.getString(R.string.api_path) + "access_token=" + MuMu.this.getString(R.string.access_token))//如何获取看README
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
            ttsSpeaking = true;
        }

        @Override
        public void onSpeakPaused() {
            ttsSpeaking = false;
        }

        @Override
        public void onSpeakResumed() {
            ttsSpeaking = true;
        }

        @Override
        public void onCompleted(SpeechError error) {
            ttsSpeaking = false;
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
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

    public void playPCMFile(String filePath) {
        AudioTrack audioTrack = null;
        try {
            FileInputStream fis = new FileInputStream(filePath);
            int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
            audioTrack.play();

            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                audioTrack.write(buffer, 0, bytesRead);
            }
            audioTrack.stop();
            audioTrack.release();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (audioTrack != null) {
                audioTrack.release();
            }
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
        handler.removeCallbacks(animationControlTask); // 停止任务防止内存泄漏
        super.onDestroy();
    }

    private void showPrivacyDialog() {
        AppCompatTextView textView = new AppCompatTextView(this);
        textView.setPadding(100, 50, 100, 50);
        textView.setText(
                HtmlCompat.fromHtml("我们非常重视对您个人信息的保护，承诺严格按照讯飞开放平台<font color='#3B5FF5'>《隐私政策》</font>保护及处理您的信息，是否确定同意？",
                        HtmlCompat.FROM_HTML_MODE_LEGACY));
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://www.xfyun.cn/doc/policy/sdk_privacy.html"));
                startActivity(intent);
            }
        });
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("温馨提示")
                .setView(textView)
                .setPositiveButton("同意", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSharedPreferences.edit().putBoolean(SpeechApp.PRIVACY_KEY, true).apply();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("不同意", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSharedPreferences.edit().putBoolean(SpeechApp.PRIVACY_KEY, false).apply();
                        finish();
                        System.exit(0);
                    }
                })
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void controlAnimation() {
        if (ttsSpeaking && !anim.isRunning()) {
            anim.start();
        } else if (!ttsSpeaking && anim.isRunning()) {
            anim.stop();
            anim.selectDrawable(0);  // 选择第一帧，索引为0
        }
    }


}
