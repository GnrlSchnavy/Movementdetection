package com.example.stemmeriky.testapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.Serializable;


public class MainActivity extends Activity implements Serializable{
    private boolean serviceRunning = false;
    public int progressChangedValue = 0;
    TextView t;
    SeekBar simpleSeekBar;
    String s;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        t = findViewById(R.id.textView);
        t.setText(String.format("%d",50));
        EventBus.getDefault().register(this);
        simpleSeekBar=findViewById(R.id.simpleSeekBar);
        simpleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
                t.setText(String.format("%d", progressChangedValue));
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "Seek bar progress is :" + progressChangedValue,
                        Toast.LENGTH_SHORT).show();
                EventBus.getDefault().post(new ThresholChangeEvent(progressChangedValue));

            }
        });

    }

    @Subscribe
    public void onDestroy(){
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent event){
        Toast.makeText(getApplicationContext(), event.getMessage(), Toast.LENGTH_SHORT).show();
    }

    public void startService(View view){
        if(!serviceRunning) {
            Toast.makeText(this, "started", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, MainActivity_show_camera_service.class);
            s = t.getText().toString();
            i.putExtra("Threshold", s);
            startService(i);
            serviceRunning = true;
        }
        else{
            Toast.makeText(this,"service already running", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopService(View view) {
        if(serviceRunning) {
            Toast.makeText(this, "stopped", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(this, MainActivity_show_camera_service.class);
            stopService(i);
            serviceRunning=false;
        }
        else{
            Toast.makeText(this, "service already stopped",Toast.LENGTH_SHORT).show();
        }
    }

    public void runStandardCameraFunction(View view){
        Intent intent = new Intent(this,MainActivity_show_camera.class);
        startActivity(intent);
    }



}