package net.ijoon.ijoonweatherstation.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mingle.sweetpick.CustomDelegate;
import com.mingle.sweetpick.SweetSheet;
import com.skyfishjy.library.RippleBackground;
import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.util.DataProgrssBar;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.util.ComputableDate;
import net.ijoon.ijoonweatherstation.util.SunSet;
import net.ijoon.scnet_android.Connection;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.RendezvousSession;
import net.ijoon.scnet_android.SCNetAppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class MonitorActivity extends SCNetAppCompatActivity implements  SwipeRefreshLayout.OnRefreshListener {

    private RecyclerView horizontal_recycler_view;
    HorizontalAdapter horizontalAdapter;
    public List<DataProgrssBar> horizontalList;
    SwipeRefreshLayout mSwipeRefreshLayout;
    SweetSheet mSweetSheet;
    String mToken;
    CustomApplication mCustomApplication;
    TextView mTvWindDirection;
    TextView mTvTemperature;
    TextView mTvHumidity;//습기
    TextView mTvPressure;//압력
    TextView mTvWindVelocityValue;
    TextView mTvWindTitle;
    TextView mTvWindSubTitle;
    TextView mTvWeatherComment;
    TextView mTvMaxVelocity;
    TextView mTvMaxVelocityTime;
    RelativeLayout mRlTopTemp;
    ImageView mIvWeather;
    DataProgrssBar dataProgrssBar;
    RelativeLayout mRootView;
    RelativeLayout mRelativeWindDirection;
    Toolbar toolbar;
    RippleBackground rippleBackground;
    ImageView mIvCompass;
    TextView mTvCompass;
    int screenWidth;
    int screenHeight;
    ComputableDate computableDate;
    envsensor.Sensor mEnvSensor;
    float mWindDirection;
    boolean isRain;
    boolean isTimerFinish;
    int mTouchCheck;
    SunSet sunSet;
    SunSet sunRise;
    RelativeLayout rl;
    int prevValue = 0;

    CountDownTimer countDownTimer = new CountDownTimer(600000, 1000) {
        public void onTick(long millisUntilFinished) { }
        public void onFinish() {
            isTimerFinish = true;
            isRain = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);
        mSwipeRefreshLayout = findViewById(R.id.swipe_layout);
        mCustomApplication = (CustomApplication) getApplicationContext();
        mEnvSensor = mCustomApplication.getEnvSensor();
        rl = findViewById(R.id.rl);
        IntentFilter filter = new IntentFilter();
        filter.addAction("maxVelocityData");
        registerReceiver(mBroadcastReceiver, filter);
        mTvWindDirection = findViewById(R.id.tvWindDirection);
        mTvTemperature = findViewById(R.id.tvTemp);
        mTvHumidity = findViewById(R.id.tvHumidityValue);//습기
        mTvPressure = findViewById(R.id.tvAtmValue);//압력
        mTvWindVelocityValue = findViewById(R.id.tvWindVelocityValue);
        mTvWindTitle = findViewById(R.id.tvWindTitle);
        mTvWindSubTitle = findViewById(R.id.tvWindSubTitle);
        mIvWeather = findViewById(R.id.ivWeather);
        mTvWeatherComment = findViewById(R.id.tvWeather);
        mRootView = findViewById(R.id.rlRoot);
        mRlTopTemp = findViewById(R.id.rlWind);
        mTvMaxVelocity = findViewById(R.id.tv_wind_velocity_max);
        mTvMaxVelocityTime = findViewById(R.id.tv_velocity_max_time);
        rippleBackground = findViewById(R.id.content);
        mRelativeWindDirection = findViewById(R.id.rlMidWindDirection);
        mIvCompass = findViewById(R.id.ivCompass);
        mTvCompass = findViewById(R.id.tvDirection);
        computableDate = new ComputableDate();
        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mRlTopTemp.getLayoutParams();
        params.height = screenHeight/4;
        params.width = screenWidth;
        mRlTopTemp.setLayoutParams(params);
        RelativeLayout.LayoutParams ivParams = (RelativeLayout.LayoutParams) mIvWeather.getLayoutParams();
        ivParams.height = screenHeight/8;
        ivParams.width = screenHeight/8;
        mIvWeather.setLayoutParams(ivParams);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(Html.fromHtml("<font color='#FFFFFF'>" + mEnvSensor.getName() + "</font>"));
        setSupportActionBar(toolbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));
        }
        sunSet = new SunSet();
        sunRise = new SunSet();
        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
        mToken = preferences.getString("token","");
        horizontal_recycler_view= findViewById(R.id.horizontal_recycler_view);
        horizontalList=new ArrayList<>();
        dataProgrssBar = new DataProgrssBar(0,"0");
        horizontalList.add(dataProgrssBar);
        horizontalAdapter=new HorizontalAdapter(horizontalList);
        LinearLayoutManager horizontalLayoutManagaer
                = new LinearLayoutManager(MonitorActivity.this, LinearLayoutManager.HORIZONTAL, false);
        horizontal_recycler_view.setLayoutManager(horizontalLayoutManagaer);
        horizontal_recycler_view.setAdapter(horizontalAdapter);
        horizontal_recycler_view.setNestedScrollingEnabled(false);
        horizontal_recycler_view.setLayoutManager(new LinearLayoutManager(this){
            @Override
            public boolean canScrollVertically() { // 세로스크롤 막기
                return false;
            }
            @Override
            public boolean canScrollHorizontally() { //가로 스크롤막기
                return false;
            }
        });
        mSwipeRefreshLayout.setRefreshing(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        envsensor.StreamingStopRequest request = envsensor.StreamingStopRequest.newBuilder().setAccessToken(mToken).setSerial(mEnvSensor.getSerial()).build();
        mCustomApplication.mServerSession.request(request);
        envsensor.StreamingStartRequest request1 = envsensor.StreamingStartRequest.newBuilder().setAccessToken(mToken).setSerial(mEnvSensor.getSerial()).build();
        mCustomApplication.mServerSession.request(request1);
        mSweetSheet = new SweetSheet(rl);
        mRootView.setOnClickListener(v -> {
            if(mSweetSheet.isShow()){
                mSweetSheet.dismiss();
            }
            mTouchCheck++;
            if(mTouchCheck == 10) {
                Intent intent = new Intent(MonitorActivity.this, DevActivity.class);
                startActivity(intent);
                finish();
            }
        });
        mIvWeather.setOnClickListener(v -> {
            if(mSweetSheet.isShow()) {
                return;
            }
            if(sunSet.getSunSetHour() == null) {
                return;
            }
            setupCustomView();
            mSweetSheet.toggle();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onRefresh() {
    }

    public class HorizontalAdapter extends RecyclerView.Adapter<HorizontalAdapter.MyViewHolder> {
        private List<DataProgrssBar> horizontalList;
        public class MyViewHolder extends RecyclerView.ViewHolder {
            public ProgressBar txtView;
            TextView x;
            public MyViewHolder(View view) {
                super(view);
                txtView = view.findViewById(R.id.pb);
                x = view.findViewById(R.id.TextViewID);
            }
        }
        public HorizontalAdapter(List<DataProgrssBar> horizontalList) {
            this.horizontalList = horizontalList;
        }
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.single_pb, parent, false);
            return new MyViewHolder(itemView);
        }
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void onBindViewHolder(final MyViewHolder holder, final int position) {
            holder.txtView.setProgress((horizontalList.get(position).getProgress()));
            holder.x.setText((horizontalList.get(position).getDate()));
            if(position == horizontalList.size()-1 ){
                holder.x.setBackground(getResources().getDrawable(R.drawable.circlenew));
                holder.x.setTextColor(Color.WHITE);
            }
        }
        @Override
        public int getItemCount() {
            return horizontalList.size();
        }
    }

    @Override
    public void onResponse(MessageHeader messageHeader, final byte[] bodyBuf) {
        if(messageHeader == null) {
            return;
        }
        switch (messageHeader.mPacketType) {
            case 0: {
                break;
            }
            case 1: {
                try {
                    envsensor.StreamingStartResponse response = envsensor.StreamingStartResponse.parseFrom(bodyBuf);
                    if(response.getResult() == envsensor.StreamingStartResponse.Result.success) {
                        runOnUiThread(() -> {
                            if(mSwipeRefreshLayout.isRefreshing()) {
                                mSwipeRefreshLayout.setRefreshing(false);
                            }
                            float tempWindDirection = 0;
                            if(response.getData().getDirection() == 0 ) {
                                tempWindDirection = mWindDirection;
                            }else {
                                tempWindDirection = response.getData().getDirection();
                                mWindDirection = response.getData().getDirection();
                            }
                            rotationCompass(tempWindDirection);
                            if (tempWindDirection <= 15 || tempWindDirection >= 345){
                                //북
                                rippleBackground.setX(screenWidth/2);
                                rippleBackground.setY(-rippleBackground.getHeight());
                                rippleBackground.startRippleAnimation();
                                mTvCompass.setText(getResources().getString(R.string.text_wind_north));
                            }else if (tempWindDirection >15&& tempWindDirection <=75){
                                //북동
                                rippleBackground.setX(screenWidth);
                                rippleBackground.setY(-rippleBackground.getHeight());
                                rippleBackground.startRippleAnimation();
                                mTvCompass.setText(getResources().getString(R.string.text_wind_east_north));
                            }else if (tempWindDirection >75 && tempWindDirection <= 105){
                                //동
                                rippleBackground.setX(screenWidth);
                                rippleBackground.setY(screenHeight/2);
                                rippleBackground.startRippleAnimation();
                                mTvCompass.setText(getResources().getString(R.string.text_wind_east));
                            }else if (tempWindDirection > 105 && tempWindDirection <= 165){
                                //남동
                                rippleBackground.setX(screenWidth);
                                rippleBackground.setY(screenHeight/2);
                                rippleBackground.startRippleAnimation();
                                mTvCompass.setText(getResources().getString(R.string.text_wind_south_east));
                            }else if (tempWindDirection > 165 && tempWindDirection <= 195){
                                //남
                                rippleBackground.setX(screenWidth/2);
                                rippleBackground.setY(screenHeight);
                                rippleBackground.startRippleAnimation();
                                mTvCompass.setText(getResources().getString(R.string.text_wind_south));
                            }else if (tempWindDirection > 195 && tempWindDirection <= 255){
                                //남서
                                rippleBackground.setX(-screenWidth);
                                rippleBackground.setY(screenHeight);
                                rippleBackground.startRippleAnimation();
                                mTvCompass.setText(getResources().getString(R.string.text_wind_south_west));
                            }else if (tempWindDirection > 255 && tempWindDirection <= 285){
                                //서
                                rippleBackground.setX(-screenWidth);
                                rippleBackground.setY(screenHeight/2);
                                rippleBackground.startRippleAnimation();
                                mTvCompass.setText(getResources().getString(R.string.text_wind_west));
                            }else if (tempWindDirection > 285 && tempWindDirection < 345){
                                //북서
                                rippleBackground.setX(-screenWidth);
                                rippleBackground.setY(-rippleBackground.getHeight());
                                rippleBackground.startRippleAnimation();
                                mTvCompass.setText(getResources().getString(R.string.text_wind_north_west));
                            }
                            mTvMaxVelocity.setText(String.format("%.1f",mCustomApplication.getEnvSensor().getMaxVelocity()) + "m/s");
                            String[] tempMaxVelocityTime = mCustomApplication.getEnvSensor().getMaxTime().split(" ");
                            mTvMaxVelocityTime.setText(tempMaxVelocityTime[0]);
                            mTvTemperature.setText(String.format("%.2f",response.getData().getTemperature())+"℃");
                            mTvHumidity.setText(String.format("%.2f",response.getData().getHumidity())+" %");//습기
                            mTvPressure.setText(String.format("%.2f",response.getData().getPressure())+" hPa");//압력
                            sunSet.getSunset(computableDate.getYear(),computableDate.getMonth(),computableDate.getDay(),Double.valueOf(mEnvSensor.getLatitude()),Double.valueOf(mEnvSensor.getLongitude()));
                            sunRise.getSunrise(computableDate.getYear(),computableDate.getMonth(),computableDate.getDay(),Double.valueOf(mEnvSensor.getLatitude()),Double.valueOf(mEnvSensor.getLongitude()));
                            if(response.getData().getRain() >= 5) {
                                //rain
                                isRain = true;
                                countDownTimer.cancel();
                                countDownTimer.start();
                                mIvWeather.setImageResource(R.drawable.ic_rain);
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title6));
                            }else {
                                if(computableDate.getAMPM() != 1 ) {
                                    //오전
                                    if(response.getData().getLuminosity() > 100) {
                                        //cloud
                                        mIvWeather.setImageResource(R.drawable.ic_moon);
                                    }else {
                                        mIvWeather.setImageResource(R.drawable.ic_moon_cloud);
                                    }
                                    if(computableDate.getHour() <= sunRise.getSunSetHour()) {
                                        if(computableDate.getHour() == sunRise.getSunSetHour()) {
                                            if(computableDate.getMinute() < sunRise.getSunSetMinute()) {
                                                if(response.getData().getLuminosity() > 100) {
                                                    //cloud
                                                    mIvWeather.setImageResource(R.drawable.ic_moon);
                                                }else {
                                                    mIvWeather.setImageResource(R.drawable.ic_moon_cloud);
                                                }
                                            }else {
                                                //일출
                                                if(response.getData().getLuminosity() >= 30000 ) {
                                                    //sun
                                                    mIvWeather.setImageResource(R.drawable.ic_sun);
                                                }else if(response.getData().getLuminosity() < 30000 && response.getData().getLuminosity() > 15000 ) {
                                                    //sun_cloud
                                                    mIvWeather.setImageResource(R.drawable.ic_sun_cloud);
                                                }else  {
                                                    //cloud
                                                    mIvWeather.setImageResource(R.drawable.ic_cloud);
                                                }
                                            }
                                        }
                                    }else {
                                        // 해 뜬 오전
                                        if(response.getData().getLuminosity() >= 30000 ) {
                                            //sun
                                            mIvWeather.setImageResource(R.drawable.ic_sun);
                                        }else if(response.getData().getLuminosity() < 30000 && response.getData().getLuminosity() > 15000 ) {
                                            //sun_cloud
                                            mIvWeather.setImageResource(R.drawable.ic_sun_cloud);
                                        }else  {
                                            //cloud
                                            mIvWeather.setImageResource(R.drawable.ic_cloud);
                                        }
                                    }
                                }else {
                                    //오후
                                    if(computableDate.getHour() >= sunSet.getSunSetHour()) {
                                        //오후 6시 이후
                                        if (computableDate.getHour() == sunSet.getSunSetHour()) {
                                            if (computableDate.getMinute() <= sunSet.getSunSetMinute()) {
                                                if(response.getData().getLuminosity() >= 30000 ) {
                                                    //sun
                                                    mIvWeather.setImageResource(R.drawable.ic_sun);
                                                }else if(response.getData().getLuminosity() < 30000 && response.getData().getLuminosity() > 15000 ) {
                                                    //sun_cloud
                                                    mIvWeather.setImageResource(R.drawable.ic_sun_cloud);
                                                }else  {
                                                    //cloud
                                                    mIvWeather.setImageResource(R.drawable.ic_cloud);
                                                }
                                            }else {
                                                if(response.getData().getLuminosity() > 100) {
                                                    //cloud
                                                    mIvWeather.setImageResource(R.drawable.ic_moon);
                                                }else {
                                                    mIvWeather.setImageResource(R.drawable.ic_moon_cloud);
                                                }
                                            }
                                        }
                                        if(response.getData().getLuminosity() > 100) {
                                            //cloud
                                            mIvWeather.setImageResource(R.drawable.ic_moon);
                                        }else {
                                            mIvWeather.setImageResource(R.drawable.ic_moon_cloud);
                                        }
                                    }else {
                                        if(response.getData().getLuminosity() >= 30000 ) {
                                            //sun
                                            mIvWeather.setImageResource(R.drawable.ic_sun);
                                        }else if(response.getData().getLuminosity() < 30000 && response.getData().getLuminosity() > 15000 ) {
                                            //sun_cloud
                                            mIvWeather.setImageResource(R.drawable.ic_sun_cloud);
                                        }else  {
                                            //cloud
                                            mIvWeather.setImageResource(R.drawable.ic_cloud);
                                        }
                                    }
                                }
                                if(computableDate.getMonth() <=4 || computableDate.getMonth() >=11) {
                                    //겨울
                                    if(response.getData().getTemperature() < 0 ) {
                                        //영하
                                        mTvWeatherComment.setText(getResources().getString(R.string.midle_title8));
                                    }else {
                                        //영상
                                        mTvWeatherComment.setText(getResources().getString(R.string.midle_title1));
                                    }
                                }else if(computableDate.getMonth() < 6 || computableDate.getMonth() > 4 ) {
                                    //봄
                                    if(response.getData().getTemperature() < 15 ) {
                                        mTvWeatherComment.setText(getResources().getString(R.string.midle_title1));
                                    }else {
                                        mTvWeatherComment.setText(getResources().getString(R.string.midle_title4));
                                    }
                                }else if (computableDate.getMonth() >= 6 || computableDate.getMonth() <= 9) {
                                    //여름
                                    if(response.getData().getTemperature() < 30 ) {
                                        mTvWeatherComment.setText(getResources().getString(R.string.midle_title5));
                                    }else {
                                        if(response.getData().getHumidity() > 80 ) {
                                            mTvWeatherComment.setText(getResources().getString(R.string.midle_title7));
                                        }else {
                                            mTvWeatherComment.setText(getResources().getString(R.string.midle_title9));
                                        }
                                    }
                                }else {
                                    //가을
                                    if(response.getData().getTemperature() < 10) {
                                        mTvWeatherComment.setText(getResources().getString(R.string.midle_title1));
                                    }else {
                                        mTvWeatherComment.setText(getResources().getString(R.string.midle_title3));
                                    }
                                }
                            }
                            if(response.getData().getRain()  < 5 && isRain && !isTimerFinish) {
                                mIvWeather.setImageResource(R.drawable.ic_rain);
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title6));
                            }
                            if(response.getData().getVelocity() <= 0.1) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation));
                                mRootView.setBackgroundResource(R.drawable.blue_skies);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
                                    getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
                                }
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
                            }else if(response.getData().getVelocity() >= 0.11 && response.getData().getVelocity() <= 1.5 ){
                                mTvWindTitle.setText(getResources().getString(R.string.Wind1));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation1));
                                mRootView.setBackgroundResource(R.drawable.blue_skies);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
                                    getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
                                }
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
                            } else if(response.getData().getVelocity() >= 1.6 && response.getData().getVelocity() <=3.3 ) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind2));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation2));
                                mRootView.setBackgroundResource(R.drawable.blue_skies);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
                                    getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
                                }
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
                            }else if(response.getData().getVelocity() >= 3.4  && response.getData().getVelocity() <= 5.4 ) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind3));
                                mRootView.setBackgroundResource(R.drawable.blue_skies);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    getWindow().setNavigationBarColor(Color.parseColor("#52CC7D"));//bottom
                                    getWindow().setStatusBarColor(Color.parseColor("#F4D101"));//statusBar
                                }
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFF4D101));
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation3));
                            }else if(response.getData().getVelocity() >= 5.5  && response.getData().getVelocity() <= 7.9 ) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind4));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation4));
                                mRootView.setBackgroundResource(R.drawable.blue_skies);
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    getWindow().setNavigationBarColor(Color.parseColor("#52CC7D"));//bottom
                                    getWindow().setStatusBarColor(Color.parseColor("#F4D101"));//statusBar
                                }
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFF4D101));
                            }else if(response.getData().getVelocity() >= 8.0  && response.getData().getVelocity() <= 9.9 ) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind5));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation5));
                                mRootView.setBackgroundResource(R.drawable.blue_skies);
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    getWindow().setNavigationBarColor(Color.parseColor("#A990D3"));//bottom
                                    getWindow().setStatusBarColor(Color.parseColor("#F7C0EA"));//statusBar
                                }
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFF7C0EA));//title
                            }else if(response.getData().getVelocity() >= 10.0  && response.getData().getVelocity() <= 13.8 ) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind6));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation6));
                                mRootView.setBackgroundResource(R.drawable.bg_lv1);
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    getWindow().setNavigationBarColor(Color.parseColor("#A990D3"));//bottom
                                    getWindow().setStatusBarColor(Color.parseColor("#F7C0EA"));//statusBar
                                }
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFF7C0EA));//title
                            }else if(response.getData().getVelocity() >= 13.9  && response.getData().getVelocity() <= 14.9 ) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind7));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation7));
                                mRootView.setBackgroundResource(R.drawable.bg_lv1);
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    getWindow().setNavigationBarColor(Color.parseColor("#A990D3"));//bottom
                                    getWindow().setStatusBarColor(Color.parseColor("#F7C0EA"));//statusBar
                                }
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFF7C0EA));//title
                            }else if(response.getData().getVelocity() >= 15.0 && response.getData().getVelocity() <= 20.7) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind8));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation8));
                                mRootView.setBackgroundResource(R.drawable.bg_lv2);
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    getWindow().setNavigationBarColor(Color.parseColor("#FFAE98"));//bottom
                                    getWindow().setStatusBarColor(Color.parseColor("#FF0B45"));//statusBar
                                }
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFFF0B45));//title
                            }else if(response.getData().getVelocity() >= 20.8  && response.getData().getVelocity() <= 24.4) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind9));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation9));
                                mRootView.setBackgroundResource(R.drawable.bg_lv2);
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    getWindow().setNavigationBarColor(Color.parseColor("#FFAE98"));//bottom
                                    getWindow().setStatusBarColor(Color.parseColor("#FF0B45"));//statusBar
                                }
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFFF0B45));//title
                            }else if(response.getData().getVelocity() >= 24.5 && response.getData().getVelocity() <= 28.4) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind10));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation10));
                                mRootView.setBackgroundResource(R.drawable.bg_lv3);
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    getWindow().setNavigationBarColor(Color.parseColor("#FFAE98"));//bottom
                                    getWindow().setStatusBarColor(Color.parseColor("#FF0B45"));//statusBar
                                }
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFFF0B45));//title
                            }else if(response.getData().getVelocity() >= 28.5 && response.getData().getVelocity() <= 32.6) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind11));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation11));
                                mRootView.setBackgroundResource(R.drawable.bg_lv3);
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    getWindow().setNavigationBarColor(Color.parseColor("#FFAE98"));//bottom
                                    getWindow().setStatusBarColor(Color.parseColor("#FF0B45"));//statusBar
                                }
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFFF0B45));//title
                            }else if(response.getData().getVelocity() >= 32.7){
                                mTvWindTitle.setText(getResources().getString(R.string.Wind12));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation12));
                                mRootView.setBackgroundResource(R.drawable.bg_lv3);
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    getWindow().setNavigationBarColor(Color.parseColor("#FFAE98"));//bottom
                                    getWindow().setStatusBarColor(Color.parseColor("#FF0B45"));//statusBar
                                }
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFFF0B45));//title
                            }
                            mTvWindVelocityValue.setText(String.valueOf(String.format("%.2f",response.getData().getVelocity()) + "m/s"));
                            dataProgrssBar.setProgress((int)response.getData().getVelocity());
                            dataProgrssBar.setDate(String.valueOf(String.format("%.2f",response.getData().getVelocity())));
                            horizontalList.set(0,dataProgrssBar);
                            horizontalAdapter=new HorizontalAdapter(horizontalList);
                            horizontal_recycler_view.setAdapter(horizontalAdapter);
                        });
                    }else {
                        Log.d("yot132","envsensor.StreamingStartResponse.Result.failed called");
                    }
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                //streamingStartResponse
                break;
            }
            case 2: {
                break;
            }
            case 3: {
                //streamingStopResponse
                break;
            }
            case 4: {
                try {
                    final envsensor.EnvSensorData response = envsensor.EnvSensorData.parseFrom(bodyBuf);
                    runOnUiThread(() -> {
                        if(mSwipeRefreshLayout.isRefreshing()) {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                        float tempWindDirection = 0;
                        if(response.getDirection() == 0 ) {
                            tempWindDirection = mWindDirection;
                        }else {
                            tempWindDirection = response.getDirection();
                            mWindDirection = response.getDirection();
                        }
                        rotationCompass(tempWindDirection);
                        if (tempWindDirection <= 15 || tempWindDirection >= 345){
                            //북
                            rippleBackground.setX(screenWidth/2);
                            rippleBackground.setY(-rippleBackground.getHeight());
                            rippleBackground.startRippleAnimation();
                            mTvCompass.setText(getResources().getString(R.string.text_wind_north));
                        }else if (tempWindDirection >15&& tempWindDirection <=75){
                            //북동
                            rippleBackground.setX(screenWidth);
                            rippleBackground.setY(-rippleBackground.getHeight());
                            rippleBackground.startRippleAnimation();
                            mTvCompass.setText(getResources().getString(R.string.text_wind_east_north));
                        }else if (tempWindDirection >75 && tempWindDirection <= 105){
                            //동
                            rippleBackground.setX(screenWidth);
                            rippleBackground.setY(screenHeight/2);
                            rippleBackground.startRippleAnimation();
                            mTvCompass.setText(getResources().getString(R.string.text_wind_east));
                        }else if (tempWindDirection > 105 && tempWindDirection <= 165){
                            //남동
                            rippleBackground.setX(screenWidth);
                            rippleBackground.setY(screenHeight/2);
                            rippleBackground.startRippleAnimation();
                            mTvCompass.setText(getResources().getString(R.string.text_wind_south_east));
                        }else if (tempWindDirection > 165 && tempWindDirection <= 195){
                            //남
                            rippleBackground.setX(screenWidth/2);
                            rippleBackground.setY(screenHeight);
                            rippleBackground.startRippleAnimation();
                            mTvCompass.setText(getResources().getString(R.string.text_wind_south));
                        }else if (tempWindDirection > 195 && tempWindDirection <= 255){
                            //남서
                            rippleBackground.setX(-screenWidth);
                            rippleBackground.setY(screenHeight);
                            rippleBackground.startRippleAnimation();
                            mTvCompass.setText(getResources().getString(R.string.text_wind_south_west));
                        }else if (tempWindDirection > 255 && tempWindDirection <= 285){
                            //서
                            rippleBackground.setX(-screenWidth);
                            rippleBackground.setY(screenHeight/2);
                            rippleBackground.startRippleAnimation();
                            mTvCompass.setText(getResources().getString(R.string.text_wind_west));
                        }else if (tempWindDirection > 285 && tempWindDirection < 345){
                            //북서
                            rippleBackground.setX(-screenWidth);
                            rippleBackground.setY(-rippleBackground.getHeight());
                            rippleBackground.startRippleAnimation();
                            mTvCompass.setText(getResources().getString(R.string.text_wind_north_west));
                        }
                        mTvMaxVelocity.setText(String.format("%.1f",mCustomApplication.getEnvSensor().getMaxVelocity()) + "m/s");
                        String[] tempMaxVelocityTime = mCustomApplication.getEnvSensor().getMaxTime().split(" ");
                        mTvMaxVelocityTime.setText(tempMaxVelocityTime[0]);
                        mTvTemperature.setText(String.format("%.2f",response.getTemperature())+"℃");
                        mTvHumidity.setText(String.format("%.2f",response.getHumidity())+" %");//습기
                        mTvPressure.setText(String.format("%.2f",response.getPressure())+" hPa");//압력
                        sunSet.getSunset(computableDate.getYear(),computableDate.getMonth(),computableDate.getDay(),Double.valueOf(mEnvSensor.getLatitude()),Double.valueOf(mEnvSensor.getLongitude()));
                        sunRise.getSunrise(computableDate.getYear(),computableDate.getMonth(),computableDate.getDay(),Double.valueOf(mEnvSensor.getLatitude()),Double.valueOf(mEnvSensor.getLongitude()));
                        if(response.getRain() >= 5) {
                            //rain
                            isRain = true;
                            countDownTimer.cancel();
                            countDownTimer.start();
                            mIvWeather.setImageResource(R.drawable.ic_rain);
                            mTvWeatherComment.setText(getResources().getString(R.string.midle_title6));
                        }else {
                            if(computableDate.getAMPM() != 1 ) {
                                //오전
                                if(response.getLuminosity() > 100) {
                                    //cloud
                                    mIvWeather.setImageResource(R.drawable.ic_moon);
                                }else {
                                    mIvWeather.setImageResource(R.drawable.ic_moon_cloud);
                                }
                                if(computableDate.getHour() <= sunRise.getSunSetHour()) {
                                    if(computableDate.getHour() == sunRise.getSunSetHour()) {
                                        if(computableDate.getMinute() < sunRise.getSunSetMinute()) {
                                            if(response.getLuminosity() > 100) {
                                                //cloud
                                                mIvWeather.setImageResource(R.drawable.ic_moon);
                                            }else {
                                                mIvWeather.setImageResource(R.drawable.ic_moon_cloud);
                                            }
                                        }else {
                                            //일출
                                            if(response.getLuminosity() >= 30000 ) {
                                                //sun
                                                mIvWeather.setImageResource(R.drawable.ic_sun);
                                            }else if(response.getLuminosity() < 30000 && response.getLuminosity() > 15000 ) {
                                                //sun_cloud
                                                mIvWeather.setImageResource(R.drawable.ic_sun_cloud);
                                            }else  {
                                                //cloud
                                                mIvWeather.setImageResource(R.drawable.ic_cloud);
                                            }
                                        }
                                    }
                                }else {
                                    // 해 뜬 오전
                                    if(response.getLuminosity() >= 30000 ) {
                                        //sun
                                        mIvWeather.setImageResource(R.drawable.ic_sun);
                                    }else if(response.getLuminosity() < 30000 && response.getLuminosity() > 15000 ) {
                                        //sun_cloud
                                        mIvWeather.setImageResource(R.drawable.ic_sun_cloud);
                                    }else  {
                                        //cloud
                                        mIvWeather.setImageResource(R.drawable.ic_cloud);
                                    }
                                }
                            }else {
                                //오후
                                if(computableDate.getHour() >= sunSet.getSunSetHour()) {
                                    //오후 6시 이후
                                    if (computableDate.getHour() == sunSet.getSunSetHour()) {
                                        if (computableDate.getMinute() <= sunSet.getSunSetMinute()) {
                                            if(response.getLuminosity() >= 30000 ) {
                                                //sun
                                                mIvWeather.setImageResource(R.drawable.ic_sun);
                                            }else if(response.getLuminosity() < 30000 && response.getLuminosity() > 15000 ) {
                                                //sun_cloud
                                                mIvWeather.setImageResource(R.drawable.ic_sun_cloud);
                                            }else  {
                                                //cloud
                                                mIvWeather.setImageResource(R.drawable.ic_cloud);
                                            }
                                        }else {
                                            if(response.getLuminosity() > 100) {
                                                //cloud
                                                mIvWeather.setImageResource(R.drawable.ic_moon);
                                            }else {
                                                mIvWeather.setImageResource(R.drawable.ic_moon_cloud);
                                            }
                                        }
                                    }
                                    if(response.getLuminosity() > 100) {
                                        //cloud
                                        mIvWeather.setImageResource(R.drawable.ic_moon);
                                    }else {
                                        mIvWeather.setImageResource(R.drawable.ic_moon_cloud);
                                    }
                                }else {
                                    if(response.getLuminosity() >= 30000 ) {
                                        //sun
                                        mIvWeather.setImageResource(R.drawable.ic_sun);
                                    }else if(response.getLuminosity() < 30000 && response.getLuminosity() > 15000 ) {
                                        //sun_cloud
                                        mIvWeather.setImageResource(R.drawable.ic_sun_cloud);
                                    }else  {
                                        //cloud
                                        mIvWeather.setImageResource(R.drawable.ic_cloud);
                                    }
                                }
                            }
                            if(computableDate.getMonth() <=4 || computableDate.getMonth() >=11) {
                                //겨울
                                if(response.getTemperature() < 0 ) {
                                    //영하
                                    mTvWeatherComment.setText(getResources().getString(R.string.midle_title8));
                                }else {
                                    //영상
                                    mTvWeatherComment.setText(getResources().getString(R.string.midle_title1));
                                }
                            }else if(computableDate.getMonth() < 6 || computableDate.getMonth() > 4 ) {
                                //봄
                                if(response.getTemperature() < 15 ) {
                                    mTvWeatherComment.setText(getResources().getString(R.string.midle_title1));
                                }else {
                                    mTvWeatherComment.setText(getResources().getString(R.string.midle_title4));
                                }
                            }else if (computableDate.getMonth() >= 6 || computableDate.getMonth() <= 9) {
                                //여름
                                if(response.getTemperature() < 30 ) {
                                    mTvWeatherComment.setText(getResources().getString(R.string.midle_title5));
                                }else {
                                    if(response.getHumidity() > 80 ) {
                                        mTvWeatherComment.setText(getResources().getString(R.string.midle_title7));
                                    }else {
                                        mTvWeatherComment.setText(getResources().getString(R.string.midle_title9));
                                    }
                                }
                            }else {
                                //가을
                                if(response.getTemperature() < 10) {
                                    mTvWeatherComment.setText(getResources().getString(R.string.midle_title1));
                                }else {
                                    mTvWeatherComment.setText(getResources().getString(R.string.midle_title3));
                                }
                            }
                        }
                        if(response.getRain()  < 5 && isRain && !isTimerFinish) {
                            mIvWeather.setImageResource(R.drawable.ic_rain);
                            mTvWeatherComment.setText(getResources().getString(R.string.midle_title6));
                        }
                        if(response.getVelocity() <= 0.1) {
                            mTvWindTitle.setText(getResources().getString(R.string.Wind));
                            mTvWindSubTitle.setText(getResources().getString(R.string.Explanation));
                            mRootView.setBackgroundResource(R.drawable.blue_skies);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
                            }
                            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
                        }else if(response.getVelocity() >= 0.11 && response.getVelocity() <= 1.5 ){
                            mTvWindTitle.setText(getResources().getString(R.string.Wind1));
                            mTvWindSubTitle.setText(getResources().getString(R.string.Explanation1));
                            mRootView.setBackgroundResource(R.drawable.blue_skies);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
                            }
                            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
                        } else if(response.getVelocity() >= 1.6 && response.getVelocity() <=3.3 ) {
                            mTvWindTitle.setText(getResources().getString(R.string.Wind2));
                            mTvWindSubTitle.setText(getResources().getString(R.string.Explanation2));
                            mRootView.setBackgroundResource(R.drawable.blue_skies);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
                            }
                            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
                        }else if(response.getVelocity() >= 3.4  && response.getVelocity() <= 5.4 ) {
                            mTvWindTitle.setText(getResources().getString(R.string.Wind3));
                            mRootView.setBackgroundResource(R.drawable.bg_lv1);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                getWindow().setNavigationBarColor(Color.parseColor("#52CC7D"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#F4D101"));//statusBar
                            }
                            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFF4D101));
                            mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                            mTvWindSubTitle.setText(getResources().getString(R.string.Explanation3));
                        }else if(response.getVelocity() >= 5.5  && response.getVelocity() <= 7.9 ) {
                            mTvWindTitle.setText(getResources().getString(R.string.Wind4));
                            mTvWindSubTitle.setText(getResources().getString(R.string.Explanation4));
                            mRootView.setBackgroundResource(R.drawable.bg_lv1);
                            mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                getWindow().setNavigationBarColor(Color.parseColor("#52CC7D"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#F4D101"));//statusBar
                            }
                            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFF4D101));
                        }else if(response.getVelocity() >= 8.0  && response.getVelocity() <= 10.7 ) {
                            mTvWindTitle.setText(getResources().getString(R.string.Wind5));
                            mTvWindSubTitle.setText(getResources().getString(R.string.Explanation5));
                            mRootView.setBackgroundResource(R.drawable.bg_lv2);
                            mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                getWindow().setNavigationBarColor(Color.parseColor("#A990D3"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#F7C0EA"));//statusBar
                            }
                            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFF7C0EA));//title
                        }else if(response.getVelocity() >= 10.8  && response.getVelocity() <= 13.8 ) {
                            mTvWindTitle.setText(getResources().getString(R.string.Wind6));
                            mTvWindSubTitle.setText(getResources().getString(R.string.Explanation6));
                            mRootView.setBackgroundResource(R.drawable.bg_lv2);
                            mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                getWindow().setNavigationBarColor(Color.parseColor("#A990D3"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#F7C0EA"));//statusBar
                            }
                            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFF7C0EA));//title
                        }else if(response.getVelocity() >= 13.9  && response.getVelocity() <= 17.1 ) {
                            mTvWindTitle.setText(getResources().getString(R.string.Wind7));
                            mTvWindSubTitle.setText(getResources().getString(R.string.Explanation7));
                            mRootView.setBackgroundResource(R.drawable.bg_lv2);
                            mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                getWindow().setNavigationBarColor(Color.parseColor("#A990D3"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#F7C0EA"));//statusBar
                            }
                            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFF7C0EA));//title
                        }else if(response.getVelocity() >= 17.2  && response.getVelocity() <= 20.7) {
                            mTvWindTitle.setText(getResources().getString(R.string.Wind8));
                            mTvWindSubTitle.setText(getResources().getString(R.string.Explanation8));
                            mRootView.setBackgroundResource(R.drawable.bg_lv3);
                            mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                getWindow().setNavigationBarColor(Color.parseColor("#FFAE98"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#FF0B45"));//statusBar
                            }
                            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFFF0B45));//title
                        }else if(response.getVelocity() >= 20.8  && response.getVelocity() <= 24.4) {
                            mTvWindTitle.setText(getResources().getString(R.string.Wind9));
                            mTvWindSubTitle.setText(getResources().getString(R.string.Explanation9));
                            mRootView.setBackgroundResource(R.drawable.bg_lv3);
                            mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                getWindow().setNavigationBarColor(Color.parseColor("#FFAE98"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#FF0B45"));//statusBar
                            }
                            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFFF0B45));//title
                        }else if(response.getVelocity() >= 24.5 && response.getVelocity() <= 28.4) {
                            mTvWindTitle.setText(getResources().getString(R.string.Wind10));
                            mTvWindSubTitle.setText(getResources().getString(R.string.Explanation10));
                            mRootView.setBackgroundResource(R.drawable.bg_lv3);
                            mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                getWindow().setNavigationBarColor(Color.parseColor("#FFAE98"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#FF0B45"));//statusBar
                            }
                            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFFF0B45));//title
                        }else if(response.getVelocity() >= 28.5 && response.getVelocity() <= 32.6) {
                            mTvWindTitle.setText(getResources().getString(R.string.Wind11));
                            mTvWindSubTitle.setText(getResources().getString(R.string.Explanation11));
                            mRootView.setBackgroundResource(R.drawable.bg_lv3);
                            mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                getWindow().setNavigationBarColor(Color.parseColor("#FFAE98"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#FF0B45"));//statusBar
                            }
                            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFFF0B45));//title
                        }else if(response.getVelocity() >= 32.7){
                            mTvWindTitle.setText(getResources().getString(R.string.Wind12));
                            mTvWindSubTitle.setText(getResources().getString(R.string.Explanation12));
                            mRootView.setBackgroundResource(R.drawable.bg_lv3);
                            mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                getWindow().setNavigationBarColor(Color.parseColor("#FFAE98"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#FF0B45"));//statusBar
                            }
                            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFFF0B45));//title
                        }
                        mTvWindVelocityValue.setText(String.valueOf(String.format("%.2f",response.getVelocity()) + "m/s"));
                        dataProgrssBar.setProgress((int)response.getVelocity());
                        dataProgrssBar.setDate(String.valueOf(String.format("%.2f",response.getVelocity())));
                        horizontalList.set(0,dataProgrssBar);
                        horizontalAdapter=new HorizontalAdapter(horizontalList);
                        horizontal_recycler_view.setAdapter(horizontalAdapter);
                    });
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
        super.onResponse(messageHeader, bodyBuf);
    }

    @Override
    protected void onPause() {
        super.onPause();
        envsensor.StreamingStopRequest request = envsensor.StreamingStopRequest.newBuilder().setAccessToken(mToken).setSerial(mEnvSensor.getSerial()).build();
        mCustomApplication.mServerSession.request(request);
    }

    @Override
    public void onConnected(RendezvousSession rendezvousSession, Connection connection) {
        Log.d("yot132","connected called");
        super.onConnected(rendezvousSession, connection);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // ACCESS_FINE_LOCATION 에 대한 권한 획득.
        } else {
            // ACCESS_FINE_LOCATION 에 대한 권한 거부.
        }
    }

    @Override
    public void onAttached() {
        envsensor.StreamingStopRequest request = envsensor.StreamingStopRequest.newBuilder().setAccessToken(mToken).setSerial(mEnvSensor.getSerial()).build();
        mCustomApplication.mServerSession.request(request);
        envsensor.StreamingStartRequest request1 = envsensor.StreamingStartRequest.newBuilder().setAccessToken(mToken).setSerial(mEnvSensor.getSerial()).build();
        mCustomApplication.mServerSession.request(request1);
        super.onAttached();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:{ //toolbar의 back키 눌렀을 때 동작
                finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(mSweetSheet.isShow()){
            mSweetSheet.dismiss();
        }else {
            super.onBackPressed();
        }
    }

    private void setupCustomView() {
        CustomDelegate customDelegate = new CustomDelegate(true,
                CustomDelegate.AnimationType.DuangLayoutAnimation);
        View view = LayoutInflater.from(this).inflate(R.layout.sunset_sunrise_view, null, false);
        customDelegate.setCustomView(view);
        mSweetSheet.setDelegate(customDelegate);
        final TextView tvTimeSunrise = view.findViewById(R.id.tvTime);
        final TextView tvTimeSunset = view.findViewById(R.id.tvTimeSunset);
        int tempSunsetHour = 0;
        if(sunSet.getSunSetHour() >= 13) {
            tempSunsetHour = sunSet.getSunSetHour().intValue() - 12;
        }else {
            tempSunsetHour = sunSet.getSunSetHour().intValue();
        }
        tvTimeSunrise.setText(getResources().getString(R.string.am) + " " + sunRise.getSunSetHour().intValue() + " : " + sunRise.getSunSetMinute().intValue());
        tvTimeSunset.setText(getResources().getString(R.string.pm) + " " + tempSunsetHour + " : " + sunSet.getSunSetMinute().intValue());
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String maxVelocity = intent.getStringExtra("maxVelocity");
            String maxVelocityTime = intent.getStringExtra("maxVelocityTime");
            String serial = intent.getStringExtra("serial");
            if(mEnvSensor.getSerial().equals(serial)) {
                runOnUiThread(() -> {
                    mTvMaxVelocity.setText(maxVelocity + "m/s");
                    mTvMaxVelocityTime.setText(maxVelocityTime);
                });
            }
        }
    };

    public void rotationCompass(float value) {
        RotateAnimation ra = new RotateAnimation(prevValue, value, Animation.RELATIVE_TO_SELF,0.5f, Animation.RELATIVE_TO_SELF,0.5f);
        ra.setDuration(250);
        ra.setFillAfter(true);
        mIvCompass.startAnimation(ra);
        prevValue = (int)value;
    }
}