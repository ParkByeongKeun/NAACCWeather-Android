package net.ijoon.ijoonweatherstation.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import net.ijoon.ijoonweatherstation.login.ResetPasswordCheckRequest;
import net.ijoon.ijoonweatherstation.login.ResetPasswordCheckResponse;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ForgotPWAuthenticationActivity extends AppCompatActivity {

    Button mBtnOk;
    EditText mEtAuth;
    RelativeLayout mRlRoot;
    String mStrAuth;
    String mStrUserId;
    CustomApplication mCustomApplication;
    View view;
    InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_pw_authentication);
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        mStrUserId = getIntent().getStringExtra("userId");

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
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + getResources().getString(R.string.textChangePassword) + "</font>"));
        }

        mBtnOk = findViewById(R.id.btnOk);
        mEtAuth = findViewById(R.id.etAuth);
        mRlRoot = findViewById(R.id.rlRoot);
        mBtnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStrAuth = mEtAuth.getText().toString();
                checkResetPassword(mStrUserId, mStrAuth);
            }
        });

        mRlRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imm.hideSoftInputFromWindow(mEtAuth.getWindowToken(), 0);
            }
        });
    }

    void checkResetPassword(String userId, String verificationCode) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(new Interceptor() {
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
        final Call<ResetPasswordCheckResponse> resetPasswordCheckResponseCall = service.resetPassword(new ResetPasswordCheckRequest(userId, verificationCode));
        resetPasswordCheckResponseCall.enqueue(new Callback<ResetPasswordCheckResponse>() {
            @Override
            public void onResponse(Call<ResetPasswordCheckResponse> call, Response<ResetPasswordCheckResponse> response) {
                ResetPasswordCheckResponse body = response.body();

                if(body == null) {

                    Log.d("yot132","res = " + response);
                    return;
                }

                if(body.getSuccess()) {
                    switch (body.getErrNo()) {
                        case -1 : {
                            break;
                        }
                        case 0 : {
                            Intent intent = new Intent(ForgotPWAuthenticationActivity.this,ForgotPWConfirmActivity.class);
                            intent.putExtra("userId",userId);
                            intent.putExtra("auth",verificationCode);
                            startActivity(intent);
                            finish();
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
                    showAlertDialog(getResources().getString(R.string.textUnKnown));
                }
            }
            @Override
            public void onFailure(Call<ResetPasswordCheckResponse> call, Throwable t) {
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