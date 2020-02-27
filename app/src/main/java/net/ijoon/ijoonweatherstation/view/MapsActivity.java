package net.ijoon.ijoonweatherstation.view;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
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
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mingle.sweetpick.CustomDelegate;
import com.mingle.sweetpick.SweetSheet;
import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.util.BackPressCloseHandler;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
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
    boolean isSetGPS = false;
    String sensorLng;
    String sensorLong;
    ActionBar actionBar;
    private BackPressCloseHandler mBackPressCloseHandler;
    View marker_root_view;
    TextView tv_marker;
    RelativeLayout rl_message;
    String mToken;

    private void setCustomMarkerView() {
        marker_root_view = LayoutInflater.from(this).inflate(R.layout.marker_layout, null);
        tv_marker = marker_root_view.findViewById(R.id.tv_marker);
        rl_message = marker_root_view.findViewById(R.id.rlMessage);
    }

    private Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
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

        mBackPressCloseHandler = new BackPressCloseHandler(this);
        mArrMarkerOptions = new ArrayList<>();
        mCustomApplication = (CustomApplication) getApplicationContext();

        actionBar = getSupportActionBar();
        setCustomMarkerView();
        envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();

        mCustomApplication.mServerSession.request(request);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        rl = findViewById(R.id.rl);
        mSweetSheet = new SweetSheet(rl);


        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        boolean isWhiteListing = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            isWhiteListing = pm.isIgnoringBatteryOptimizations(getApplicationContext().getPackageName());
        }
        if (!isWhiteListing) {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
            startActivity(intent);
        }
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("yot132", "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();
                    }
                });
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
    }

    private void sendNotification(String messageBody) {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, PendingIntent.FLAG_ONE_SHOT);

        String channelId = "fcm_default_channel";//getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.logo_weather)//drawable.splash)
                        .setContentTitle("즉시 작업을 중단하시오")
                        .setContentText("세종시 행복동 최대 풍속 11.6 m/s")
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,"Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
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


                //firebase 토큰 값 가져오기

//                FirebaseMessaging.getInstance().subscribeToTopic("weather")
//                        .addOnCompleteListener(new OnCompleteListener<Void>() {
//                            @Override
//                            public void onComplete(@NonNull Task<Void> task) {
//                                String msg = getString(R.string.msg_subscribed);
//                                if (!task.isSuccessful()) {
//                                    msg = getString(R.string.msg_subscribe_failed);
//                                }
//                                Log.d("yot132", msg);
//                                Toast.makeText(MapsActivity.this, msg, Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                FirebaseInstanceId.getInstance().getInstanceId()
//                        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
//                            @Override
//                            public void onComplete(@NonNull Task<InstanceIdResult> task) {
//                                if (!task.isSuccessful()) {
//                                    Log.w("yot132", "getInstanceId failed", task.getException());
//                                    return;
//                                }
//
//                                // Get new Instance ID token
//                                String token = task.getResult().getToken();
//
//                                // Log and toast
//                                String msg = getString(R.string.msg_token_fmt, token);
//                                Log.d("yot132", msg);
//                                Toast.makeText(MapsActivity.this, msg, Toast.LENGTH_SHORT).show();
//                            }
//                        });

                envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
                mCustomApplication.mServerSession.request(request);
                return true;
            }
            case android.R.id.home:
            {
                Intent intent = new Intent(MapsActivity.this, InfomationActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_add: {
                Intent intent = new Intent(MapsActivity.this, SensorAddActivity.class);
                startActivity(intent);
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
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if(isSetGPS) {
                    sensorLng =  String.valueOf(latLng.latitude);
                    sensorLong = String.valueOf(latLng.longitude);
                    showPositionAlertDialog(getResources().getString(R.string.text_notifycation),(getResources().getString(R.string.text_current_location) + mSensor.get(mPosition).getId() + getResources().getString(R.string.text_move_sensor)) ,getResources().getString(R.string.text_ok),"");
                }
            }
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
            case 0: {
                break;
            }
            case 6: {
                Log.d("yot132","onresponse called !!! ");

                //getSensorListResponse
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
                                showDialog("센서가 없습니다.");
                            } else {

                                MarkerOptions markerOptions = new MarkerOptions();
                                    for(int i = 0 ; i < response.getSensorCount() ; i ++) {

                                        if(mSensor.get(i).getMaxVelocity() >= 10.0) {
                                            rl_message.setBackgroundResource(R.drawable.ic_message_danger);
                                        }else {
                                            rl_message.setBackgroundResource(R.drawable.ic_message_default);
                                        }
                                        tv_marker.setText(String.format("%.1f", mSensor.get(i).getMaxVelocity()) + "m/s");
                                        LatLng position = new LatLng(response.getSensor(i).getLatitude(),response.getSensor(i).getLongitude());
                                        markerOptions.position(position);
                                        markerOptions.title(response.getSensor(i).getName());
                                        markerOptions.snippet(String.valueOf(response.getSensor(i).getId()));
                                        mArrMarkerOptions.add(markerOptions);
                                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(getApplicationContext(), marker_root_view)));
                                        Marker marker = mMap.addMarker(mArrMarkerOptions.get(i));
                                        marker.setAlpha(1f);

                                }

                                LatLng firstGPS = new LatLng(response.getSensor(0).getLatitude(), response.getSensor(0).getLongitude());
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom((firstGPS),15));
                            }
                        } catch (InvalidProtocolBufferException e) {
                            e.printStackTrace();
                        }
                    }
                });
                break;
            }
            case 8 : {
                //SetSensorPosition
                runOnUiThread(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void run() {
                        try {
                            final envsensor.SetSensorPositionResponse response = envsensor.SetSensorPositionResponse.parseFrom(bodyBuf);
                            if(response.getResult() == envsensor.SetSensorPositionResponse.Result.success) {
                                getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
//                                mAppBar.setBackgroundColor(Color.parseColor("#56CBF2"));
                                actionBar.setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
                                actionBar.setTitle("iJoonWeatherStation");
                                envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
                                mCustomApplication.mServerSession.request(request);
                                showSuccessAlertDialog(getResources().getString(R.string.text_changed_sensor_position_success));
                            }
                            isSetGPS = false;
                        } catch (InvalidProtocolBufferException e) {
                            e.printStackTrace();
                        }
                    }
                });
                break;
            }
            case 11 : {
                //SetSensorNameRes
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
                    }
                });
                break;
            }
            case 25 : {
                //unregist
                try {
                    final envsensor.UnregistSensorResponse response = envsensor.UnregistSensorResponse.parseFrom(bodyBuf);
                    if(response.getResult() == envsensor.UnregistSensorResponse.Result.success) {
                        showDeleteSuccessAlertDialog(getResources().getString(R.string.textDelete));
                    }else {
                        showDialog(getResources().getString(R.string.textServerError));
                    }
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                break;
            }

        }
    }

    @Override
    public void onAttached() {
        //server request
        Log.d("yot132","on Attach called!!");
        envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
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
        view.findViewById(R.id.rlGPSSetting).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                getWindow().setNavigationBarColor(Color.parseColor("#FFAE98"));//bottom
                getWindow().setStatusBarColor(Color.parseColor("#FF0B45"));//statusBar
                if (actionBar != null) {
                    actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF0B45")));
                    actionBar.setTitle(R.string.text_select_location_on_the_map);
                }
                mSweetSheet.dismiss();
                isSetGPS = true;
            }
        });
        view.findViewById(R.id.rlSetName).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNicknameChangeDialog();
            }
        });
        view.findViewById(R.id.rlShare).setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this,SensorSharingActivity.class);
            startActivity(intent);
        });
        view.findViewById(R.id.rlHandOver).setOnClickListener(v -> {

        });
        view.findViewById(R.id.rlSharedUsers).setOnClickListener(v -> {

        });
        view.findViewById(R.id.rlUnRegist).setOnClickListener(v -> {
            showUnRegistDialog(mSensor.get(mPosition).getName() + " 센서를 삭제하시겠습니까?");

        });

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBackPressed() {
        if(mSweetSheet.isShow()){
            mSweetSheet.dismiss();
        } else if(isSetGPS) {
            getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
            getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
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
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        envsensor.SetSensorNameRequest request = envsensor.SetSensorNameRequest.newBuilder().setSerial(mCustomApplication.getEnvSensor().getSerial()).setName(et.getText().toString()).setAccessToken(mToken).build();
                        mCustomApplication.mServerSession.request(request);
                    }
                });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertDialog.show();
            }
        });
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        et.setLayoutParams(params);
    }

    public void showSuccessAlertDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle(R.string.text_notifycation);
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton(R.string.text_ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                    }
                });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertDialog.show();
            }
        });
    }

    public void showPositionAlertDialog(final String title, final String message, String btnSuccess, String btnCancel ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(btnSuccess, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                envsensor.SetSensorPositionRequest request = envsensor.SetSensorPositionRequest.newBuilder().setSerial(mSensor.get(mPosition).getSerial()).setLatitude(Float.valueOf(sensorLng)).setLongitude(Float.valueOf(sensorLong)).setAccessToken(mToken).build();
                mCustomApplication.mServerSession.request(request);
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener( new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
            }
        });
        dialog.show();
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
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {


                    }
                });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertDialog.show();
            }
        });
    }

    public void showUnRegistDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        envsensor.UnregistSensorRequest unregisterRequest = envsensor.UnregistSensorRequest.newBuilder().setAccessToken(mToken).setSerial(mSensor.get(mPosition).getSerial()).build();
                        mCustomApplication.mServerSession.request(unregisterRequest);
                    }
                });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertDialog.show();
            }
        });
    }

    public void showDeleteSuccessAlertDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog
                .setMessage(message);
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        envsensor.GetSensorListRequest stationRequest = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
                        mCustomApplication.mServerSession.request(stationRequest);
                        dialog.cancel();
                    }
                });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertDialog.show();
            }
        });
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String maxVelocity = intent.getStringExtra("maxVelocity");
            String maxVelocityTime = intent.getStringExtra("maxVelocityTime");
            String serial = intent.getStringExtra("serial");
            Log.d("yot132","@@@@@@@@@@@@@@@@@@@@@" + maxVelocity);
        }
    };
}