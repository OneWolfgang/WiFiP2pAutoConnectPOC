package com.cmc.wifiautoconnectpoc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.cmc.wifiautoconnectpoc.wifi.p2p.WiFiP2pReceiver;
import com.cmc.wifiautoconnectpoc.wifi.p2p.WifiP2pServiceDiscoveryHelper;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by Sahar on 04/05/2016.
 */
public class MainActivity extends Activity implements WiFiP2pReceiver.WifiP2pListener, WifiP2pServiceDiscoveryHelper.WifiP2pServiceDiscoveryListener {

    private final IntentFilter mWifiP2PIntentFilter = new IntentFilter();
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    private boolean mIsWifiP2pEnabled = false;
    private WiFiP2pReceiver mWifiP2pReceiver;
    private final String TAG = "MainActivity";

    private final int MENU_ITEM_CREATE_HOTSPOT = 0;
    private final int MENU_ITEM_REMOVE_HOTSPOT = 1;
    private final int MENU_ITEM_REGISTER_SERVICE = 2;
    private final int MENU_ITEM_DISCOVER_SERVICE = 3;
    private WifiP2pServiceDiscoveryHelper mWifiP2pServiceDiscoveryHelper = null;
    private String mSSID;
    private String mIpAddress;
    private String mPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.main);
        initializeP2P();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ITEM_CREATE_HOTSPOT, MENU_ITEM_CREATE_HOTSPOT, "create hotspot");
        menu.add(0, MENU_ITEM_REMOVE_HOTSPOT, MENU_ITEM_REMOVE_HOTSPOT, "remove hotspot");
        menu.add(0, MENU_ITEM_REGISTER_SERVICE, MENU_ITEM_REGISTER_SERVICE, "register service");
        menu.add(0, MENU_ITEM_DISCOVER_SERVICE, MENU_ITEM_DISCOVER_SERVICE, "discover service");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case MENU_ITEM_CREATE_HOTSPOT:{
                Log.d(TAG, "onOptionsItemSelected: case MENU_ITEM_CREATE_HOTSPOT");
                mWifiP2pReceiver.createHotspot();
                break;
            }
            case MENU_ITEM_REMOVE_HOTSPOT:{
                Log.d(TAG, "onOptionsItemSelected: case MENU_ITEM_REMOVE_HOTSPOT");
                mWifiP2pReceiver.removeHotspot();
                break;
            }
            case MENU_ITEM_REGISTER_SERVICE:{
                Log.d(TAG, "onOptionsItemSelected: case MENU_ITEM_REGISTER_SERVICE");
                if(mWifiP2pServiceDiscoveryHelper != null){
                    if(TextUtils.isEmpty(mSSID) || TextUtils.isEmpty(mIpAddress) || TextUtils.isEmpty(mPassword)){
                        Log.d(TAG, "onOptionsItemSelected: ip / ssid / password is empty!");
                        break;
                    }
                    mWifiP2pServiceDiscoveryHelper.startRegistration(mIpAddress, mSSID, mPassword);
                }
                break;
            }
            case MENU_ITEM_DISCOVER_SERVICE:{
                Log.d(TAG, "onOptionsItemSelected: case MENU_ITEM_DISCOVER_SERVICE");
                if(mWifiP2pServiceDiscoveryHelper != null){
                    mWifiP2pServiceDiscoveryHelper.discoverService();
                }
                break;
            }
            default:{
                Log.d(TAG, "onOptionsItemSelected: default case");
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void initializeP2P() {
        Log.d(TAG, "initializeP2P");

        // intent filter
        // change in wifi p2p status
        mWifiP2PIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // list of available peers has changed
        mWifiP2PIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // wifiP2P connectivity changed
        mWifiP2PIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // device details changed
        mWifiP2PIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mManager = (WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mWifiP2pServiceDiscoveryHelper = new WifiP2pServiceDiscoveryHelper(mManager, mChannel, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mWifiP2pReceiver = new WiFiP2pReceiver(mManager, mChannel, this);
        registerReceiver(mWifiP2pReceiver, mWifiP2PIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        unregisterReceiver(mWifiP2pReceiver);
    }

    @Override
    public void onHotspotError(int reason) {
        Log.d(TAG, "onHotspotError: reason = " + reason);
    }

    @Override
    public void setP2pEnabled(boolean isEnabled) {
        Log.d(TAG, "setP2pEnabled: isEnabled = " + isEnabled);
        mIsWifiP2pEnabled = isEnabled;
    }

    @Override
    public void onHotspotCreated(String ssid, String ipAdd, String password) {
        Log.d(TAG, "onHotspotCreated: ssid= " + ssid + ", ip = " + ipAdd + ", pass = " + password);
        mSSID = ssid;
        mIpAddress = ipAdd;
        mPassword = password;
    }

    @Override
    public void onServiceAvailable(String instanceName, String registerType, WifiP2pDevice resourceType) {
        Log.d(TAG, "onServiceAvailable: instanceName = " + instanceName + ", registerType = " + registerType + ", srcDevice = " + resourceType);
    }

    @Override
    public void onTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice resourceType) {
        Log.d(TAG, "onTxtRecordAvailable: fullDomain = " + fullDomain + ", srcDevice = " + resourceType + ", map: " + printMap(record));
    }

    private String printMap(Map record) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if(record != null) {
            Iterator<Map.Entry> iter = record.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = iter.next();
                sb.append("<" + entry.getKey() + "," + entry.getValue() + ">");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public void onServiceAddSuccess() {
        Log.d(TAG, "onServiceAddSuccess");
    }

    @Override
    public void onServiceAddFailed(int reason) {
        Log.d(TAG, "onServiceAddFailed: reason = " + reason);
    }
}
