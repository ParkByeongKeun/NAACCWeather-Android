package net.ijoon.ijoonweatherstation.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.ijoon.ijoonweatherstation.CustomApplication;
import net.ijoon.scnet_android.Connection;
import net.ijoon.scnet_android.MessageHeader;
import net.ijoon.scnet_android.RendezvousClientCallback;
import net.ijoon.scnet_android.RendezvousSession;

public class NetworkChangeReceiver extends BroadcastReceiver implements RendezvousClientCallback {
    NetworkUtil.State prevState = NetworkUtil.State.TYPE_NOT_CONNECTED;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        NetworkUtil.State state = NetworkUtil.getConnectivityStatus(context);
        CustomApplication globalApplication = (CustomApplication) context.getApplicationContext();

        Log.d("dsfnkldsfnklsf", "NetworkChanged ..... state : " + state);

        if(prevState == state) {
            return;
        }

        switch (state) {
            case TYPE_WIFI:
            case TYPE_MOBILE:
            {
                globalApplication.startServer();
            }
                break;
            case TYPE_NOT_CONNECTED:
            {
                globalApplication.stopServer();
            }
                break;
        }

        prevState = state;
    }

    @Override
    public void onConnectedToRendezvousServer(String myPublicIP, String myPublicPort) {

    }

    @Override
    public void onConnecting(int connectionID) {

    }

    @Override
    public void onConnected(RendezvousSession rendezvousSession, Connection connection) {

    }

    @Override
    public void onConnectionIDCreated(RendezvousSession rendezvousSession, String ip, int port) {

    }

    @Override
    public void onTargetInvalid(String ip, int port) {

    }

    @Override
    public void onConnectFailed(int connectionID) {

    }

    @Override
    public void onConnectionUpdate(RendezvousSession rendezvousSession, Connection connection) {

    }

    @Override
    public void onConnectionRemoved(RendezvousSession rendezvousSession, Connection connection) {

    }

    @Override
    public void onDisconnected(int connectionID) {

    }

    @Override
    public void onMessage(MessageHeader messageHeader, byte[] buffer) {

    }
}