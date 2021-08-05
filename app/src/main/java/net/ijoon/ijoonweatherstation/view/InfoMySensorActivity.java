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
import java.util.Arrays;
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
        mListView = findViewById(R.id.layout_listview);
        mListView.setZoomRatio(ParallaxScollListView.ZOOM_X2);
        mListView.setOnItemClickListener((parent, view, position, id) -> {
            mCustomApplication.setEnvSensor(mSensor.get(position));
            Intent intent = new Intent(InfoMySensorActivity.this, SensorSettingActivity.class);
            intent.putExtra("nickname",mSensor.get(position).getName());
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        envsensor.GetSensorListRequest getSensorRequest = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
        mCustomApplication.mServerSession.request(getSensorRequest);
    }

    @Override
    public void onResponse(MessageHeader messageHeader, final byte[] bodyBuf) {
        super.onResponse(messageHeader, bodyBuf);
        if (messageHeader == null) {
            return;
        }
        switch (messageHeader.mPacketType) {
            case 6: {
                try {
                    SensorListViewAdapter adapter = new SensorListViewAdapter();
                    final envsensor.GetSensorListResponse response = envsensor.GetSensorListResponse.parseFrom(bodyBuf);
                    mSensor = new ArrayList<>();
                    ArrayList<envsensor.Sensor> tempArr1 = new ArrayList<>();
                    ArrayList<envsensor.Sensor> tempArr2 = new ArrayList<>();
                    for(int i = 0 ; i < response.getSensorCount() ; i ++) {
                        if(response.getSensor(i).getPing() < MAXPING) {
                            tempArr1.add(response.getSensor(i));
                        }else {
                            tempArr2.add(response.getSensor(i));
                        }
                    }
                    mSensor.addAll(tempArr1);
                    mSensor.addAll(tempArr2);
                    for(int i = 0 ; i < mSensor.size() ; i++) {
                        if(mSensor.get(i).getPing() > MAXPING) {
                            adapter.addItem(R.drawable.logo_weather_gray,mSensor.get(i).getName(),false,mSensor.get(i).getSerial());
                        }else {
                            adapter.addItem(R.drawable.logo_weather,mSensor.get(i).getName(),true,mSensor.get(i).getSerial());
                        }
                    }
                    runOnUiThread(() -> mListView.setAdapter(adapter));
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
            default: {
                finish();
                return true;
            }
        }
    }
}