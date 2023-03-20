package com.example.look;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Analysis extends AppCompatActivity {
    String prompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            this.prompt = extras.getString("prompt");
        }
        this.loadAndAnalyze();
    }

    private JSONObject openJson(String name) {
        String contents = "";
        try{
            InputStream is = getAssets().open(name);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            contents = new String(buffer);
        }
        catch(IOException e){
            System.out.println(name + " doesn't exist");
        }
        JSONObject json;
        try{
            json = new JSONObject(contents);
        }
        catch(JSONException j){
            System.out.println("JSON couldn't decode");
            json = null;
        }
        return json;
    }

    private List<String> getStopwords(){
        JSONObject json = this.openJson("stopwords.json");
        List<String> stopwords = new ArrayList<String>();
        try {
            JSONArray raw = json.getJSONArray("stopwords");
            for (int i = 0; i < raw.length(); i++) {
                stopwords.add(raw.getString(i));
            }
        }
        catch (JSONException e){
            System.out.println("Stopwords didn't work");
        }
        return stopwords;
    }

    private String getEncoding(int output){
        JSONObject json = this.openJson("encode2label.json");
        String label = "";
        try{
            label = json.getString(String.valueOf(output));
        } catch(JSONException e){
            System.out.println("JSON reading went wrong");
        }

        return label;
    }

    private List<String> getSongList(String label){
        JSONObject json = this.openJson("song_data.json");
        List<String> songs = new ArrayList<String>();
        try{
            JSONArray rawSongs = json.getJSONArray(label);
            Random random = new Random();
            for (int i = 0; i < 5; i++) {
                songs.add(rawSongs.getString(random.nextInt(rawSongs.length())));
            }
        } catch(JSONException e){
            System.out.println("Song JSON decode failed");
        }

        return songs;
    }

    private List<String> cleanTextData(){
        // This is a manual preprocessing of text data: Stanford CoreNLP is perhaps too heavy for android
        // Text preprocessing is not all that difficult anyway
        String[] promptArr = this.prompt.split(" ");
        List<String> stopwords = this.getStopwords();
        List<String> cleanedText = new ArrayList<String>();
        for(String s: promptArr){
            String sl = s.replaceAll("[^A-Za-z]+", "").toLowerCase();
            if(!stopwords.contains(sl)){
                cleanedText.add(sl);
            }
        }
        // Can't lemmatize since not tokenizing. WTF

        return cleanedText;
    }

    private List<Long> words2seq(List<String> cleanedText){
        // Seq length is 30
        JSONObject vocab = this.openJson("vocab.json");
        List<Long> seq = new ArrayList<Long>();

        try {
            for (String s : cleanedText) {
                // If entry is in vocab
                if (vocab.has(s)) {
                    seq.add(vocab.getLong(s));
                }
                // If string not in vocab, add <UNK> token
                else {
                    seq.add(vocab.getLong("<UNK>"));
                }
            }
        }
        catch (JSONException e){
            System.out.println("Words to seq didn't work");
        }

        // Just straight up double if too many empty
        List<Long> seqLong = new ArrayList<Long>();
        seqLong.addAll(seq);
        if(seq.size() <= 10){
            int times = Math.floorDiv(20, seq.size());
            for(int i = 0; i < times; i++) seqLong.addAll(seq);
        }

        if(seqLong.size() < 30){
            // Pad (0)
            while(seqLong.size() != 30){
                seqLong.add((long)0);
            }
        }
        else{
            seqLong = seqLong.subList(0, 30);
        }

        return seqLong;
    }

    private String assetPath(Context context, String name) throws IOException{
        File file = new File(context.getFilesDir(), name);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(name)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    private void decodeOutput(float[] outputs){
        TextView outText = findViewById(R.id.outputDisp);
        TextView songList = findViewById(R.id.songList);

        int output = 0;
        float maximum = 0;
        for(int i = 0; i < outputs.length; i++){
            if(outputs[i] > maximum){
                output = i;
                maximum = outputs[i];
            }
        }
        String label = this.getEncoding(output);
        outText.setText("Detected mood: " + label);

        List<String> songs = this.getSongList(label);
        songList.setText(String.join("\n", songs));
    }

    public void loadAndAnalyze(){
        List<String> cleanedText = this.cleanTextData();
        List<Long> seq = this.words2seq(cleanedText);
        long[] inputs = new long[seq.size()];
        for(int i = 0; i < seq.size(); i++){
            inputs[i] = seq.get(i);
        }
        System.out.println(seq);
        System.out.println(cleanedText);

        String spath = null;
        try {
             spath = this.assetPath(getApplicationContext(), "mobile_model.ptl");
        } catch(IOException e){
            System.out.println("couldn't load model artifacts");
            finish();
        }
        System.out.println(spath);
        Module module = Module.load(spath);

        // Prepare input and output tensors
        long[] outputShape = new long[]{1};
        long[] inputShape = new long[]{1, 30};
        Tensor inputTensor = Tensor.fromBlob(inputs, inputShape);
        System.out.println("input and outputs are prepared");
        // model forward
        Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
        float[] outputs = outputTensor.getDataAsFloatArray();

        System.out.println("Model output: " + outputs);
        this.decodeOutput(outputs);
    }

    public void startOver(View view){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}