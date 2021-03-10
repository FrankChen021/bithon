package com.sbss.bithon.server.config;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * @author frankchen
 */
@Slf4j
@Configuration
public class DataSourceConfig {

    @Bean(name = "bizDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.druid.biz-datasource")
    public DataSource bizDataSource() {
        log.info("-------------------- bizDataSource init ---------------------");
        return DruidDataSourceBuilder.create().build();
    }

}
