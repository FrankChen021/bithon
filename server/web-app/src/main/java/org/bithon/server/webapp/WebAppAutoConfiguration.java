package org.bithon.server.webapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.InvalidConfigurationException;
import org.bithon.server.storage.web.IDashboardStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

/**
 * @author Frank Chen
 * @date 19/8/22 2:21 pm
 */
@Configuration
@Conditional(value = WebAppModuleEnabler.class)
public class WebAppAutoConfiguration {

    @Bean
    public IDashboardStorage createDashboardStorage(ObjectMapper om,
                                                    @Value("${bithon.storage.web.type}") String type) throws IOException {
        InvalidConfigurationException.throwIf(!StringUtils.hasText(type),
                                              "[bithon.storage.web.type] can't be blank");

        // create storage
        String jsonType = StringUtils.format("{\"type\":\"%s\"}", type);
        IDashboardStorage storage = om.readValue(jsonType, IDashboardStorage.class);
        storage.initialize();

        // load or update schemas
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:/dashboard/*.json");
            for (Resource resource : resources) {

                // deserialize and then serialize again to compact the json string
                String payload = om.writeValueAsString(om.readTree(resource.getInputStream()));

                storage.putIfNotExist(resource.getFilename().replace(".json", ""), payload);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return storage;
    }
}
