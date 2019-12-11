package net.ijoon.ijoonweatherstation;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mingle.sweetpick.CustomDelegate;
import com.mingle.sweetpick.SweetSheet;

import net.ijoon.envsensor;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.SCNetAppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends SCNetAppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    ArrayList<MarkerOptions> mArrMarkerOptions;
    CustomApplication mCustomApplication;
    List<envsensor.Sensor> mSensor;
    SweetSheet mSweetSheet;
    RelativeLayout rl;
    int mPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mArrMarkerOptions = new ArrayList<>();
        mCustomApplication = (CustomApplication) getApplicationContext();

        envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().build();
        mCustomApplication.mServerSession.request(request);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        rl = findViewById(R.id.rl);

        mSweetSheet = new SweetSheet(rl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_refresh,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_refresh: {
                envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().build();
                mCustomApplication.mServerSession.request(request);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the camera
        mMap.setOnMarkerClickListener(this);

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

            case 6: {
                //getSensorListResponse
                mSensor = new ArrayList<>();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final envsensor.GetSensorListResponse response = envsensor.GetSensorListResponse.parseFrom(bodyBuf);
                            for (int i = 0; i < response.getSensorCount(); i++) {
                                mSensor.add(response.getSensor(i));
                            }
                            if (response.getSensorCount() == 0) {
                                //Todo : 센서가 없습니다.
                            } else {
                                MarkerOptions markerOptions = new MarkerOptions();
                                for(int i = 0 ; i < response.getSensorCount() ; i ++) {
                                    LatLng position = new LatLng(response.getSensor(i).getLatitude(),response.getSensor(i).getLongitude());
                                    markerOptions.position(position);
                                    markerOptions.title(response.getSensor(i).getName());
                                    markerOptions.snippet(String.valueOf(response.getSensor(i).getId()));

                                    mArrMarkerOptions.add(markerOptions);

                                    mMap.addMarker(mArrMarkerOptions.get(i));
                                }
                                LatLng firstGPS = new LatLng(response.getSensor(0).getLatitude(), response.getSensor(0).getLongitude());

                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom((firstGPS),12));
                            }
                        } catch (InvalidProtocolBufferException e) {
                            e.printStackTrace();
                        }
                    }
                });
                break;
            }
        }
    }

    @Override
    public void onAttached() {
        //server request
        Log.d("yot132","on Attach called!!");
        envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().build();
        mCustomApplication.mServerSession.request(request);
        super.onAttached();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        for (int i = 0 ; i < mSensor.size() ; i ++) {
            if(String.valueOf(mSensor.get(i).getId()).equals(marker.getSnippet())) {
                mPosition = i;
            }
        }
        setupCustomView();
        mCustomApplication.setEnvSensor(mSensor.get(mPosition));
        mSweetSheet.toggle();
        return false;
    }

    private void setupCustomView() {

        mSweetSheet = new SweetSheet(rl);
        CustomDelegate customDelegate = new CustomDelegate(true,
                CustomDelegate.AnimationType.DuangLayoutAnimation);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_custom_view, null, false);
        customDelegate.setCustomView(view);
        mSweetSheet.setDelegate(customDelegate);
        final TextView tvName = view.findViewById(R.id.tvName);
        if(!mSensor.isEmpty()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvName.setText(mSensor.get(mPosition).getName());
                }
            });
        }
        view.findViewById(R.id.rlBottom).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this,ScrollingActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {

        if(mSweetSheet.isShow()){

            mSweetSheet.dismiss();
        }
        else{
            super.onBackPressed();
        }
    }
}