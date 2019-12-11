package net.ijoon.ijoonweatherstation.util;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.snackbar.Snackbar;

import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;

public class CustomDialog extends Dialog implements SwipeRefreshLayout.OnRefreshListener {
    CustomApplication mCustomApplication;

    SwipeRefreshLayout mSwipeRefreshLayout;
    private Button mCloseBtn;
    private Activity mActivity;
    private ListView mListView;
    private ListViewAdapter mListViewAdapter;
    private TextView mTvDialogTitle;
    private RelativeLayout mRlRegister;
    private int mPosition;
    private String mId;

    public CustomDialog(Activity activity) {
        super(activity);
        mActivity = activity;
        mCustomApplication = (CustomApplication) activity.getApplicationContext();

        requestWindowFeature(Window.FEATURE_NO_TITLE);   //다이얼로그의 타이틀바를 없애주는 옵션
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));  //다이얼로그의 배경을 투명
        setContentView(R.layout.custom_dialog);     //다이얼로그에서 사용할 레이아웃
        mListView = findViewById(R.id.listView);
        mTvDialogTitle = findViewById(R.id.dialogTitle);
        mRlRegister = findViewById(R.id.rlRegister);
        mSwipeRefreshLayout = findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mListViewAdapter = new ListViewAdapter();
        mListView.setAdapter(mListViewAdapter);
        mRlRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar.make(mSwipeRefreshLayout, getContext().getResources().getString(R.string.textWorkingOn), Snackbar.LENGTH_LONG).setAction("Action", null).show();


//                Intent intent = new Intent(getContext(), SensorAddActivity.class);
//                getContext().startActivity(intent);
            }
        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mPosition = position;
                dismiss();
            }
        });

        mCloseBtn = findViewById(R.id.btnCancel);
        mCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

    }

    public void addItem(String title, boolean isOn, String owner) {
//        if (isOn) {
//            if (owner.equals(mId)) {
//                mListViewAdapter.addItem(R.drawable.ic_logo, title, true);
//            }else {
//                mListViewAdapter.addItem(R.drawable.ic_logo, title, true);
//            }
//        }else {
//            if (owner.equals(mId)) {
//                mListViewAdapter.addItem(R.drawable.ic_logo_off, title, false);
//            }else {
//                mListViewAdapter.addItem(R.drawable.ic_logo_off, title, false);
//            }
//        }

        mListViewAdapter.addItem(R.drawable.logo_weather, title, true);

    }

    public void clear() {
        mListViewAdapter.clear();
    }

    public void setDialogTitleCountText(int count) {
        mTvDialogTitle.setText("Sensor");
    }

    public int getPosition() {
        return mPosition;
    }

    public boolean isDialogShowing() {
        return isShowing();
    }

    @Override
    public void onRefresh() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
//                mCustomApplication.refreshStationList();
                try {
                    Thread.sleep(1400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
        thread.start();
    }
}