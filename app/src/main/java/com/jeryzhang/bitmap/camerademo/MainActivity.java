package com.jeryzhang.bitmap.camerademo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

/**
 * https://blog.csdn.net/YuQing_Cat/article/details/83586796
 */
public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_OK_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPermission();
    }


    private void getPermission() {
        if (Build.VERSION.SDK_INT > 22) {
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_OK_CODE);
                Log.i("MainActivity", "requestPermissions");
            } else {
                Log.i("MainActivity", "已经获取了权限");
                setContentView(R.layout.activity_main);
            }
        } else {
            Log.i("MainActivity", "这个说明系统版本在6.0之下，不需要动态获取权限。");
            setContentView(R.layout.activity_main);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_OK_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setContentView(R.layout.activity_main);
        }
    }
}
