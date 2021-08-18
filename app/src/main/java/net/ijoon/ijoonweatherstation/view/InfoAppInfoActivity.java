package net.ijoon.ijoonweatherstation.view;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;

public class InfoAppInfoActivity extends AppCompatActivity {

    TextView tvVersion;
    CustomApplication mCustomApplication;
    ActionBar actionBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_info);
        tvVersion = findViewById(R.id.tvVersion);
        tvVersion.setText("App version\n " + getAppVersionName());
        mCustomApplication = (CustomApplication) getApplicationContext();
        actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + getResources().getString(R.string.textAppInfomation) + "</font>"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_list: {
                return true;
            }
            default: {
                finish();
                return true;
            }
        }
    }

    public String getAppVersionName(){
        PackageInfo packageInfo;
        try{
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        }catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
            return "";
        }
        return packageInfo.versionName;
    }
}