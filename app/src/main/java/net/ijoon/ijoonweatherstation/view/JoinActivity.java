package net.ijoon.ijoonweatherstation.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.login.AuthService;
import net.ijoon.ijoonweatherstation.login.JoinRequest;
import net.ijoon.ijoonweatherstation.login.JoinResponse;

import org.json.JSONObject;

import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        ActionBar actionBar = this.getSupportActionBar();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));
        }
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
        mEtNickName.setFilters(new InputFilter[] { filterKoEnNum });
        mEtNickName.setOnKeyListener((v, keyCode, event) -> {
            if(keyCode == event.KEYCODE_ENTER) {
                hideKeyboard();
                return true;
            }
            return false;
        });
        mEtId.setFilters(new InputFilter[] {new InputFilter.LengthFilter(64)});
        mEtPassword.setFilters(new InputFilter[] {new InputFilter.LengthFilter(15)});
        mEtNickName.setFilters(new InputFilter[] {new InputFilter.LengthFilter(15)});
        mEtRePassword.setFilters(new InputFilter[] {new InputFilter.LengthFilter(15)});

        mRlRoot.setOnClickListener(v -> imm.hideSoftInputFromWindow(mEtId.getWindowToken(), 0));
        mBtnJoin.setOnClickListener(v -> {
            mStrId = mEtId.getText().toString();
            mStrPassword = mEtPassword.getText().toString();
            mStrRePassword = mEtRePassword.getText().toString();
            mStrName = mEtNickName.getText().toString();
            if(android.util.Patterns.EMAIL_ADDRESS.matcher(mStrId).matches()) {
                if(mStrPassword.equals(mStrRePassword)) {
                    if (mStrPassword.length() >= 6 && mStrPassword.length() <= 15) {
                        if(!mEtNickName.equals("")) {
                            SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
                            preferences.edit().putString("nickname","");
                            preferences.edit().apply();
                            join(mStrId,mStrPassword, mStrRePassword, mStrName);
                        }else {
                            showAlertDialog(getResources().getString(R.string.textNicknameEmpty));
                        }
                    }else {
                        showAlertDialog(getResources().getString(R.string.text_characters_count));
                    }
                }else {
                    showAlertDialog(getResources().getString(R.string.textPasswordNotMatch));
                }
            }else {
                showAlertDialog(getResources().getString(R.string.textNotEmailFormat));
            }
        });
    }

    private void hideKeyboard()
    {
        imm.hideSoftInputFromWindow(mEtNickName.getWindowToken(), 0);
    }

    void join(String id, String pw, String rePw, String name) {
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
        final Call<JoinResponse> joinResponseCall = service.join(new JoinRequest(id, pw, rePw, name));
        joinResponseCall.enqueue(new Callback<JoinResponse>() {
            @Override
            public void onResponse(Call<JoinResponse> call, Response<JoinResponse> response) {
                JoinResponse body = response.body();
                if(body == null) {
                        try {
                            JSONObject jObjError = new JSONObject(response.errorBody().string());
                            switch (jObjError.getString("errno")) {
                                case "0" : {
                                    break;
                                }
                                case "1" : {
                                    showAlertDialog(getResources().getString(R.string.textRegistered));
                                    break;
                                }
                                case "2" : {
                                    showAlertDialog(getResources().getString(R.string.textJoinFailed));
                                    break;
                                }
                                case "3" : {
                                    showAlertDialog(getResources().getString(R.string.textJoinFailed));
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    return;
                }
                if(body.getSuccess()) {
                    Intent intent = new Intent(JoinActivity.this,JoinEmailActivity.class);
                    intent.putExtra("id", mEtId.getText().toString());
                    startActivity(intent);
                    finish();
                    overridePendingTransition(0,0);
                } else {
                    showAlertDialog(getResources().getString(R.string.textFailed));
                }
            }
            @Override
            public void onFailure(Call<JoinResponse> call, Throwable t) {
                showAlertDialog(getResources().getString(R.string.textJoinFailed));
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(JoinActivity.this,DefaultActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0,0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(JoinActivity.this,DefaultActivity.class);
        startActivity(intent);
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

    //한글, 숫자, 영어 허용
    protected InputFilter filterKoEnNum = new InputFilter() {
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            Pattern ps = Pattern.compile("^[a-zA-Z0-9ㄱ-ㅎ가-힣]+$");

            if (!ps.matcher(source).matches()) {
                return "";
            }
            return null;
        }
    };
}