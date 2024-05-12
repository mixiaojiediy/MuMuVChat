package com.mumuavchat.voicedemo;

import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class animTest extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anim_test);
        // 使状态栏隐藏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ImageView imageView = findViewById(R.id.imageView);
        imageView.setBackgroundResource(R.drawable.anim_emoticon_sequence);
        AnimationDrawable anim = (AnimationDrawable) imageView.getBackground();
        imageView.post(anim::start);
    }
}