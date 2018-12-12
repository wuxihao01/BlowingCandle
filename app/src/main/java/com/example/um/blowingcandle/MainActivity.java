package com.example.um.blowingcandle;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.suke.widget.SwitchButton;

public class MainActivity extends Activity {

    static final int SAMPLE_RATE_IN_HZ = 8000;
    static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
    AudioRecord mAudioRecord;
    boolean isGetVoiceRun;
    private LinearLayout layout;
    boolean admin;
    Object mLock;
    private Handler mHandler = new Handler();
    private DevicePolicyManager policyManager;
    private ComponentName adminReceiver;

    //申请录音权限变量
    private static final int GET_RECODE_AUDIO = 1;
    private static String[] PERMISSION_AUDIO = {
            Manifest.permission.RECORD_AUDIO
    };

    //申请读写权限变量
    private static int REQUEST_PERMISSION_CODE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        layout=(LinearLayout)findViewById(R.id.layout);
        verifyAudioPermissions(MainActivity.this);
        verifyStoragePermissions(MainActivity.this);
        policyManager = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminReceiver = new ComponentName(this, ScreenOffAdminReceiver.class);
        admin = policyManager.isAdminActive(adminReceiver);
        if(!admin){
            Intent intent = new Intent();
            intent.setAction(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminReceiver);
            startActivity(intent);
            admin = policyManager.isAdminActive(adminReceiver);
        }
        mLock = new Object();
        getNoiseLevel();
        initButton();
    }

    private void initButton() {
        com.suke.widget.SwitchButton switchButton = (com.suke.widget.SwitchButton)
                findViewById(R.id.switch_button);

        switchButton.setChecked(true);
        switchButton.isChecked();
        switchButton.toggle();     //switch state
        switchButton.toggle(false);//switch without animation
        switchButton.setShadowEffect(false);//disable shadow effect
        switchButton.setEnabled(true);//disable button
        switchButton.setEnableEffect(true);//disable the switch animation
        switchButton.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                if(isChecked){
                    getNoiseLevel();
                }else{
                    stoprecorder();
                }
            }
        });
    }


    //申请录音权限
    public static void verifyAudioPermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.RECORD_AUDIO);
        //检测是否有录音的权限
        if (permission != PackageManager.PERMISSION_GRANTED) {
            //若没有录音权限，则会申请，会弹出对话框
            ActivityCompat.requestPermissions(activity, PERMISSION_AUDIO,
                    GET_RECODE_AUDIO);
        }
    }
    //申请读写权限
    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_PERMISSION_CODE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getNoiseLevel() {
        if (isGetVoiceRun) {
            Log.e("ummmm", "还在录着呢");
            return;
        }
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT,
                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        if (mAudioRecord == null) {
            Log.e("ummmm", "mAudioRecord初始化失败");
        }
        isGetVoiceRun = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.startRecording();
                short[] buffer = new short[BUFFER_SIZE];
                while (isGetVoiceRun) {
                    //r是实际读取的数据长度，一般而言r会小于buffersize
                    int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
                    long v = 0;
                    // 将 buffer 内容取出，进行平方和运算
                    for (int i = 0; i < buffer.length; i++) {
                        v += buffer[i] * buffer[i];
                    }
                    // 平方和除以数据总长度，得到音量大小。
                    double mean = v / (double) r;
                    double volume = 10 * Math.log10(mean);
                    Log.d("ummmm", "分贝值:" + volume);
                    Log.d("ok?", "admin:" + String.valueOf(admin));

                        if(volume>60) {

                            if (admin) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        layout.setBackground(getApplicationContext().getDrawable(R.drawable.dead));
                                    }
                                });
                                mHandler.postDelayed(new Runnable(){

                                    @Override
                                    public void run() {
                                        policyManager.lockNow();
                                    }
                                }, 2000);
                                finish();
                            } else {
                                Log.d("abc", "没有设备管理权限");
                            }
                        }

                        else if(volume>45){
                            mHandler.postDelayed(new Runnable(){

                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            layout.setBackground(getApplicationContext().getDrawable(R.drawable.goingdead));

                                        }
                                    });
                                }
                            }, 1000);
                        }
                        else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    layout.setBackground(getApplicationContext().getDrawable(R.drawable.living));

                                }
                            });
                        }

                    // 大概一秒十次
                    synchronized (mLock) {
                        try {
                            mLock.wait(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    public void stoprecorder(){
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;
        isGetVoiceRun=false;
    }

}
