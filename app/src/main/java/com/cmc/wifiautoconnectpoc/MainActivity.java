package com.cmc.wifiautoconnectpoc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.cmc.wifiautoconnectpoc.wifi.WiFiHelper;
import com.cmc.wifiautoconnectpoc.wifi.p2p.WiFiP2PHelper;
import com.cmc.wifiautoconnectpoc.wifi.p2p.WiFiP2pReceiver;
import com.cmc.wifiautoconnectpoc.wifi.p2p.WifiP2pServiceDiscoveryHelper;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by Sahar on 04/05/2016.
 */
public class MainActivity extends Activity implements WiFiP2pReceiver.WifiP2pListener, WifiP2pServiceDiscoveryHelper.WifiP2pServiceDiscoveryListener, WiFiHelper.IWiFiStateCallback, WiFiP2PHelper.IWiFiP2PConnectionListener {

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
    private final int MENU_ITEM_CONNECT_TO_NETWORK = 4;
    private final int MENU_ITEM_DISCOVER_PEERS = 5;
    private WifiP2pServiceDiscoveryHelper mWifiP2pServiceDiscoveryHelper = null;
    private String mSSID;
    private String mIpAddress;
    private String mPassword;
    private String mTargetSSID;
    private String mTargetPassword;
    private TextView mTxtDeviceInfo;
    private TextView mTxtWiFiP2PInfoSelf;
    private TextView mTxtWiFiP2PInfoOther;
    private WifiP2pDevice mTargetDevice;
    private TextView mTxtServiceInfo;
    private TextView mTxtWiFiP2PConnectionInfo;
    private TextView mTxtIsGroupOwner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.main);
        initUIComponents();
        initializeP2P();
    }

    private void initUIComponents() {
        mTxtDeviceInfo = (TextView)findViewById(R.id.txtV_device_info);
        mTxtWiFiP2PInfoSelf = (TextView)findViewById(R.id.txtV_wifip2p_info);
        mTxtWiFiP2PInfoOther = (TextView)findViewById(R.id.txtV_wifip2p_info_other);
        mTxtServiceInfo = (TextView)findViewById(R.id.txtV_self_service_info);
        mTxtWiFiP2PConnectionInfo = (TextView)findViewById(R.id.txtV_wifip2p_connection_info);
        mTxtIsGroupOwner = (TextView)findViewById(R.id.txtV_is_group_owner);

        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String api = Build.VERSION.RELEASE;
        mTxtDeviceInfo.setText(String.format("Manufacturer: %s, Model: %s, API Version: %s", manufacturer, model, api));

        mTxtWiFiP2PInfoSelf.setText("none");
        mTxtWiFiP2PInfoOther.setText("none");
        mTxtServiceInfo.setText("unregistered");
        mTxtWiFiP2PConnectionInfo.setText("not connected");
        mTxtIsGroupOwner.setText("isGroupOwner: unknown");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ITEM_CREATE_HOTSPOT, MENU_ITEM_CREATE_HOTSPOT, "create hotspot");
        menu.add(0, MENU_ITEM_REMOVE_HOTSPOT, MENU_ITEM_REMOVE_HOTSPOT, "remove hotspot");
        menu.add(0, MENU_ITEM_REGISTER_SERVICE, MENU_ITEM_REGISTER_SERVICE, "register service");
        menu.add(0, MENU_ITEM_DISCOVER_SERVICE, MENU_ITEM_DISCOVER_SERVICE, "discover service");
        menu.add(0, MENU_ITEM_CONNECT_TO_NETWORK, MENU_ITEM_CONNECT_TO_NETWORK, "connect to network");
        menu.add(0, MENU_ITEM_DISCOVER_PEERS, MENU_ITEM_DISCOVER_PEERS, "discover peers");
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
                mTxtWiFiP2PInfoSelf.setText("none");
                mTxtWiFiP2PConnectionInfo.setText("not connected");
                mTxtServiceInfo.setText("unregistered");
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
            case MENU_ITEM_CONNECT_TO_NETWORK:{
                Log.d(TAG, "onOptionsItemSelected: case MENU_ITEM_CONNECT_TO_NETWORK");
                if(TextUtils.isEmpty(mTargetSSID) || TextUtils.isEmpty(mTargetPassword)){
                    break;
                }
                WiFiP2PHelper.connectP2P(mManager, mChannel, this, mTargetDevice);
                break;
            }
            case MENU_ITEM_DISCOVER_PEERS:{
                Log.d(TAG, "onOptionsItemSelected: case MENU_ITEM_DISCOVER_PEERS");
                mWifiP2pReceiver.discoverPeers();
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
    public void onHotspotCreated(String ssid, String ipAdd, String password, boolean isGroupOwner, WifiP2pDevice groupOwner) {
        Log.d(TAG, "onHotspotCreated: ssid= " + ssid + ", ip = " + ipAdd + ", pass = " + password);
        mSSID = ssid;
        mIpAddress = ipAdd;
        mPassword = password;

        mTxtWiFiP2PInfoSelf.setText(String.format("SSID: %s, IP: %s, Password: %s", mSSID, mIpAddress, mPassword));

        if(isGroupOwner) {
            Log.d(TAG, "hotSpotCreated, i'm the group owner , auto-registering service...");
            mTxtWiFiP2PConnectionInfo.setText("Group formed (i'm owner), registering service...");
            mWifiP2pServiceDiscoveryHelper.startRegistration(mIpAddress, mSSID, mPassword);
            mTxtIsGroupOwner.setText("isGroupOwner: true");
            mTxtIsGroupOwner.setTextColor(Color.GREEN);
        }else{
            Log.d(TAG, "hotSpotCreated, i'm the NOT the group owner , connected to other device!");
            mTxtIsGroupOwner.setText("isGroupOwner: false");
            mTxtIsGroupOwner.setTextColor(Color.RED);
            StringBuilder sb = new StringBuilder();
            sb.append("connected to: ");
            if(groupOwner != null){
                sb.append("GO: " + groupOwner.deviceName + ", ");
                sb.append("GO Address: " + groupOwner.deviceAddress + ", ");
                sb.append("GO Prim. type: " + groupOwner.primaryDeviceType + ", ");
                sb.append("GO Sec. type: " + groupOwner.secondaryDeviceType + ", ");
            }
            mTxtWiFiP2PConnectionInfo.setText(sb.toString());
        }
    }

    @Override
    public void onServiceAvailable(String instanceName, String registerType, WifiP2pDevice resourceType) {
        Log.d(TAG, "onServiceAvailable: instanceName = " + instanceName + ", registerType = " + registerType + ", srcDevice = " + resourceType);
        parseNetworkInfo(instanceName, resourceType);
    }

    private void parseNetworkInfo(String instanceName, WifiP2pDevice resourceType) {
        if(!TextUtils.isEmpty(instanceName)){
            String[] credentials = instanceName.split("_");
            if(credentials == null || credentials.length != 2){
                Log.e(TAG, "parseNetworkInfo: invalid instanceName format!");
            }else{
                mTargetSSID = credentials[0];
                mTargetPassword = credentials[1];
                mTargetDevice = resourceType;
                mTxtWiFiP2PInfoOther.setText(String.format("Found service: SSID = %s, Password = %s", mTargetSSID, mTargetPassword));
            }
        }
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
        mTxtServiceInfo.setText("registered successfully!");
        mTxtWiFiP2PConnectionInfo.setText("service registered!");
    }

    @Override
    public void onServiceAddFailed(int reason) {
        Log.d(TAG, "onServiceAddFailed: reason = " + reason);
        mTxtServiceInfo.setText("unable to register");
    }

    @Override
    public void onConnectionStateChanged(boolean connected) {
        Log.d(TAG, "onConnectionStateChanged: connected = " + connected);
    }

    @Override
    public void onConnectionSuccess() {
        Log.d(TAG, "onConnectionSuccess");
    }

    @Override
    public void onConnectionFailure(int reason) {
        Log.d(TAG, "onConnectionFailure: reason = " + reason);
    }
}
