package net.ijoon.ijoonweatherstation.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.protobuf.InvalidProtocolBufferException;

import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.RestApi.RegPushTokenRequest;
import net.ijoon.ijoonweatherstation.RestApi.RegPushTokenResponse;
import net.ijoon.ijoonweatherstation.RestApi.RetrofitClient;
import net.ijoon.ijoonweatherstation.login.AuthService;
import net.ijoon.ijoonweatherstation.login.LoginRequest;
import net.ijoon.ijoonweatherstation.login.LoginResponse;
import net.ijoon.ijoonweatherstation.util.UUIDManager;
import net.ijoon.scnet_android.Connection;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.RendezvousClientCallback;
import net.ijoon.scnet_android.RendezvousSession;
import net.ijoon.scnet_android.SCNetAppCompatActivity;
import net.ijoon.scnet_android.ServerManagerCallback;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends SCNetAppCompatActivity {

    Button mBtnLogin;
    EditText mEtId;
    EditText mEtPassword;
    RelativeLayout mRlRoot;
    String mStrId;
    String mStrPassword;
    int checkToken;
    CustomApplication mCustomApplication;
    View view;
    boolean isLoginSuccess;
    InputMethodManager imm;
    TextView mTvForgot;
    TextView mTvForgotEmail;
    long mLastClickTime = 0;
    String mToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        UUIDManager.getDeviceUUID(this);

        view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));
        }
        mCustomApplication = (CustomApplication)getApplication();
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + getResources().getString(R.string.textLogin) + "</font>"));
        }
        checkToken = getIntent().getIntExtra("tokenCheck",0);
        if(checkToken == 1) {
            showAlertDialog(getResources().getString(R.string.textExpiredToken));
        }
        mTvForgot = findViewById(R.id.tvForgot);
        mTvForgotEmail = findViewById(R.id.tvEmailForgot);
        mBtnLogin = findViewById(R.id.btnLogin);
        mEtId = findViewById(R.id.etId);
        mEtPassword = findViewById(R.id.etPassword);
        mRlRoot = findViewById(R.id.rlRoot);
        mBtnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStrId = mEtId.getText().toString().toString();
                mEtId.setText(mStrId);
                mStrPassword = mEtPassword.getText().toString();

                login(mStrId,mStrPassword);
//                if(android.util.Patterns.EMAIL_ADDRESS.matcher(mStrAuth).matches()) {
//                    resetPW(mStrAuth, mStrPassword);
//                }else {
//                    showAlertDialog(getResources().getString(R.string.textNotEmailFormat));
//                }
            }
        });

        mTvForgotEmail.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this,EmailConfirmActivity.class);
            startActivity(intent);
        });
        mTvForgot.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this,ForgotPWActivity.class);
            startActivity(intent);
        });

        mRlRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imm.hideSoftInputFromWindow(mEtId.getWindowToken(), 0);

            }
        });
    }

    void login(String id, String pw) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://1.246.219.189:20217")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        AuthService service = retrofit.create(AuthService.class);
        final Call<LoginResponse> loginResponseCall = service.login(new LoginRequest(id, pw));
        loginResponseCall.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                LoginResponse body = response.body();

                if(body == null) {
                    // Todo: 로그인인증서버에서 HTTP Status Code에 따라 body가 null이 되는지 확인
                    showAlertDialog(getResources().getString(R.string.textLoginFailed));
                    return;
                }

                if(body.getSuccess()) {
                    String token = body.getAccessToken();
                    SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
                    SharedPreferences.Editor editor =  preferences.edit();
                    editor.putString("token", token);
                    editor.putString("id", mStrId);
                    editor.apply();
                    mToken = token;
                    regPushToken(UUIDManager.getDeviceUUID(getApplicationContext()),mCustomApplication.fcmToken);
                    isLoginSuccess = true;
                    Log.d("yot132","resetPW success");
                    envsensor.GetSensorListRequest sensorListRequest = envsensor.GetSensorListRequest.newBuilder().setAccessToken(token).build();
                    mCustomApplication.mServerSession.request(sensorListRequest);

                }else {
                    showAlertDialog(getResources().getString(R.string.textLoginFailed));
                }

                switch (body.getErrNo()) {
                    case -1 : {
                        break;
                    }
                    case 0 : {
                        break;
                    }
                    case 1 : {
                        Intent intent = new Intent(LoginActivity.this,JoinEmailActivity.class);
                        startActivity(intent);
                        finish();
                        overridePendingTransition(0,0);
                        break;
                    }
                    case 2 : {
                        break;
                    }
                    case 3 : {
                        break;
                    }
                }
            }
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.d("logdinobei", "onFailure" + t.getMessage());
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(LoginActivity.this,DefaultActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0,0);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        Log.d("yot132","Login onAttached called");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(LoginActivity.this,DefaultActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0,0);
        return super.onOptionsItemSelected(item);
    }

    public void showAlertDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog
                .setMessage(message);
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
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

    @Override
    public void onConnectedToRendezvousServer(String myPublicIP, String myPublicPort) {

    }

    @Override
    public void onConnecting(int connectionID) {

    }

    @Override
    public void onConnected(RendezvousSession rendezvousSession, Connection connection) {

    }

    @Override
    public void onConnectionIDCreated(RendezvousSession rendezvousSession, String ip, int port) {

    }

    @Override
    public void onTargetInvalid(String ip, int port) {

    }

    @Override
    public void onConnectFailed(int connectionID) {

    }

    @Override
    public void onConnectionUpdate(RendezvousSession rendezvousSession, Connection connection) {

    }

    @Override
    public void onConnectionRemoved(RendezvousSession rendezvousSession, Connection connection) {

    }

    @Override
    public void onDisconnected(int connectionID) {

    }

    @Override
    public void onResponse(MessageHeader messageHeader, final byte[] bodyBuf) {
        super.onResponse(messageHeader, bodyBuf);
        if (messageHeader == null) {
            Log.d("yot132", "packetType is null");
            return;
        }
        switch (messageHeader.mPacketType) {

            case 1: {
            }
            case 5: {
            }
            case 6: {
                //getSensorListResponse
                try {
                    final envsensor.GetSensorListResponse response = envsensor.GetSensorListResponse.parseFrom(bodyBuf);
                    if(response.getSensorCount() > 0) {
                        Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                        startActivity(intent);
                        finish();
                        overridePendingTransition(0,0);
                        Log.d("yot132","count = " + response.getSensorCount());
                    }else {
                        if(isLoginSuccess) {
                            Log.d("yot132","count = 0");
                            Intent intent = new Intent(LoginActivity.this,MapsActivity.class);
                            startActivity(intent);
                            finish();
                            overridePendingTransition(0,0);
                        }
                    }
                } catch (InvalidProtocolBufferException e) {
                    Log.d("yot132","error - " + e);
                    e.printStackTrace();
                }
                break;
            }    default: {
                Log.d("yot132","[onMessage] Unknown PacketType");
                break;
            }
        }
    }

    void regPushToken(final String UUID, final String pushToken) {

        new Thread(() -> {
            Call<RegPushTokenResponse> call = RetrofitClient
                    .getApiService(mToken)
                    .regPushToken(new RegPushTokenRequest(UUID, pushToken));
            call.enqueue(new Callback<RegPushTokenResponse>() {
                @Override
                public void onResponse(Call<RegPushTokenResponse> call, Response<RegPushTokenResponse> response) {
                    Log.d("yot132","pushtoken reg response = " + response.message());
                }

                @Override
                public void onFailure(Call<RegPushTokenResponse> call, Throwable t) {
                    Log.d("yot132", "onFailure...");
                }
            });
        }).start();
    }
}