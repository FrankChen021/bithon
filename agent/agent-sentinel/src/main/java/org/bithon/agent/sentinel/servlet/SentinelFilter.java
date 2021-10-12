/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.agent.sentinel.servlet;

import org.bithon.agent.sentinel.ISentinelListener;
import org.bithon.agent.sentinel.SentinelRuleManager;
import shaded.com.alibaba.csp.sentinel.Entry;
import shaded.com.alibaba.csp.sentinel.EntryType;
import shaded.com.alibaba.csp.sentinel.ResourceTypeConstants;
import shaded.com.alibaba.csp.sentinel.SphU;
import shaded.com.alibaba.csp.sentinel.slots.block.BlockException;
import shaded.com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import shaded.com.alibaba.csp.sentinel.slots.block.flow.FlowException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author frankchen
 */
public class SentinelFilter implements Filter {

    ISentinelListener listener;

    public SentinelFilter(ISentinelListener listener) {
        SentinelRuleManager.getInstance().setListener(listener);
        this.listener = listener;
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
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
                    this.listener.onFlowControlled(httpServletRequest);
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
                    this.listener.onDegraded(httpServletRequest);
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

    @Override
    public void destroy() {
    }
}
