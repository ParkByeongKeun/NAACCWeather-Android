package net.ijoon.ijoonweatherstation.view;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.protobuf.InvalidProtocolBufferException;
import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.SCNetAppCompatActivity;

public class DevActivity extends SCNetAppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{

        SwipeRefreshLayout mSwipeRefreshLayout;
        View view;
        CustomApplication mCustomApplication;
        RelativeLayout mRlSharedUsersList;
        RelativeLayout mRlSharing;
        RelativeLayout mRlSWVersion;
        TextView mTvLumi;
        TextView mTvTemp;
        TextView mTvRain;
        TextView mTvHum;
        TextView mTvDirection;
        TextView mTvPre;
        TextView mTvVelocity;
        envsensor.Sensor mEnvSensor;
        ActionBar actionBar;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_dev);
                mCustomApplication = (CustomApplication)getApplication();
                mEnvSensor = mCustomApplication.getEnvSensor();
                mRlSharedUsersList = findViewById(R.id.rlSharedUserList);
                mRlSharing = findViewById(R.id.rlSharing);
                mRlSWVersion = findViewById(R.id.rlSWVersion);
                mTvLumi = findViewById(R.id.tvLuminosity);
                mTvTemp = findViewById(R.id.tvTemp);
                mTvRain = findViewById(R.id.tvRain);
                mTvHum = findViewById(R.id.tvHumidity);
                mTvDirection = findViewById(R.id.tvDirection);
                mTvPre = findViewById(R.id.tvPre);
                mTvVelocity = findViewById(R.id.tvVelocity);
                mSwipeRefreshLayout = findViewById(R.id.swipe_layout);
                mSwipeRefreshLayout.setOnRefreshListener(this);
                view = getWindow().getDecorView();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
                        getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
                }
                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
                actionBar = getSupportActionBar();
                actionBar.setTitle(mEnvSensor.getName());
        }


        @Override
        public void onResponse(MessageHeader messageHeader, final byte[] bodyBuf) {
                if(messageHeader == null) {
                        Log.d("yot132", "packetType is null");
                        return;
                }
                switch (messageHeader.mPacketType) {
                        case 4: {
                                runOnUiThread(() -> mSwipeRefreshLayout.setRefreshing(false));
                                try {
                                        final envsensor.EnvSensorData response = envsensor.EnvSensorData.parseFrom(bodyBuf);
                                        runOnUiThread(() -> {
                                                mTvLumi.setText(String.valueOf(response.getLuminosity()));
                                                mTvTemp.setText(String.valueOf(response.getTemperature()));
                                                mTvRain.setText(String.valueOf(response.getRain()));
                                                mTvHum.setText(String.valueOf(response.getHumidity()));
                                                mTvDirection.setText(String.valueOf(response.getDirection()));
                                                mTvPre.setText(String.valueOf(response.getPressure()));
                                                mTvVelocity.setText(String.valueOf(response.getVelocity()));
                                        });
                                } catch (InvalidProtocolBufferException e) {
                                        e.printStackTrace();
                                }
                                break;
                        }
                        default: {
                                break;
                        }
                }
                super.onResponse(messageHeader, bodyBuf);
        }

        @Override
        protected void onResume() {
                super.onResume();
                envsensor.StreamingStartRequest request1 = envsensor.StreamingStartRequest.newBuilder().setSerial(mEnvSensor.getSerial()).build();
                mCustomApplication.mServerSession.request(request1);
        }

        @Override
        protected void onPause() {
                super.onPause();
                envsensor.StreamingStopRequest request = envsensor.StreamingStopRequest.newBuilder().setSerial(mEnvSensor.getSerial()).build();
                mCustomApplication.mServerSession.request(request);
        }

        @Override
        public void onRefresh() {
                envsensor.StreamingStopRequest request = envsensor.StreamingStopRequest.newBuilder().setSerial(mEnvSensor.getSerial()).build();
                mCustomApplication.mServerSession.request(request);
                envsensor.StreamingStartRequest request1 = envsensor.StreamingStartRequest.newBuilder().setSerial(mEnvSensor.getSerial()).build();
                mCustomApplication.mServerSession.request(request1);
        }
}