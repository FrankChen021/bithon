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

package org.bithon.server.pipeline.common.transformer;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.input.IInputRow;

/**
 * @author Frank Chen
 * @date 22/1/24 10:49 pm
 */
@Slf4j
public class ExceptionSafeTransformer implements ITransformer {
    private final ITransformer delegate;

    /**
     * There are too many spans, suppress exception logs to avoid too many logs
     */
    private long lastLogTimestamp = System.currentTimeMillis();
    private String lastException;

    public ExceptionSafeTransformer(ITransformer delegate) {
        this.delegate = delegate;
    }

    @Override
    public TransformResult transform(IInputRow inputRow) throws TransformException {
        try {
            return delegate.transform(inputRow);
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            if (now - lastLogTimestamp < 5_000) {
                return TransformResult.CONTINUE;
            }

            log.error(StringUtils.format("Fail to transform, message [%s], Span [%s]", e.getMessage(), inputRow),
                      e.getClass().getName().equals(this.lastException) ? null : e);

            this.lastLogTimestamp = now;
            this.lastException = e.getClass().getName();
            return TransformResult.CONTINUE;
        }
    }
}
