package com.cmc.wifiautoconnectpoc.wifi.p2p;

import android.annotation.TargetApi;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.util.Log;

/**
 * Created by Sahar on 04/05/2016.
 */
public class WiFiP2PHelper {

    private static final String TAG = "WiFiP2PHelper";

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static void connectP2P(WifiP2pManager manager, WifiP2pManager.Channel channel, final IWiFiP2PConnectionListener listener, WifiP2pDevice device){
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess");
                if(listener != null){
                    listener.onConnectionSuccess();
                }
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "onFailure: reason = " + reason);
                if(listener != null){
                    listener.onConnectionFailure(reason);
                }
            }
        });

    }

    public interface IWiFiP2PConnectionListener {
        void onConnectionSuccess();
        void onConnectionFailure(int reason);
    }
}
