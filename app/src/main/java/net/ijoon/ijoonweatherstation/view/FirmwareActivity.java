package net.ijoon.ijoonweatherstation.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import com.google.protobuf.InvalidProtocolBufferException;
import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.scnet_android.Connection;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.RendezvousSession;
import net.ijoon.scnet_android.SCNetAppCompatActivity;

public class FirmwareActivity extends SCNetAppCompatActivity {

    TextView mTvUpdateVersionText;
    TextView mTvDownLoadText;
    TextView mTvCurrentText;
    TextView mTvLatestText;
    Button mBtnVersionUpdate;
    View view;
    CustomApplication mCustomApplication;
    String mToken;
    String mSerial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firmware);
        brManager();
        mCustomApplication = (CustomApplication)getApplication();
        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
        mToken = preferences.getString("token","");
        mSerial = mCustomApplication.getEnvSensor().getSerial();
        mTvUpdateVersionText = findViewById(R.id.tvUpdateVersionText);
        mTvDownLoadText = findViewById(R.id.tvDownloadText);
        mTvCurrentText = findViewById(R.id.tvCurrentVersion);
        mTvLatestText = findViewById(R.id.tvLatestVersion);
        mBtnVersionUpdate = findViewById(R.id.btnVersionUpdate);
        mBtnVersionUpdate.setVisibility(View.GONE);
        mTvUpdateVersionText.setText(getResources().getString(R.string.textUsingLatestVersion));
        view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
        }
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + getResources().getString(R.string.textSoftWareVersion) + "</font>"));
        }
        if(mCustomApplication.getEnvSensor().getVersion() != mCustomApplication.getEnvSensor().getLatestVersion()) {
            mBtnVersionUpdate.setVisibility(View.VISIBLE);
            mTvUpdateVersionText.setText("최신 버전이 있습니다.");
            mTvLatestText.setText(mCustomApplication.getEnvSensor().getLatestVersion());
        }else {
            mTvUpdateVersionText.setText("최신 버전입니다.");
            mTvLatestText.setText(mCustomApplication.getEnvSensor().getLatestVersion());
        }
        mBtnVersionUpdate.setOnClickListener(v -> {
            envsensor.VersionUpdateRequest request = envsensor.VersionUpdateRequest.newBuilder().setSerial(mCustomApplication.getEnvSensor().getSerial()).setAccessToken(mToken).setVersion(mCustomApplication.getEnvSensor().getLatestVersion()).build();
            mCustomApplication.mServerSession.request(request);
        });
    }

    @Override
    public void onConnectedToRendezvousServer(String myPublicIP, String myPublicPort) {

    }

    @Override
    public void onConnecting(int connectionID) {

    }

    @Override
    public void onConnected(RendezvousSession rendezvousSession, Connection connection) {

    }

    @Override
    public void onConnectionIDCreated(RendezvousSession rendezvousSession, String ip, int port) {

    }

    @Override
    public void onTargetInvalid(String ip, int port) {

    }

    @Override
    public void onConnectFailed(int connectionID) {

    }

    @Override
    public void onConnectionUpdate(RendezvousSession rendezvousSession, Connection connection) {

    }

    @Override
    public void onConnectionRemoved(RendezvousSession rendezvousSession, Connection connection) {

    }

    @Override
    public void onDisconnected(int connectionID) {

    }

    @Override
    public void onResponse(MessageHeader messageHeader, final byte[] bodyBuf) {
        super.onResponse(messageHeader, bodyBuf);
        if (messageHeader == null) {
            return;
        }
        switch (messageHeader.mPacketType) {
            case 32: {
                try {
                    final envsensor.VersionUpdateResponse response = envsensor.VersionUpdateResponse.parseFrom(bodyBuf);
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }

                break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    public void brManager() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("delete");
        registerReceiver(mBroadcastReceiver, filter);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
            preferences.edit().clear().apply();
            preferences.edit().putString("id","");
            preferences.edit().putString("token","");
            preferences.edit().apply();
            FirmwareActivity.super.activityFinish();
            Intent intent1 = new Intent(FirmwareActivity.this, DeleteActivity.class);
            startActivity(intent1);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }
}