/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.server.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.event.ConsumerStoppedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/14 14:02
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "collector-kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConsumerEventListener implements ApplicationListener<ConsumerStoppedEvent>, ApplicationContextAware {
    private ApplicationContext applicationContext;

    @PostConstruct
    void create() {
        log.info("KafkaConsumerEventListener started");
    }

    @Override
    public void onApplicationEvent(ConsumerStoppedEvent event) {
        log.error("Consumer stopped because of {}. Will exit the application.", event.getReason());
        SpringApplication.exit(applicationContext, () -> 0);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
