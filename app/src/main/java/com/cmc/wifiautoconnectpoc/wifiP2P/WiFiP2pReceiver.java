package com.cmc.wifiautoconnectpoc.wifiP2P;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Sahar on 04/05/2016.
 */
public class WiFiP2pReceiver extends BroadcastReceiver implements WifiP2pManager.ActionListener, WifiP2pManager.ConnectionInfoListener, WifiP2pManager.GroupInfoListener {

    private final String TAG = "WiFiP2pReceiver";

    private final WifiP2pListener mListener;
    private final WifiP2pManager mManager;
    private final WifiP2pManager.Channel mChannel;
    private String mDeviceIP;
    private ArrayList<WifiP2pDevice> peers = new ArrayList();
    private WifiP2pManager.PeerListListener mPeersListListener = new WifiP2pManager.PeerListListener() {
        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peersList) {
            peers.clear();
            peers.addAll(peersList.getDeviceList());
            printPeersList();
        }
    };

    private void printPeersList() {
        for(int i = 0; i < peers.size(); i++){
            WifiP2pDevice p = peers.get(i);
            Log.d(TAG, String.format("WifiP2pDevice #%d: name = %s, address = %s, primDeviceType = %s, secondDeviceType = %s, status = %d",
                    i, p.deviceName, p.deviceAddress, p.primaryDeviceType, p.secondaryDeviceType, p.status));
        }
    }

    public WiFiP2pReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, WifiP2pListener listener){
        this.mManager = manager;
        this.mChannel = channel;
        this.mListener = listener;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null) {
            String action = intent.getAction();
            if(!TextUtils.isEmpty(action)){
                if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
                    Log.d(TAG, "onReceive: WIFI_P2P_STATE_CHANGED_ACTION");
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                    if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                        this.mListener.setP2pEnabled(true);
                    }else{
                        this.mListener.setP2pEnabled(false);
                    }
                }else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
                    Log.d(TAG, "onReceive: WIFI_P2P_PEERS_CHANGED_ACTION");
                    if(mManager != null){
                        mManager.requestPeers(mChannel, mPeersListListener);
                    }
                } else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
                    Log.d(TAG, "onReceive: WIFI_P2P_CONNECTION_CHANGED_ACTION");
                    //TODO: make sure we are connected to wifi
                    if(mManager != null){
                        mManager.requestConnectionInfo(mChannel, this);
                    }

                } else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
                    Log.d(TAG, "onReceive: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void createHotspot(){
        Log.d(TAG, "createHotspot");
        mManager.createGroup(mChannel, this);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void removeHotspot() {
        Log.d(TAG, "createHotspot");
        mManager.removeGroup(mChannel, null);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void discoverPeers(){
        Log.d(TAG, "discoverPeers");
        mManager.discoverPeers(mChannel, null);
    }

    @Override
    public void onSuccess() {
        Log.d(TAG, "onSuccess");
    }

    @Override
    public void onFailure(int reason) {
        Log.e(TAG, "onFailure: reason = " + reason);
        if(mListener != null){
            mListener.onHotspotError(reason);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        Log.d(TAG, "onConnectionInfoAvailable: info = " + info);
        if(info != null){
            if (info.groupFormed = true && info.groupOwnerAddress != null) {
                mDeviceIP = info.groupOwnerAddress.getHostAddress();
                mManager.requestGroupInfo(mChannel, this);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        Log.d(TAG, "onGroupInfoAvailable: group = " + group);
        try {
            String ssid = group.getNetworkName();
            String password = group.getPassphrase();
            if (mListener != null) {
                mListener.onHotspotCreated(ssid, mDeviceIP, password);
            }
        } catch (Exception e) {
            mManager.removeGroup(mChannel, null);
            if (mListener != null) {
                mListener.onHotspotError(-1);
            }
        }
    }

    public interface WifiP2pListener {
        void onHotspotError(int reason);
        void setP2pEnabled(boolean isEnabled);
        void onHotspotCreated(String ssid, String ipAdd, String password);
    }
}
