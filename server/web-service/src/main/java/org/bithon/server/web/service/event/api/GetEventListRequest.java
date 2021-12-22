package org.bithon.server.web.service.event.api;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author Frank Chen
 * @date 22/12/21 11:19 AM
 */
@Data
public class GetEventListRequest {

    @NotBlank
    private String application;

    @NotBlank
    private String startTimeISO8601;

    @NotBlank
    private String endTimeISO8601;
}
