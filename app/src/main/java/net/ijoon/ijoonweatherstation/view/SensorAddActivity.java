package net.ijoon.ijoonweatherstation.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.scnet_android.GlobalApplication;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.SCNetAppCompatActivity;
import net.ijoon.scnet_android.SCNetClientCallback;

public class SensorAddActivity extends SCNetAppCompatActivity {

    View view;
    EditText mEtSerial;
    EditText mEtAuth;
    Button mBtnRegist;
    CustomApplication mCustomApplication;
    RelativeLayout mRlRoot;
    String mToken;
    InputMethodManager imm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        mCustomApplication = (CustomApplication)getApplication();
        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
        mToken = preferences.getString("token","");
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + getResources().getString(R.string.textNewStationRegistration) + "</font>"));
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
            getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
        }
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
        mEtSerial = findViewById(R.id.etSerial);
        mEtAuth = findViewById(R.id.etAuth);
        mBtnRegist = findViewById(R.id.btnRegist);
        mRlRoot = findViewById(R.id.rlRoot);

        view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        mRlRoot.setOnClickListener(v -> imm.hideSoftInputFromWindow(mEtSerial.getWindowToken(), 0));

        mBtnRegist.setOnClickListener(v -> {
            mEtSerial.getText().toString();
            mEtAuth.getText().toString();
            if(mEtSerial.getText().toString().equals("")  || mEtAuth.getText().toString().equals("")) {
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.textCheckInformation),Toast.LENGTH_SHORT).show();
            }else {
                float lat = 36;
                float lng = 127;
                GlobalApplication globalApplication = (GlobalApplication)getApplicationContext();
                globalApplication.mCallback = SensorAddActivity.this;//testClass;

                envsensor.RegistSensorRequest registSensorRequest = envsensor.RegistSensorRequest.newBuilder().setLatitude(lat).setLongitude(lng).setAccessToken(mToken).setSerial(mEtSerial.getText().toString()).setAuth(mEtAuth.getText().toString()).build();
                mCustomApplication.mServerSession.request(registSensorRequest);
            }
        });
    }

    @Override
    public void onResponse(MessageHeader messageHeader, byte[] bodyBuf) {
        if (messageHeader == null) {
            Log.d("yot132", "packetType is null");
            return;
        }
        switch (messageHeader.mPacketType) {
            case 23: {
                try {
                    final envsensor.RegistSensorResponse response = envsensor.RegistSensorResponse.parseFrom(bodyBuf);
                    if (response.getResult() == envsensor.RegistSensorResponse.Result.success) {
                        Toast.makeText(getApplicationContext(),getResources().getString(R.string.textRegistSuccess),Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(getApplicationContext(),getResources().getString(R.string.textRegistFaile),Toast.LENGTH_SHORT).show();
                    }
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                break;
            }
            case 2 : {
                break;
            }
            default: {
                Log.d("yot132","[onMessage] Unknown PacketType");
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_qrcode, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_qr : {
                IntentIntegrator integrator = new IntentIntegrator(SensorAddActivity.this);
                integrator.setBeepEnabled(false);
                integrator.setPrompt("제품의 QR코드를 화면 중앙에 맞춰주세요.");
                integrator.initiateScan();
                break;
            }
            default: {
                finish();
            }
        }


        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            String contents = result.getContents();
            if(contents != null) {
                String []contentsParsed = contents.split(",");
                if(contentsParsed.length == 2) {

                    String serial = contentsParsed[0];
                    String auth = contentsParsed[1];
                    envsensor.RegistSensorRequest registSensorRequest = envsensor.RegistSensorRequest.newBuilder().setAccessToken(mToken).setSerial(serial).setAuth(auth).build();
                    mCustomApplication.mServerSession.request(registSensorRequest);
                }
                else {
                    Toast.makeText(getApplicationContext(),"error",Toast.LENGTH_LONG).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    class TestClass implements SCNetClientCallback {

        public TestClass () {

        }
        @Override
        public void onAttaching() {

        }

        @Override
        public void onAttached() {

        }

        @Override
        public void onAttachFailed() {

        }

        @Override
        public void onDetach() {

        }

        @Override
        public void onTimedOut() {

        }

        @Override
        public void onResponse(MessageHeader messageHeader, byte[] bodyBuf) {
        }
    }
}