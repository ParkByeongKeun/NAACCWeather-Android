package net.ijoon.ijoonweatherstation.view;


import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.protobuf.InvalidProtocolBufferException;

import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.SCNetAppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MoniterActivity extends SCNetAppCompatActivity  {

    CustomApplication mCustomApplication;
    List<envsensor.Sensor> mSensor;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moniter);
        mCustomApplication = (CustomApplication) getApplicationContext();


        envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().build();
        mCustomApplication.mServerSession.request(request);

    }

    @Override
    public void onResponse(MessageHeader messageHeader, byte[] bodyBuf) {
        if (messageHeader == null) {
            Log.d("yot132", "packetType is null");
            return;
        }
        switch (messageHeader.mPacketType) {
            case 6: {
                mSensor = new ArrayList<>();
                try {
                    final envsensor.GetSensorListResponse response = envsensor.GetSensorListResponse.parseFrom(bodyBuf);
                    for(int i = 0 ; i < response.getSensorCount() ; i ++) {
                        mSensor.add(response.getSensor(i));
                    }
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }

                break;
            }
            default: {
                Log.d("yot132", "[onMessage] Unknown PacketType");
                break;
            }
        }
        super.onResponse(messageHeader, bodyBuf);
    }
}