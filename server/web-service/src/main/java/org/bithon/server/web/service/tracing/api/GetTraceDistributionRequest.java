package org.bithon.server.web.service.tracing.api;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author Frank Chen
 * @date 17/12/21 4:06 PM
 */
@Data
public class GetTraceDistributionRequest {
    @NotBlank
    private String startTimeISO8601;

    @NotBlank
    private String endTimeISO8601;

    @NotBlank
    private String application;
}
