package com.example.look;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    private EditText text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = findViewById(R.id.written_prompt);
    }

    public void choose(View view){
        Intent intent = new Intent(this, Choose.class);
        startActivity(intent);
    }

    public void textEntry(View view){
        String prompt = text.getText().toString();
        Intent intent = new Intent(this, Analysis.class);
        intent.putExtra("prompt", prompt);
        startActivity(intent);
    }
}