package com.cmc.wifiautoconnectpoc.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Sahar on 04/05/2016.
 */
public class WiFiHelper {

    final static int CONNECTION_TIMEOUT = 60 * 1000; // 60 second

    private static boolean mIsStateChanged;
    private static WiFiStateBroadcastReceiver mStateBroadcastReceiver;
    private static IWiFiStateCallback sCallback = null;
    private static final String TAG = "WiFiHelper";
    private static boolean mIsObtainedIPAddress = false;

    private static boolean sIsStateChanged;

    private static BroadcastReceiver sConnectionBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                Log.d("celle", "state: " + info.getDetailedState().toString());
                switch(info.getDetailedState()){
                    case OBTAINING_IPADDR:
                        sIsStateChanged = true;
                        break;

                    case CONNECTED:
                        if(sIsStateChanged){
                            stopTimer();
                            context.unregisterReceiver(this);
                            if(sCallback != null){
                                if (Build.VERSION.SDK_INT >= 21) {
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            sCallback.onConnectionStateChanged(true);
                                        }
                                    }, 1500);
                                } else {
                                    sCallback.onConnectionStateChanged(true);
                                }
                            }
                        }
                        break;
                }
            }
        }
    };
    private static Timer sTimeoutTimer;
    private static int sNetworkId;

    public static void connectToNetwork(Context context, String ssid, String passowrd, IWiFiStateCallback listener){
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

        // if wifi is off
        if(!wifiManager.isWifiEnabled()){
            Log.d(TAG, "wifi is off, trying to enable...");
            IntentFilter intentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
            mStateBroadcastReceiver = new WiFiStateBroadcastReceiver(ssid, passowrd);
            context.registerReceiver(mStateBroadcastReceiver, intentFilter);
            boolean success = wifiManager.setWifiEnabled(true); // try enabling wifi - asynchronous
            if(!success){ // error - cannot enable wifi!
                Log.d(TAG, "cannot enable wifi!");
                if(sCallback != null){
                    sCallback.onConnectionStateChanged(false);
                }
            }
        } else { // wifi is on
            Log.d(TAG, "wifi is on");
            // if already connected to requested network - done!
            if(ssid.equals(getConnectedNetworkSSID(context))){
                Log.d(TAG, "already connected to network: \"" + ssid  +"\"");
                if(sCallback != null){
                    sCallback.onConnectionStateChanged(true);
                }
            }

            mIsObtainedIPAddress = false;
            IntentFilter intentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            context.registerReceiver(sConnectionBroadcastReceiver, intentFilter);
            WifiConfiguration wifiConfiguration = new WifiConfiguration();
            wifiConfiguration.SSID = String.format("\"%s\"", ssid);
            wifiConfiguration.preSharedKey = String.format("\"%s\"", passowrd);
            int netId = -1;

            // check if requested network already configured (and have id)
            List<WifiConfiguration> wifiConfigurationList = wifiManager.getConfiguredNetworks();
            for(int i = 0; i < wifiConfigurationList.size(); i++){
                WifiConfiguration config = wifiConfigurationList.get(i);
                if(config.SSID.equals("\"" + ssid + "\"")){
                    Log.d(TAG, "requested net: " + ssid + " already configured");
                    netId = config.networkId;
                    break;

                }
            }

            if(netId == -1){
                netId = wifiManager.addNetwork(wifiConfiguration);
            }
            sNetworkId = netId;
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            startTimer(context);
            boolean success = wifiManager.reconnect();
            if(!success){
                Log.d(TAG, "unable to connect to " + ssid);
                context.unregisterReceiver(sConnectionBroadcastReceiver);
                if(sCallback != null){
                    sCallback.onConnectionStateChanged(false);
                }
            }
        }
    }

    private static String getConnectedNetworkSSID(Context context) {
        String ssid = null;

        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            ssid = wifiInfo.getSSID();
            if(ssid.startsWith("\"") && ssid.endsWith("\"")){
                ssid = ssid.substring(1, ssid.length()-1);
            }

        }catch (Exception e) {
        }

        return ssid;
    }


    public interface IWiFiStateCallback {
        public void onConnectionStateChanged(boolean connected);
    }

    private static class WiFiStateBroadcastReceiver extends BroadcastReceiver {

        private String mSsid;
        private String mPassword;

        public WiFiStateBroadcastReceiver(String ssid, String password){
            mSsid = ssid;
            mPassword = password;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                final int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                switch (state) {
                    case WifiManager.WIFI_STATE_ENABLING:
                        mIsStateChanged = true;
                        break;

                    case WifiManager.WIFI_STATE_ENABLED:
                        if(mIsStateChanged){
                            mIsStateChanged = false;
                            context.unregisterReceiver(this);
                            //WiFi turned on, trying to connect
                            connectToNetwork(context, mSsid, mPassword, sCallback);
                        }
                        break;
                }
            }
        }
    }

    /**
     */
    static void startTimer(final Context context){
        stopTimer();
        sTimeoutTimer = new Timer();
        sTimeoutTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                try {
                    context.unregisterReceiver(sConnectionBroadcastReceiver);
                } catch (Exception e) {
                }
                if (sCallback != null) {
                    sCallback.onConnectionStateChanged(false);
                }
            }
        }, CONNECTION_TIMEOUT);
    }

    /**
     */
    static void stopTimer(){
        if(sTimeoutTimer != null) {
            sTimeoutTimer.cancel();
            sTimeoutTimer = null;
        }
    }
}
