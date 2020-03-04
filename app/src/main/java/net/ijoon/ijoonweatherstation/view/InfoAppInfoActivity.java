package net.ijoon.ijoonweatherstation.view;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.ijoon.ijoonweatherstation.R;

public class InfoAppInfoActivity extends AppCompatActivity {

    TextView tvVersion;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_info);


        tvVersion.setText("ver " + getAppVersionName());

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
