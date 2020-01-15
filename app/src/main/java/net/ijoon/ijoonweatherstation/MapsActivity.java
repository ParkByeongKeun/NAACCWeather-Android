package net.ijoon.ijoonweatherstation;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
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
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
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
    boolean isSetGPS = false;
    String sensorLng;
    String sensorLong;
    ActionBar actionBar;
    private BackPressCloseHandler mBackPressCloseHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mBackPressCloseHandler = new BackPressCloseHandler(this);
        mArrMarkerOptions = new ArrayList<>();
        mCustomApplication = (CustomApplication) getApplicationContext();
        actionBar = getSupportActionBar();
        envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().build();

        if(mCustomApplication.mServerSession == null) {
            mCustomApplication.startServer();
        }

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
                                envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().build();
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
                                envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().build();
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
                        envsensor.SetSensorNameRequest request = envsensor.SetSensorNameRequest.newBuilder().setId(mCustomApplication.getEnvSensor().getId()).setName(et.getText().toString()).build();
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
                envsensor.SetSensorPositionRequest request = envsensor.SetSensorPositionRequest.newBuilder().setId(mSensor.get(mPosition).getId()).setLatitude(Float.valueOf(sensorLng)).setLongitude(Float.valueOf(sensorLong)).build();
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
}