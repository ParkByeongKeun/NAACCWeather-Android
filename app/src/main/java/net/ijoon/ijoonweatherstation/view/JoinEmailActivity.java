
package net.ijoon.ijoonweatherstation.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.login.AuthService;
import net.ijoon.ijoonweatherstation.login.LoginResponse;
import net.ijoon.ijoonweatherstation.login.ResendRequest;
import net.ijoon.ijoonweatherstation.login.ResendResponse;
import net.ijoon.ijoonweatherstation.login.VerifyJoinRequest;
import net.ijoon.ijoonweatherstation.login.VerifyJoinResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class JoinEmailActivity extends AppCompatActivity {

    Button mBtnRetryEmail;
    Button mBtnLoginPage;
    String mId;
    View view;
    Button mBtnAuthOk;
    EditText mEtAuth;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_email);
        mBtnRetryEmail = findViewById(R.id.btnRetryEmail);
        mBtnLoginPage = findViewById(R.id.btnLoginPage);
        mBtnAuthOk = findViewById(R.id.btnAuthOk);
        mEtAuth = findViewById(R.id.etAuth);

        mId = getIntent().getStringExtra("id");

        view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(Color.parseColor("#196482"));//statusBar

        getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + "Circular" + "</font>"));
            actionBar.setBackgroundDrawable(new ColorDrawable(0xFF196482));//title
        }

        mBtnAuthOk.setOnClickListener(v -> {
            verifyJoin(mId,mEtAuth.getText().toString());
        });

        mBtnRetryEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resend(mId);
            }
        });
        mBtnLoginPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(JoinEmailActivity.this, DefaultActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(0,0);
            }
        });
    }

    void verifyJoin(String id,String verifyJoinNumber) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://1.246.219.189:20217")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        AuthService service = retrofit.create(AuthService.class);
        final Call<VerifyJoinResponse> verifyJoinResponseCall = service.verifyJoin(new VerifyJoinRequest(id,verifyJoinNumber));
        verifyJoinResponseCall.enqueue(new Callback<VerifyJoinResponse>() {
            @Override
            public void onResponse(Call<VerifyJoinResponse> call, Response<VerifyJoinResponse> response) {
                VerifyJoinResponse body = response.body();
                Log.d("yot132","id = " + id + " auth = " + verifyJoinNumber );
                if(body == null) {
                    // Todo: 로그인인증서버에서 HTTP Status Code에 따라 body가 null이 되는지 확인
                    showAlertDialog("인증 실패");
                    return;
                }
                if(body.getSuccess()) {
                    showAuthAlertDialog("인증 성공! 로그인 후 사용해주세요.");
                }else {
                    showAlertDialog("인증 실패");
                }
            }
            @Override
            public void onFailure(Call<VerifyJoinResponse> call, Throwable t) {
                Log.d("logdinobei", "onFailure" + t.getMessage());
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
                Log.d("logdinobei", "onFailure" + t.getMessage());
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

    public void showAuthAlertDialog(String message) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                this);
        alertDialog.setTitle(getResources().getString(R.string.textNotification));
        alertDialog
                .setMessage(message);
        alertDialog.setPositiveButton(getResources().getString(R.string.textOk),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        Intent intent = new Intent(JoinEmailActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                        overridePendingTransition(0,0);
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