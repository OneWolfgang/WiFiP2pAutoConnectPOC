package com.cmc.wifiautoconnectpoc.wifiP2P;

import android.annotation.TargetApi;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sahar on 04/05/2016.
 */
public class WifiP2pServiceDiscoveryHelper {

    private final WifiP2pManager mManager;
    private final WifiP2pManager.Channel mChannel;

    private WifiP2pServiceDiscoveryListener mListener = null;

    private final String TAG = "WifiP2pServiceDiscoveryHelper";

    private final String KEY_IP = "ip";

    public WifiP2pServiceDiscoveryHelper(WifiP2pManager manager, WifiP2pManager.Channel channel, WifiP2pServiceDiscoveryListener listener){
        this.mManager = manager;
        this.mChannel = channel;
        this.mListener = listener;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void startRegistration(String ip, String ssid, String passowrd){
        Map record = new HashMap();
        record.put(KEY_IP, ip);
        WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(ssid + "_" + passowrd, "_presence._tcp",record);
        mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess (addLocalService)");
                if(mListener != null){
                    mListener.onServiceAddSuccess();
                }
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "onFailure: reason = " + reason);
                if(mListener != null){
                    mListener.onServiceAddFailed(reason);
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void discoverService(){
        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
                Log.d(TAG, "onDnsSdTxtRecordAvailable: domainName = " + fullDomainName);
                if(mListener != null){
                    mListener.onTxtRecordAvailable(fullDomainName, txtRecordMap, srcDevice);
                }
            }
        };

        WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
                Log.d(TAG, "onDnsSdServiceAvailable: instanceName = " + instanceName + ", registrationType = " + registrationType);
                if(mListener != null){
                    mListener.onServiceAvailable(instanceName, registrationType, srcDevice);
                }
            }
        };

        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess (addServiceRequest)");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "onFailure: reason = " + reason);
            }
        });
        mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess (discoverServices)");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "discoverServices: reason = " + reason);
            }
        });
    }

    public interface WifiP2pServiceDiscoveryListener {
        void onServiceAvailable(String instanceName, String registerType, WifiP2pDevice resourceType);
        void onTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice resourceType);
        void onServiceAddSuccess();
        void onServiceAddFailed(int reason);
    }
}

