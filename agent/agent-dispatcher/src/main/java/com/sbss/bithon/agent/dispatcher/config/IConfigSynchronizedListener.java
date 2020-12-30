package com.sbss.bithon.agent.dispatcher.config;

import shaded.com.alibaba.fastjson.JSONObject;

/**
 * @author frankchen
 * @Date 2020-05-27 14:41:22
 */
public interface IConfigSynchronizedListener {
    void onSync(JSONObject config);
}
