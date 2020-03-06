package net.ijoon.ijoonweatherstation.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.RestApi.RegPushTokenRequest;
import net.ijoon.ijoonweatherstation.RestApi.RegPushTokenResponse;
import net.ijoon.ijoonweatherstation.RestApi.RetrofitClient;
import net.ijoon.ijoonweatherstation.RestApi.RetrofitHeader;
import net.ijoon.ijoonweatherstation.login.AuthService;
import net.ijoon.ijoonweatherstation.login.CheckRequest;
import net.ijoon.ijoonweatherstation.login.CheckResponse;
import net.ijoon.ijoonweatherstation.util.UUIDManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class LoadingActivity extends Activity {

    View view;
    Thread loadingThread;
    TextView mTvVersion;
    String mToken;
    CustomApplication mCustomApplication;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        mCustomApplication = (CustomApplication)getApplication();
        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
        mToken = preferences.getString("token","");
        mTvVersion = findViewById(R.id.tvVersion);

        mTvVersion.setText("ver " + getAppVersionName());
        getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));
        view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(Color.parseColor("#f2f2f2"));
        loadingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                    regPushToken(UUIDManager.getDeviceUUID(getApplicationContext()),mCustomApplication.fcmToken);
                    if(!mToken.equals("")) {
                        check(mToken);
                    }else {
                        Intent intent = new Intent(LoadingActivity.this, DefaultActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        loadingThread.start();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(loadingThread.isAlive()) {
            loadingThread.interrupt();
        }
    }

    void check(final String token) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request request = chain.request().newBuilder().addHeader("x-access-token", token).build();
                return chain.proceed(request);
            }
        });
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://1.246.219.189:20217")
//                .baseUrl("https://192.168.128.28")
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
        AuthService service = retrofit.create(AuthService.class);
        final Call<CheckResponse> checkResponseCall = service.check(new CheckRequest());

        checkResponseCall.enqueue(new Callback<CheckResponse>() {
            @Override
            public void onResponse(Call<CheckResponse> call, Response<CheckResponse> response) {
                CheckResponse body = response.body();
                if(body == null) {
                    showFailedLoginDialog();
                    return;
                }

                if(body.getSuccess()) {
                    Intent intent = new Intent(LoadingActivity.this,MapsActivity.class);
                    startActivity(intent);
                    finish();
                    // 로그인 상태
                }
                else {
                    // 토큰 만료
                    showExpirationDialog();
                }
            }
            @Override
            public void onFailure(Call<CheckResponse> call, Throwable t) {
                showServerConnectionDialog();
            }
        });
    }



    public void showServerConnectionDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog.setMessage(getResources().getString(R.string.textTryAgainServer));
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                mCustomApplication.stopServer();
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                mCustomApplication.startServer();
                            }
                        });
                    }
                });
        alertDialog.setNegativeButton(getResources().getString(R.string.textCancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                        finish();
                    }
                });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertDialog.show();
            }
        });
    }

    public void showFailedLoginDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog.setMessage(getResources().getString(R.string.textFailedLogin));
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(LoadingActivity.this, DefaultActivity.class);
                                intent.putExtra("tokenCheck",1);
                                startActivity(intent);
                                finish();
                            }
                        }).start();
                    }
                });
        alertDialog.setNegativeButton(getResources().getString(R.string.textCancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertDialog.show();
            }
        });
    }

    public void showExpirationDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog.setMessage(getResources().getString(R.string.textTokenExpires));
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(LoadingActivity.this, DefaultActivity.class);
                                intent.putExtra("tokenCheck",1);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                });
        alertDialog.setNegativeButton(getResources().getString(R.string.textCancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertDialog.show();
            }
        });
    }

    public String getAppVersionName(){
        PackageInfo packageInfo;
        try{
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        }catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
            return "";
        }
        return packageInfo.versionName;
    }


    void regPushToken(final String UUID, final String pushToken) {

        new Thread(() -> {
            Call<RegPushTokenResponse> call = RetrofitClient
                    .getApiService(mToken)
                    .regPushToken(new RegPushTokenRequest(UUID, pushToken));
            call.enqueue(new Callback<RegPushTokenResponse>() {
                @Override
                public void onResponse(Call<RegPushTokenResponse> call, Response<RegPushTokenResponse> response) {

                }

                @Override
                public void onFailure(Call<RegPushTokenResponse> call, Throwable t) {
                    Log.d("yot132", "onFailure...");
                }
            });
        }).start();
    }
}