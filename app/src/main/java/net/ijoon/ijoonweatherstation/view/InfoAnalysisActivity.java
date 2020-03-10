package net.ijoon.ijoonweatherstation.view;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.protobuf.InvalidProtocolBufferException;
import net.ijoon.envsensor;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import net.ijoon.ijoonweatherstation.util.ChartAnalysisBase;
import net.ijoon.ijoonweatherstation.util.ComputableDate;
import net.ijoon.ijoonweatherstation.util.CustomDialog;
import net.ijoon.scnet_android.MessageHeader;
import java.util.ArrayList;
import java.util.List;

public class InfoAnalysisActivity extends ChartAnalysisBase {

    CustomApplication mCustomApplication;
    ActionBar actionBar;
    String mToken;
    ComputableDate mComputableDate;
    private CombinedChart chart;
    List<envsensor.Item> sensorVelocityList;
    List<envsensor.Sensor> mSensor;
    TextView mTvCalendal;
    ImageView mIvleft;
    ImageView mIvRight;
    int position = 0;
    CustomDialog mCustomDialog;
    int MAXPING = 15;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);
        mCustomApplication = (CustomApplication) getApplicationContext();
        sensorVelocityList = new ArrayList<>();
        mComputableDate = new ComputableDate();
        mCustomDialog = new CustomDialog(this);
        mIvleft = findViewById(R.id.ivLeft);
        mIvRight = findViewById(R.id.ivRight);
        mTvCalendal = findViewById(R.id.tvCalendal);
        mComputableDate.setDateFormat("yyyy");
        mTvCalendal.setText(mComputableDate.getCurrentTimeText());

        actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + mCustomApplication.getEnvSensor().getName() + "</font>"));
        }
        getWindow().setNavigationBarColor(Color.parseColor("#3286EE"));//bottom
        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
        mToken = preferences.getString("token","");
        envsensor.StatisticalDataOfYearRequest yearRequest = envsensor.StatisticalDataOfYearRequest.newBuilder().setYear(mComputableDate.getYear()).setAccessToken(mToken)
                .setSerial(mCustomApplication.getEnvSensor().getSerial()).setIsIntervalDay(true).build();
        mCustomApplication.mServerSession.request(yearRequest);

        envsensor.GetSensorListRequest getSensorRequest = envsensor.GetSensorListRequest.newBuilder().setAccessToken(mToken).build();
        mCustomApplication.mServerSession.request(getSensorRequest);

        mCustomDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mComputableDate.setDateFormat("yyyy");
                actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>" + mSensor.get(mCustomDialog.getPosition()).getName() + "</font>"));
                envsensor.StatisticalDataOfYearRequest request = envsensor.StatisticalDataOfYearRequest.newBuilder().setYear(mComputableDate.getYear()).setAccessToken(mToken)
                        .setSerial(mSensor.get(mCustomDialog.getPosition()).getSerial()).setIsIntervalDay(true).build();
                mCustomApplication.mServerSession.request(request);
                position = mCustomDialog.getPosition();
            }
        });

        mIvleft.setOnClickListener(v -> {
            mComputableDate.addYear(-1);
            mComputableDate.setDateFormat("yyyy");
            mTvCalendal.setText(mComputableDate.getCurrentTimeText());
            envsensor.StatisticalDataOfYearRequest request = envsensor.StatisticalDataOfYearRequest.newBuilder().setYear(mComputableDate.getYear()).setAccessToken(mToken)
                    .setSerial(mSensor.get(mCustomDialog.getPosition()).getSerial()).setIsIntervalDay(true).build();
            mCustomApplication.mServerSession.request(request);
        });

        mIvRight.setOnClickListener(v -> {
            mComputableDate.addYear(1);
            mComputableDate.setDateFormat("yyyy");
            mTvCalendal.setText(mComputableDate.getCurrentTimeText());
            envsensor.StatisticalDataOfYearRequest request = envsensor.StatisticalDataOfYearRequest.newBuilder().setYear(mComputableDate.getYear()).setAccessToken(mToken)
                    .setSerial(mSensor.get(mCustomDialog.getPosition()).getSerial()).setIsIntervalDay(true).build();
            mCustomApplication.mServerSession.request(request);
        });
    }

    @Override
    protected void saveToGallery() {

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
            case 30: {
                //StatisticalDataOfYearResponse

                try {
                    final envsensor.StatisticalDataOfYearResponse response = envsensor.StatisticalDataOfYearResponse.parseFrom(bodyBuf);
                    sensorVelocityList = response.getItemsList();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
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
    }


    private LineData generateLineData() {

        LineData d = new LineData();

        ArrayList<Entry> entries = new ArrayList<>();
        for (int index = 0; index < sensorVelocityList.size(); index++)
            entries.add(new Entry(index, sensorVelocityList.get(index).getAvgVelocity()));

        LineDataSet set = new LineDataSet(entries, "일별 평균 풍속");
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
}
