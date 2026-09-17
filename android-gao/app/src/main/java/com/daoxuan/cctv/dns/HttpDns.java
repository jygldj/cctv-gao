package com.daoxuan.cctv.dns;


import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.Record;
import com.qiniu.android.dns.dns.DnsUdpResolver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Dns;
import com.daoxuan.cctv.util.LogUtil;

public class HttpDns implements Dns {

    private DnsManager dnsManager;

    public HttpDns() {
        IResolver[] resolvers = new IResolver[2];
            resolvers[0] = new DnsUdpResolver("223.5.5.5"); 
            resolvers[1] = new DnsUdpResolver("223.6.6.6");
            dnsManager = new DnsManager(NetworkInfo.normal, resolvers);

    }

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        if (dnsManager == null)  
            return Dns.SYSTEM.lookup(hostname);

        try {
            Record[] records = dnsManager.queryRecords(hostname);  
            if (records == null || records.length == 0) {
                return Dns.SYSTEM.lookup(hostname);
            }

            List<InetAddress> result = new ArrayList<>();
            for (Record record : records) {  
                LogUtil.i("record value",hostname+" "+record.value);
                if(record.value.equals("0.0.0.0")){
                    continue;
                }
                result.addAll(Arrays.asList(InetAddress.getAllByName(record.value)));
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Dns.SYSTEM.lookup(hostname);
    }
}