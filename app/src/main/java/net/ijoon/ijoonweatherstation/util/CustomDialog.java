package net.ijoon.ijoonweatherstation.util;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.ijoonweatherstation.R;
import java.util.ArrayList;

public class CustomDialog extends Dialog implements SwipeRefreshLayout.OnRefreshListener {
    
    CustomApplication mCustomApplication;
    SwipeRefreshLayout mSwipeRefreshLayout;
    private Button mCloseBtn;
    private Activity mActivity;
    private ListView mListView;
    private ListViewAdapter mListViewAdapter;
    private TextView mTvDialogTitle;
    private int mPosition;
    ArrayList<String> arrayList;

    public CustomDialog(Activity activity) {
        super(activity);
        mActivity = activity;
        arrayList = new ArrayList();
        mCustomApplication = (CustomApplication) activity.getApplicationContext();
        this.setCancelable(false);
        requestWindowFeature(Window.FEATURE_NO_TITLE);   //다이얼로그의 타이틀바를 없애주는 옵션
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));  //다이얼로그의 배경을 투명
        setContentView(R.layout.custom_dialog);     //다이얼로그에서 사용할 레이아웃
        mListView = findViewById(R.id.listView);
        mTvDialogTitle = findViewById(R.id.dialogTitle);
        mSwipeRefreshLayout = findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mListViewAdapter = new ListViewAdapter();
        mListView.setAdapter(mListViewAdapter);
        mListView.setOnItemClickListener((parent, view, position, id) -> {
            mPosition = position;
            dismiss();
        });

        mCloseBtn = findViewById(R.id.btnCancel);
        mCloseBtn.setOnClickListener(view -> {
            for(int i = 0 ; i < arrayList.size() ; i++) {
                if (arrayList.get(i).equals(mCustomApplication.getEnvSensor().getName())) {
                    mPosition = i;
                }
            }
            dismiss();
        });
    }

    public void addItem(String title, boolean isOn, String owner) {
        arrayList.add(title);
        if (isOn) {
            mListViewAdapter.addItem(R.drawable.logo_weather, title, true);
        }else {
            mListViewAdapter.addItem(R.drawable.logo_weather_gray, title, false);
        }
        mActivity.runOnUiThread(() -> mListViewAdapter.notifyDataSetChanged());
    }

    public void clear() {
        mListViewAdapter.clear();
    }

    public void setDialogTitleCountText(int count) {
        mTvDialogTitle.setText("센서");
    }

    public int getPosition() {
        return mPosition;
    }

    public boolean isDialogShowing() {
        return isShowing();
    }

    @Override
    public void onRefresh() {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(1400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mActivity.runOnUiThread(() -> mSwipeRefreshLayout.setRefreshing(false));
        });
        thread.start();
    }
}