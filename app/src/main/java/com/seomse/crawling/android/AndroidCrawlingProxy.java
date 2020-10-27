package com.seomse.crawling.android;

import com.seomse.api.ApiRequests;
import com.seomse.commons.communication.HostAddrPort;
import com.seomse.commons.file.FileUtil;
import com.seomse.system.commons.PingApi;
import com.seomse.commons.utils.ExceptionUtil;
import com.seomse.crawling.proxy.CrawlingProxy;
import com.seomse.crawling.proxy.CrawlingProxyStarter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class AndroidCrawlingProxy extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(CrawlingProxyStarter.class);
    private static final long sleepTime = 10000L;
    private boolean isStop = false;
    private CrawlingProxy crawlingProxy = null;

    private static final String CONFIG =
            "{\"connection_infos\":[{\"host_address\":\"dev.seomse.com\",\"port\":33335}],\"communication_count\":5}";

    public AndroidCrawlingProxy() {
    }

    public void run() {
        try {
            JSONObject jsonObject = new JSONObject(CONFIG);
            int communicationCount = jsonObject.getInt("communication_count");
            JSONArray jsonArray = jsonObject.getJSONArray("connection_infos");
            HostAddrPort[] hostAddrPortArray = new HostAddrPort[jsonArray.length()];

            int i;
            for (i = 0; i < hostAddrPortArray.length; ++i) {
                JSONObject info = jsonArray.getJSONObject(i);
                HostAddrPort hostAddrPort = new HostAddrPort();
                hostAddrPort.setHostAddress(info.getString("host_address"));
                hostAddrPort.setPort(info.getInt("port"));
                hostAddrPortArray[i] = hostAddrPort;
            }

            while (!this.isStop) {
                try {
                    for (i = 0; i < hostAddrPortArray.length; ++i) {
                        String response = ApiRequests.sendToReceiveMessage(hostAddrPortArray[i].getHostAddress(), hostAddrPortArray[i].getPort(), "com.seomse.crawling.ha", "ActiveAddrPortApi", "");
                        if (response.startsWith("S")) {
                            String[] activeInfo = response.substring(1).split(",");
                            if (PingApi.ping(activeInfo[0], Integer.parseInt(activeInfo[1]))) {
                                this.crawlingProxy = new CrawlingProxy(activeInfo[0], Integer.parseInt(activeInfo[1]), communicationCount);
                                break;
                            }
                        }
                    }

                    if (this.crawlingProxy == null) {
                        Thread.sleep(10000L);
                    } else {
                        while (!this.crawlingProxy.isEnd()) {
                            Thread.sleep(10000L);
                        }
                    }
                } catch (Exception e) {
                    logger.error(ExceptionUtil.getStackTrace(e));
                }
            }
        } catch(JSONException e){

        }

    }

    public void stopService() {
        this.isStop = true;
        if (this.crawlingProxy != null) {
            this.crawlingProxy.stop();
        }

    }

}
