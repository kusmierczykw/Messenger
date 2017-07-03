package com.kusmierczyk.wojciech.messenger;

import android.os.Bundle;
import android.widget.ImageButton;

/**
 * Created by wojciech on 03.07.2017.
 */

public class SettingsActivity extends MainActivity {
    private final String TAG = "SettingsActivity";

    private ImageButton uploadAvatar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initialization();

    }

    private void initialization() {

    }
}
