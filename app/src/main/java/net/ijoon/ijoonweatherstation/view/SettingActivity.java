package net.ijoon.ijoonweatherstation.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Html;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.kakao.sdk.user.UserApiClient;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.RestApi.RetrofitClient;
import net.ijoon.ijoonweatherstation.RestApi.UnRegPushTokenRequest;
import net.ijoon.ijoonweatherstation.RestApi.UnRegPushTokenResponse;
import net.ijoon.ijoonweatherstation.login.AuthService;
import net.ijoon.ijoonweatherstation.login.DeleteRequest;
import net.ijoon.ijoonweatherstation.login.DeleteResponse;
import net.ijoon.ijoonweatherstation.login.UpdateDispNameRequest;
import net.ijoon.ijoonweatherstation.login.UpdateDispNameResponse;
import net.ijoon.ijoonweatherstation.login.UpdatePasswordRequest;
import net.ijoon.ijoonweatherstation.login.UpdatePasswordResponse;
import net.ijoon.ijoonweatherstation.util.UUIDManager;
import net.ijoon.scnet_android.SCNetAppCompatActivity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SettingActivity extends SCNetAppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    CustomApplication mCustomApplication;
    SwipeRefreshLayout mSwipeRefreshLayout;
    View view;
    RelativeLayout mRlNickname;
    RelativeLayout mRlPasswordChange;
    RelativeLayout mRlLogout;
    RelativeLayout mRlWithdrawal;
    RelativeLayout mRlKakaoSharing;
    TextView mTvId;
    TextView mTvNickname;
    TextView mTvKaKao;
    String mToken;
    String mNickname;
    String mId;
    String mKakao;
    private long mLastClickTime = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        mCustomApplication = (CustomApplication) getApplicationContext();
        Intent intent = getIntent();
        mNickname = intent.getStringExtra("nickname");
        mSwipeRefreshLayout = findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
        mToken = preferences.getString("token","");
        mId = preferences.getString("id","");
        mKakao = preferences.getString("kakao","");
        mRlNickname = findViewById(R.id.rlNickname);
        mRlPasswordChange = findViewById(R.id.rlPasswordChange);
        mRlLogout = findViewById(R.id.rlLogout);
        mRlWithdrawal = findViewById(R.id.rlWithdrawal);
        mRlKakaoSharing = findViewById(R.id.rlKakaotalk);
        mTvId = findViewById(R.id.tvId);
        mTvNickname = findViewById(R.id.tvNickname);
        mTvNickname.setText(mNickname);
        mTvKaKao = findViewById(R.id.tvKakao);
        if(mKakao.equals("")) {
            mTvKaKao.setText(getResources().getString(R.string.no_pairing));
        }else {
            mTvKaKao.setText(getResources().getString(R.string.pairing));
        }
        mTvId.setText(mId);
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
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + getResources().getString(R.string.textSetting) + "</font>"));
        }
        mRlLogout.setOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();
            showLogoutAlertDialog(getResources().getString(R.string.textAreYouSureWantToLogout));
        });
        mRlNickname.setOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();
            showNicknameChangeDialog();
        });
        mRlPasswordChange.setOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();
            showPasswordChangeEditTextAlertDialog(getResources().getString(R.string.textEditPasswordChange));
        });
        mRlWithdrawal.setOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();
            showWidthdrawalEditTextAlertDialog(getResources().getString(R.string.textCancelCircularMembership));
        });
        mRlKakaoSharing.setOnClickListener(v -> {
            SharedPreferences preferences1 = getSharedPreferences("preferences",MODE_PRIVATE);
            if(preferences1.getString("kakao","").equals("")) {
                if(UserApiClient.getInstance().isKakaoTalkLoginAvailable(getApplicationContext())) {
                    UserApiClient.getInstance().loginWithKakaoTalk(getApplicationContext(), (oAuthToken, throwable) -> {
                        if(throwable != null) {
                            UserApiClient.getInstance().loginWithKakaoAccount(getApplicationContext(), (oAuthToken1, throwable1) -> {
                                if(throwable1 != null) {
                                } else if(oAuthToken1 != null) {
                                    preferences1.edit().putString("kakao",String.valueOf(oAuthToken1));
                                    preferences1.edit().apply();
                                }
                                return null;
                            });
                        } else if(oAuthToken != null) {
                            preferences1.edit().putString("kakao",String.valueOf(oAuthToken));
                            preferences1.edit().apply();
                        }
                        return null;
                    });
                } else {
                    UserApiClient.getInstance().loginWithKakaoAccount(getApplicationContext(), (oAuthToken, throwable) -> {
                        if(throwable != null) {
                        } else if(oAuthToken != null) {
                            preferences1.edit().putString("kakao",String.valueOf(oAuthToken));
                            preferences1.edit().apply();
                        }
                        return null;
                    });
                }
            }
        });
    }

    void delete(String pw) {
        if(pw.equals("")) {
            showAlertDialog(getResources().getString(R.string.textFailedToWithdrawal));
            return;
        }
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
        final Call<DeleteResponse> deleteResponseCall = service.delete(new DeleteRequest(pw));
        deleteResponseCall.enqueue(new Callback<DeleteResponse>() {
            @Override
            public void onResponse(Call<DeleteResponse> call, Response<DeleteResponse> response) {
                if(response.body() == null) {
                    showAlertDialog(getResources().getString(R.string.textFailedToWithdrawal));
                    return;
                }
                if(response.body().getSuccess()) {
                    SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
                    preferences.edit().clear().apply();
                    preferences.edit().putString("id","");
                    preferences.edit().putString("token","");
                    preferences.edit().apply();
                    SettingActivity.super.activityFinish();
                    Intent intent = new Intent(SettingActivity.this, DeleteActivity.class);
                    startActivity(intent);
                }else {
                    showAlertDialog(getResources().getString(R.string.textFailedToWithdrawal));
                }
            }
            @Override
            public void onFailure(Call<DeleteResponse> call, Throwable t) {
                Log.d("logdinobei", "onFailure");
            }
        });
    }

    public void showLogoutAlertDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                (dialog, which) -> {
                    unRegPushToken(UUIDManager.getDeviceUUID(getApplicationContext()));
                    SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
                    preferences.edit().clear().apply();
                    preferences.edit().putString("id","");
                    preferences.edit().putString("token","");
                    preferences.edit().apply();
                    SettingActivity.super.activityFinish();
                    Intent intent = new Intent(SettingActivity.this, DefaultActivity.class);
                    startActivity(intent);
                });
        runOnUiThread(() -> alertDialog.show());
    }

    public void showAlertDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                (dialog, which) -> dialog.cancel());
        runOnUiThread(() -> alertDialog.show());
    }

    public void showWidthdrawalEditTextAlertDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        final EditText et = new EditText(SettingActivity.this);
        et.setHint(getResources().getString(R.string.textPassword));
        et.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        et.setTransformationMethod(PasswordTransformationMethod.getInstance());
        alertDialog.setView(et);
        alertDialog.setTitle(getResources().getString(R.string.textCircularWithdrawal));
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                (dialog, which) -> delete(et.getText().toString()));
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

    public void showPasswordChangeEditTextAlertDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        final EditText existingEditText;
        final EditText changeEditText;
        final EditText confirmChangeEditText;
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.custom_two_edit_item,null);
        alertDialog.setView(layout);
        existingEditText = layout.findViewById(R.id.etExisting);
        existingEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        existingEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        changeEditText = layout.findViewById(R.id.etChange);
        changeEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        changeEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        confirmChangeEditText = layout.findViewById(R.id.etConfirmChange);
        confirmChangeEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmChangeEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        alertDialog.setTitle(getResources().getString(R.string.textPasswordChange));
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                (dialog, which) -> updatePassword(existingEditText.getText().toString(),changeEditText.getText().toString(),confirmChangeEditText.getText().toString()));
        runOnUiThread(() -> alertDialog.show());
    }

    private void showNicknameChangeDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        final EditText et = new EditText(SettingActivity.this);
        et.setHint(getResources().getString(R.string.textNickname));
        et.setSingleLine(true);
        et.setLines(1);
        alertDialog.setView(et);
        alertDialog.setTitle(getResources().getString(R.string.textChangeNickname));
        alertDialog.setMessage(getResources().getString(R.string.textEditNicknamechange));
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk), (dialog, which) -> updateNickName(et.getText().toString()));
        runOnUiThread(() -> alertDialog.show());
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        et.setLayoutParams(params);
    }

    void updateNickName(final String id) {
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
        final Call<UpdateDispNameResponse> loginResponseCall = service.updateDispName(new UpdateDispNameRequest(id));
        loginResponseCall.request().header(mToken);
        loginResponseCall.enqueue(new Callback<UpdateDispNameResponse>() {
            @Override
            public void onResponse(Call<UpdateDispNameResponse> call, Response<UpdateDispNameResponse> response) {
                if(response.body().getSuccess()) {
                    String accessToken = response.body().getAccessToken();
                    showAlertDialog(getResources().getString(R.string.textNicknameChangedSuccess)+ "\" " + id + "\"");
                    mTvNickname.setText(id);
                    SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
                    SharedPreferences.Editor editor =  preferences.edit();
                    editor.putString("token", accessToken);
                    editor.putString("nickname", id);
                    editor.apply();
                }else {
                    showAlertDialog(getResources().getString(R.string.textNicknameChangedFail));
                }
            }

            @Override
            public void onFailure(Call<UpdateDispNameResponse> call, Throwable t) {
                Log.d("logdinobei", "onFailure" + t.getMessage());
            }
        });
    }

    void updatePassword(String pw, String pwNew, String pwConfirmNew) {
        if(!pwNew.equals("") && pwNew.equals(pwConfirmNew)) {
            if(pwNew.length() >= 6 && pwNew.length() <= 15) {
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
                final Call<UpdatePasswordResponse> updatePasswordResponseCall = service.updatePassword(new UpdatePasswordRequest(pw, pwNew));
                updatePasswordResponseCall.enqueue(new Callback<UpdatePasswordResponse>() {
                    @Override
                    public void onResponse(Call<UpdatePasswordResponse> call, Response<UpdatePasswordResponse> response) {
                        if(response.body() != null) {
                            if(response.body().getSuccess()) {
                                showAlertDialog(getResources().getString(R.string.textPasswordChangeSuccess));
                            }else {
                                showAlertDialog(getResources().getString(R.string.textPasswordChangeFail));
                            }
                        }else {
                            showAlertDialog(getResources().getString(R.string.textCheckYourExistingPassword));
                        }
                    }
                    @Override
                    public void onFailure(Call<UpdatePasswordResponse> call, Throwable t) {
                        Log.d("logdinobei", "onFailure");
                    }
                });
            }else {
                //6~ 15자로 입력해주세요
                showAlertDialog(getResources().getString(R.string.text_characters_count));
            }
        }else {
            //정보를 확인하세요
            showAlertDialog(getResources().getString(R.string.textCheckInformation));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(1400);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runOnUiThread(() -> mSwipeRefreshLayout.setRefreshing(false));
        });
        thread.start();
    }

    void unRegPushToken(final String UUID) {
        new Thread(() -> {
            Call<UnRegPushTokenResponse> call = RetrofitClient
                    .getApiService(mToken)
                    .unRegPushToken(new UnRegPushTokenRequest(UUID));
            call.enqueue(new Callback<UnRegPushTokenResponse>() {
                @Override
                public void onResponse(Call<UnRegPushTokenResponse> call, Response<UnRegPushTokenResponse> response) {
                    Log.d("yot132","pushtoken reg response = " + response.message());
                }

                @Override
                public void onFailure(Call<UnRegPushTokenResponse> call, Throwable t) {
                    Log.d("yot132", "onFailure...");
                }
            });
        }).start();
    }
}