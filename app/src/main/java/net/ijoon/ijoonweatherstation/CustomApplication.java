package net.ijoon.ijoonweatherstation;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.protobuf.MessageLite;
import com.kakao.sdk.common.KakaoSdk;

import net.ijoon.envsensor;
import net.ijoon.scnet_android.Connection;
import net.ijoon.scnet_android.GlobalApplication;
import net.ijoon.scnet_android.Registry;
import net.ijoon.scnet_android.RendezvousSession;
import net.ijoon.scnet_android.ServerSession;

import java.util.Stack;
import java.util.UUID;

public class CustomApplication extends GlobalApplication {

        private final static String mServerIp = "1.246.219.189";
//    private final static String mServerIp = "192.168.0.30";
    private final static int mServerPort = 19193;
    MyActivityLifecycleCallbacks mMyActivityLifecycleCallbacks;
    public ServerSession mServerSession;
    private envsensor.Sensor mEnvSensor;

    public envsensor.Sensor getEnvSensor() {
        return mEnvSensor;
    }

    public void setEnvSensor(envsensor.Sensor sensor) {
        mEnvSensor = sensor;
    }
    public String mToken;
    public String fcmToken;



    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreate() {
        KakaoSdk.init(this, "6ddc904dfe7ad11e585a1a70995003bc");

        Registry.registMessage("StreamingStartRequest", envsensor.PacketType.streamingStartReq.getNumber());
        Registry.registMessage("StreamingStartResponse", envsensor.PacketType.streamingStartRes.getNumber());
        Registry.registMessage("StreamingStopRequest", envsensor.PacketType.streamingStopReq.getNumber());
        Registry.registMessage("StreamingStopResponse", envsensor.PacketType.streamingStopRes.getNumber());
        Registry.registMessage("EnvSensorData", envsensor.PacketType.envSensorData.getNumber());
        Registry.registMessage("GetSensorListRequest", envsensor.PacketType.getSensorListReq.getNumber());
        Registry.registMessage("GetSensorListResponse", envsensor.PacketType.getSensorListRes.getNumber());
        Registry.registMessage("SetSensorPositionRequest", envsensor.PacketType.setSensorPositionReq.getNumber());
        Registry.registMessage("SetSensorPositionResponse", envsensor.PacketType.setSensorPositionRes.getNumber());
        Registry.registMessage("RegisterSensor", envsensor.PacketType.registerSensor.getNumber());
        Registry.registMessage("SetSensorNameRequest", envsensor.PacketType.setSensorNameReq.getNumber());
        Registry.registMessage("SetSensorNameResponse", envsensor.PacketType.setSensorNameRes.getNumber());

        Registry.registMessage("ShareSensorRequest", envsensor.PacketType.shareSensorReq.getNumber());
        Registry.registMessage("ShareSensorResponse", envsensor.PacketType.shareSensorRes.getNumber());
        Registry.registMessage("ShareSensorCancelRequest", envsensor.PacketType.shareSensorCancelReq.getNumber());
        Registry.registMessage("ShareSensorCancelResponse", envsensor.PacketType.shareSensorCancelRes.getNumber());
        Registry.registMessage("SharedUsersRequest", envsensor.PacketType.sharedUsersReq.getNumber());
        Registry.registMessage("SharedUsersResponse", envsensor.PacketType.sharedUsersRes.getNumber());
        Registry.registMessage("HandoverSensorRequest", envsensor.PacketType.handoverSensorReq.getNumber());
        Registry.registMessage("HandoverSensorResponse", envsensor.PacketType.handoverSensorRes.getNumber());
        Registry.registMessage("SetDisclosureOptionRequest", envsensor.PacketType.setDisclosureOptionReq.getNumber());
        Registry.registMessage("SetDisclosureOptionResponse", envsensor.PacketType.setDisclosureOptionRes.getNumber());
        Registry.registMessage("RegistSensorRequest", envsensor.PacketType.registSensorReq.getNumber());
        Registry.registMessage("RegistSensorResponse", envsensor.PacketType.registSensorRes.getNumber());
        Registry.registMessage("UnregistSensorRequest", envsensor.PacketType.unregistSensorReq.getNumber());
        Registry.registMessage("UnregistSensorResponse", envsensor.PacketType.unregistSensorRes.getNumber());
        Registry.registMessage("Ping", envsensor.PacketType.ping.getNumber());

        Registry.registMessage("StatisticalDataOfDayRequest", envsensor.PacketType.statisticalDataOfDayRequest.getNumber());
        Registry.registMessage("StatisticalDataOfDayResponse", envsensor.PacketType.statisticalDataOfDayResponse.getNumber());
        Registry.registMessage("StatisticalDataOfYearRequest", envsensor.PacketType.statisticalDataOfYearRequest.getNumber());
        Registry.registMessage("StatisticalDataOfYearResponse", envsensor.PacketType.statisticalDataOfYearResponse.getNumber());

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("yot132", "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();

                        // Log and toast
                        fcmToken = token;
                        Log.d("yot132","fcmToken = " + fcmToken);
                    }
                });
        SharedPreferences preferences = getSharedPreferences("preferences",MODE_PRIVATE);
        mToken = preferences.getString("token","");
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);

        this.mMyActivityLifecycleCallbacks = new MyActivityLifecycleCallbacks();
        registerActivityLifecycleCallbacks(mMyActivityLifecycleCallbacks);

//        setMainServerInfo("192.168.100.1", 9190, 1000); // 반드시 super.onCreate(); 이전에 호출해야 함


        super.onCreate();
    }

    public void startServer() {
        mServerSession = new ServerSession(mServerIp, mServerPort, 1000, CustomApplication.this);
        Log.d("yot132","server session start called");
        mServerSession.start();
    }

    public void stopServer() {

        mServerSession.interrupt();
        mServerSession = null;

        Log.d("yot132","onActivityDestroyed called");
    }

    public class MyActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

        // running activity count
        private int running = 0;
        private Stack<Activity> mApplicationStack = new Stack<>();

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {

        }

        @Override
        public void onActivityStarted(Activity activity) {
            mApplicationStack.push(activity);

            Log.d("yot132","onActivityStarted called");

            if (++running == 1) {
                // running activity is 1,
                // app must be returned from background just now (or first launch)
                mServerSession = new ServerSession(mServerIp, mServerPort, 1000, CustomApplication.this);
                Log.d("yot132","server session start called1");
                mServerSession.start();
            } else if (running > 1) {
                // 2 or more running activities,
                // should be foreground already.
            }
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @RequiresApi(api = Build.VERSION_CODES.DONUT)
        @Override
        public void onActivityStopped(Activity activity) {
            mApplicationStack.pop();
            if (--running == 0) {
                // no active activity
                // app goes to background
                mServerSession.interrupt();
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }


    }

    @Override
    public void onAttaching() {
        Log.d("yot132","onAttaching called");
        super.onAttaching();
    }

    @Override
    public void onAttached() {
        Log.d("yot132","onAttached called");
        super.onAttached();
    }

    @Override
    public void onAttachFailed() {
        Log.d("yot132","onAttachFailed called");
        super.onAttachFailed();
    }

    @Override
    public void onDetach() {
        Log.d("yot132","onDetach called");
        super.onDetach();
    }

    @Override
    public void onTimedOut() {
        mServerSession.request(envsensor.Ping.newBuilder().build());

    }


    @Override
    public void onConnected(RendezvousSession rendezvousSession, Connection connection) {
        super.onConnected(rendezvousSession, connection);
    }

    public void request(MessageLite message) {
        throw new RuntimeException("call setUseAutoServerSession(true) if you want to use this method");
    }



}