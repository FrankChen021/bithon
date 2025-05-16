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

package org.bithon.server.web.service.agent.api;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.discovery.client.DiscoveredServiceInvoker;
import org.bithon.server.discovery.client.IDiscoveryClient;
import org.bithon.server.discovery.client.ServiceInvocationExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import java.util.function.Consumer;

/**
 * @author frank.chen021@outlook.com
 * @date 16/4/25 10:36 pm
 */
public class AgentDiagnosisApiTest {

    private static DiscoveredServiceInvoker discoveredServiceInvoker;
    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void setUp() {
        objectMapper = new ObjectMapper();
        discoveredServiceInvoker = new DiscoveredServiceInvoker(Mockito.mock(IDiscoveryClient.class), new ServiceInvocationExecutor());
    }

    @Test
    public void test_SELECT_ConfigurationTable_MissingAppName() {
        HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        AgentDiagnosisApi api = new AgentDiagnosisApi(discoveredServiceInvoker, objectMapper, null);

        assertThrows(HttpMappableException.class,
                     () -> api.query("SELECT * FROM agent.configuration", servletRequest, servletResponse),
                     (message) -> assertStartWith(message, "Missing filter on 'appName' in the given SQL"));

    }

    @Test
    public void test_SELECT_ConfigurationTable_MissingInstance() {
        HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        AgentDiagnosisApi api = new AgentDiagnosisApi(discoveredServiceInvoker, objectMapper, null);

        assertThrows(HttpMappableException.class,
                     () -> api.query("SELECT * FROM agent.configuration WHERE appName = '1'", servletRequest, servletResponse),
                     (message) -> assertStartWith(message, "Missing filter on 'instance' in the given SQL"));

    }

    @Test
    public void test_UPDATE_Logger_MissingWHERE() {
        HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        AgentDiagnosisApi api = new AgentDiagnosisApi(discoveredServiceInvoker, objectMapper, null);

        assertThrows(HttpMappableException.class,
                     () -> api.query("UPDATE agent.logger SET level = 'info'", servletRequest, servletResponse),
                     (message) -> assertStartWith(message, "Missing filter on 'appName' in the given SQL"));
    }

    @Test
    public void test_UPDATE_Logger_MissingApName() {
        HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        AgentDiagnosisApi api = new AgentDiagnosisApi(discoveredServiceInvoker, objectMapper, null);

        assertThrows(HttpMappableException.class,
                     () -> api.query("UPDATE agent.logger SET level = 'info' WHERE xxx = 'x'", servletRequest, servletResponse),
                     (message) -> assertStartWith(message, "Missing filter on 'appName' in the given SQL"));
    }

    @Test
    public void test_UPDATE_Logger_MissingInstance() {
        HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        AgentDiagnosisApi api = new AgentDiagnosisApi(discoveredServiceInvoker, objectMapper, null);

        assertThrows(HttpMappableException.class,
                     () -> api.query("UPDATE agent.logger SET level = 'info' WHERE appName = '1'", servletRequest, servletResponse),
                     (message) -> assertStartWith(message, "Missing filter on 'instance' in the given SQL"));
    }

    @Test
    public void test_UPDATE_Logger_MissingName() {
        HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);
        AgentDiagnosisApi api = new AgentDiagnosisApi(discoveredServiceInvoker, objectMapper, null);

        assertThrows(Preconditions.InvalidValueException.class,
                     () -> api.query("UPDATE agent.logger SET level = 'info' WHERE appName = '1' AND instance = 'y'", servletRequest, servletResponse),
                     (message) -> assertContains(message, "'name'"));
    }

    public static void assertThrows(Class<? extends Throwable> expectedType, Executable executable, Consumer<String> messagePredicate) {
        try {
            executable.execute();
        } catch (Throwable e) {
            Assertions.assertTrue(expectedType.isInstance(e), "Expected exception of type " + expectedType.getName() + " but got " + e.getClass().getName() + " with message: " + e.getMessage());
            messagePredicate.accept(e.getMessage());
            return;
        }
        Assertions.fail("Expected exception of type " + expectedType.getName() + " was not thrown");
    }

    public static void assertStartWith(String actual, String pattern) {
        Assertions.assertTrue(actual.startsWith(pattern), "Expected message to start with '" + pattern + "' but got '" + actual + "'");
    }

    public static void assertContains(String actual, String pattern) {
        Assertions.assertTrue(actual.contains(pattern), "Expected message to contain '" + pattern + "' but got '" + actual + "'");
    }
}
