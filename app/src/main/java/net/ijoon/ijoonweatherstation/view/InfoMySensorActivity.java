package net.ijoon.ijoonweatherstation.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import com.google.protobuf.InvalidProtocolBufferException;
import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.util.ParallaxScollListView;
import net.ijoon.ijoonweatherstation.util.SensorListViewAdapter;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.SCNetAppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class InfoMySensorActivity extends SCNetAppCompatActivity {

    CustomApplication mCustomApplication;
    ActionBar actionBar;
    private ParallaxScollListView mListView;
    String mToken;
    List<envsensor.Sensor> mSensor;
    int MAXPING = 15;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mysensor);
        mCustomApplication = (CustomApplication) getApplicationContext();
        actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + getResources().getString(R.string.textMySensor) + "</font>"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
        }
        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
        mToken = preferences.getString("token","");
        envsensor.GetSensorListRequest getSensorRequest = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
        mCustomApplication.mServerSession.request(getSensorRequest);
        mListView = findViewById(R.id.layout_listview);
        mListView.setZoomRatio(ParallaxScollListView.ZOOM_X2);
        mListView.setOnItemClickListener((parent, view, position, id) -> {
            mCustomApplication.setEnvSensor(mSensor.get(position));
            Intent intent = new Intent(InfoMySensorActivity.this,ScrollingActivity.class);
            startActivity(intent);
        });
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
                    SensorListViewAdapter adapter = new SensorListViewAdapter();
                    final envsensor.GetSensorListResponse response = envsensor.GetSensorListResponse.parseFrom(bodyBuf);
                    mSensor = new ArrayList<>();
                    for(int i = 0 ; i < response.getSensorCount() ; i ++) {
                        mSensor.add(response.getSensor(i));
                        if(mSensor.get(i).getPing() > MAXPING) {
                            adapter.addItem(R.drawable.logo_weather_gray,mSensor.get(i).getName(),false,mSensor.get(i).getSerial());
                        }else {
                            adapter.addItem(R.drawable.logo_weather,mSensor.get(i).getName(),true,mSensor.get(i).getSerial());
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mListView.setAdapter(adapter);
                        }
                    });
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_add,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_add: {
                Intent intent = new Intent(InfoMySensorActivity.this, SensorAddActivity.class);
                startActivity(intent);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}