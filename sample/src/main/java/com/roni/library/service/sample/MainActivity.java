package com.roni.library.service.sample;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.roni.library.service.NativeServiceSDK;
import com.roni.library.service.NativeServiceSDKConfig;

/**
 * Sample Activity demonstrating NativeServiceSDK usage.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the library
        NativeServiceSDKConfig config = new NativeServiceSDKConfig.Builder()
            .debugEnabled(true)
            .tag("SampleApp")
            .build();

        NativeServiceSDK.getInstance().init(getApplicationContext(), config);

        TextView statusView = findViewById(R.id.tv_status);
        boolean initialized = NativeServiceSDK.getInstance().isInitialized();
        statusView.setText("NativeServiceSDK initialized: " + initialized);
    }
}
