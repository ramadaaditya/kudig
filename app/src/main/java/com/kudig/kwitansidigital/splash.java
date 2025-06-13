package com.kudig.kwitansidigital;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class splash extends AppCompatActivity {

    private static final long SPLASH_DURATION = 1500; // Durasi splash screen dalam milidetik (ms)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Menjalankan splash screen
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Pindah ke Activity berikutnya setelah splash screen selesai
                Intent intent = new Intent(splash.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DURATION);
    }
}
