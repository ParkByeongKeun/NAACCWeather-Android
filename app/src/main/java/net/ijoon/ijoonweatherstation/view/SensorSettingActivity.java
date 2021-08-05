package net.ijoon.ijoonweatherstation.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.protobuf.InvalidProtocolBufferException;
import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.SCNetAppCompatActivity;

public class SensorSettingActivity extends SCNetAppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    CustomApplication mCustomApplication;
    SwipeRefreshLayout mSwipeRefreshLayout;
    View view;
    RelativeLayout mRlSensorShare;
    RelativeLayout mRlHandover;
    RelativeLayout mRlModifySensorName;
    RelativeLayout mRlFirmware;
    RelativeLayout mRlDeleteSensor;
    RelativeLayout mRlRecord;
    String mToken;
    String mNickname;
    String mId;
    private long mLastClickTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_setting);
        mCustomApplication = (CustomApplication) getApplicationContext();
        Intent intent = getIntent();
        mNickname = intent.getStringExtra("nickname");
        mSwipeRefreshLayout = findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
        mToken = preferences.getString("token","");
        mId = preferences.getString("id","");
        mRlSensorShare = findViewById(R.id.rlShare);
        mRlHandover = findViewById(R.id.rlHandover);
        mRlModifySensorName = findViewById(R.id.rlModifySensorName);
        mRlFirmware = findViewById(R.id.rlFirmware);
        mRlDeleteSensor = findViewById(R.id.rlDeleteSensor);
        mRlRecord = findViewById(R.id.rlRecord);
      
        view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
            getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
        }
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + mNickname + "</font>"));
        }
        mRlModifySensorName.setOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();
            showNicknameChangeDialog();
        });
        mRlFirmware.setOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();
            Intent intentFirmware = new Intent(SensorSettingActivity.this,FirmwareActivity.class);
            startActivity(intentFirmware);
        });
        mRlSensorShare.setOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();
            Intent intentShare = new Intent(SensorSettingActivity.this,SensorSharingActivity.class);
            startActivity(intentShare);
        });
        mRlHandover.setOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();
            Intent intentHandover = new Intent(SensorSettingActivity.this,StationHandOfOutActivity.class);
            startActivity(intent);
        });
        mRlDeleteSensor.setOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();
            showUnRegistDialog(mNickname + getResources().getString(R.string.textDeleteSensor));

        });
        mRlRecord.setOnClickListener(v -> {
            Intent intentRecord = new Intent(SensorSettingActivity.this, VelocityRecordActivity.class);
            startActivity(intentRecord);
        });
    }

    public void showUnRegistDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                (dialog, which) -> {
                    envsensor.UnregistSensorRequest unregisterRequest = envsensor.UnregistSensorRequest.newBuilder().setAccessToken(mToken).setSerial(mCustomApplication.getEnvSensor().getSerial()).build();
                    mCustomApplication.mServerSession.request(unregisterRequest);
                });
        runOnUiThread(() -> alertDialog.show());
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(1400);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runOnUiThread(() -> mSwipeRefreshLayout.setRefreshing(false));
        });
        thread.start();
    }

    private void showNicknameChangeDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        final EditText et = new EditText(SensorSettingActivity.this);
        et.setHint(R.string.text_new_sensor_name);
        et.setSingleLine(true);
        et.setLines(1);
        alertDialog.setView(et);
        alertDialog.setTitle(R.string.text_changing_sensor_name);
        alertDialog.setMessage(R.string.text_subtitle_changing_sensor_name);
        alertDialog.setPositiveButton(R.string.text_ok,
                (dialog, which) -> {
                    envsensor.SetSensorNameRequest request = envsensor.SetSensorNameRequest.newBuilder().setSerial(mCustomApplication.getEnvSensor().getSerial()).setName(et.getText().toString()).setAccessToken(mToken).build();
                    mCustomApplication.mServerSession.request(request);
                });
        runOnUiThread(() -> alertDialog.show());
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        et.setLayoutParams(params);
    }

    public void showSuccessAlertDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle(R.string.text_notifycation);
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton(R.string.text_ok,
                (dialog, which) -> {
                });
        runOnUiThread(() -> alertDialog.show());
    }

    public void showDeleteSuccessAlertDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog
                .setMessage(message);
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                (dialog, which) -> {
                    envsensor.GetSensorListRequest stationRequest = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
                    mCustomApplication.mServerSession.request(stationRequest);
                    dialog.cancel();
                });
        runOnUiThread(() -> alertDialog.show());
    }

    public void showDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog.setMessage(message);
        alertDialog.setNegativeButton(getResources().getString(R.string.textCancel),
                (dialog, which) -> {
                });
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                (dialog, which) -> {
                    Intent intent = new Intent(SensorSettingActivity.this, SensorAddActivity.class);
                    startActivity(intent);
                });
        runOnUiThread(() -> alertDialog.show());
    }

    @Override
    public void onResponse(MessageHeader messageHeader, final byte[] bodyBuf) {
        super.onResponse(messageHeader, bodyBuf);
        if (messageHeader == null) {
            return;
        }
        switch (messageHeader.mPacketType) {
            case 0: {
                break;
            }
            case 6: {
                break;
            }
            case 7 : {
                break;
            }
            case 8 : {
                //SetSensorPosition
                runOnUiThread(() -> {
                    try {
                        final envsensor.SetSensorPositionResponse response = envsensor.SetSensorPositionResponse.parseFrom(bodyBuf);
                        if(response.getResult() == envsensor.SetSensorPositionResponse.Result.success) {
                            envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
                            mCustomApplication.mServerSession.request(request);
                            showSuccessAlertDialog(getResources().getString(R.string.text_changed_sensor_position_success));
                        }else {
                            //set sensor position failed
                        }
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }
                });
                break;
            }
            case 11 : {
                //SetSensorNameRes
                runOnUiThread(() -> {
                    try {
                        final envsensor.SetSensorNameResponse response = envsensor.SetSensorNameResponse.parseFrom(bodyBuf);
                        if(response.getResult() == envsensor.SetSensorNameResponse.Result.success) {
                            envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
                            mCustomApplication.mServerSession.request(request);
                            showSuccessAlertDialog(getResources().getString(R.string.text_changed_sensor_name_success));
                        }
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }
                });
                break;
            }
            case 25 : {
                //unregist
                try {
                    final envsensor.UnregistSensorResponse response = envsensor.UnregistSensorResponse.parseFrom(bodyBuf);
                    if(response.getResult() == envsensor.UnregistSensorResponse.Result.success) {
                        showDeleteSuccessAlertDialog(getResources().getString(R.string.textDelete));
                    }else {
                        runOnUiThread(() -> {
                            showDialog(getResources().getString(R.string.textServerError));
                        });
                    }
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
}