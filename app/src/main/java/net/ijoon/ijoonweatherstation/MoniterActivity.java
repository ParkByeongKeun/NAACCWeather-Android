//package net.ijoon.weathermonitor;
//
//
//import android.Manifest;
//import android.content.Context;
//import android.content.pm.PackageManager;
//import android.graphics.Color;
//import android.location.Location;
//import android.location.LocationListener;
//import android.location.LocationManager;
//import android.os.Build;
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.ImageView;
//
//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.OnMapReadyCallback;
//import com.google.android.gms.maps.SupportMapFragment;
//import com.google.android.gms.maps.model.BitmapDescriptorFactory;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.MarkerOptions;
//
//import net.ijoon.scnet_android.SCNetAppCompatActivity;
//
//import androidx.annotation.RequiresApi;
//import androidx.core.app.ActivityCompat;
//
//
//public class MoniterActivity extends SCNetAppCompatActivity implements OnMapReadyCallback {
//
//    Double mLatitude, mLongitude;
//    GoogleMap mMap;
//    LocationListener mLocationListener;
//    double[] mLatlang;
//    private GpsInfo mGps;
//    LocationManager mLocationManager;
//    ImageView mIvGPS;
//
//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_moniter);
//        mLatlang = new double[2];
//
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
//                .findFragmentById(R.id.map);
//        assert mapFragment != null;
//        mapFragment.getMapAsync(this);
//        getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));
//
//        mapFragment.getMapAsync(this);
//        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
//        mGps = new GpsInfo(this);
//        // GPS 사용유무 가져오기
//        if (mGps.isGetLocation()) {
//            if(mGps.getLatitude()!=0)
//                Log.d("yot132","gps = 0");
//            else
//                mGps.showSettingsAlert();
//            Log.d("yot132","mLatla = " + String.valueOf(mGps.getLatitude()));
//            mLatlang[0] = mGps.getLatitude();
//            mLatlang[1] = mGps.getLongitude();
//        } else {
//            mGps.showSettingsAlert();
//        }
//    }
//
//    @Override
//    public void onMapReady(final GoogleMap googleMap) {
//
//        mMap = googleMap;
//        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
//        mLocationListener = new LocationListener() {
//            public void onLocationChanged(Location location) {
//                    mMap.clear();
//                    mLatitude = location.getLatitude();
//                    mLongitude = location.getLongitude();
//                    String msg = "Latitude : " + mLatitude + "\nLongitude:" + mLongitude;
//                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLatitude, mLongitude), 17));
//                    Log.d("yot132", "msg = " + msg);
//                    LatLng latlng = new LatLng(mLatitude, mLongitude);
//                    MarkerOptions markerOptions = new MarkerOptions();
//                    markerOptions.position(latlng);
//                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker());
//                    mMap.addMarker(markerOptions);
//
//            }
//            public void onProviderDisabled(String provider) {
//            }
//
//            public void onProviderEnabled(String provider) {
//            }
//
//            public void onStatusChanged(String provider, int status, Bundle extras) {
//            }
//        };
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//            return;
//        }
//        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, mLocationListener);
//        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, mLocationListener);
//
//    }
//
//}
//

package net.ijoon.ijoonweatherstation;


import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.protobuf.InvalidProtocolBufferException;

import net.ijoon.envsensor;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.SCNetAppCompatActivity;

import java.util.ArrayList;
import java.util.List;

//implements OnMapReadyCallback
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


                    Log.d("yot132","123 = " + response.getSensorList());
                    for(int i = 0 ; i < response.getSensorCount() ; i ++) {
                        mSensor.add(response.getSensor(i));
                    }

                    for (int i = 0 ; i < response.getSensorCount() ; i ++)  {
                        Log.d("yot132","ss = " + mSensor.get(i).getId());
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