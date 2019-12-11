package net.ijoon.ijoonweatherstation;

import android.Manifest;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.protobuf.InvalidProtocolBufferException;
import com.skyfishjy.library.RippleBackground;

import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.util.ComputableDate;
import net.ijoon.scnet_android.Connection;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.RendezvousSession;
import net.ijoon.scnet_android.SCNetAppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ScrollingActivity extends SCNetAppCompatActivity implements  SwipeRefreshLayout.OnRefreshListener {

    //progressBar
    private int progressStatus = 0;
    private Handler handler = new Handler();
    private RecyclerView vertical_recycler_view, horizontal_recycler_view;
    HorizontalAdapter horizontalAdapter;
    public List<DataProgrssBar> horizontalList;
    private ChaosCompassView chaosCompassView;
    SwipeRefreshLayout mSwipeRefreshLayout;
    private boolean isSensorAvailable = false;
    private boolean isMenuItemClick = false;

    private int mCurrentPosition;
    CustomApplication mCustomApplication;
    //위치 권한 여부
    private static int REQUEST_ACCESS_FINE_LOCATION = 1000;
    TextView mTvWindDirection;
    TextView mTvTemperature;
    TextView mTvHumidity;//습기
    TextView mTvPressure;//압력
    TextView mTvWindVelocityValue;
    TextView mTvWindTitle;
    TextView mTvWindSubTitle;
    TextView mTvWeatherComment;
    TextView mTvVersion;
    RelativeLayout mRlTopTemp;
    ImageView mIvWeather;
    DataProgrssBar dataProgrssBar;
    RelativeLayout mRootView;
    RelativeLayout mRelativeWindDirection;
    Toolbar toolbar;
    RippleBackground rippleBackground;
    int screenWidth;
    int screenHeight;
    ComputableDate computableDate;
    envsensor.Sensor mEnvSensor;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSwipeRefreshLayout = findViewById(R.id.swipe_layout);
        //application
        mCustomApplication = (CustomApplication) getApplicationContext();
        mEnvSensor = mCustomApplication.getEnvSensor();

        Log.d("yot132","test = " + mEnvSensor.getId());
        //init view
        mTvWindDirection = findViewById(R.id.tvWindDirection);
        mTvTemperature = findViewById(R.id.tvTemp);
        mTvHumidity = findViewById(R.id.tvHumidityValue);//습기
        mTvPressure = findViewById(R.id.tvAtmValue);//압력
        mTvWindVelocityValue = findViewById(R.id.tvWindVelocityValue);
        mTvWindTitle = findViewById(R.id.tvWindTitle);
        mTvWindSubTitle = findViewById(R.id.tvWindSubTitle);
        mIvWeather = findViewById(R.id.ivWeather);
        mTvWeatherComment = findViewById(R.id.tvWeather);
        ImageView imageView = findViewById(R.id.centerImage);
        mRootView = findViewById(R.id.rlRoot);
        mRlTopTemp = findViewById(R.id.rlWind);
        rippleBackground = findViewById(R.id.content);
        mRelativeWindDirection = findViewById(R.id.rlMidWindDirection);
        chaosCompassView = (ChaosCompassView) findViewById(R.id.ccv);
        computableDate = new ComputableDate();
        mTvVersion = findViewById(R.id.tvVersion);
        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mRlTopTemp.getLayoutParams();
        params.height = screenHeight/4;
        params.width = screenWidth;
        mRlTopTemp.setLayoutParams(params);
        RelativeLayout.LayoutParams ivParams = (RelativeLayout.LayoutParams) mIvWeather.getLayoutParams();
        ivParams.height = screenHeight/7;
        ivParams.width = screenHeight/7;
        mIvWeather.setLayoutParams(ivParams);
        ViewGroup.LayoutParams compassViewParams = (ViewGroup.LayoutParams) chaosCompassView.getLayoutParams();
        compassViewParams.width = (screenWidth/3) + 50;
        chaosCompassView.setLayoutParams(compassViewParams);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(Html.fromHtml("<font color='#FFFFFF'>" + mEnvSensor.getName() + "</font>"));
        setSupportActionBar(toolbar);
        chaosCompassView.setVal(0);
        getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));

        //progressBar
        horizontal_recycler_view= findViewById(R.id.horizontal_recycler_view);
        horizontalList=new ArrayList<>();
        dataProgrssBar = new DataProgrssBar(0,"0");
        horizontalList.add(dataProgrssBar);
        horizontalAdapter=new HorizontalAdapter(horizontalList);
        LinearLayoutManager horizontalLayoutManagaer
                = new LinearLayoutManager(ScrollingActivity.this, LinearLayoutManager.HORIZONTAL, false);
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
        mTvVersion.setText("ver " + getAppVersionName());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);

        envsensor.StreamingStopRequest request = envsensor.StreamingStopRequest.newBuilder().setId(mEnvSensor.getId()).build();
        mCustomApplication.mServerSession.request(request);
        envsensor.StreamingStartRequest request1 = envsensor.StreamingStartRequest.newBuilder().setId(mEnvSensor.getId()).build();
        mCustomApplication.mServerSession.request(request1);
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
                txtView = (ProgressBar) view.findViewById(R.id.pb);
                x = (TextView) view.findViewById(R.id.TextViewID);
            }
        }
        public HorizontalAdapter(List<DataProgrssBar> horizontalList) {
            this.horizontalList = horizontalList;
        }
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.single_pb, parent, false);
            ProgressBar pb = itemView.findViewById(R.id.pb);
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
            Log.d("yot132", "packetType is null");
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
                        Log.d("yot132","envsensor.StreamingStartResponse.Result.success called");
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
                    runOnUiThread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void run() {
                            if(mSwipeRefreshLayout.isRefreshing()) {
                                mSwipeRefreshLayout.setRefreshing(false);
                            }
                                if (response.getDirection()<=15||response.getDirection()>=345){
                                    //북
                                    rippleBackground.setX(screenWidth/2);
                                    rippleBackground.setY(screenHeight);
                                    rippleBackground.startRippleAnimation();
                            }else if (response.getDirection()>15&&response.getDirection()<=75){
                                    //북동
                                    rippleBackground.setX(-screenWidth);
                                    rippleBackground.setY(screenHeight);
                                    rippleBackground.startRippleAnimation();
                            }else if (response.getDirection()>75&&response.getDirection()<=105){
                                    //동
                                    rippleBackground.setX(-screenWidth);
                                    rippleBackground.setY(screenHeight/2);
                                    rippleBackground.startRippleAnimation();
                            }else if (response.getDirection()>105&&response.getDirection()<=165){
                                    //남동
                                    rippleBackground.setX(-screenWidth);
                                    rippleBackground.setY(-rippleBackground.getHeight());
                                    rippleBackground.startRippleAnimation();
                            }else if (response.getDirection()>165&&response.getDirection()<=195){
                                    //남
                                    rippleBackground.setX(screenWidth/2);
                                    rippleBackground.setY(-rippleBackground.getHeight());
                                    rippleBackground.startRippleAnimation();
                            }else if (response.getDirection()>195&&response.getDirection()<=255){
                                    //남서
                                    rippleBackground.setX(screenWidth);
                                    rippleBackground.setY(-rippleBackground.getHeight());
                                    rippleBackground.startRippleAnimation();
                            }else if (response.getDirection()>255&&response.getDirection()<=285){
                                    //서
                                    rippleBackground.setX(screenWidth);
                                    rippleBackground.setY(screenHeight/2);
                                    rippleBackground.startRippleAnimation();
                            }else if (response.getDirection()>285&&response.getDirection()<345){
                                    //북서
                                    rippleBackground.setX(screenWidth);
                                    rippleBackground.setY(screenHeight);
                                    rippleBackground.startRippleAnimation();
                            }
                            chaosCompassView.setVal(response.getDirection());
                            mTvTemperature.setText(String.valueOf(String.format("%.2f",response.getTemperature()))+"℃");
                            mTvHumidity.setText(String.valueOf(response.getHumidity())+" %");//습기
                            mTvPressure.setText(String.valueOf(response.getPressure())+" hPa");//압력
                            if(response.getRain() > 5 ) {
                                //rain
                                mIvWeather.setImageResource(R.drawable.ic_rain);
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title6));
                            }else {
                                if(computableDate.getAMPM() != 1 ) {
                                    //오전
                                    if(computableDate.getHour() <= 6) {
                                        //오전 6시 이전
                                        if(response.getLuminosity() < 300) {
                                            mIvWeather.setImageResource(R.drawable.ic_moon_cloud);
                                        }else {
                                            mIvWeather.setImageResource(R.drawable.ic_moon);
                                        }
                                    }else {
                                        if(response.getLuminosity() >= 1800 ) {
                                            //sun
                                            mIvWeather.setImageResource(R.drawable.ic_sun);
                                        }else if(response.getLuminosity() < 300 ) {
                                            //cloud
                                            mIvWeather.setImageResource(R.drawable.ic_cloud);
                                        }else if(response.getLuminosity() < 1800 && 301 < response.getLuminosity())  {
                                            //sun_cloud
                                            mIvWeather.setImageResource(R.drawable.ic_sun_cloud);
                                        }
                                    }
                                }else {
                                    //오후
                                    if(computableDate.getHour() >= 18) {
                                        //오후 6시 이후
                                        if(response.getLuminosity() < 30) {
                                            mIvWeather.setImageResource(R.drawable.ic_cloud);
                                        }else if (response.getLuminosity() < 100 &&response.getLuminosity() > 31) {
                                            mIvWeather.setImageResource(R.drawable.ic_moon_cloud);
                                        }else {
                                            mIvWeather.setImageResource(R.drawable.ic_moon);
                                        }
                                    }else {
                                        if(response.getLuminosity() >= 30000 ) {
                                            //sun
                                            mIvWeather.setImageResource(R.drawable.ic_sun);
                                        }else if(response.getLuminosity() < 15000 ) {
                                            //cloud
                                            mIvWeather.setImageResource(R.drawable.ic_cloud);
                                        }else if(response.getLuminosity() < 30000 && 15000 < response.getLuminosity())  {
                                            //sun_cloud
                                            mIvWeather.setImageResource(R.drawable.ic_sun_cloud);
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
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title));
                            }
                            if(response.getVelocity() <= 0.1) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation));
                                mRootView.setBackgroundResource(R.drawable.blue_skies);
                                getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
//                                mAppBar.setBackgroundColor(Color.parseColor("#56CBF2"));
                            }else if(response.getVelocity() >= 0.11 && response.getVelocity() <= 1.5 ){
                                mTvWindTitle.setText(getResources().getString(R.string.Wind1));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation1));
                                mRootView.setBackgroundResource(R.drawable.blue_skies);
                                getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
//                                mAppBar.setBackgroundColor(Color.parseColor("#56CBF2"));
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
                            } else if(response.getVelocity() >= 1.6 && response.getVelocity() <=3.3 ) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind2));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation2));
                                mRootView.setBackgroundResource(R.drawable.blue_skies);
                                getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#56CBF2"));//statusBar
//                                mAppBar.setBackgroundColor(Color.parseColor("#56CBF2"));
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFF56CBF2));//title
                            }else if(response.getVelocity() >= 3.4  && response.getVelocity() <= 5.4 ) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind3));
                                mRootView.setBackgroundResource(R.drawable.bg_lv1);
                                getWindow().setNavigationBarColor(Color.parseColor("#52CC7D"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#F4D101"));//statusBar
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFF4D101));
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
//                                mAppBar.setBackgroundColor(Color.parseColor("#F4D101"));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation3));
                            }else if(response.getVelocity() >= 5.5  && response.getVelocity() <= 7.9 ) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind4));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation4));
                                mRootView.setBackgroundResource(R.drawable.bg_lv1);
//                                mAppBar.setBackgroundColor(Color.parseColor("#F4D101"));
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                getWindow().setNavigationBarColor(Color.parseColor("#52CC7D"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#F4D101"));//statusBar
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFF4D101));
                            }else if(response.getVelocity() >= 8.0  && response.getVelocity() <= 10.7 ) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind5));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation5));
                                mRootView.setBackgroundResource(R.drawable.bg_lv2);
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                getWindow().setNavigationBarColor(Color.parseColor("#A990D3"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#F7C0EA"));//statusBar
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFF7C0EA));//title
//                                mAppBar.setBackgroundColor(Color.parseColor("#F7C0EA"));
                            }else if(response.getVelocity() >= 10.8  && response.getVelocity() <= 13.8 ) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind6));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation6));
                                mRootView.setBackgroundResource(R.drawable.bg_lv2);
//                                mAppBar.setBackgroundColor(Color.parseColor("#F7C0EA"));
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                getWindow().setNavigationBarColor(Color.parseColor("#A990D3"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#F7C0EA"));//statusBar
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFF7C0EA));//title
                            }else if(response.getVelocity() >= 13.9  && response.getVelocity() <= 17.1 ) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind7));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation7));
                                mRootView.setBackgroundResource(R.drawable.bg_lv2);
//                                mAppBar.setBackgroundColor(Color.parseColor("#F7C0EA"));
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                getWindow().setNavigationBarColor(Color.parseColor("#A990D3"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#F7C0EA"));//statusBar
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFF7C0EA));//title
                            }else if(response.getVelocity() >= 17.2  && response.getVelocity() <= 20.7) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind8));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation8));
                                mRootView.setBackgroundResource(R.drawable.bg_lv3);
//                                mAppBar.setBackgroundColor(Color.parseColor("#FF0B45"));
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                getWindow().setNavigationBarColor(Color.parseColor("#FFAE98"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#FF0B45"));//statusBar
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFFF0B45));//title
                            }else if(response.getVelocity() >= 20.8  && response.getVelocity() <= 24.4) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind9));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation9));
                                mRootView.setBackgroundResource(R.drawable.bg_lv3);
//                                mAppBar.setBackgroundColor(Color.parseColor("#FF0B45"));
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                getWindow().setNavigationBarColor(Color.parseColor("#FFAE98"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#FF0B45"));//statusBar
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFFF0B45));//title
                            }else if(response.getVelocity() >= 24.5 && response.getVelocity() <= 28.4) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind10));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation10));
                                mRootView.setBackgroundResource(R.drawable.bg_lv3);
//                                mAppBar.setBackgroundColor(Color.parseColor("#FF0B45"));
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                getWindow().setNavigationBarColor(Color.parseColor("#FFAE98"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#FF0B45"));//statusBar
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFFF0B45));//title
                            }else if(response.getVelocity() >= 28.5 && response.getVelocity() <= 32.6) {
                                mTvWindTitle.setText(getResources().getString(R.string.Wind11));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation11));
                                mRootView.setBackgroundResource(R.drawable.bg_lv3);
//                                mAppBar.setBackgroundColor(Color.parseColor("#FF0B45"));
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                getWindow().setNavigationBarColor(Color.parseColor("#FFAE98"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#FF0B45"));//statusBar
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFFF0B45));//title
                            }else if(response.getVelocity() >= 32.7){
                                mTvWindTitle.setText(getResources().getString(R.string.Wind12));
                                mTvWindSubTitle.setText(getResources().getString(R.string.Explanation12));
                                mRootView.setBackgroundResource(R.drawable.bg_lv3);
//                                mAppBar.setBackgroundColor(Color.parseColor("#FF0B45"));
                                mTvWeatherComment.setText(getResources().getString(R.string.midle_title2));
                                getWindow().setNavigationBarColor(Color.parseColor("#FFAE98"));//bottom
                                getWindow().setStatusBarColor(Color.parseColor("#FF0B45"));//statusBar
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFFF0B45));//title
                            }
                            mTvWindVelocityValue.setText(String.valueOf(response.getVelocity()+"m/s"));
                            dataProgrssBar.setProgress((int)response.getVelocity());
                            dataProgrssBar.setDate(String.valueOf(response.getVelocity()));
                            horizontalList.set(0,dataProgrssBar);
                            horizontalAdapter=new HorizontalAdapter(horizontalList);
                            horizontal_recycler_view.setAdapter(horizontalAdapter);
                        }
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
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        envsensor.StreamingStopRequest request = envsensor.StreamingStopRequest.newBuilder().setId(mEnvSensor.getId()).build();
        mCustomApplication.mServerSession.request(request);
    }

    @Override
    public void onConnected(RendezvousSession rendezvousSession, Connection connection) {
        Log.d("yot132","connected called");
        super.onConnected(rendezvousSession, connection);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // grantResults[0] 거부 -> -1
        // grantResults[0] 허용 -> 0 (PackageManager.PERMISSION_GRANTED)

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // ACCESS_FINE_LOCATION 에 대한 권한 획득.
        } else {
            // ACCESS_FINE_LOCATION 에 대한 권한 거부.
        }
    }

    @Override
    public void onAttached() {
        //server request
        Log.d("yot132","on Attach called!!");
        envsensor.StreamingStopRequest request = envsensor.StreamingStopRequest.newBuilder().setId(mEnvSensor.getId()).build();
        mCustomApplication.mServerSession.request(request);
        envsensor.StreamingStartRequest request1 = envsensor.StreamingStartRequest.newBuilder().setId(mEnvSensor.getId()).build();
        mCustomApplication.mServerSession.request(request1);
        super.onAttached();
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
}