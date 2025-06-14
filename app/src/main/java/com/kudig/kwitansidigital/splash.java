package com.kudig.kwitansidigital;

import static com.kudig.kwitansidigital.utils.ConstantsKt.SPLASH_DURATION;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class splash extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(splash.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
}
