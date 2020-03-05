package net.ijoon.ijoonweatherstation.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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

import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.login.AuthService;
import net.ijoon.ijoonweatherstation.login.CheckUserIdRequest;
import net.ijoon.ijoonweatherstation.login.CheckUserIdResponse;
import net.ijoon.ijoonweatherstation.login.LoginRequest;
import net.ijoon.ijoonweatherstation.login.LoginResponse;
import net.ijoon.ijoonweatherstation.util.UUIDManager;
import net.ijoon.scnet_android.Connection;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.RendezvousSession;
import net.ijoon.scnet_android.SCNetAppCompatActivity;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EmailConfirmActivity extends SCNetAppCompatActivity {

    Button mBtnOk;
    EditText mEtId;
    RelativeLayout mRlRoot;
    String mStrId;
    CustomApplication mCustomApplication;
    View view;
    boolean isLoginSuccess;
    InputMethodManager imm;
    long mLastClickTime = 0;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_confirm);
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        UUIDManager.getDeviceUUID(this);

        view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));
        mCustomApplication = (CustomApplication)getApplication();
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + "" + "</font>"));
        }

        mBtnOk = findViewById(R.id.btnOk);
        mEtId = findViewById(R.id.etId);
        mRlRoot = findViewById(R.id.rlRoot);
        mBtnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStrId = mEtId.getText().toString();
                checkUserId(mStrId);
            }
        });

        mRlRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imm.hideSoftInputFromWindow(mEtId.getWindowToken(), 0);

            }
        });
    }

    void checkUserId(String userId) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request request = chain.request().newBuilder().addHeader("Content-Type", "application/json").build();
                return chain.proceed(request);
            }
        });
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://1.246.219.189:20217")
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
        AuthService service = retrofit.create(AuthService.class);
        final Call<CheckUserIdResponse> CheckUserIdResponseCall = service.checkUserId(new CheckUserIdRequest(userId));
        CheckUserIdResponseCall.enqueue(new Callback<CheckUserIdResponse>() {
            @Override
            public void onResponse(Call<CheckUserIdResponse> call, Response<CheckUserIdResponse> response) {
                CheckUserIdResponse body = response.body();

                if(body == null) {
                    showAlertDialog("확인할 수 없습니다.");
                    return;
                }

                if(body.getSuccess()) {
                    if(body.getIsExist()) {
                        showAlertDialog("등록된 아이디입니다.");
                    }else {
                        showAlertDialog("등록된 아이디가 아닙니다.");
                    }

                }else {
                    showAlertDialog("확인할 수 없습니다.");
                }

                switch (body.getErrNo()) {
                    case -1 : {
                        break;
                    }
                    case 0 : {
                        break;
                    }
                    case 1 : {
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
            public void onFailure(Call<CheckUserIdResponse> call, Throwable t) {
                Log.d("yot132", "onFailure" + t.getMessage());
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onAttached() {
        super.onAttached();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

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

    }
}