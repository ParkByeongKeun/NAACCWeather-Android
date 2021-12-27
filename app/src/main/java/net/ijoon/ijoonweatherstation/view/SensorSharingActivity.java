package net.ijoon.ijoonweatherstation.view;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
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
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
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
import net.ijoon.ijoonweatherstation.util.SharePermissionAdapter;
import net.ijoon.ijoonweatherstation.util.UserList;
import net.ijoon.ijoonweatherstation.util.UserListAdapter;
import net.ijoon.scnet_android.Connection;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.RendezvousSession;
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
    List<String> data;
    TextView tvSharing;
    ListView lvUser;
    String mId;
    TextView mTvSharingCount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sharing);
        mCustomApplication = (CustomApplication)getApplication();
        brManager();
        handler = new Handler();
        getNameRunnable = () -> userList(mEtName.getText().toString());
        mBtnSuccess = findViewById(R.id.btnSuccess);
        mEtName = findViewById(R.id.etId);
        mEtName.setFilters(new InputFilter[] {new InputFilter.LengthFilter(64)});
        mUserListView = findViewById(R.id.list);
        mRlRoot = findViewById(R.id.rlRoot);
        tvSharing = findViewById(R.id.tvSharing);
        tvSharing.setVisibility(View.GONE);
        lvUser = findViewById(R.id.listView1);
        mTvSharingCount = findViewById(R.id.tvSharingCount);
        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
        mToken = preferences.getString("token","");
        mId = preferences.getString("id","");
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
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + mCustomApplication.getEnvSensor().getName() + "</font>"));
        }
        envsensor.SharedUsersRequest registSensorRequest = envsensor.SharedUsersRequest.newBuilder().setAccessToken(mToken).setSerial(mCustomApplication.getEnvSensor().getSerial()).build();
        mCustomApplication.mServerSession.request(registSensorRequest);
        data = new ArrayList<>();
        mBtnSuccess.setOnClickListener(v -> {
            mUserListView.setVisibility(View.GONE);
            envsensor.Authority builder = envsensor.Authority.newBuilder().setName(true).setPosition(true).setSharing(true).setUpdate(true).setStatistic(true).build();
            envsensor.ShareSensorRequest stationNameRequest = envsensor.ShareSensorRequest.newBuilder().setAuth(builder).setAccessToken(mToken).setSerial(mSerial).setUserId(mEtName.getText().toString()).build();
            mCustomApplication.mServerSession.request(stationNameRequest);
        });
        mEtName.setOnClickListener(v -> {
            isBlockSearch = false;
            mEtName.setText("");
            mEtName.setSelection(mEtName.length());
        });
        mEtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvSharing.setVisibility(View.GONE);
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
        mUserListView.setOnItemClickListener((parent, view, position, id) -> {
            isBlockSearch = true;
            imm.hideSoftInputFromWindow(mEtName.getWindowToken(), 0);
            mEtName.setText(mArrUserList.get(position).getId());
            mEtName.setSelection(mEtName.length());
            mUserListView.setVisibility(View.GONE);
        });
        mRlRoot.setOnClickListener(v -> {
            imm.hideSoftInputFromWindow(mEtName.getWindowToken(), 0);
            if(mUserListView.getVisibility() == View.VISIBLE) {
                mUserListView.setVisibility(View.GONE);
            }
        });
        lvUser.setOnItemLongClickListener((parent, view, position, id) -> {
            if(mCustomApplication.getEnvSensor().getOwner().equals(data.get(position))) {
                if(mId.equals(data.get(position))) {
                    return false;
                }
                Toast.makeText(getApplicationContext(),"권한이 없습니다.",Toast.LENGTH_SHORT).show();
            }else if(mId.equals(data.get(position))) {//내 계정
            }else {
                showAlertDialog(data.get(position) + "님과의 공유를 해제하시겠습니까?",position);
            }
            return false;
        });
    }

    void userList(final String id) {
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
                    final envsensor.ShareSensorResponse response = envsensor.ShareSensorResponse.parseFrom(bodyBuf);
                    if(response.getResult() == envsensor.ShareSensorResponse.Result.success) {
                        runOnUiThread(() -> {
                            tvSharing.setVisibility(View.VISIBLE);
                            tvSharing.setText(getResources().getString(R.string.textSharingSucceded));
                            tvSharing.setTextColor(getResources().getColor(R.color.color_share_success));
                            envsensor.SharedUsersRequest registSensorRequest = envsensor.SharedUsersRequest.newBuilder().setAccessToken(mToken).setSerial(mCustomApplication.getEnvSensor().getSerial()).build();
                            mCustomApplication.mServerSession.request(registSensorRequest);
                        });
                    }else if(response.getResult() == envsensor.ShareSensorResponse.Result.unauthorized) {
                        runOnUiThread(() -> {
                            tvSharing.setVisibility(View.VISIBLE);
                            tvSharing.setText(getResources().getString(R.string.textNotPermi));
                            tvSharing.setTextColor(getResources().getColor(R.color.color_share_fail));
                        });
                    }else if(response.getResult() == envsensor.ShareSensorResponse.Result.expiredToken) {
                        runOnUiThread(() -> {
                            SensorSharingActivity.super.activityFinish();
                            Intent intent = new Intent(SensorSharingActivity.this, DefaultActivity.class);
                            startActivity(intent);
                        });
                    }else if(response.getResult() == envsensor.ShareSensorResponse.Result.userNotExist) {
                        runOnUiThread(() -> {
                            tvSharing.setVisibility(View.VISIBLE);
                            tvSharing.setText(getResources().getString(R.string.textUserNotExist));
                            tvSharing.setTextColor(getResources().getColor(R.color.color_share_fail));
                        });
                    }else {
                        runOnUiThread(() -> {
                            tvSharing.setVisibility(View.VISIBLE);
                            tvSharing.setText(getResources().getString(R.string.textCheckInfomation));
                            tvSharing.setTextColor(getResources().getColor(R.color.color_share_fail));
                        });
                    }
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                break;
            }
            case 15 : {
                runOnUiThread(() -> {
                    try {
                        final envsensor.ShareSensorCancelResponse response = envsensor.ShareSensorCancelResponse.parseFrom(bodyBuf);
                        if(response.getResult() == envsensor.ShareSensorCancelResponse.Result.success) {
                            Toast.makeText(getApplicationContext(),"삭제되었습니다.",Toast.LENGTH_SHORT).show();
                            envsensor.SharedUsersRequest registSensorRequest = envsensor.SharedUsersRequest.newBuilder().setAccessToken(mToken).setSerial(mCustomApplication.getEnvSensor().getSerial()).build();
                            mCustomApplication.mServerSession.request(registSensorRequest);
                        }else {
                            Toast.makeText(getApplicationContext(),"다시 시도해주세요.",Toast.LENGTH_SHORT).show();
                        }
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }
                });
                break;
            }
            case 17 : {//sharedUsers
                runOnUiThread(() -> {
                    try {
                        final envsensor.SharedUsersResponse response = envsensor.SharedUsersResponse.parseFrom(bodyBuf);
                        if(response.getResult() == envsensor.SharedUsersResponse.Result.success) {
                            data = response.getSharedUsersList();
                            SharePermissionAdapter sharePermissionAdapter = new SharePermissionAdapter(
                                    this,
                                    data,
                                    mCustomApplication.getEnvSensor().getOwner(),
                                    mId);
                            lvUser.setAdapter(sharePermissionAdapter);
                            sharePermissionAdapter.notifyDataSetChanged();

                            if(response.getSharedUsersList().size() > 1) {
                                mTvSharingCount.setText(response.getSharedUsersList().size() + "명이 함께 사용중입니다.");
                            }else {
                                mTvSharingCount.setText("함께할 사용자를 공유해주세요.");
                            }
                        }
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }
                });
                break;
            }
            default: {
                Log.d("yot132","[onMessage] Unknown PacketType");
                break;
            }
        }
    }

    public void showAlertDialog(String message, int position) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog
                .setMessage(message);
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                (dialog, which) -> {
                    envsensor.ShareSensorCancelRequest shareSensorCancelRequest = envsensor.ShareSensorCancelRequest.newBuilder().setAccessToken(mToken).setSerial(mCustomApplication.getEnvSensor().getSerial()).setUserId(String.valueOf(data.get(position))).build();
                    mCustomApplication.mServerSession.request(shareSensorCancelRequest);
                    dialog.cancel();
                });
        runOnUiThread(() -> alertDialog.show());
    }

    public void brManager() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("delete");
        registerReceiver(mBroadcastReceiver, filter);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
            preferences.edit().clear().apply();
            preferences.edit().putString("id","");
            preferences.edit().putString("token","");
            preferences.edit().apply();
            SensorSharingActivity.super.activityFinish();
            Intent intent1 = new Intent(SensorSharingActivity.this, DeleteActivity.class);
            startActivity(intent1);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }
}