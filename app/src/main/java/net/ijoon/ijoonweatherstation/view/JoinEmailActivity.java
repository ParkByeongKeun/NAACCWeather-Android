
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

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.login.AuthService;
import net.ijoon.ijoonweatherstation.login.ResendRequest;
import net.ijoon.ijoonweatherstation.login.ResendResponse;

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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_email);
        mBtnRetryEmail = findViewById(R.id.btnRetryEmail);
        mBtnLoginPage = findViewById(R.id.btnLoginPage);
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
}