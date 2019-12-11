package net.ijoon.ijoonweatherstation;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import net.ijoon.scnet_android.RendezvousClientCallback;
import net.ijoon.scnet_android.SCNetAppCompatActivity;

public class SensorAddActivity extends SCNetAppCompatActivity implements RendezvousClientCallback {

    View view;
    EditText mEtSerial;
    EditText mEtAuth;
    Button mBtnRegist;
    CustomApplication mCustomApplication;
    RelativeLayout mRlRoot;
    String mToken;
    InputMethodManager imm;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_sensor);
    }
}