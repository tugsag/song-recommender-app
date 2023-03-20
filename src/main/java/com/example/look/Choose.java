package com.example.look;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Choose extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);
    }

    public void badChosen(View view){
        // Straight up pass simple prompt for model to handle
        String prompt = "bad";
        Intent intent = new Intent(this, Analysis.class);
        intent.putExtra("prompt", prompt);
        startActivity(intent);
    }

    public void goodChosen(View view){
        // Straight up pass simple prompt for model to handle
        String prompt = "good";
        Intent intent = new Intent(this, Analysis.class);
        intent.putExtra("prompt", prompt);
        startActivity(intent);
    }
}