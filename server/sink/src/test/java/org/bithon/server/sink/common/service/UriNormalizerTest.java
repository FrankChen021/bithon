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

package org.bithon.server.sink.common.service;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 14/2/22 8:53 PM
 */
public class UriNormalizerTest {

    private final UriNormalizer normalizer = new UriNormalizer();

    @Test
    public void testNoChange() {
        UriNormalizer.NormalizedResult r = normalizer.normalize("test-app", "/");
        Assert.assertEquals("/", r.getUri());
        Assert.assertFalse(r.isNormalized());
    }

    @Test
    public void testUriWithParameters() {
        UriNormalizer.NormalizedResult r = normalizer.normalize("test-app", "/?query=select+1");
        Assert.assertEquals("/", r.getUri());
        Assert.assertTrue(r.isNormalized());
    }

    @Test
    public void testFullUri() {
        UriNormalizer.NormalizedResult r = normalizer.normalize("test-app", "http://localhost/?query=select+1");
        Assert.assertEquals("http://localhost", r.getUri());
        Assert.assertTrue(r.isNormalized());
    }
}
