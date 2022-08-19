package org.bithon.server.storage.web;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * @author Frank Chen
 * @date 19/8/22 5:42 pm
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dashboard {
    private String name;
    private String payload;
    private String signature;
    private Timestamp timestamp;
}
