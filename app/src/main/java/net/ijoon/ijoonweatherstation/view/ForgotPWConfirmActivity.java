package net.ijoon.ijoonweatherstation.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.login.AuthService;
import net.ijoon.ijoonweatherstation.login.ResetPasswordAuthRequest;
import net.ijoon.ijoonweatherstation.login.ResetPasswordAuthResponse;
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

public class ForgotPWConfirmActivity extends AppCompatActivity {

    Button mBtnOk;
    EditText mEtPassword;
    EditText mEtRePassword;
    RelativeLayout mRlRoot;
    String mPw;
    String mPwRe;
    CustomApplication mCustomApplication;
    View view;
    String mStrUserId;
    String mStrAuth;
    InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_pw_confirm);
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        mStrUserId = getIntent().getStringExtra("userId");
        mStrAuth = getIntent().getStringExtra("auth");

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
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + "비밀번호 재설정" + "</font>"));
        }

        mBtnOk = findViewById(R.id.btnOk);
        mEtPassword = findViewById(R.id.etPassword);
        mEtRePassword = findViewById(R.id.etRePassword);
        mRlRoot = findViewById(R.id.rlRoot);
        mBtnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPw = mEtPassword.getText().toString();
                mPwRe = mEtRePassword.getText().toString();
                if (mPw.equals(mPwRe)) {
                    changePassword(mStrUserId, mPw, mStrAuth);
                }else {
                    showAlertDialog(getResources().getString(R.string.textCheckInfomation));
                }
            }
        });

        mRlRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imm.hideSoftInputFromWindow(mEtPassword.getWindowToken(), 0);

            }
        });
    }

    void changePassword(String userId, String password, String verificationCode) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(new Interceptor()    {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request request = chain.request().newBuilder().addHeader("Content-Type", "application/json").build();
                return chain.proceed(request);
            }
        });
        Gson gson = new GsonBuilder().setLenient().create();

        Retrofit retrofit = new Retrofit.Builder()
                .client(httpClient.build())
                .baseUrl("http://1.246.219.189:20217")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        AuthService service = retrofit.create(AuthService.class);
        final Call<ResetPasswordAuthResponse> resetPasswordResponseCall = service.resetPassword(new ResetPasswordAuthRequest(userId,password,verificationCode));
        resetPasswordResponseCall.enqueue(new Callback<ResetPasswordAuthResponse>() {
            @Override
            public void onResponse(Call<ResetPasswordAuthResponse> call, Response<ResetPasswordAuthResponse> response) {
                ResetPasswordAuthResponse body = response.body();

                if(body == null) {
                    showAlertDialog("error : 304");
                    return;
                }

                if(body.getSuccess()) {
                    switch (body.getErrNo()) {
                        case -1 : {
                            break;
                        }
                        case 0 : {
                            showAlertDialog("비밀번호가 재설정 되었습니다. \n변경된 비밀번호로 로그인 후 사용해주세요");
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
                }else {
                    showAlertDialog("확인할 수 없습니다.");
                }
            }
            @Override
            public void onFailure(Call<ResetPasswordAuthResponse> call, Throwable t) {
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
}