package com.iflytek.voicedemo;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.text.HtmlCompat;

import com.iflytek.speech.setting.TtsSettings;
import com.iflytek.voicedemo.faceonline.OnlineFaceDemo;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences mSharedPreferences;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        initView();
        mSharedPreferences = getSharedPreferences(TtsSettings.PREFER_NAME, Activity.MODE_PRIVATE);
        boolean privacyConfirm = mSharedPreferences.getBoolean(SpeechApp.PRIVACY_KEY, false);
        if (!privacyConfirm) {
            showPrivacyDialog();
        }
    }

    private Intent intent;

    private void isPrivacyConfirm(Intent i) {
        boolean privacyConfirm = mSharedPreferences.getBoolean(SpeechApp.PRIVACY_KEY, false);
        if (privacyConfirm) {
            intent = i;
            mPermissionResult.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void initView() {
        TextView tipTv = (TextView) findViewById(R.id.tip);
        String buf = "当前APPID为：" +
                getString(R.string.app_id) + "\n" +
                getString(R.string.example_explain);
        tipTv.setText(buf);
        // 语音转写
        findViewById(R.id.iatBtn).setOnClickListener(v -> {
            isPrivacyConfirm(new Intent(MainActivity.this, IatDemo.class));
        });
        // 语法识别
        findViewById(R.id.asrBtn).setOnClickListener(v -> {
            isPrivacyConfirm(new Intent(MainActivity.this, AsrDemo.class));
        });
        // 语义理解
        findViewById(R.id.nlpBtn).setOnClickListener(v -> {
            showTip("请登录：http://www.xfyun.cn/ 下载aiui体验吧！");
        });
        // 语音合成
        findViewById(R.id.ttsBtn).setOnClickListener(v -> {
            isPrivacyConfirm(new Intent(MainActivity.this, TtsDemo.class));
        });
        // 语音评测
        findViewById(R.id.iseBtn).setOnClickListener(v -> {
            isPrivacyConfirm(new Intent(MainActivity.this, IseDemo.class));
        });
        // 人脸识别
        findViewById(R.id.faceBtn).setOnClickListener(v -> {
            isPrivacyConfirm(new Intent(MainActivity.this, OnlineFaceDemo.class));
        });
        // 人脸识别
        findViewById(R.id.vChatBtn).setOnClickListener(v -> {
            isPrivacyConfirm(new Intent(MainActivity.this, MuMuVChat.class));
        });
    }

    private Toast mToast;

    private void showTip(final String str) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT);
        mToast.show();
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

    private ActivityResultLauncher<String> mPermissionResult = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            result -> {
                if (result) {
                    if (intent !=null){
                        MainActivity.this.startActivity(intent);
                    }
                    SpeechApp.initializeMsc(MainActivity.this);
                }
            });

}
