package com.sbss.bithon.agent.dispatcher.config;


import com.sbss.bithon.agent.core.config.CoreConfig;
import com.sbss.bithon.agent.dispatcher.rpc.RpcClient;
import shaded.com.alibaba.fastjson.JSONObject;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Agent配置管理
 *
 * @author frankchen
 * @Date 2020-05-27 15:29:51
 */
public class PluginSettingManager {
    private static final Logger log = LoggerFactory.getLogger(PluginSettingManager.class);
    private volatile static JSONObject config = new JSONObject();
    private static String appName;
    private static String env;
    private static RpcClient rpcClient;
    private static List<IConfigSynchronizedListener> listeners = new ArrayList<>();

    public static void addListener(IConfigSynchronizedListener listener) {
        listeners.add(listener);
    }

    public static void init(CoreConfig config,
                            RpcClient sender) {
        appName = config.getBootstrap().getAppName();
        env = config.getBootstrap().getEnv();
        PluginSettingManager.rpcClient = sender;
        PluginSettingManager.startSync();
    }

    public static JSONObject getConfig() {
        return config;
    }

    public static String getAppName() {
        return appName;
    }

    public static String getEnv() {
        return env;
    }

    private static void startSync() {
        new Timer("infra-ac-setting-sync").schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    fetchConfig();
                } catch (Exception e) {
                    log.error("获取配置信息出错", e);
                }
            }
        }, 0, 5 * 60 * 1000);
    }

    private static void fetchConfig() {
        String conf = rpcClient.getAgentConfig(appName);

        boolean isEmpty = conf == null || "".equals(conf.trim());
        log.info("agent config synchronized for {}, config is: {} ", appName, (isEmpty ? "EMPTY" : conf));
        if (isEmpty)
            return;

        PluginSettingManager.config = JSONObject.parseObject(conf);

        //notify
        for (IConfigSynchronizedListener listener : listeners) {
            try {
                listener.onSync(config);
            } catch (Exception e) {
                log.error("onSync error", e);
            }
        }
    }
}
