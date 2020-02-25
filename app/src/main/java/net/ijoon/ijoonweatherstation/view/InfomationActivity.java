package net.ijoon.ijoonweatherstation.view;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.protobuf.InvalidProtocolBufferException;

import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.RendezvousClientCallback;
import net.ijoon.scnet_android.SCNetAppCompatActivity;

public class InfomationActivity extends SCNetAppCompatActivity implements RendezvousClientCallback, SwipeRefreshLayout.OnRefreshListener{

        SwipeRefreshLayout mSwipeRefreshLayout;
        View view;
        CustomApplication mCustomApplication;

        envsensor.Sensor mEnvSensor;
        ActionBar actionBar;
        Button btnInfo;

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_infomation);
                mCustomApplication = (CustomApplication)getApplication();
                mEnvSensor = mCustomApplication.getEnvSensor();

                btnInfo = findViewById(R.id.btnInfo);
                mSwipeRefreshLayout = findViewById(R.id.swipe_layout);
                mSwipeRefreshLayout.setOnRefreshListener(this);

                view = getWindow().getDecorView();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
                getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
                getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
//                                mAppBar.setBackgroundColor(Color.parseColor("#56CBF2"));
                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
                actionBar = getSupportActionBar();

                btnInfo.setOnClickListener(v -> {
                        Intent intent = new Intent(InfomationActivity.this, SettingActivity.class);
                        startActivity(intent);
                });

        }


        @Override
        public void onResponse(MessageHeader messageHeader, final byte[] bodyBuf) {
                if(messageHeader == null) {
                        Log.d("yot132", "packetType is null");
                        return;
                }
                switch (messageHeader.mPacketType) {

                        case 4: {
                                runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                                mSwipeRefreshLayout.setRefreshing(false);
                                        }
                                });
                                try {
                                        final envsensor.EnvSensorData response = envsensor.EnvSensorData.parseFrom(bodyBuf);

                                } catch (InvalidProtocolBufferException e) {
                                        e.printStackTrace();
                                }
                                break;
                        }
                        default: {
                                Log.d("yot132","[onMessage] Unknown PacketType");
                                break;
                        }
                }
                super.onResponse(messageHeader, bodyBuf);
        }

        @Override
        protected void onResume() {
                super.onResume();
//                envsensor.StreamingStartRequest request1 = envsensor.StreamingStartRequest.newBuilder().setSerial(mEnvSensor.getSerial()).build();
//                mCustomApplication.mServerSession.request(request1);
        }

        @Override
        protected void onPause() {
                super.onPause();
//                envsensor.StreamingStopRequest request = envsensor.StreamingStopRequest.newBuilder().setSerial(mEnvSensor.getSerial()).build();
//                mCustomApplication.mServerSession.request(request);
        }

        @Override
        public void onRefresh() {
//                envsensor.StreamingStopRequest request = envsensor.StreamingStopRequest.newBuilder().setSerial(mEnvSensor.getSerial()).build();
//                mCustomApplication.mServerSession.request(request);
//                envsensor.StreamingStartRequest request1 = envsensor.StreamingStartRequest.newBuilder().setSerial(mEnvSensor.getSerial()).build();
//                mCustomApplication.mServerSession.request(request1);
        }
}