# Spring Framework 7 / Spring Boot 4 Upgrade Notes

This document tracks important information about the Spring Framework 7 / Spring Boot 4 upgrade performed on this project.

## Upgrade Summary

| Component | Previous Version | Current Version |
|-----------|-----------------|-----------------|
| Spring Boot | 3.3.1 | 4.0.0 |
| Spring Cloud | 2023.0.2 | 2025.1.0 |
| Spring Cloud Alibaba | 2023.0.1.0 | 2025.1.0.0 |
| Hibernate Validator | 8.0.1.Final | 9.0.0.Final |
| Java Baseline | 17 | 17 (unchanged) |

## Spring Cloud Alibaba

**Status**: Using stable release 2025.1.0.0 (available on Maven Central).

The project uses `spring-cloud-alibaba-dependencies:2025.1.0.0`, which supports Spring Boot 4.0.x and Spring Cloud 2025.1.x. No GitHub Packages repository or authentication is required.

## API Migration: javax.* to jakarta.*

Spring Framework 7 removes support for `javax.annotation` and `javax.inject` annotations. The following files were migrated from `javax.annotation.Nullable` to `jakarta.annotation.Nullable`:

## API Migration: DataSourceAutoConfiguration Package Change

In Spring Boot 4.0, `DataSourceAutoConfiguration` moved from `org.springframework.boot.autoconfigure.jdbc` to `org.springframework.boot.jdbc.autoconfigure`. The following modules were updated (import + `spring-boot-jdbc` dependency):

- `server/storage-jdbc-h2` - H2StorageModuleAutoConfiguration
- `server/storage-jdbc-mysql` - MySQLStorageModuleAutoConfiguration
- `server/storage-jdbc-postgresql` - PostgresqlStorageModuleAutoConfiguration

Note: `server/storage-jdbc-clickhouse` does not use `@AutoConfigureBefore(DataSourceAutoConfiguration.class)` and required no changes.

## API Migration: ServerProperties Package Change

In Spring Boot 4.0, `ServerProperties` moved from `org.springframework.boot.autoconfigure.web` to `org.springframework.boot.web.server.autoconfigure`. The following 13 files were updated:

- `server/web-security/src/main/java/.../AsyncHttpRequestSecurityCustomizer.java`
- `server/web-security/src/main/java/.../SecurityAutoConfiguration.java`
- `server/alerting/evaluator/src/main/java/.../AlertEvaluator.java`
- `server/alerting/evaluator/src/main/java/.../EvaluatorModuleAutoConfiguration.java`
- `server/alerting/evaluator/src/test/java/.../AlertEvaluatorTest.java`
- `server/alerting/manager/src/main/java/.../AlertCommandService.java`
- `server/collector/src/main/java/.../ZipkinHttpTraceReceiverEnabler.java`
- `server/collector/src/main/java/.../JaegerHttpTraceReceiverEnabler.java`
- `server/collector/src/main/java/.../BithonHttpTraceEnabler.java`
- `server/collector/src/main/java/.../OtlpHttpTraceReceiverEnabler.java`
- `server/storage-jdbc/src/main/java/.../NotificationChannelJdbcStorage.java`
- `server/storage-jdbc-clickhouse/src/main/java/.../NotificationChannelStorage.java`
- `server/storage-jdbc-postgresql/src/main/java/.../NotificationChannelStorage.java`

## API Migration: jOOQ Auto-Configuration Package Change

In Spring Boot 4.0, jOOQ auto-configuration classes moved from `org.springframework.boot.autoconfigure.jooq` to `org.springframework.boot.jooq.autoconfigure`. The following files were updated:

- `server/storage-jdbc/src/main/java/.../JdbcStorageProviderConfiguration.java`
- `server/storage-jdbc-clickhouse/src/main/java/.../ClickHouseStorageProviderConfiguration.java`

## API Migration: javax.annotation.Nullable

- `server/web-service/src/main/java/org/bithon/server/web/service/datasource/api/impl/QueryFilter.java`
- `server/web-service/src/main/java/org/bithon/server/web/service/datasource/api/IntervalRequest.java`
- `server/web-service/src/main/java/org/bithon/server/web/service/datasource/api/QueryField.java`
- `server/web-service/src/main/java/org/bithon/server/web/service/datasource/api/GetDimensionRequest.java`
- `server/web-service/src/main/java/org/bithon/server/web/service/tracing/api/GetTraceByIdRequest.java`
- `server/web-service/src/main/java/org/bithon/server/web/service/meta/api/GetApplicationsRequest.java`
- `server/web-service/src/main/java/org/bithon/server/web/service/common/calcite/SqlExecutionEngine.java`
- `server/web-service/src/main/java/org/bithon/server/web/service/common/calcite/InformationSchema.java`
- `server/metric-expression/src/main/java/org/bithon/server/metric/expression/api/MetricQueryApi.java`
- `server/metric-expression/src/main/java/org/bithon/server/metric/expression/ast/MetricExpression.java`
- `server/pipeline/src/main/java/org/bithon/server/pipeline/metrics/SchemaMetricMessage.java`
- `server/pipeline/src/main/java/org/bithon/server/pipeline/common/transformer/TransformSpec.java`
- `server/alerting/evaluator/src/main/java/org/bithon/server/alerting/evaluator/evaluator/AlertEvaluator.java`
- `server/alerting/manager/src/main/java/org/bithon/server/alerting/manager/api/model/GetRuleFoldersRequest.java`
- `server/alerting/manager/src/main/java/org/bithon/server/alerting/manager/api/model/GetAlertRecordListRequest.java`

**Note**: The `server/jOOQ/` directory contains third-party vendored code and was intentionally NOT modified.

## Jackson 2 Configuration (Spring Boot 4)

Spring Boot 4 defaults to Jackson 3. To use Jackson 2 (for `Jackson2ObjectMapperBuilder` and custom serializers):

1. **Dependency**: Add `spring-boot-jackson2` to `server-starter/pom.xml`
2. **Configuration**: Set `spring.http.converters.preferred-json-mapper: jackson2` in `bootstrap.yml`

## Known Issues and Considerations

1. **Spring Cloud Alibaba Compatibility**: Using stable 2025.1.0.0 release from Maven Central.

2. **jOOQ Library**: The vendored jOOQ library in `server/jOOQ/` still contains `javax.*` imports. This is third-party code and should not be modified. It should work as long as the jOOQ library itself maintains backward compatibility.

3. **Druid Spring Boot Starter**: Version 1.2.27 is used. Verify compatibility with Spring Boot 4.0.x during testing.

## References

- [Spring Framework 7.0 Release Notes](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)
- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [Spring Cloud Alibaba GitHub](https://github.com/alibaba/spring-cloud-alibaba)

---

*Last Updated: February 2026*

