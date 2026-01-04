# Spring Framework 7 / Spring Boot 4 Upgrade Notes

This document tracks important information about the Spring Framework 7 / Spring Boot 4 upgrade performed on this project.

## Upgrade Summary

| Component | Previous Version | Current Version |
|-----------|-----------------|-----------------|
| Spring Boot | 3.3.1 | 4.0.0 |
| Spring Cloud | 2023.0.2 | 2025.1.0 |
| Spring Cloud Alibaba | 2023.0.1.0 | 2025.1.0.0-SNAPSHOT |
| Hibernate Validator | 8.0.1.Final | 9.0.0.Final |
| Java Baseline | 17 | 17 (unchanged) |

## Spring Cloud Alibaba SNAPSHOT Usage

**Status**: Using SNAPSHOT version until stable release is available.

### Current Configuration

The project currently uses `spring-cloud-alibaba-dependencies:2025.1.0.0-SNAPSHOT` because the stable 2025.1.x release for Spring Boot 4.0.x is not yet available in Maven Central.

### GitHub Packages Repository

A repository configuration has been added to `server/pom.xml` to fetch the SNAPSHOT:

```xml
<repositories>
    <repository>
        <id>github-spring-cloud-alibaba</id>
        <url>https://maven.pkg.github.com/alibaba/spring-cloud-alibaba</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

### GitHub Authentication Required

To use GitHub Packages, you need to configure authentication in your Maven `settings.xml`:

```xml
<servers>
    <server>
        <id>github-spring-cloud-alibaba</id>
        <username>YOUR_GITHUB_USERNAME</username>
        <password>YOUR_GITHUB_TOKEN</password>
    </server>
</servers>
```

The GitHub token must have `read:packages` permission.

### TODO: Update to Stable Release

**Monitor**: https://github.com/alibaba/spring-cloud-alibaba/releases

When Spring Cloud Alibaba 2025.1.x stable is released:

1. Update `server/pom.xml`:
   ```xml
   <alibaba-cloud.version>2025.1.0.0</alibaba-cloud.version>
   ```

2. Remove the GitHub Packages repository configuration (or set `<snapshots><enabled>false</enabled></snapshots>`).

3. Remove GitHub authentication from `settings.xml` if no longer needed.

## API Migration: javax.* to jakarta.*

Spring Framework 7 removes support for `javax.annotation` and `javax.inject` annotations. The following files were migrated from `javax.annotation.Nullable` to `jakarta.annotation.Nullable`:

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

## Known Issues and Considerations

1. **Spring Cloud Alibaba Compatibility**: Using SNAPSHOT version - may have instability issues. Monitor for stable release.

2. **jOOQ Library**: The vendored jOOQ library in `server/jOOQ/` still contains `javax.*` imports. This is third-party code and should not be modified. It should work as long as the jOOQ library itself maintains backward compatibility.

3. **Druid Spring Boot Starter**: Version 1.2.27 is used. Verify compatibility with Spring Boot 4.0.x during testing.

## References

- [Spring Framework 7.0 Release Notes](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)
- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [Spring Cloud Alibaba GitHub](https://github.com/alibaba/spring-cloud-alibaba)

---

*Last Updated: January 2026*

