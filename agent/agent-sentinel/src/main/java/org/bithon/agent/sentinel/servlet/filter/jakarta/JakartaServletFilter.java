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

package org.bithon.agent.sentinel.servlet.filter.jakarta;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bithon.agent.core.config.ConfigurationManager;
import org.bithon.agent.sentinel.ISentinelListener;
import org.bithon.agent.sentinel.SentinelRuleManager;
import org.bithon.agent.sentinel.config.SentinelConfig;
import org.bithon.shaded.com.alibaba.csp.sentinel.Entry;
import org.bithon.shaded.com.alibaba.csp.sentinel.EntryType;
import org.bithon.shaded.com.alibaba.csp.sentinel.ResourceTypeConstants;
import org.bithon.shaded.com.alibaba.csp.sentinel.SphU;
import org.bithon.shaded.com.alibaba.csp.sentinel.slots.block.BlockException;
import org.bithon.shaded.com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import org.bithon.shaded.com.alibaba.csp.sentinel.slots.block.flow.FlowException;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/5 23:33
 */
class JakartaServletFilter implements Filter {

    private final ISentinelListener listener;
    private final SentinelConfig config;

    JakartaServletFilter(ISentinelListener listener) {
        SentinelRuleManager.getInstance().setListener(listener);
        this.listener = listener;
        this.config = ConfigurationManager.getInstance()
                                          .getConfig(SentinelConfig.class);
    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        if (!config.isEnabled()) {
            chain.doFilter(request, response);
        }

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        SentinelRuleManager.CompositeRule rule = SentinelRuleManager.getInstance()
                                                                    .matches(httpServletRequest.getRequestURI());
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }

        Entry entry = null;
        try {
            entry = SphU.entry(rule.getUrlMatcher().getPattern(), ResourceTypeConstants.COMMON_WEB, EntryType.IN);
            chain.doFilter(request, response);
        } catch (FlowException e) {
            // TOO MANY REQUESTS
            ((HttpServletResponse) response).sendError(
                429,
                "Flow Controlled by agent sentinel"
            );
            if (this.listener != null) {
                try {
                    this.listener.onFlowControlled(httpServletRequest.getRequestURI(),
                                                   httpServletRequest.getMethod(),
                                                   httpServletRequest::getHeader);
                } catch (Throwable ignored) {
                }
            }
        } catch (DegradeException e) {
            ((HttpServletResponse) response).sendError(
                429,
                "Degraded by agent sentinel"
            );
            if (this.listener != null) {
                try {
                    this.listener.onDegraded(httpServletRequest.getRequestURI(),
                                             httpServletRequest.getMethod(),
                                             httpServletRequest::getHeader);
                } catch (Throwable ignored) {
                }
            }
        } catch (BlockException e) {
            ((HttpServletResponse) response).sendError(
                429,
                "Blocked by agent sentinel"
            );
        } catch (Throwable e) {
            if (entry != null) {
                entry.setError(e);
            }
            throw e;
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }
}
