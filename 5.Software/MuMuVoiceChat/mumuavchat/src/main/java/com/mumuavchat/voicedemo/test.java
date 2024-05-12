package com.mumuavchat.voicedemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.mumuavchat.speech.util.JsonParser;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;

@SuppressWarnings("all")
public class test extends AppCompatActivity {

    // View
    private Button startRecord;
    private Button stopRecord;
    private Button playRecord;
    private TextView contentRecord;

    // 系统的音频文件
    private AudioRecord audioRecord;
    private File pcmFile;
    private final String TAG = "test";
    private boolean isRecording;
    private int bufferSize;
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private AudioManager audioManager;

    private SpeechRecognizer mIat;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<>();
    private Toast mToast;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        initView();
        createAudioRecord();
        initPCMFile();
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 0x123);
        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = SpeechRecognizer.createRecognizer(this, null);
    }

    private void initView() {
        // 获取程序界面中的两个按钮
        startRecord = findViewById(R.id.startRecord);
        stopRecord = findViewById(R.id.stopRecord);
        playRecord = findViewById(R.id.playRecord);
        contentRecord = findViewById(R.id.contentRecord);
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败，错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }
        }
    };

    private void showTip(final String str) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT);
        mToast.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0x123 && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE, channelConfig, audioFormat, bufferSize);
            View.OnClickListener listener = source ->
            {
                int id = source.getId();// 单击录音按钮
                if (id == R.id.startRecord) {
                    startRecord();
                } else if (id == R.id.stopRecord) {
                    stopRecord();
                } else if (id == R.id.playRecord) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                playPCMFile(getFilesDir().toString() + "/raw.pcm");
                            } catch (Exception e) {
                                Log.e("AudioPlay", "Error playing PCM file", e);
                            }
                        }
                    }).start();
                    executeStream(pcmFile);
                }
            };
            // 为两个按钮的单击事件绑定监听器
            startRecord.setOnClickListener(listener);
            stopRecord.setOnClickListener(listener);
            playRecord.setOnClickListener(listener);
        }
    }

    private void createAudioRecord() {
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);
    }

    private void initPCMFile() {
        pcmFile = new File(getFilesDir().toString() + "/raw.pcm");
        Log.d(TAG, "initPCMFile: pcmFile=" + pcmFile);
    }

    private void startRecord() {
        if (pcmFile.exists()) {
            pcmFile.delete();
        }
        isRecording = true;
        final byte[] buffer = new byte[bufferSize];

        audioRecord.startRecording();
        new Thread(() -> {
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(pcmFile);
                while (isRecording) {
                    int readStatus = audioRecord.read(buffer, 0, bufferSize);
                    Log.d(TAG, "run: readStatus=" + readStatus);
                    fileOutputStream.write(buffer);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "run: ", e);
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void stopRecord() {
//        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        audioManager.setMode(AudioManager.MODE_NORMAL);
//        audioManager.setSpeakerphoneOn(false);
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
        }
    }


    public void playPCMFile(String filePath) {
        AudioTrack audioTrack = null;
        try {
            FileInputStream fis = new FileInputStream(filePath);
            int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL_CONFIG,
                    AUDIO_FORMAT, bufferSize, AudioTrack.MODE_STREAM);
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

    /**
     * 参数设置
     *
     * @return
     */
    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 设置语言区域
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, "4000");

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, "4000");

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "1");

        // 设置音频保存路径，保存音频格式支持pcm、wav.
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH,
                getExternalFilesDir("msc").getAbsolutePath() + "/iat.wav");
    }


    /**
     * 执行音频流识别操作
     */
    private void executeStream(File file) {
        contentRecord.setText(null);// 清空显示内容
        mIatResults.clear();
        // 设置参数
        setParam();
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
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            Log.d(TAG, "onError " + error.getPlainDescription(true));
            showTip(error.getPlainDescription(true));

        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            showTip("结束说话");
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, results.getResultString());
            if (isLast) {
                Log.d(TAG, "onResult 结束");
            }
            printResult(results);
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前正在说话，音量大小 = " + volume + " 返回音频数据 = " + data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    /**
     * 显示结果
     */
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
        contentRecord.setText(resultBuffer.toString());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIat != null) {
            // 退出时释放连接
            mIat.cancel();
            mIat.destroy();
        }
    }


}