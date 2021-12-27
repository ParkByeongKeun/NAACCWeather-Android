package net.ijoon.ijoonweatherstation.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.protobuf.InvalidProtocolBufferException;
import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidListener;
import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.util.ChartAnalysisBase;
import net.ijoon.ijoonweatherstation.util.ComputableDate;
import net.ijoon.ijoonweatherstation.util.CustomDialog;
import net.ijoon.scnet_android.MessageHeader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InfoAnalysisActivity extends ChartAnalysisBase {

    CustomApplication mCustomApplication;
    ActionBar actionBar;
    String mToken;
    ComputableDate mComputableDate;
    ComputableDate mCalendarComputableDate;
    private CombinedChart chart;
    List<envsensor.Item> sensorVelocityList;
    List<envsensor.Sensor> mSensor;
    TextView mTvCalendal;
    ImageView mIvleft;
    ImageView mIvRight;
    CustomDialog mCustomDialog;
    int MAXPING = 15;
    ImageView mIvCalendar;
    int MAXVELOCITY = 10;
    CaldroidFragment caldroidFragment = new CaldroidFragment();
    RelativeLayout rootView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);
        brManager();
        mCustomApplication = (CustomApplication) getApplicationContext();
        sensorVelocityList = new ArrayList<>();
        mComputableDate = new ComputableDate();
        mCalendarComputableDate = new ComputableDate();
        mCustomDialog = new CustomDialog(this);
        mIvleft = findViewById(R.id.ivLeft);
        mIvRight = findViewById(R.id.ivRight);
        mTvCalendal = findViewById(R.id.tvCalendal);
        mIvCalendar = findViewById(R.id.ivCenter);
        rootView = findViewById(R.id.rootView);
        mComputableDate.setDateFormat("yyyy");
        mTvCalendal.setText(mComputableDate.getCurrentTimeText());
        actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + mCustomApplication.getEnvSensor().getName() + "</font>"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
        }
        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
        mToken = preferences.getString("token","");
        envsensor.StatisticalDataOfYearRequest yearRequest = envsensor.StatisticalDataOfYearRequest.newBuilder().setYear(mComputableDate.getYear()).setAccessToken(mToken)
                .setSerial(mCustomApplication.getEnvSensor().getSerial()).setIsIntervalDay(true).build();
        mCustomApplication.mServerSession.request(yearRequest);
        envsensor.GetSensorListRequest getSensorRequest = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
        mCustomApplication.mServerSession.request(getSensorRequest);
        mCustomDialog.setOnDismissListener(dialog -> {
            clearCalendar();
            mComputableDate.setDateFormat("yyyy");
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + mSensor.get(mCustomDialog.getPosition()).getName() + "</font>"));
            envsensor.StatisticalDataOfYearRequest request = envsensor.StatisticalDataOfYearRequest.newBuilder().setYear(mComputableDate.getYear()).setAccessToken(mToken)
                    .setSerial(mSensor.get(mCustomDialog.getPosition()).getSerial()).setIsIntervalDay(true).build();
            mCustomApplication.mServerSession.request(request);
            mCustomApplication.setEnvSensor(mSensor.get(mCustomDialog.getPosition()));
        });
        mIvleft.setOnClickListener(v -> {
            mComputableDate.addYear(-1);
            mComputableDate.setDateFormat("yyyy");
            mTvCalendal.setText(mComputableDate.getCurrentTimeText());
            envsensor.StatisticalDataOfYearRequest request = envsensor.StatisticalDataOfYearRequest.newBuilder().setYear(mComputableDate.getYear()).setAccessToken(mToken)
                    .setSerial(mCustomApplication.getEnvSensor().getSerial()).setIsIntervalDay(true).build();
            mCustomApplication.mServerSession.request(request);
        });
        mIvRight.setOnClickListener(v -> {
            mComputableDate.addYear(1);
            mComputableDate.setDateFormat("yyyy");
            mTvCalendal.setText(mComputableDate.getCurrentTimeText());
            envsensor.StatisticalDataOfYearRequest request = envsensor.StatisticalDataOfYearRequest.newBuilder().setYear(mComputableDate.getYear()).setAccessToken(mToken)
                    .setSerial(mCustomApplication.getEnvSensor().getSerial()).setIsIntervalDay(true).build();
            mCustomApplication.mServerSession.request(request);
        });
        mIvCalendar.setOnClickListener(v -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            if(caldroidFragment.isHidden()) {
                fragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .show(caldroidFragment)
                        .commit();
            } else {
                runOnUiThread(() -> {
                    updateCalendar(); // update when open calendar
                });

                fragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .hide(caldroidFragment)
                        .commit();
            }
        });
        rootView.setOnClickListener(v -> {
            if(!caldroidFragment.isHidden()) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                runOnUiThread(() -> {
                    updateCalendar(); // update when open calendar
                });
                fragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .hide(caldroidFragment)
                        .commit();
            }
        });
        calendarSetting();
    }

    @Override
    protected void saveToGallery() {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_list: {
                if(!caldroidFragment.isHidden()) {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    runOnUiThread(() -> {
                        updateCalendar(); // update when open calendar
                    });
                    fragmentManager.beginTransaction()
                            .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                            .hide(caldroidFragment)
                            .commit();
                }
                mCustomDialog.show();//sensor list call
                return true;
            }
            default: {
                finish();
                return true;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_list,menu);
        return true;
    }

    @Override
    public void onResponse(MessageHeader messageHeader, final byte[] bodyBuf) {
        super.onResponse(messageHeader, bodyBuf);
        if (messageHeader == null) {
            Log.d("yot132", "packetType is null");
            return;
        }
        switch (messageHeader.mPacketType) {
            case 6: {
                try {
                    final envsensor.GetSensorListResponse response = envsensor.GetSensorListResponse.parseFrom(bodyBuf);
                    mSensor = new ArrayList<>();
                    ArrayList<envsensor.Sensor> tempArr1 = new ArrayList<>();
                    ArrayList<envsensor.Sensor> tempArr2 = new ArrayList<>();
                    for(int i = 0 ; i < response.getSensorCount() ; i ++) {
                        if(response.getSensor(i).getPing() < MAXPING) {
                            tempArr1.add(response.getSensor(i));
                        }else {
                            tempArr2.add(response.getSensor(i));
                        }
                    }
                    mSensor.addAll(tempArr1);
                    mSensor.addAll(tempArr2);
                    for(int i = 0 ; i < mSensor.size() ; i++) {
                        if(mSensor.get(i).getPing() > MAXPING) {
                            mCustomDialog.addItem(mSensor.get(i).getName(),false,"");
                        }else {
                            mCustomDialog.addItem(mSensor.get(i).getName(),true,"");
                        }
                    }
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                break;
            }
            case 30: {//StatisticalDataOfYearResponse
                try {
                    final envsensor.StatisticalDataOfYearResponse response = envsensor.StatisticalDataOfYearResponse.parseFrom(bodyBuf);
                    sensorVelocityList = response.getItemsList();
                    runOnUiThread(() -> {
                        chart = findViewById(R.id.chart1);
                        chart.getDescription().setEnabled(false);
                        chart.setBackgroundColor(Color.WHITE);
                        chart.setDrawGridBackground(false);
                        chart.setDrawBarShadow(false);
                        chart.setHighlightFullBarEnabled(false);
                        chart.setDrawOrder(new CombinedChart.DrawOrder[]{// draw bars behind lines
                                CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.BUBBLE, CombinedChart.DrawOrder.CANDLE, CombinedChart.DrawOrder.LINE, CombinedChart.DrawOrder.SCATTER
                        });
                        Legend l = chart.getLegend();
                        l.setWordWrapEnabled(true);
                        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
                        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
                        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
                        l.setDrawInside(false);
                        YAxis rightAxis = chart.getAxisRight();
                        rightAxis.setDrawGridLines(false);
                        rightAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)
                        YAxis leftAxis = chart.getAxisLeft();
                        leftAxis.setDrawGridLines(false);
                        leftAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)
                        XAxis xAxis = chart.getXAxis();
                        xAxis.setPosition(XAxis.XAxisPosition.BOTH_SIDED);
                        xAxis.setAxisMinimum(0f);
                        xAxis.setGranularity(1f);
                        for(int i = 0 ; i < sensorVelocityList.size() ; i ++) {
                            mComputableDate.setDateFormat("MM월dd일");
                            mComputableDate.setDate(sensorVelocityList.get(i).getUtc());
                            time[i] = mComputableDate.getCurrentTimeText();

                        }
                        xAxis.setValueFormatter(new ValueFormatter() {
                            @Override
                            public String getFormattedValue(float value) {
                                return time[(int) value % time.length];
                            }
                        });
                        CombinedData data = new CombinedData();
                        data.setData(generateLineData());
                        data.setValueTypeface(null);
                        xAxis.setAxisMaximum(data.getXMax() + 0.25f);
                        chart.setData(data);
                        chart.invalidate();
                    });
                    runOnUiThread(() -> {
                        updateCalendar(); // update when open calendar
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
    }

    private LineData generateLineData() {
        LineData d = new LineData();
        ArrayList<Entry> entries = new ArrayList<>();
        for (int index = 0; index < sensorVelocityList.size(); index++)
            entries.add(new Entry(index, sensorVelocityList.get(index).getAvgVelocity()));
        LineDataSet set = new LineDataSet(entries, getResources().getString(R.string.textAverageDailyVelocity));
        set.setColor(Color.rgb(170, 198, 30));
        set.setLineWidth(2.5f);
        set.setCircleColor(Color.rgb(170, 198, 30));
        set.setCircleRadius(5f);
        set.setFillColor(Color.rgb(170, 198, 30));
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(true);
        set.setValueTextSize(10f);
        set.setValueTextColor(Color.rgb(0, 0, 0));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        d.addDataSet(set);
        return d;
    }

    private void updateCalendar() {
        for(int i = 0 ; i < sensorVelocityList.size() ; i ++) {
            if(sensorVelocityList.get(i).getMaxVelocity() >= MAXVELOCITY) {
                caldroidFragment.setBackgroundDrawableForDate(ResourcesCompat.getDrawable(getResources(), R.drawable.calendar_danger, null), mCalendarComputableDate.setDate(sensorVelocityList.get(i).getUtc()).getDate());
            }else {
                caldroidFragment.setBackgroundDrawableForDate(ResourcesCompat.getDrawable(getResources(), R.drawable.calendar_normal, null), mCalendarComputableDate.setDate(sensorVelocityList.get(i).getUtc()).getDate());
            }
        }
        caldroidFragment.refreshView();
    }

    private void clearCalendar() {
        for(int i = 0 ; i < sensorVelocityList.size() ; i ++) {
            caldroidFragment.setBackgroundDrawableForDate(ResourcesCompat.getDrawable(getResources(), R.drawable.calendar_disable, null), mCalendarComputableDate.setDate(sensorVelocityList.get(i).getUtc()).getDate());
        }
        caldroidFragment.refreshView();
    }

    private void calendarSetting() {
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.add(R.id.flCalendar, caldroidFragment);
        t.hide(caldroidFragment);
        t.commit();
        caldroidFragment.setCaldroidListener(new CaldroidListener() {
            @Override
            public void onSelectDate(Date date, View view) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .hide(caldroidFragment)
                        .commit();
                mComputableDate.setDate(date);
            }
        });
    }

    public void brManager() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("delete");
        registerReceiver(mBroadcastReceiver, filter);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
            preferences.edit().clear().apply();
            preferences.edit().putString("id","");
            preferences.edit().putString("token","");
            preferences.edit().apply();
            InfoAnalysisActivity.super.activityFinish();
            Intent intent1 = new Intent(InfoAnalysisActivity.this, DeleteActivity.class);
            startActivity(intent1);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }
}