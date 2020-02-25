package net.ijoon.ijoonweatherstation.view;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.RendezvousClientCallback;
import net.ijoon.scnet_android.SCNetAppCompatActivity;

public class SensorAddActivity extends SCNetAppCompatActivity implements RendezvousClientCallback {

    View view;
    EditText mEtSerial;
    EditText mEtAuth;
    Button mBtnRegist;
    CustomApplication mCustomApplication;
    RelativeLayout mRlRoot;
    String mToken;
    InputMethodManager imm;
    ActionBar actionBar;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
            actionBar.setTitle(Html.fromHtml("<font color='#000000'>" + getResources().getString(R.string.textNewStationRegistration) + "</font>"));
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        }
        getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
        getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
        mEtSerial = findViewById(R.id.etSerial);
        mEtAuth = findViewById(R.id.etAuth);
        mBtnRegist = findViewById(R.id.btnRegist);
        mRlRoot = findViewById(R.id.rlRoot);

        view = getWindow().getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        mRlRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imm.hideSoftInputFromWindow(mEtSerial.getWindowToken(), 0);
            }
        });

        mBtnRegist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEtSerial.getText().toString();
                mEtAuth.getText().toString();
                if(mEtSerial.getText().toString().equals("")  || mEtAuth.getText().toString().equals("")) {
                    showAlertDialog("입력정보를 확인해주세요.");
                }else {
                    envsensor.RegistSensorRequest registSensorRequest = envsensor.RegistSensorRequest.newBuilder().setLatitude(0).setLongitude(0).setAccessToken(mToken).setSerial(mEtSerial.getText().toString()).setAuth(mEtAuth.getText().toString()).build();
                    mCustomApplication.mServerSession.request(registSensorRequest);
                }


            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMessage(MessageHeader messageHeader, byte[] buffer) {
        envsensor.PacketType packetType = envsensor.PacketType.forNumber(messageHeader.mPacketType);
        if(packetType == null) {
            Log.d("yot132", "packetType is null");
            return;
        }
        switch (packetType) {
            case registSensorRes: {
                try {
                    final envsensor.RegistSensorResponse response = envsensor.RegistSensorResponse.parseFrom(buffer);
                    if (response.getResult() == envsensor.RegistSensorResponse.Result.success) {
                        showAlertDialog(getResources().getString(R.string.textRegistSuccess));
                    }else {
                        showAlertDialog(getResources().getString(R.string.textRegistFaile));
                    }
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }

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
        getMenuInflater().inflate(R.menu.menu_qrcode, menu);
        return super.onCreateOptionsMenu(menu);
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
}