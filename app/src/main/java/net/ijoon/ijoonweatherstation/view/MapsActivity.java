package net.ijoon.ijoonweatherstation.view;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mingle.sweetpick.CustomDelegate;
import com.mingle.sweetpick.SweetSheet;
import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.login.AuthService;
import net.ijoon.ijoonweatherstation.login.UpdateTokenRequest;
import net.ijoon.ijoonweatherstation.login.UpdateTokenResponse;
import net.ijoon.ijoonweatherstation.util.BackPressCloseHandler;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.SCNetAppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapsActivity extends SCNetAppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    ArrayList<MarkerOptions> mArrMarkerOptions;
    CustomApplication mCustomApplication;
    List<envsensor.Sensor> mSensor;
    SweetSheet mSweetSheet;
    RelativeLayout rl;
    int mPosition = 0;
    boolean isSetGPS = false;
    String sensorLng;
    String sensorLong;
    ActionBar actionBar;
    private BackPressCloseHandler mBackPressCloseHandler;
    View marker_root_view;
    TextView tv_marker;
    TextView tvMarkerSubTitle;
    RelativeLayout rl_message;
    String mToken;
    MarkerOptions markerOptions = new MarkerOptions();
    boolean isRefresh;
    int ALIVEPING = 15;
    float saveLat;
    float saveLong;
    float saveZoom;
    boolean isMarkerClicked = true;
    int prevPosition = -1;
    TextView tvName;
    Handler handler;
    Runnable connCheckRunnable;
    Switch mSwitch;
    boolean isFix;
    String fixSensorName;
    private Intent serviceIntent;
    List<Marker> displayMarker;

    private void setCustomMarkerView() {
        marker_root_view = LayoutInflater.from(this).inflate(R.layout.marker_layout, null);
        tv_marker = marker_root_view.findViewById(R.id.tv_marker);
        rl_message = marker_root_view.findViewById(R.id.rlMessage);
        tvMarkerSubTitle = marker_root_view.findViewById(R.id.tv_sub_title_marker);
    }

    private void setPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setPermission();
        brManager();
        handler = new Handler();
        connCheckRunnable = () -> {
            isMarkerClicked = true;
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId  = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 좌측튼 상단 버튼
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_person_whit);
        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
        mToken = preferences.getString("token","");
        saveLat = preferences.getFloat("lat",(float) 36.5);
        saveLong = preferences.getFloat("long",(float) 127.266667);
        saveZoom = preferences.getFloat("zoom",12);
        isFix = preferences.getBoolean("fix",false);
        fixSensorName = preferences.getString("fixSensorName","");
        updateUpdateToken();
        mBackPressCloseHandler = new BackPressCloseHandler(this);
        mArrMarkerOptions = new ArrayList<>();
        displayMarker = new ArrayList<>();
        mCustomApplication = (CustomApplication) getApplicationContext();
        actionBar = getSupportActionBar();
        setCustomMarkerView();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        rl = findViewById(R.id.rl);
        mSweetSheet = new SweetSheet(rl);
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        boolean isWhiteListing = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isWhiteListing = pm.isIgnoringBatteryOptimizations(getApplicationContext().getPackageName());
        }
        if (!isWhiteListing) {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            }
            intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
            startActivity(intent);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
        }
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        return;
                    }
                });
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        CustomDelegate customDelegate = new CustomDelegate(true,
                CustomDelegate.AnimationType.AlphaAnimation);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_custom_view, null, false);
        customDelegate.setCustomView(view);
        mSweetSheet.setDelegate(customDelegate);
        tvName = view.findViewById(R.id.tvName);
        view.findViewById(R.id.rlBottom).setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, MonitorActivity.class);
            startActivity(intent);
        });
        view.findViewById(R.id.rlGPSSetting).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setNavigationBarColor(Color.parseColor("#FFAE98"));//bottom
                getWindow().setStatusBarColor(Color.parseColor("#FF0B45"));//statusBar
            }
            if (actionBar != null) {
                actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF0B45")));
                actionBar.setTitle(R.string.text_select_location_on_the_map);
            }
            if(mSweetSheet.isShow()) {
                runOnUiThread(() -> mSweetSheet.dismiss());
            }
            isSetGPS = true;
        });
        view.findViewById(R.id.rlSetName).setOnClickListener(v -> showNicknameChangeDialog());
        view.findViewById(R.id.rlShare).setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this,SensorSharingActivity.class);
            startActivity(intent);
        });
        view.findViewById(R.id.rlHandOver).setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this,StationHandOfOutActivity.class);
            startActivity(intent);

        });
        view.findViewById(R.id.rlUnRegist).setOnClickListener(v -> {
            showUnRegistDialog(mSensor.get(mPosition).getName() + getResources().getString(R.string.textDeleteSensor));
        });
        mSwitch = view.findViewById(R.id.checkBox);
        mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences sharedPreferences = getSharedPreferences("preferences",MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("fix",isChecked);
            editor.putString("fixSensorName",mSensor.get(mPosition).getName());
            editor.apply();
        });

        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(300);
                if(isFix) {
                    if(mSensor != null && mSensor.size() > 0) {
                        for(int i = 0 ; i < mSensor.size() ; i ++) {
                            if(mSensor.get(i).getName().equals(fixSensorName)) {
                                mCustomApplication.setEnvSensor(mSensor.get(i));
                                Intent intent = new Intent(MapsActivity.this, MonitorActivity.class);
                                startActivity(intent);
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mCustomApplication.mServerSession != null) {
            envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
            mCustomApplication.mServerSession.request(request);
        }
        if(mCustomApplication.pushSerial != null) {
            for (int i = 0 ; i < mSensor.size() ; i ++) {
                if(String.valueOf(mSensor.get(i).getSerial()).equals(mCustomApplication.pushSerial)) {
                    mPosition = i;
                }
            }
            if(isMarkerClicked) {
                changConnectingUI(true);
                setupCustomView();
                mCustomApplication.setEnvSensor(mSensor.get(mPosition));
                if(!mSweetSheet.isShow()) {
                    mSweetSheet.show();
                }else {
                    tvName.setText(mSensor.get(mPosition).getName());
                }
                isMarkerClicked = false;
            }
            prevPosition = mPosition;
            mSweetSheet.toggle();
        }
        displayMarker.clear();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences preferencesPosition = getSharedPreferences("preferences",MODE_PRIVATE);
        SharedPreferences.Editor editor = preferencesPosition.edit();
        editor.putFloat("lat", saveLat);
        editor.putFloat("long", saveLong);
        editor.putFloat("zoom", saveZoom);
        editor.apply();
        if(!mSensor.isEmpty()) {
            envsensor.StreamingStopAllRequest request = envsensor.StreamingStopAllRequest.newBuilder().setAccessToken(mToken).build();
            mCustomApplication.mServerSession.request(request);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
        unregisterReceiver(mActivatedBroadcastReceiver);
        if (serviceIntent!=null) {
            stopService(serviceIntent);
            serviceIntent = null;
        }
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
                isRefresh = true;
                envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
                mCustomApplication.mServerSession.request(request);
                return true;
            }
            case android.R.id.home: {
                if(mSensor.isEmpty()) {
                    Intent intent = new Intent(MapsActivity.this, InfomationActivity.class);
                    startActivity(intent);
                    return true;
                }
                Intent intent = new Intent(MapsActivity.this, InfomationActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_add: {
                Intent intent = new Intent(MapsActivity.this, VelocityRecordActivity.class);
                intent.putExtra("serial","");
                startActivity(intent);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        }

        mMap.setOnCameraMoveListener(() -> {
            saveLat = (float) mMap.getCameraPosition().target.latitude;
            saveLong = (float) mMap.getCameraPosition().target.longitude;
            saveZoom = mMap.getCameraPosition().zoom;
        });

        mMap.setOnMapClickListener(latLng -> {
            if(isSetGPS) {
                sensorLng =  String.valueOf(latLng.latitude);
                sensorLong = String.valueOf(latLng.longitude);
                showPositionAlertDialog(getResources().getString(R.string.text_notifycation),(getResources().getString(R.string.text_current_location) + mSensor.get(mPosition).getId() + getResources().getString(R.string.text_move_sensor)) ,getResources().getString(R.string.text_ok),"");
            }
            if(mSweetSheet.isShow()) {
                mSweetSheet.dismiss();
            }
        });
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
            case 4: { // real-time sensorData
                try {
                    final envsensor.EnvSensorData response = envsensor.EnvSensorData.parseFrom(bodyBuf);
                    runOnUiThread(() -> {
                        List<Marker> oldMarker = null;
                        if(displayMarker != null) {
                            oldMarker = displayMarker;
                        }
                        for(int i = 0 ; i < mSensor.size() ; i ++) {
                            if(mSensor.get(i).getSerial().equals(response.getSerial())) {
                                if (mSensor.get(i).getPing() < ALIVEPING) {
                                    displayMarker.remove(i);
                                }
                                if(mSensor.get(i).getMaxVelocity() >= 10.0) {
                                    rl_message.setBackgroundResource(R.drawable.ic_message_danger);
                                }else {
                                    rl_message.setBackgroundResource(R.drawable.ic_message_default);
                                }
                                tv_marker.setText(mSensor.get(i).getName());
                                tvMarkerSubTitle.setText(String.format("%.1f", response.getVelocity()) + "m/s" + " (" + mSensor.get(i).getMaxVelocity()+ "m/s)");
                                LatLng position = new LatLng(mSensor.get(i).getLatitude(),mSensor.get(i).getLongitude());
                                markerOptions.position(position);
                                markerOptions.title(mSensor.get(i).getName());
                                markerOptions.snippet(String.valueOf(response.getSerial()));
                                mArrMarkerOptions.set(i,markerOptions);
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(getApplicationContext(), marker_root_view)));
                                displayMarker.add(mMap.addMarker(mArrMarkerOptions.get(i)));
                                float value = 0;
                                if(mSensor.get(i).getPing() < ALIVEPING) {
                                    value = (1 + value) * mSensor.get(i).getMaxVelocity();
                                }
                                displayMarker.get(i).setZIndex(value);
                                displayMarker.get(i).setAlpha(1f);
                            }

                            if(oldMarker != null) {
                                if(oldMarker.get(i).getSnippet().equals(response.getSerial())) {
                                    oldMarker.get(i).remove();
                                }
                            }
                        }
                    });
                }catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                break;
            }
            case 6: {
                runOnUiThread(() -> {
                    try {
                        mSensor = new ArrayList<>();
                        mMap.clear();
                        mArrMarkerOptions.clear();
                        final envsensor.GetSensorListResponse response = envsensor.GetSensorListResponse.parseFrom(bodyBuf);
                        for (int i = 0; i < response.getSensorCount(); i++) {
                            mSensor.add(response.getSensor(i));
                        }
                        if (response.getSensorCount() == 0) {
                            //Todo : 센서가 없습니다.
                            runOnUiThread(() -> {
                                showDialog(getResources().getString(R.string.textSensorAvailable));
                            });
                        } else {
                            envsensor.StreamingStartAllRequest request = envsensor.StreamingStartAllRequest.newBuilder().setAccessToken(mToken).build();
                            mCustomApplication.mServerSession.request(request);

                            mCustomApplication.setEnvSensor(mSensor.get(0));
                            for(int i = 0 ; i < response.getSensorCount() ; i ++) {
                                if(mSensor.get(i).getPing() >= ALIVEPING) {
                                    rl_message.setBackgroundResource(R.drawable.ic_message_ping);
                                }else {
                                    if(mSensor.get(i).getMaxVelocity() >= 10.0) {
                                        rl_message.setBackgroundResource(R.drawable.ic_message_danger);
                                    }else {
                                        rl_message.setBackgroundResource(R.drawable.ic_message_default);
                                    }
                                }
                                tv_marker.setText(mSensor.get(i).getName());
                                tvMarkerSubTitle.setText(String.format("%.1f", 0.0) + "m/s" + " (" + mSensor.get(i).getMaxVelocity()+ "m/s)");
                                LatLng position = new LatLng(response.getSensor(i).getLatitude(),response.getSensor(i).getLongitude());
                                markerOptions.position(position);
                                markerOptions.title(response.getSensor(i).getName());
                                markerOptions.snippet(String.valueOf(response.getSensor(i).getSerial()));
                                mArrMarkerOptions.add(markerOptions);
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(getApplicationContext(), marker_root_view)));
                                displayMarker.add(mMap.addMarker(mArrMarkerOptions.get(i)));
                                float value = 0;
                                if(mSensor.get(i).getPing() < ALIVEPING) {
                                    value = (1 + value) * mSensor.get(i).getMaxVelocity();
                                }
                                displayMarker.get(i).setZIndex(value);
                                displayMarker.get(i).setAlpha(1f);
                            }
                            if(!isRefresh) {
                                LatLng firstGPS = new LatLng(saveLat, saveLong);
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom((firstGPS),saveZoom));
                                isRefresh = false;
                            }
                        }
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }
                });
                break;
            }
            case 7 : {
                break;
            }
            case 8 : {//SetSensorPosition
                runOnUiThread(() -> {
                    try {
                        final envsensor.SetSensorPositionResponse response = envsensor.SetSensorPositionResponse.parseFrom(bodyBuf);
                        if(response.getResult() == envsensor.SetSensorPositionResponse.Result.success) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
                            }
                            actionBar.setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
                            actionBar.setTitle("iJoonWeatherStation");
                            envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
                            mCustomApplication.mServerSession.request(request);
                            showSuccessAlertDialog(getResources().getString(R.string.text_changed_sensor_position_success));
                        }else {//set sensor position failed
                        }
                        isSetGPS = false;
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }
                });
                break;
            }
            case 11 : {//SetSensorNameRes
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
            case 25 : {//unregist
                try {
                    final envsensor.UnregistSensorResponse response = envsensor.UnregistSensorResponse.parseFrom(bodyBuf);
                    if(response.getResult() == envsensor.UnregistSensorResponse.Result.success) {
                        envsensor.GetSensorListRequest stationRequest = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
                        mCustomApplication.mServerSession.request(stationRequest);
                        runOnUiThread(() -> {
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.textDelete), Toast.LENGTH_SHORT).show();
                            mSweetSheet.dismiss();
                        });
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
            case 34 : {//streamingStartAllRes
                try {
                    final envsensor.StreamingStartAllResponse response = envsensor.StreamingStartAllResponse.parseFrom(bodyBuf);
                    runOnUiThread(() -> {
                        List<Marker> oldMarker = null;
                        if(displayMarker != null) {
                            oldMarker = displayMarker;
                        }
                        for(int i = 0 ; i < mSensor.size() ; i ++) {
                            for(int j = 0 ; j < response.getDataCount() ; j ++) {
                                if (mSensor.get(i).getSerial().equals(response.getData(j).getSerial())) {
                                    if (mSensor.get(i).getMaxVelocity() >= 10.0) {
                                        rl_message.setBackgroundResource(R.drawable.ic_message_danger);
                                    } else {
                                        rl_message.setBackgroundResource(R.drawable.ic_message_default);
                                    }
                                    tv_marker.setText(mSensor.get(i).getName());
                                    tvMarkerSubTitle.setText(String.format("%.1f", response.getData(j).getVelocity()) + "m/s" + " (" + mSensor.get(i).getMaxVelocity() + "m/s)");
                                    LatLng position = new LatLng(mSensor.get(i).getLatitude(), mSensor.get(i).getLongitude());
                                    markerOptions.position(position);
                                    markerOptions.title(mSensor.get(i).getName());
                                    markerOptions.snippet(String.valueOf(response.getData(j).getSerial()));
                                    mArrMarkerOptions.set(i, markerOptions);

                                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(getApplicationContext(), marker_root_view)));
                                    displayMarker.add(mMap.addMarker(mArrMarkerOptions.get(i)));
                                    float value = 0;
                                    if (mSensor.get(i).getPing() < ALIVEPING) {
                                        value = (1 + value) * mSensor.get(i).getMaxVelocity();
                                    }
                                    displayMarker.get(i).setZIndex(value);
                                    displayMarker.get(i).setAlpha(1f);

                                    if (oldMarker != null) {
                                        oldMarker.get(i).remove();
                                    }
                                }
                            }
                        }
                    });
                }catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onAttached() {
        envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
        mCustomApplication.mServerSession.request(request);
        super.onAttached();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        for (int i = 0 ; i < mSensor.size() ; i ++) {
            if(String.valueOf(mSensor.get(i).getSerial()).equals(marker.getSnippet())) {
                mPosition = i;
            }
        }
        if(isFix) {
            if(fixSensorName.equals(mSensor.get(mPosition).getName())) {
                mSwitch.setChecked(true);
            }else {
                mSwitch.setChecked(false);
            }
        }
        if(isMarkerClicked) {
            changConnectingUI(true);
            setupCustomView();
            mCustomApplication.setEnvSensor(mSensor.get(mPosition));
            if(!mSweetSheet.isShow()) {
                mSweetSheet.show();
            }else {
                tvName.setText(mSensor.get(mPosition).getName());
            }
            isMarkerClicked = false;
        }
        prevPosition = mPosition;
        return true;
    }

    private void setupCustomView() {
        if(!mSensor.isEmpty()) {
            runOnUiThread(() -> tvName.setText(mSensor.get(mPosition).getName()));
        }
    }

    public void changConnectingUI(boolean isConnecting) {
        if (isConnecting) {
            handler.postDelayed(connCheckRunnable, 800);
        }
    }

    @Override
    public void onBackPressed() {
        if(mSweetSheet.isShow()){
            runOnUiThread(() -> mSweetSheet.dismiss());
        } else if(isSetGPS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
                getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
            }
            actionBar.setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
            actionBar.setTitle("iJoonWeatherStation");
            isSetGPS = false;
        } else{
            mBackPressCloseHandler.onBackPressed();
        }
    }

    private void showNicknameChangeDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        final EditText et = new EditText(MapsActivity.this);
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

    void updateUpdateToken() {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(chain -> {
            Request request = chain.request().newBuilder().addHeader("x-access-token", mToken).build();
            return chain.proceed(request);
        });
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://1.246.219.189:20217")
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
        AuthService service = retrofit.create(AuthService.class);
        final Call<UpdateTokenResponse> ResponseCall = service.updateToken(new UpdateTokenRequest());
        ResponseCall.request().header(mToken);
        ResponseCall.enqueue(new Callback<UpdateTokenResponse>() {
            @Override
            public void onResponse(Call<UpdateTokenResponse> call, Response<UpdateTokenResponse> response) {
                if(response.body().getSuccess()) {
                    String accessToken = response.body().getAccessToken();
                    if(accessToken == null) {
                        return;
                    }
                    SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
                    SharedPreferences.Editor editor =  preferences.edit();
                    editor.putString("token", accessToken);
                    editor.apply();
                }else {
                }
            }
            @Override
            public void onFailure(Call<UpdateTokenResponse> call, Throwable t) {
            }
        });
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

    public void showPositionAlertDialog(final String title, final String message, String btnSuccess, String btnCancel ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(btnSuccess, (dialog, which) -> {
            envsensor.SetSensorPositionRequest request = envsensor.SetSensorPositionRequest.newBuilder().setSerial(mSensor.get(mPosition).getSerial()).setLatitude(Float.valueOf(sensorLng)).setLongitude(Float.valueOf(sensorLong)).setAccessToken(mToken).build();
            mCustomApplication.mServerSession.request(request);
        });
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(arg0 -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
        });
        dialog.show();
    }

    public void brManager() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("maxVelocityData");
        registerReceiver(mBroadcastReceiver, filter);

        IntentFilter filter1 = new IntentFilter();
        filter1.addAction("sensor_activated");
        registerReceiver(mActivatedBroadcastReceiver, filter1);

        IntentFilter filterShared = new IntentFilter();
        filterShared.addAction("sensor_shared");
        registerReceiver(mActivatedBroadcastReceiver, filterShared);

        IntentFilter filterSharedCanceled = new IntentFilter();
        filterSharedCanceled.addAction("sensor_share_canceled");
        registerReceiver(mActivatedBroadcastReceiver, filterSharedCanceled);

        IntentFilter filterRename = new IntentFilter();
        filterRename.addAction("sensor_renamed");
        registerReceiver(mActivatedBroadcastReceiver, filterRename);

        IntentFilter filterPosition = new IntentFilter();
        filterPosition.addAction("position");
        registerReceiver(mActivatedBroadcastReceiver, filterPosition);

        IntentFilter filterHandover = new IntentFilter();
        filterHandover.addAction("handover");
        registerReceiver(mActivatedBroadcastReceiver, filterHandover);

        IntentFilter filterSensorPublic = new IntentFilter();
        filterSensorPublic.addAction("sensor_public");
        registerReceiver(mActivatedBroadcastReceiver, filterSensorPublic);

        IntentFilter filterDeactivated= new IntentFilter();
        filterDeactivated.addAction("sensor_deactivated");
        registerReceiver(mActivatedBroadcastReceiver, filterDeactivated);

    }
    // View를 Bitmap으로 변환
    private Bitmap createDrawableFromView(Context context, View view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
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
                    Intent intent = new Intent(MapsActivity.this, SensorAddActivity.class);
                    startActivity(intent);
                });
        runOnUiThread(() -> alertDialog.show());
    }

    public void showUnRegistDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                (dialog, which) -> {
                    envsensor.UnregistSensorRequest unregisterRequest = envsensor.UnregistSensorRequest.newBuilder().setAccessToken(mToken).setSerial(mSensor.get(mPosition).getSerial()).build();
                    mCustomApplication.mServerSession.request(unregisterRequest);
                });
        runOnUiThread(() -> alertDialog.show());
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String maxVelocity = intent.getStringExtra("maxVelocity");
            String maxVelocityTime = intent.getStringExtra("maxVelocityTime");
            String serial = intent.getStringExtra("serial");
            mMap.clear();
            Log.d("yot132","maxVelocity = " +maxVelocity);
            for(int i = 0 ; i < mSensor.size() ; i ++) {
                int finalI = i;
                runOnUiThread(() -> {
                    if (serial.equals(mSensor.get(finalI).getSerial())) {
                        if (Float.valueOf(maxVelocity) >= 10.0) {
                            rl_message.setBackgroundResource(R.drawable.ic_message_danger);
                        } else {
                            rl_message.setBackgroundResource(R.drawable.ic_message_default);
                        }
                        tv_marker.setText(mSensor.get(finalI).getName());
                        tvMarkerSubTitle.setText(maxVelocity + "m/s (" + maxVelocityTime + ")");

                    }else {
                        if (mSensor.get(finalI).getPing() >= ALIVEPING) {
                            rl_message.setBackgroundResource(R.drawable.ic_message_ping);
                        }else {
                            if (mSensor.get(finalI).getMaxVelocity() >= 10.0) {
                                rl_message.setBackgroundResource(R.drawable.ic_message_danger);
                            }else {
                                rl_message.setBackgroundResource(R.drawable.ic_message_default);
                            }
                        }
                        tv_marker.setText(mSensor.get(finalI).getName());
                        tvMarkerSubTitle.setText(String.format("%.1f", mSensor.get(finalI).getMaxVelocity()) + "m/s" + " ("+maxVelocityTime + ")");
                    }
                    LatLng position = new LatLng(mSensor.get(finalI).getLatitude(),mSensor.get(finalI).getLongitude());
                    markerOptions.position(position);
                    markerOptions.title(mSensor.get(finalI).getName());
                    markerOptions.snippet(String.valueOf(mSensor.get(finalI).getSerial()));
                    mArrMarkerOptions.add(markerOptions);
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(getApplicationContext(), marker_root_view)));
                    Marker marker = mMap.addMarker(mArrMarkerOptions.get(finalI));
                    float value = 0;
                    if(mSensor.get(finalI).getPing() < ALIVEPING) {
                        value = (1 + value) * mSensor.get(finalI).getMaxVelocity();
                    }
                    marker.setZIndex(value);
                    marker.setAlpha(1f);
                });
            }
        }
    };

    private BroadcastReceiver mActivatedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            runOnUiThread(() -> {
                envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
                mCustomApplication.mServerSession.request(request);
            });
        }
    };
}