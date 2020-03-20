package net.ijoon.ijoonweatherstation.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.protobuf.InvalidProtocolBufferException;

import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.login.AuthService;
import net.ijoon.ijoonweatherstation.login.UserListItem;
import net.ijoon.ijoonweatherstation.login.UserListRequest;
import net.ijoon.ijoonweatherstation.util.UserList;
import net.ijoon.ijoonweatherstation.util.UserListAdapter;
import net.ijoon.scnet_android.Connection;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.RendezvousClientCallback;
import net.ijoon.scnet_android.RendezvousSession;
import net.ijoon.scnet_android.SCNetAppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SensorSharingActivity extends SCNetAppCompatActivity {

    String mSerial;
    String mToken;
    View view;
    Button mBtnSuccess;
    EditText mEtName;
    CustomApplication mCustomApplication;
    ListView mUserListView;
    ArrayList<UserList> mArrUserList;
    ArrayList<UserList> mData;
    UserList mUserList;
    UserListAdapter mUserListAdapter;
    RelativeLayout mRlRoot;
    boolean isBlockSearch;
    int mUserListHeight;
    InputMethodManager imm;
    Handler handler;
    Runnable getNameRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sharing);
        mCustomApplication = (CustomApplication)getApplication();
        handler = new Handler();
        getNameRunnable = new Runnable() {
            @Override
            public void run() {
                userList(mEtName.getText().toString());
            }
        };

        mBtnSuccess = findViewById(R.id.btnSuccess);
        mEtName = findViewById(R.id.etId);
        mUserListView = findViewById(R.id.list);
        mRlRoot = findViewById(R.id.rlRoot);

        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
        mToken = preferences.getString("token","");
        mSerial = mCustomApplication.getEnvSensor().getSerial();
        mArrUserList = new ArrayList<>();
        mData = new ArrayList<>();
        mUserList = new UserList();
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        mUserListHeight = mUserListView.getLayoutParams().height;
        mUserListAdapter = new UserListAdapter(this,mArrUserList);
        view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
            getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
        }
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + getResources().getString(R.string.textStationShare) + "</font>"));
        }

        mBtnSuccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                envsensor.Authority builder = envsensor.Authority.newBuilder().setName(true).setPosition(true).setSharing(true).setUpdate(true).setStatistic(true).build();
                envsensor.ShareSensorRequest stationNameRequest = envsensor.ShareSensorRequest.newBuilder().setAuth(builder).setAccessToken(mToken).setSerial(mSerial).setUserId(mEtName.getText().toString()).build();
                mCustomApplication.mServerSession.request(stationNameRequest);
            }
        });

        mEtName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isBlockSearch = true;
                mEtName.setText(mEtName.getText().toString());
                mEtName.setSelection(mEtName.length());
            }
        });


        mEtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(isBlockSearch) {
                    isBlockSearch = false;
                    return;
                }

                handler.removeCallbacks(getNameRunnable);
                handler.postDelayed(getNameRunnable, 500);
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mUserListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                isBlockSearch = true;
                imm.hideSoftInputFromWindow(mEtName.getWindowToken(), 0);
                mEtName.setText(mArrUserList.get(position).getId());
                mEtName.setSelection(mEtName.length());
                mUserListView.setVisibility(View.GONE);
            }
        });
        mRlRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imm.hideSoftInputFromWindow(mEtName.getWindowToken(), 0);
                if(mUserListView.getVisibility() == View.VISIBLE) {
                    mUserListView.setVisibility(View.GONE);
                }
            }
        });
    }

    void userList(final String id) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request request = chain.request().newBuilder().addHeader("x-access-token", mToken).addHeader("Content-Type", "application/json").build();
                return chain.proceed(request);
            }
        });
        Gson gson = new GsonBuilder().setLenient().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://1.246.219.189:20217")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build();
        AuthService service = retrofit.create(AuthService.class);
        final Call<UserListItem> userListResponseCall = service.userList(new UserListRequest(id));
        userListResponseCall.enqueue(new Callback<UserListItem>() {
            @Override
            public void onResponse(Call<UserListItem> call, Response<UserListItem> response) {
                if(response.isSuccessful()) {
                    mUserListAdapter.clear();
                    for(int i = 0 ; i < response.body().id_list.size() ; i ++) {
                        mUserList.setName(response.body().id_list.get(i).getName());
                        mUserList.setId(response.body().id_list.get(i).getId());
                        mUserListAdapter.addItem(mUserList);
                    }
                    ViewGroup.LayoutParams layoutParams = mUserListView.getLayoutParams();
                    if(response.body().id_list.size() == 0) {
                        mUserList.setName(id +getResources().getString(R.string.textNotCircularMember));
                        mUserList.setId("");
                        mUserListAdapter.addItem(mUserList);
                        layoutParams.height = 150;
                        mUserListView.setLayoutParams(layoutParams);
                    } else if(response.body().id_list.size() < 5) {
                        layoutParams.height = response.body().id_list.size() * 150;
                        mUserListView.setLayoutParams(layoutParams);
                    }else {
                        layoutParams.height = mUserListHeight;
                        mUserListView.setLayoutParams(layoutParams);
                    }
                    mUserListView.setAdapter(mUserListAdapter);
                    mUserListView.setVisibility(View.VISIBLE);
                }else {
                    mUserListView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<UserListItem> call, Throwable t) {
                Log.d("logdinobei", "onFailure" + t.getMessage());

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
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
    public void onResponse(MessageHeader messageHeader, byte[] bodyBuf) {
        envsensor.PacketType packetType = envsensor.PacketType.forNumber(messageHeader.mPacketType);
        if(packetType == null) {
            Log.d("yot132", "packetType is null");
            return;
        }
        switch (messageHeader.mPacketType) {
            case 13: {
                try {
                    Log.d("yot132","11");
                    final envsensor.ShareSensorResponse response = envsensor.ShareSensorResponse.parseFrom(bodyBuf);
                    if(response.getResult() == envsensor.ShareSensorResponse.Result.success) {
                        showAlertDialog(getResources().getString(R.string.textSharingSucceded));
                    }else if(response.getResult() == envsensor.ShareSensorResponse.Result.unauthorized) {
                        showAlertDialog(getResources().getString(R.string.textNotPermi));
                    }else if(response.getResult() == envsensor.ShareSensorResponse.Result.expiredToken) {
                        showAlertDialog(getResources().getString(R.string.textTokenExpires));
                        SensorSharingActivity.super.activityFinish();
                        Intent intent = new Intent(SensorSharingActivity.this, DefaultActivity.class);
                        startActivity(intent);
                    }else if(response.getResult() == envsensor.ShareSensorResponse.Result.userNotExist) {
                        showAlertDialog(getResources().getString(R.string.textUserNotExist));
                    }else {
                        showAlertDialog(getResources().getString(R.string.textCheckInfomation));
                    }
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                break;
            }
            default: {
                Log.d("yot132","[onMessage] Unknown PacketType");
                break;
            }
        }
    }
}
