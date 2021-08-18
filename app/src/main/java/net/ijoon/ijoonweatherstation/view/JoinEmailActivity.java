
package net.ijoon.ijoonweatherstation.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.login.AuthService;
import net.ijoon.ijoonweatherstation.login.ResendRequest;
import net.ijoon.ijoonweatherstation.login.ResendResponse;
import net.ijoon.ijoonweatherstation.login.VerifyJoinRequest;
import net.ijoon.ijoonweatherstation.login.VerifyJoinResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class JoinEmailActivity extends AppCompatActivity {

    Button mBtnRetryEmail;
    String mId;
    View view;
    Button mBtnAuthOk;
    EditText mEtAuth;
    InputMethodManager imm;
    RelativeLayout mRlRoot;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_email);
        mBtnRetryEmail = findViewById(R.id.btnRetryEmail);
        mBtnAuthOk = findViewById(R.id.btnAuthOk);
        mEtAuth = findViewById(R.id.etAuth);
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        mRlRoot = findViewById(R.id.rlRootView);
        mId = getIntent().getStringExtra("id");
        mRlRoot.setOnClickListener(v -> imm.hideSoftInputFromWindow(mEtAuth.getWindowToken(), 0));
        view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));
        }
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + getResources().getString(R.string.textConfirm) + "</font>"));
        }
        mBtnAuthOk.setOnClickListener(v -> {
            verifyJoin(mId,mEtAuth.getText().toString());
        });
        mBtnRetryEmail.setOnClickListener(v -> resend(mId));
    }

    void verifyJoin(String id,String verifyJoinNumber) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(chain -> {
            Request request = chain.request().newBuilder().addHeader("Content-Type", "application/json").build();
            return chain.proceed(request);
        });
        Gson gson = new GsonBuilder().setLenient().create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://1.246.219.189:20217")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        AuthService service = retrofit.create(AuthService.class);
        final Call<VerifyJoinResponse> verifyJoinResponseCall = service.verifyJoin(new VerifyJoinRequest(id,verifyJoinNumber));
        verifyJoinResponseCall.enqueue(new Callback<VerifyJoinResponse>() {
            @Override
            public void onResponse(Call<VerifyJoinResponse> call, Response<VerifyJoinResponse> response) {
                VerifyJoinResponse body = response.body();
                if(body == null) {// Todo: 로그인인증서버에서 HTTP Status Code에 따라 body가 null이 되는지 확인
                    showAlertDialog("인증 실패");
                    return;
                }
                if(body.getSuccess()) {
                    showAuthAlertDialog("인증 성공\n 로그인 후 사용해주세요.");
                }else {
                    showAlertDialog("인증 실패");
                }
            }
            @Override
            public void onFailure(Call<VerifyJoinResponse> call, Throwable t) {
            }
        });
    }

    void resend(String id) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://1.246.219.189:20217")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        AuthService service = retrofit.create(AuthService.class);
        final Call<ResendResponse> resendResponseCall = service.resend(new ResendRequest(id));
        resendResponseCall.enqueue(new Callback<ResendResponse>() {
            @Override
            public void onResponse(Call<ResendResponse> call, Response<ResendResponse> response) {
                if(response.body().getSuccess()) {
                    showAlertDialog(getResources().getString(R.string.textEmailResent));
                }else {
                    showAlertDialog(getResources().getString(R.string.textResentFailed));
                }
            }
            @Override
            public void onFailure(Call<ResendResponse> call, Throwable t) {
            }
        });
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

    public void showAuthAlertDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog
                .setMessage(message);
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                (dialog, which) -> {
                    Intent intent = new Intent(JoinEmailActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(0,0);
                    dialog.cancel();
                });
        runOnUiThread(() -> alertDialog.show());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(JoinEmailActivity.this,DefaultActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0,0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(JoinEmailActivity.this,DefaultActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0,0);
        return super.onOptionsItemSelected(item);
    }
}