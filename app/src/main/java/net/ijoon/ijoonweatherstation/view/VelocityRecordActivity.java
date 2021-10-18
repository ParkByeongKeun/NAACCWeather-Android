package net.ijoon.ijoonweatherstation.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.login.DelVelocityLogRequest;
import net.ijoon.ijoonweatherstation.login.DelVelocityLogResponse;
import net.ijoon.ijoonweatherstation.login.GetVelocityLogRequest;
import net.ijoon.ijoonweatherstation.login.GetVelocityLogResponse;
import net.ijoon.ijoonweatherstation.login.AuthService;
import net.ijoon.ijoonweatherstation.util.ComputableDate;
import net.ijoon.ijoonweatherstation.util.PullToRefreshListView;
import net.ijoon.ijoonweatherstation.util.RecordListViewAdapter;
import net.ijoon.ijoonweatherstation.util.VelocityRecord;
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

public class VelocityRecordActivity extends SCNetAppCompatActivity {

    CustomApplication mCustomApplication;
    ActionBar actionBar;
    private PullToRefreshListView mListView;
    String mToken;
    List<VelocityRecord> mArrVelocityRecord;
    ComputableDate computableDate;
    final static String MAX_COUNT = "30";
    String serial;
    String mCursor = "0";
    boolean isVRCountCheck;
    RecordListViewAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_velocity_record);
        mCustomApplication = (CustomApplication) getApplicationContext();
        Intent intent = getIntent();
        serial = intent.getStringExtra("serial");
        actionBar = this.getSupportActionBar();
        computableDate = new ComputableDate();
        mArrVelocityRecord = new ArrayList<>();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + getResources().getString(R.string.textVelocityRecord) + "</font>"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
        }
        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
        mToken = preferences.getString("token","");
        envsensor.GetSensorListRequest getSensorRequest = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
        mCustomApplication.mServerSession.request(getSensorRequest);
        mListView = findViewById(R.id.layout_listview);
        adapter = new RecordListViewAdapter();
        runOnUiThread(() -> mListView.setAdapter(adapter));

        mListView.setOnItemLongClickListener((parent, view, position, id) -> {
            if(mArrVelocityRecord.size() == 0) {
                return false;
            }
            showDeleteSuccessAlertDialog(getResources().getString(R.string.textDeleteTheSelectedRecord),
                    mArrVelocityRecord.get(position).getSerial(),
                    mArrVelocityRecord.get(position).getId());
            return false;
        });
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            boolean lastItemVisibleFlag = false;
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                lastItemVisibleFlag = (totalItemCount > 0) && (firstVisibleItem + visibleItemCount >= totalItemCount);
                if(lastItemVisibleFlag) {
                    if(!isVRCountCheck) {
                        isVRCountCheck = true;
                        getVelocityLogList(serial,mCursor);
                    }
                }
            }
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }
        });

        mListView.setOnRefreshListener(() -> {
            mArrVelocityRecord.clear();
            adapter.clear();
            mCursor = "0";
            mListView.onRefreshComplete();
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
            case 6: {
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }

    void getVelocityLogList(String serial, String cursor) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(chain -> {
            Request request = chain.request().newBuilder().addHeader("x-access-token", mToken).addHeader("Content-Type", "application/json").build();
            return chain.proceed(request);
        });
        Gson gson = new GsonBuilder().setLenient().create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://1.246.219.189:20217")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build();
        AuthService service = retrofit.create(AuthService.class);
        final Call<GetVelocityLogResponse> getVelocityResponseCall = service.getVelocityLog(new GetVelocityLogRequest(serial, cursor,MAX_COUNT));
        getVelocityResponseCall.enqueue(new Callback<GetVelocityLogResponse>() {
            @Override
            public void onResponse(Call<GetVelocityLogResponse> call, Response<GetVelocityLogResponse> response) {
                if(response.isSuccessful()) {
                    if(response.body().data.isEmpty() && adapter.isEmpty()) {
                        computableDate.setDateFormat("yyyy/MM/dd a hh:mm:ss");
                        adapter.addItem(R.drawable.logo_weather,
                                "기록이 없습니다.",
                                computableDate.getCurrentTimeText(),
                                true);
                    }else {
                        for(int i = 0 ; i < response.body().data.size() ; i ++) {
                            if(mArrVelocityRecord.isEmpty()) {
                                adapter.clear();
                            }
                            VelocityRecord velocityRecord= new VelocityRecord();
                            velocityRecord.setSerial(response.body().data.get(i).getSerial());
                            velocityRecord.setSubTitle(response.body().data.get(i).getAt());
                            velocityRecord.setTitle(response.body().data.get(i).getMsg());
                            velocityRecord.setId(response.body().data.get(i).getId());
                            mArrVelocityRecord.add(velocityRecord);
                            computableDate.setDateFormat("yyyy/MM/dd a hh:mm:ss");
                            computableDate.setDate(Integer.parseInt(response.body().data.get(i).getAt()));
                            mCursor = response.body().data.get(i).getId();
                            adapter.addItem(R.drawable.logo_weather,
                                    response.body().data.get(i).getMsg(),
                                    computableDate.getCurrentTimeText(),
                                    true);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    isVRCountCheck = false;
                }
            }

            @Override
            public void onFailure(Call<GetVelocityLogResponse> call, Throwable t) {
                Log.d("logdinobei", "onFailure" + t.getMessage());
            }
        });
    }

    void delVelocityLogList(String serial, String id) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(chain -> {
            Request request = chain.request().newBuilder().addHeader("x-access-token", mToken).addHeader("Content-Type", "application/json").build();
            return chain.proceed(request);
        });
        Gson gson = new GsonBuilder().setLenient().create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://1.246.219.189:20217")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build();
        AuthService service = retrofit.create(AuthService.class);
        final Call<DelVelocityLogResponse> getVelocityResponseCall = service.delVelocityLog(new DelVelocityLogRequest(serial, id));
        getVelocityResponseCall.enqueue(new Callback<DelVelocityLogResponse>() {
            @Override
            public void onResponse(Call<DelVelocityLogResponse> call, Response<DelVelocityLogResponse> response) {
                if(response.isSuccessful()) {
                    adapter.clear();
                    for(int i = 0 ; i < mArrVelocityRecord.size() ; i ++) {
                        if(id.equals(mArrVelocityRecord.get(i).getId())) {
                            mArrVelocityRecord.remove(i);
                        }
                    }
                    for(int i = 0 ; i < mArrVelocityRecord.size() ; i ++) {
                            computableDate.setDateFormat("yyyy/MM/dd a hh:mm:ss");
                            computableDate.setDate(Integer.parseInt(mArrVelocityRecord.get(i).getSubTitle()));
                            adapter.addItem(R.drawable.logo_weather,
                                    mArrVelocityRecord.get(i).getTitle(),
                                    computableDate.getCurrentTimeText(),
                                    true);
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<DelVelocityLogResponse> call, Throwable t) {
            }
        });
    }

    public void showDeleteSuccessAlertDialog(String message,String serial, String id) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog
                .setMessage(message);
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                (dialog, which) -> {
                    delVelocityLogList(serial, id);
                });
        runOnUiThread(() -> alertDialog.show());
    }
}