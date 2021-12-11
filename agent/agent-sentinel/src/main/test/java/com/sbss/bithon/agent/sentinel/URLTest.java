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

package com.sbss.bithon.agent.sentinel;

import org.junit.Assert;
import org.junit.Test;

public class URLTest {

    @Test
    public void test() {
        IUrlMatcher matcher = IUrlMatcher.createMatcher("/api/test");
        Assert.assertFalse(matcher.matches("/api"));

        Assert.assertTrue(matcher.matches("/api/test"));

        Assert.assertFalse(matcher.matches("/api/test/id"));

        Assert.assertFalse(matcher.matches("api/test"));

        Assert.assertFalse(matcher.matches("/api/test/"));
    }

    @Test
    public void test2() {
        IUrlMatcher matcher = IUrlMatcher.createMatcher("/doom/*/op/unifyupload");

        Assert.assertTrue(matcher.matches("/doom/*/op/unifyupload"));
        Assert.assertTrue(matcher.matches("/doom/1/op/unifyupload"));
        Assert.assertTrue(matcher.matches("/doom/2-2/op/unifyupload"));
        Assert.assertTrue(matcher.matches("/doom/ab/op/unifyupload"));

        Assert.assertFalse(matcher.matches("/doom/1/op2/unifyupload"));
    }

    @Test
    public void test3() {
        IUrlMatcher matcher = IUrlMatcher.createMatcher("/doom/op/*");
        Assert.assertTrue(matcher.matches("/doom/op"));
        Assert.assertTrue(matcher.matches("/doom/op/"));
        Assert.assertTrue(matcher.matches("/doom/op/unifyupload"));
        Assert.assertTrue(matcher.matches("/doom/op/unifyupload/1"));
    }

    @Test
    public void test4() {
        IUrlMatcher matcher = IUrlMatcher.createMatcher("*");
        Assert.assertTrue(matcher.matches("/doom/op"));
        Assert.assertTrue(matcher.matches("/doom/op/"));
        Assert.assertTrue(matcher.matches("/doom/op/unifyupload"));
        Assert.assertTrue(matcher.matches("/doom/op/unifyupload/1"));
    }
}
