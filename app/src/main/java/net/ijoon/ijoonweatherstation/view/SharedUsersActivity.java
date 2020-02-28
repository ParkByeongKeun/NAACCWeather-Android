package net.ijoon.ijoonweatherstation.view;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.RequiresApi;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.protobuf.InvalidProtocolBufferException;

import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.SCNetAppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class SharedUsersActivity extends SCNetAppCompatActivity {

    CustomApplication mCustomApplication;
    List<String> data;
    String mToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_users);
        mCustomApplication = (CustomApplication)getApplication();
        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
        mToken = preferences.getString("token","");
        envsensor.SharedUsersRequest registSensorRequest = envsensor.SharedUsersRequest.newBuilder().setAccessToken(mToken).setSerial(mCustomApplication.getEnvSensor().getSerial()).build();
        mCustomApplication.mServerSession.request(registSensorRequest);
        data = new ArrayList<>();


    }


    @Override
    public void onResponse(MessageHeader messageHeader, final byte[] bodyBuf) {
        super.onResponse(messageHeader, bodyBuf);
        if (messageHeader == null) {
            Log.d("yot132", "packetType is null");
            return;
        }
        switch (messageHeader.mPacketType) {
            case 0: {
                break;
            }

            case 7 : {
                Log.d("yot132","1");
                break;
            }

            case 17 : {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.d("yot132","!~!d");
                            final envsensor.SharedUsersResponse response = envsensor.SharedUsersResponse.parseFrom(bodyBuf);
                            if(response.getResult() == envsensor.SharedUsersResponse.Result.success) {
                              data = response.getSharedUsersList();
                                ArrayAdapter adapter = new ArrayAdapter(
                                        getApplicationContext(),
                                        R.layout.myrow,
                                        data); // 다량의 데이터

                                ListView lv = findViewById(R.id.listView1);
                                lv.setAdapter(adapter);

                            }
                        } catch (InvalidProtocolBufferException e) {
                            e.printStackTrace();
                        }
                    }
                });
                break;
            }
            default: {
                break;
            }
        }
    }
}
