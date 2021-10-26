package net.ijoon.ijoonweatherstation.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.InputFilter;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.login.AuthService;
import net.ijoon.ijoonweatherstation.login.ResetPasswordRequest;
import net.ijoon.ijoonweatherstation.login.ResetPasswordResponse;
import net.ijoon.ijoonweatherstation.util.UUIDManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ForgotPWActivity extends AppCompatActivity {

    Button mBtnOk;
    EditText mEtId;
    RelativeLayout mRlRoot;
    String mStrId;
    CustomApplication mCustomApplication;
    View view;
    InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_pw);
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        UUIDManager.getDeviceUUID(this);
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
        mEtId = findViewById(R.id.etId);
        mEtId.setFilters(new InputFilter[] {new InputFilter.LengthFilter(64)});
        mRlRoot = findViewById(R.id.rlRoot);
        mBtnOk.setOnClickListener(v -> {
            mStrId = mEtId.getText().toString();
            resetPW(mStrId);
        });
        mRlRoot.setOnClickListener(v -> imm.hideSoftInputFromWindow(mEtId.getWindowToken(), 0));
    }

    void resetPW(String userId) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(chain -> {
            Request request = chain.request().newBuilder().addHeader("Content-Type", "application/json").build();
            return chain.proceed(request);
        });
        Gson gson = new GsonBuilder().setLenient().create();
        Retrofit retrofit = new Retrofit.Builder()
                .client(httpClient.build())
                .baseUrl("http://1.246.219.189:20217")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        AuthService service = retrofit.create(AuthService.class);
        final Call<ResetPasswordResponse> resetPasswordResponseCall = service.resetPasswordRequest(new ResetPasswordRequest(userId));
        resetPasswordResponseCall.enqueue(new Callback<ResetPasswordResponse>() {
            @Override
            public void onResponse(Call<ResetPasswordResponse> call, Response<ResetPasswordResponse> response) {
                ResetPasswordResponse body = response.body();
                if(body == null) {
                    showAlertDialog(getResources().getString(R.string.textCheckInfomation));
                    return;
                }
                if(body.getSuccess()) {
                    switch (body.getErrNo()) {
                        case -1 : {
                            break;
                        }
                        case 0 : {
                            Intent intent = new Intent(ForgotPWActivity.this,ForgotPWAuthenticationActivity.class);
                            intent.putExtra("userId",userId);
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
                    showAlertDialog(getResources().getString(R.string.textCheckInfomation));
                }
            }
            @Override
            public void onFailure(Call<ResetPasswordResponse> call, Throwable t) {
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
                (dialog, which) -> dialog.cancel());
        runOnUiThread(() -> alertDialog.show());
    }
}