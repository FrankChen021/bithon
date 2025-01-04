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

package org.bithon.server.storage.jdbc.common.dialect;

import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.MapAccessExpression;
import org.bithon.component.commons.utils.StringUtils;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/5/9 21:54
 */
public class MapAccessExpressionTransformer {

    /**
     * This transformer turns the map access expression into a LIKE comparator for those stores Map object as plain text.
     * It's used by H2, MySQL dialects
     */
    public static IExpression transform(ConditionalExpression expression) {
        MapAccessExpression mapAccessExpression = (MapAccessExpression) expression.getLhs();
        if (!(mapAccessExpression.getMap() instanceof IdentifierExpression)) {
            throw new UnsupportedOperationException(StringUtils.format("Map access expression [%s] only allows on identifier", mapAccessExpression.serializeToText()));
        }

        String mapName = ((IdentifierExpression) mapAccessExpression.getMap()).getIdentifier();
        String key = mapAccessExpression.getKey();
        String value = ((LiteralExpression<?>) expression.getRhs()).getValue().toString();

        if (!(expression instanceof ComparisonExpression.EQ)) {
            // Since we store JSON formatted string in H2, it's hard to implement other operators except EQ
            // And because these DBs now are only for test, we simplify disallow other operators
            throw new UnsupportedOperationException(StringUtils.format("H2 does not support operators other than EQ on column [%s]", mapName));
        }

        return new LikeOperator(new IdentifierExpression(mapName),
                                LiteralExpression.ofString("%\"" + key + "\":\"" + value + "\"%"));
    }
}
