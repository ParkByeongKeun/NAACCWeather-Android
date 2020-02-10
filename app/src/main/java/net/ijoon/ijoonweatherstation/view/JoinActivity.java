package net.ijoon.ijoonweatherstation.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import net.ijoon.ijoonweatherstation.R;

import retrofit2.Retrofit;

public class JoinActivity extends AppCompatActivity {

    EditText mEtId;
    EditText mEtPassword;
    EditText mEtNickName;
    EditText mEtRePassword;
    Button mBtnJoin;
    String mStrId;
    String mStrPassword;
    String mStrRePassword;
    String mStrName;
    View view;
    RelativeLayout mRlRoot;
    InputMethodManager imm;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));
        view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        ActionBar actionBar = this.getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + getResources().getString(R.string.textJoin) + "</font>"));

        }


        mEtId = findViewById(R.id.etId);
        mEtPassword = findViewById(R.id.etPassword);
        mEtNickName = findViewById(R.id.etNick);
        mEtRePassword = findViewById(R.id.etRePassword);
        mRlRoot = findViewById(R.id.rlRoot);
        mBtnJoin = findViewById(R.id.btnJoin);
        mRlRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imm.hideSoftInputFromWindow(mEtId.getWindowToken(), 0);
            }
        });
        mBtnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStrId = mEtId.getText().toString();
                mStrPassword = mEtPassword.getText().toString();
                mStrRePassword = mEtRePassword.getText().toString();
                mStrName = mEtNickName.getText().toString();

                if(android.util.Patterns.EMAIL_ADDRESS.matcher(mStrId).matches()) {
                    if(mStrPassword.equals(mStrRePassword)) {
                        if (mStrPassword.length() >6) {
                            if(!mEtNickName.equals("")) {
                                join(mStrId,mStrPassword, mStrRePassword, mStrName);
                            }else {
                                showAlertDialog(getResources().getString(R.string.textNicknameEmpty));
                            }
                        }else {
                            showAlertDialog(getResources().getString(R.string.textPasswordMustSixCharacters));
                        }
                    }else {
                        showAlertDialog(getResources().getString(R.string.textPasswordNotMatch));
                    }
                }else {
                    showAlertDialog(getResources().getString(R.string.textNotEmailFormat));
                }
            }
        });
    }

    void join(String id, String pw, String rePw, String name) {
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl("http://circular.ijoon.net")
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//        AuthService service = retrofit.create(AuthService.class);
//        final Call<JoinResponse> joinResponseCall = service.join(new JoinRequest(id, pw, rePw, name));
//        joinResponseCall.enqueue(new Callback<JoinResponse>() {
//            @Override
//            public void onResponse(Call<JoinResponse> call, Response<JoinResponse> response) {
//                Intent intent = new Intent(JoinActivity.this,JoinEmailActivity.class);
//                intent.putExtra("id", mEtId.getText().toString());
//                startActivity(intent);
//                finish();
//            }
//            @Override
//            public void onFailure(Call<JoinResponse> call, Throwable t) {
//                showAlertDialog(getResources().getString(R.string.textJoinFailed));
//            }
//        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(JoinActivity.this,DefaultActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(JoinActivity.this,DefaultActivity.class);
        startActivity(intent);
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
}