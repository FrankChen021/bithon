package com.sbss.bithon.server.setting;

import com.sbss.bithon.component.db.dao.SettingDAO;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 7:37 下午
 */
@Service
public class AgentSettingService {

    private final SettingDAO dao;

    public AgentSettingService(SettingDAO dao) {
        this.dao = dao;
    }

    public Map<String, String> getSettings(String appName, String env, long since) {
        return dao.getSettings(appName + "-" + env, since);
    }
}
