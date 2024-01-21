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

package org.bithon.server.alerting.common.utils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

import java.io.StringWriter;
import java.io.Writer;

/**
 * @author frank.chen021@outlook.com
 * @date 2020-03-16 14:31:36
 */
public class FreeMarkerUtil {

    private static final Configuration CFG;

    static {
        CFG = new Configuration(Configuration.VERSION_2_3_23);
        CFG.setDefaultEncoding("utf-8");
        CFG.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        CFG.setClassForTemplateLoading(FreeMarkerUtil.class, "/");
    }

    public static String applyTemplate(String templateName,
                                       Object dataModel) throws Exception {

        Template template = CFG.getTemplate(templateName);
        try (Writer writer = new StringWriter()) {
            template.process(dataModel, writer);
            return writer.toString();
        }
    }
}
