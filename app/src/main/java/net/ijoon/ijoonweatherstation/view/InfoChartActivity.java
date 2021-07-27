package net.ijoon.ijoonweatherstation.view;

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
import androidx.appcompat.app.ActionBar;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
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
import net.ijoon.ijoonweatherstation.util.ChartBase;
import net.ijoon.ijoonweatherstation.util.ComputableDate;
import net.ijoon.ijoonweatherstation.util.CustomDialog;
import net.ijoon.scnet_android.MessageHeader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InfoChartActivity extends ChartBase {

    private CombinedChart chart;
    private int INTERVAL = 60;
    CustomApplication mCustomApplication;
    String mToken;
    List<envsensor.Item> sensorVelocityList;
    List<envsensor.Item> yearSensorVelocityList;
    CustomDialog mCustomDialog;
    ComputableDate mComputableDate;
    List<envsensor.Sensor> mSensor;
    TextView mTvCalendal;
    ImageView mIvleft;
    ImageView mIvRight;
    ImageView mIvCalendar;
    int position = 0;
    ActionBar actionBar;
    int MAXPING = 15;
    int MAXVELOCITY = 10;
    CaldroidFragment caldroidFragment = new CaldroidFragment();
    RelativeLayout rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);
        mCustomApplication = (CustomApplication) getApplicationContext();
        actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + mCustomApplication.getEnvSensor().getName() + "</font>"));
        }
        mCustomDialog = new CustomDialog(this);
        mIvleft = findViewById(R.id.ivLeft);
        mIvRight = findViewById(R.id.ivRight);
        mTvCalendal = findViewById(R.id.tvCalendal);
        mIvCalendar = findViewById(R.id.ivCenter);
        rootView = findViewById(R.id.rootView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
        }
        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
        mToken = preferences.getString("token","");
        sensorVelocityList = new ArrayList<>();
        mComputableDate = new ComputableDate();
        mComputableDate.setDateFormat("yyyy-MM-dd");
        mTvCalendal.setText(mComputableDate.getCurrentTimeText());
        envsensor.StatisticalDataOfYearRequest yearRequest = envsensor.StatisticalDataOfYearRequest.newBuilder().setYear(mComputableDate.getYear()).setAccessToken(mToken)
                .setSerial(mCustomApplication.getEnvSensor().getSerial()).setIsIntervalDay(true).build();
        mCustomApplication.mServerSession.request(yearRequest);

        envsensor.StatisticalDataOfDayRequest request = envsensor.StatisticalDataOfDayRequest.newBuilder().setAccessToken(mToken).setDate(mComputableDate.getCurrentTimeText())
                .setSerial(mCustomApplication.getEnvSensor().getSerial()).setIntervalMin(INTERVAL).build();
        mCustomApplication.mServerSession.request(request);

        envsensor.GetSensorListRequest getSensorRequest = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
        mCustomApplication.mServerSession.request(getSensorRequest);

        mCustomDialog.setOnDismissListener(dialog -> {
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + mSensor.get(mCustomDialog.getPosition()).getName() + "</font>"));
            envsensor.StatisticalDataOfDayRequest request1 = envsensor.StatisticalDataOfDayRequest.newBuilder().setAccessToken(mToken).setDate(mComputableDate.getCurrentTimeText())
                    .setSerial(mSensor.get(mCustomDialog.getPosition()).getSerial()).setIntervalMin(INTERVAL).build();
            mCustomApplication.mServerSession.request(request1);
            position = mCustomDialog.getPosition();
        });

        mIvleft.setOnClickListener(v -> {
            mComputableDate.addDayOfMonth(-1);
            mTvCalendal.setText(mComputableDate.getCurrentTimeText());
            envsensor.StatisticalDataOfDayRequest requestLeft = envsensor.StatisticalDataOfDayRequest.newBuilder().setAccessToken(mToken).setDate(mComputableDate.getCurrentTimeText())
                    .setSerial(mSensor.get(position).getSerial()).setIntervalMin(INTERVAL).build();
            mCustomApplication.mServerSession.request(requestLeft);
        });

        mIvRight.setOnClickListener(v -> {
            mComputableDate.addDayOfMonth(1);
            mTvCalendal.setText(mComputableDate.getCurrentTimeText());
            envsensor.StatisticalDataOfDayRequest requestRight = envsensor.StatisticalDataOfDayRequest.newBuilder().setAccessToken(mToken).setDate(mComputableDate.getCurrentTimeText())
                    .setSerial(mSensor.get(position).getSerial()).setIntervalMin(INTERVAL).build();
            mCustomApplication.mServerSession.request(requestRight);
        });

        mIvCalendar.setOnClickListener(v -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            if(caldroidFragment.isHidden()) {
                fragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .show(caldroidFragment)
                        .commit();
            } else {
                updateCalendar(); // update when open calendar
                fragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .hide(caldroidFragment)
                        .commit();
            }
        });

        rootView.setOnClickListener(v -> {
            if(!caldroidFragment.isHidden()) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                updateCalendar(); // update when open calendar
                fragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .hide(caldroidFragment)
                        .commit();
            }
        });
        calendarSetting();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_list: {
                mCustomDialog.show();
                //sensor list call
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

    private LineData generateLineData() {
        LineData d = new LineData();
        ArrayList<Entry> entries = new ArrayList<>();
        for (int index = 0; index < sensorVelocityList.size(); index++)
            entries.add(new Entry(index + 0.5f, sensorVelocityList.get(index).getAvgVelocity()));

        LineDataSet set = new LineDataSet(entries, getResources().getString(R.string.textAverageVelocity));
        set.setColor(Color.rgb(210, 198, 90));
        set.setLineWidth(2.5f);
        set.setCircleColor(Color.rgb(210, 198, 90));
        set.setCircleRadius(5f);
        set.setFillColor(Color.rgb(210, 198, 90));
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(true);
        set.setValueTextSize(10f);
        set.setValueTextColor(Color.rgb(0, 0, 0));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        d.addDataSet(set);

        return d;
    }

    private BarData generateBarData() {

        ArrayList<BarEntry> entries1 = new ArrayList<>();
        ArrayList<BarEntry> entries2 = new ArrayList<>();

        for (int index = 0; index < sensorVelocityList.size(); index++) {
            entries1.add(new BarEntry(0, sensorVelocityList.get(index).getMaxVelocity()));

            // stacked
            entries2.add(new BarEntry(0, 0, 0));
        }

        BarDataSet set1 = new BarDataSet(entries1, getResources().getString(R.string.textMaxVelocity));
        set1.setColor(Color.rgb(23, 197, 255));
        set1.setValueTextColor(Color.rgb(61, 165, 255));
        set1.setValueTextSize(10f);
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);

        BarDataSet set2 = new BarDataSet(entries2, "");
        set2.setColor(Color.rgb(255, 255, 255));
        set2.setValueTextColor(Color.rgb(255, 255, 255));
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);

        float groupSpace = 0.06f;
        float barSpace = 0.02f; // x2 dataset
        float barWidth = 0.45f; // x2 dataset
        // (0.45 + 0.02) * 2 + 0.06 = 1.00 -> interval per "group"

        BarData d = new BarData(set1,set2);

        d.setBarWidth(barWidth);

        // make this BarData object grouped
        d.groupBars(0, groupSpace, barSpace); // start at x = 0

        return d;
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
                    for(int i = 0 ; i < response.getSensorCount() ; i ++) {
                        if(response.getSensor(i).getPing() > MAXPING) {
                            mCustomDialog.addItem(response.getSensor(i).getName(),false,"");
                        }else {
                            mCustomDialog.addItem(response.getSensor(i).getName(),true,"");
                        }

                        mSensor.add(response.getSensor(i));
                    }
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }

                break;
            }
            case 28: {
                //StatisticalDataOfDayResponse

                try {
                    final envsensor.StatisticalDataOfDayResponse response = envsensor.StatisticalDataOfDayResponse.parseFrom(bodyBuf);

                    sensorVelocityList = response.getItemsList();

                    runOnUiThread(() -> {
                        chart = findViewById(R.id.chart1);
                        chart.getDescription().setEnabled(false);
                        chart.setBackgroundColor(Color.WHITE);
                        chart.setDrawGridBackground(false);
                        chart.setDrawBarShadow(false);
                        chart.setHighlightFullBarEnabled(false);

                        // draw bars behind lines
                        chart.setDrawOrder(new CombinedChart.DrawOrder[]{
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
                        xAxis.setValueFormatter(new ValueFormatter() {
                            @Override
                            public String getFormattedValue(float value) {
                                return time[(int) value % time.length];
                            }
                        });

                        CombinedData data = new CombinedData();

                        data.setData(generateLineData());
                        data.setData(generateBarData());
                        data.setValueTypeface(null);

                        xAxis.setAxisMaximum(data.getXMax() + 0.25f);

                        chart.setData(data);
                        chart.invalidate();
                    });

                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                break;
            }
            case 30: {
                //StatisticalDataOfYearResponse

                try {
                    final envsensor.StatisticalDataOfYearResponse response = envsensor.StatisticalDataOfYearResponse.parseFrom(bodyBuf);
                    yearSensorVelocityList = response.getItemsList();
                    updateCalendar();
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
    public void saveToGallery() { /* Intentionally left empty */ }

    private void updateCalendar() {
        for(int i = 0 ; i < yearSensorVelocityList.size() ; i ++) {
            if(yearSensorVelocityList.get(i).getMaxVelocity() >= MAXVELOCITY) {
                caldroidFragment.setBackgroundDrawableForDate(ResourcesCompat.getDrawable(getResources(), R.drawable.calendar_danger, null), mComputableDate.setDate(yearSensorVelocityList.get(i).getUtc()).getDate());
            }else {
                caldroidFragment.setBackgroundDrawableForDate(ResourcesCompat.getDrawable(getResources(), R.drawable.calendar_normal, null), mComputableDate.setDate(yearSensorVelocityList.get(i).getUtc()).getDate());
            }
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
                mTvCalendal.setText(mComputableDate.getCurrentTimeText());
                envsensor.StatisticalDataOfDayRequest requestLeft = envsensor.StatisticalDataOfDayRequest.newBuilder().setAccessToken(mToken).setDate(mComputableDate.getCurrentTimeText())
                        .setSerial(mSensor.get(position).getSerial()).setIntervalMin(INTERVAL).build();
                mCustomApplication.mServerSession.request(requestLeft);
            }
        });
    }
}