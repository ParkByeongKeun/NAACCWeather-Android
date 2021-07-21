package net.ijoon.ijoonweatherstation.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import com.google.protobuf.InvalidProtocolBufferException;
import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.util.ParallaxScollListView;
import net.ijoon.ijoonweatherstation.util.RecordListViewAdapter;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.SCNetAppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class VelocityRecordActivity extends SCNetAppCompatActivity {

    CustomApplication mCustomApplication;
    ActionBar actionBar;
    private ParallaxScollListView mListView;
    String mToken;
    List<envsensor.Sensor> mSensor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_velocity_record);
        mCustomApplication = (CustomApplication) getApplicationContext();
        actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + getResources().getString(R.string.textVelocityRecord) + "</font>"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
        }
        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
        mToken = preferences.getString("token","");
        envsensor.GetSensorListRequest getSensorRequest = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
        mCustomApplication.mServerSession.request(getSensorRequest);
        mListView = findViewById(R.id.layout_listview);
//        mListView.setOnItemClickListener((parent, view, position, id) -> {
//            mCustomApplication.setEnvSensor(mSensor.get(position));
//            Intent intent = new Intent(VelocityRecordActivity.this, MonitorActivity.class);
//            startActivity(intent);
//        });
    }

    @Override
    public void onResponse(MessageHeader messageHeader, final byte[] bodyBuf) {
        super.onResponse(messageHeader, bodyBuf);
        if (messageHeader == null) {
            Log.d("yot132", "packetType is null");
            return;
        }
        switch (messageHeader.mPacketType) {
            case 6: {
                try {
                    RecordListViewAdapter adapter = new RecordListViewAdapter();
                    final envsensor.GetSensorListResponse response = envsensor.GetSensorListResponse.parseFrom(bodyBuf);
                    mSensor = new ArrayList<>();
                    for(int i = 0 ; i < response.getSensorCount() ; i ++) {
                        mSensor.add(response.getSensor(i));
                        String s = "4-1생 M4 극동건설: 16.0m/s 타워크레인의 설치, 수리, 점검 또는 해체작업을 중지해야합니다.";

                        adapter.addItem(R.drawable.logo_weather,s,"2021/07/21(화) 오후 21:11",true);

                    }
                    runOnUiThread(() -> mListView.setAdapter(adapter));
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }
}