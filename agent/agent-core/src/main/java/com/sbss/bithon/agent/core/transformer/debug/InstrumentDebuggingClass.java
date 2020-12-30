/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.sbss.bithon.agent.core.transformer.debug;


import com.sbss.bithon.agent.core.config.CoreConfig;
import shaded.net.bytebuddy.dynamic.DynamicType;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class InstrumentDebuggingClass {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstrumentDebuggingClass.class);

    private static InstrumentDebuggingClass INSTANCE;


    private File debuggingClassesRootPath;

    public static void init(String path) {
        INSTANCE = new InstrumentDebuggingClass(path);
    }

    public static InstrumentDebuggingClass getInstance() {
        return INSTANCE;
    }

    public InstrumentDebuggingClass(String outputPath) {
        setDebugClassRootPath(outputPath);
    }

    public void setDebugClassRootPath(String path) {
        if (debuggingClassesRootPath == null) {
            try {
                debuggingClassesRootPath = new File(path, "/debugging");
                if (!debuggingClassesRootPath.exists()) {
                    debuggingClassesRootPath.mkdir();
                }
            } catch (Exception e) {
                LOGGER.error("log error", e);
            }
        }
    }

    public void log(DynamicType dynamicType) {
        if (!CoreConfig.getInstance().isDebug()) {
            return;
        }

        synchronized (this) {
            try {
                try {
                    dynamicType.saveIn(debuggingClassesRootPath);
                } catch (IOException e) {
                    LOGGER.error("Can't save class {} to file." + dynamicType.getTypeDescription().getActualName(), e);
                }
            } catch (Throwable t) {
                LOGGER.error("Save debugging classes fail.", t);
            }
        }
    }
}
