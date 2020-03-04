package net.ijoon.ijoonweatherstation.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.login.AuthService;
import net.ijoon.ijoonweatherstation.login.DeleteRequest;
import net.ijoon.ijoonweatherstation.login.DeleteResponse;
import net.ijoon.ijoonweatherstation.login.UpdateDispNameRequest;
import net.ijoon.ijoonweatherstation.login.UpdateDispNameResponse;
import net.ijoon.ijoonweatherstation.login.UpdatePasswordRequest;
import net.ijoon.ijoonweatherstation.login.UpdatePasswordResponse;
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

public class SettingActivity extends SCNetAppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    CustomApplication mCustomApplication;
    SwipeRefreshLayout mSwipeRefreshLayout;
    View view;
    RelativeLayout mRlNickname;
    RelativeLayout mRlPasswordChange;
    RelativeLayout mRlLogout;
    RelativeLayout mRlWithdrawal;
    TextView mTvId;
    TextView mTvNickname;
    String mToken;
    String mNickname;
    String mId;
    private long mLastClickTime = 0;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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

        mRlNickname = findViewById(R.id.rlNickname);
        mRlPasswordChange = findViewById(R.id.rlPasswordChange);
        mRlLogout = findViewById(R.id.rlLogout);
        mRlWithdrawal = findViewById(R.id.rlWithdrawal);
        mTvId = findViewById(R.id.tvId);
        mTvNickname = findViewById(R.id.tvNickname);
        mTvNickname.setText(mNickname);
        mTvId.setText(mId);
        view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
        getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + getResources().getString(R.string.textSetting) + "</font>"));
        }
        mRlLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                showLogoutAlertDialog(getResources().getString(R.string.textAreYouSureWantToLogout));
            }
        });
        mRlNickname.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                showNicknameChangeDialog();
            }
        });
        mRlPasswordChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                showPasswordChangeEditTextAlertDialog(getResources().getString(R.string.textEditPasswordChange));
            }
        });
        mRlWithdrawal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                showWidthdrawalEditTextAlertDialog(getResources().getString(R.string.textCancelCircularMembership));
            }
        });
    }

    void delete(String pw) {
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
        final Call<DeleteResponse> deleteResponseCall = service.delete(new DeleteRequest(pw));
        deleteResponseCall.enqueue(new Callback<DeleteResponse>() {
            @Override
            public void onResponse(Call<DeleteResponse> call, Response<DeleteResponse> response) {
                if(response.body().getSuccess()) {

                    showDeleteAlertDialog(getResources().getString(R.string.textMeantimeThankYouForUsing));
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
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
                        preferences.edit().clear().apply();
                        preferences.edit().putString("id","");
                        preferences.edit().putString("token","");
                        preferences.edit().apply();
                        SettingActivity.super.activityFinish();

                        Intent intent = new Intent(SettingActivity.this, DefaultActivity.class);
                        startActivity(intent);
                    }
                });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertDialog.show();
            }
        });
    }

    public void showDeleteAlertDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        dialog.cancel();

                        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
                        preferences.edit().clear().apply();
                        preferences.edit().putString("id","");
                        preferences.edit().putString("token","");
                        preferences.edit().apply();
                        SettingActivity.super.activityFinish();

                        Intent intent = new Intent(SettingActivity.this, DefaultActivity.class);
                        startActivity(intent);
                    }
                });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertDialog.show();
            }
        });
    }

    public void showAlertDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog.setMessage(message);
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
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        delete(et.getText().toString());
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void showPasswordChangeEditTextAlertDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        final EditText existingEditText;
        final EditText changeEditText;
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.custom_two_edit_item,null);
        alertDialog.setView(layout);

        existingEditText = layout.findViewById(R.id.etExisting);
        existingEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        existingEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());

        changeEditText = layout.findViewById(R.id.etChange);
        changeEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        changeEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());

        alertDialog.setTitle(getResources().getString(R.string.textPasswordChange));
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        updatePassword(existingEditText.getText().toString(),changeEditText.getText().toString());
                    }
                });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertDialog.show();
            }
        });
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
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        updateNickName(et.getText().toString());
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

    void updateNickName(final String id) {
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

    void updatePassword(String pw, String pwNew) {
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
        final Call<UpdatePasswordResponse> updatePasswordResponseCall = service.updatePassword(new UpdatePasswordRequest(pw, pwNew));
        updatePasswordResponseCall.enqueue(new Callback<UpdatePasswordResponse>() {
            @Override
            public void onResponse(Call<UpdatePasswordResponse> call, Response<UpdatePasswordResponse> response) {
                if(response.body().getSuccess()) {
                    showAlertDialog(getResources().getString(R.string.textPasswordChangeSuccess));
                }else {
                    showAlertDialog(getResources().getString(R.string.textPasswordChangeFail));
                }
            }
            @Override
            public void onFailure(Call<UpdatePasswordResponse> call, Throwable t) {
                Log.d("logdinobei", "onFailure");
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
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
    }
}