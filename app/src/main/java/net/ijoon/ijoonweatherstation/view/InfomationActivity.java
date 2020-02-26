package net.ijoon.ijoonweatherstation.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.login.AuthService;
import net.ijoon.ijoonweatherstation.login.NameRequest;
import net.ijoon.ijoonweatherstation.login.NameResponse;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.RendezvousClientCallback;
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

public class InfomationActivity extends SCNetAppCompatActivity implements RendezvousClientCallback, SwipeRefreshLayout.OnRefreshListener{

        SwipeRefreshLayout mSwipeRefreshLayout;
        View view;
        CustomApplication mCustomApplication;

        String mToken;
        String mId;
        String mNickname;
        envsensor.Sensor mEnvSensor;
        ActionBar actionBar;
        Button btnInfo;
        TextView tvId;
        TextView tvNickname;

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_infomation);
                mCustomApplication = (CustomApplication)getApplication();
                mEnvSensor = mCustomApplication.getEnvSensor();
                tvId = findViewById(R.id.tvId);
                tvNickname = findViewById(R.id.tvNickname);
                btnInfo = findViewById(R.id.btnInfo);
                mSwipeRefreshLayout = findViewById(R.id.swipe_layout);
                mSwipeRefreshLayout.setOnRefreshListener(this);
                view = getWindow().getDecorView();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
                getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
                getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
//                                mAppBar.setBackgroundColor(Color.parseColor("#56CBF2"));
                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
                actionBar = getSupportActionBar();

                if (actionBar != null) {
                        actionBar.setDisplayHomeAsUpEnabled(true);
                        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
                        actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + getResources().getString(R.string.textInfo) + "</font>"));
                }

                btnInfo.setOnClickListener(v -> {
                        Intent intent = new Intent(InfomationActivity.this, SettingActivity.class);
                        intent.putExtra("nickname",mNickname);
                        startActivity(intent);
                });

        }


        @Override
        public void onResponse(MessageHeader messageHeader, final byte[] bodyBuf) {
                if(messageHeader == null) {
                        Log.d("yot132", "packetType is null");
                        return;
                }
                switch (messageHeader.mPacketType) {

                        case 6: {
                                Thread thread = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                                try {
                                                        Thread.sleep(1400);

                                                } catch (InterruptedException e) {
                                                        e.printStackTrace();
                                                }
                                                runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                                mSwipeRefreshLayout.setRefreshing(false);
                                                        }
                                                });
                                        }
                                });
                                thread.start();
                                break;
                        }
                        default: {
                                Log.d("yot132","[onMessage] Unknown PacketType");
                                break;
                        }
                }
                super.onResponse(messageHeader, bodyBuf);
        }

        void getNickname() {
                OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
                httpClient.addInterceptor(new Interceptor() {
                        @Override
                        public okhttp3.Response intercept(Chain chain) throws IOException {
                                Request request = chain.request().newBuilder().addHeader("x-access-token", mToken).build();
                                return chain.proceed(request);
                        }
                });
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("http://1.246.219.189:20217")
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(httpClient.build())
                        .build();
                AuthService service = retrofit.create(AuthService.class);
                final Call<NameResponse> checkResponseCall = service.getName(new NameRequest());
                checkResponseCall.enqueue(new Callback<NameResponse>() {
                        @Override
                        public void onResponse(Call<NameResponse> call, Response<NameResponse> response) {
                                NameResponse body = response.body();
                                if(body == null) {
                                        return;
                                }
                                mNickname = body.getName();
                                tvNickname.setText(mNickname);
                                SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
                                preferences.edit().putString("nickname",mNickname);
                                preferences.edit().apply();
                        }
                        @Override
                        public void onFailure(Call<NameResponse> call, Throwable t) {
                                //실패
                        }
                });
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
                finish();
                return super.onOptionsItemSelected(item);
        }

        @Override
        protected void onResume() {
                super.onResume();
                SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
                mToken = preferences.getString("token","");
                mId = preferences.getString("id","");
                mNickname = preferences.getString("nickname","");
                getNickname();
                tvId.setText(mId);
                tvNickname.setText(mNickname);

                envsensor.GetSensorListRequest request1 = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
                mCustomApplication.mServerSession.request(request1);
        }

        @Override
        protected void onPause() {
                super.onPause();
        }

        @Override
        public void onRefresh() {
                envsensor.GetSensorListRequest request = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
                mCustomApplication.mServerSession.request(request);

        }


}