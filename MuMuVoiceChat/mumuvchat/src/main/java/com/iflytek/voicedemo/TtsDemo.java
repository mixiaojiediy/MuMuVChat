package com.iflytek.voicedemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.speech.setting.TtsSettings;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;


public class TtsDemo extends Activity implements OnClickListener {
    private static String TAG = TtsDemo.class.getSimpleName();
    // 语音合成对象
    private SpeechSynthesizer mTts;

    // 默认发音人
    private String voicer = "xiaoyan";

    private String[] mCloudVoicersEntries;
    private String[] mCloudVoicersValue;
    private String texts = "";

    // 缓冲进度
    private int mPercentForBuffering = 0;
    // 播放进度
    private int mPercentForPlaying = 0;

    // 云端/本地单选按钮
    private RadioGroup mRadioGroup;
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    private Toast mToast;
    private SharedPreferences mSharedPreferences;
    private File pcmFile;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.ttsdemo);
        texts = getResources().getString(R.string.text_tts_source);
        initLayout();
        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(TtsDemo.this, mTtsInitListener);
        // 云端发音人名称列表
        mCloudVoicersEntries = getResources().getStringArray(R.array.voicer_cloud_entries);
        mCloudVoicersValue = getResources().getStringArray(R.array.voicer_cloud_values);
        mSharedPreferences = getSharedPreferences(TtsSettings.PREFER_NAME, MODE_PRIVATE);
    }

    /**
     * 初始化Layout。
     */
    private void initLayout() {
        findViewById(R.id.tts_play).setOnClickListener(TtsDemo.this);
        findViewById(R.id.tts_cancel).setOnClickListener(TtsDemo.this);
        findViewById(R.id.tts_pause).setOnClickListener(TtsDemo.this);
        findViewById(R.id.tts_resume).setOnClickListener(TtsDemo.this);
        findViewById(R.id.image_tts_set).setOnClickListener(TtsDemo.this);
        findViewById(R.id.tts_btn_person_select).setOnClickListener(TtsDemo.this);

        mRadioGroup = ((RadioGroup) findViewById(R.id.tts_rediogroup));
        mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.tts_radioCloud) {
                    mEngineType = SpeechConstant.TYPE_CLOUD;
                }

            }
        });
    }

    @Override
    public void onClick(View view) {
        if (null == mTts) {
            // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
            this.showTip("创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化");
            return;
        }

        int id = view.getId();
        if (id == R.id.image_tts_set) {
            if (SpeechConstant.TYPE_CLOUD.equals(mEngineType)) {
                Intent intent = new Intent(TtsDemo.this, TtsSettings.class);
                startActivity(intent);
            } else {
                showTip("请前往xfyun.cn下载离线合成体验");
            }
            // 开始合成
            // 收到onCompleted 回调时，合成结束、生成合成音频
            // 合成的音频格式：只支持pcm格式
        } else if (id == R.id.tts_play) {
            pcmFile = new File(getExternalCacheDir().getAbsolutePath(), "tts_pcmFile.pcm");
            pcmFile.delete();
            texts = ((EditText) findViewById(R.id.tts_text)).getText().toString();
            // 设置参数
            setParam();
            // 合成并播放
            int code = mTts.startSpeaking(texts, mTtsListener);
//			/**
//			 * 只保存音频不进行播放接口,调用此接口请注释startSpeaking接口
//			 * text:要合成的文本，uri:需要保存的音频全路径，listener:回调接口
//			*/
//                String path = getExternalFilesDir("msc").getAbsolutePath() + "/tts.pcm";
//                //  synthesizeToUri 只保存音频不进行播放
//                int code = mTts.synthesizeToUri(texts, path, mTtsListener);

            if (code != ErrorCode.SUCCESS) {
                showTip("语音合成失败,错误码: " + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }
            // 取消合成
        } else if (id == R.id.tts_cancel) {
            mTts.stopSpeaking();
            // 暂停播放
        } else if (id == R.id.tts_pause) {
            mTts.pauseSpeaking();
            // 继续播放
        } else if (id == R.id.tts_resume) {
            mTts.resumeSpeaking();
            // 选择发音人
        } else if (id == R.id.tts_btn_person_select) {
            showPersonSelectDialog();
        }
    }

    private int selectedNum = 0;

    /**
     * 发音人选择。
     */
    private void showPersonSelectDialog() {
        // 选择在线合成
        if (mRadioGroup.getCheckedRadioButtonId() == R.id.tts_radioCloud) {
            new AlertDialog.Builder(this).setTitle("在线合成发音人选项")
                    .setSingleChoiceItems(mCloudVoicersEntries, // 单选框有几项,各是什么名字
                            selectedNum, // 默认的选项
                            new DialogInterface.OnClickListener() { // 点击单选框后的处理
                                public void onClick(DialogInterface dialog,
                                                    int which) { // 点击了哪一项
                                    voicer = mCloudVoicersValue[which];
                                    if ("catherine".equals(voicer) || "henry".equals(voicer) || "vimary".equals(voicer)) {
                                        ((EditText) findViewById(R.id.tts_text)).setText(R.string.text_tts_source_en);
                                    } else {
                                        ((EditText) findViewById(R.id.tts_text)).setText(R.string.text_tts_source);
                                    }
                                    selectedNum = which;
                                    dialog.dismiss();
                                }
                            }).show();
        }
    }

    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            showTip("开始播放");
        }

        @Override
        public void onSpeakPaused() {
            showTip("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            showTip("继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
            Log.e("MscSpeechLog_", "percent =" + percent);
            mPercentForBuffering = percent;
            showTip(String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
            Log.e("MscSpeechLog_", "percent =" + percent);
            mPercentForPlaying = percent;
            showTip(String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));

            SpannableStringBuilder style = new SpannableStringBuilder(texts);
            Log.e(TAG, "beginPos = " + beginPos + "  endPos = " + endPos);
            style.setSpan(new BackgroundColorSpan(Color.RED), beginPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ((EditText) findViewById(R.id.tts_text)).setText(style);
        }

        @Override
        public void onCompleted(SpeechError error) {
            showTip("播放完成");
            if (error != null) {
                showTip(error.getPlainDescription(true));
                return;
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            //	 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            //	 若使用本地能力，会话id为null
            if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
                Log.d(TAG, "session id =" + sid);
            }
            // 当设置 SpeechConstant.TTS_DATA_NOTIFY 为1时，抛出buf数据
            if (SpeechEvent.EVENT_TTS_BUFFER == eventType) {
                byte[] buf = obj.getByteArray(SpeechEvent.KEY_EVENT_TTS_BUFFER);
                Log.e(TAG, "EVENT_TTS_BUFFER = " + buf.length);
                // 保存文件
                appendFile(pcmFile, buf);
            }

        }
    };

    private void showTip(final String str) {
        runOnUiThread(() -> {
            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT);
            mToast.show();
        });
    }

    /**
     * 参数设置
     *
     * @return
     */
    private void setParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        // 根据合成引擎设置相应参数
        if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            // 支持实时音频返回，仅在 synthesizeToUri 条件下支持
            mTts.setParameter(SpeechConstant.TTS_DATA_NOTIFY, "1");
            //	mTts.setParameter(SpeechConstant.TTS_BUFFER_TIME,"1");

            // 设置在线合成发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
            //设置合成语速
            mTts.setParameter(SpeechConstant.SPEED, mSharedPreferences.getString("speed_preference", "50"));
            //设置合成音调
            mTts.setParameter(SpeechConstant.PITCH, mSharedPreferences.getString("pitch_preference", "50"));
            //设置合成音量
            mTts.setParameter(SpeechConstant.VOLUME, mSharedPreferences.getString("volume_preference", "50"));
        } else {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            mTts.setParameter(SpeechConstant.VOICE_NAME, "");

        }

        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, mSharedPreferences.getString("stream_preference", "3"));
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "false");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "pcm");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH,
                getExternalFilesDir("msc").getAbsolutePath() + "/tts.pcm");
    }

    @Override
    protected void onDestroy() {
        if (null != mTts) {
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
        super.onDestroy();
    }


    /**
     * 给file追加数据
     */
    private void appendFile(File file, byte[] buffer) {
        try {
            if (!file.exists()) {
                boolean b = file.createNewFile();
            }
            RandomAccessFile randomFile = new RandomAccessFile(file, "rw");
            randomFile.seek(file.length());
            randomFile.write(buffer);
            randomFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
