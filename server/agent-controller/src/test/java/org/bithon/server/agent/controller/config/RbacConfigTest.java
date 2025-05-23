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

package org.bithon.server.agent.controller.config;

import org.bithon.server.agent.controller.rbac.Operation;
import org.bithon.server.agent.controller.rbac.Permission;
import org.bithon.server.agent.controller.rbac.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 22/1/25 5:49 pm
 */
public class RbacConfigTest {
    @Test
    public void test_SinglePermissionRule() {
        RbacConfig config = new RbacConfig();
        config.setUsers(List.of(User.builder()
                                    .name("user1")
                                    .permissions(List.of(new Permission(Operation.RW, "application", "dataSource1")))
                                    .build()));

        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "application", "dataSource1"));
        Assertions.assertTrue(config.isPermitted(Operation.READ, "user1", "application", "dataSource1"));
        Assertions.assertTrue(config.isPermitted(Operation.RW, "user1", "application", "dataSource1"));

        Assertions.assertFalse(config.isPermitted(Operation.WRITE, "user1", "application1", "dataSource1"));
        Assertions.assertFalse(config.isPermitted(Operation.WRITE, "user1", "application", "dataSource2"));
        Assertions.assertFalse(config.isPermitted(Operation.WRITE, "user2", "application", "dataSource2"));
    }

    @Test
    public void test_SinglePermissionRule_RO() {
        RbacConfig config = new RbacConfig();
        config.setUsers(List.of(User.builder()
                                    .name("user1")
                                    .permissions(List.of(new Permission(Operation.READ, "application", "dataSource1")))
                                    .build()));

        Assertions.assertFalse(config.isPermitted(Operation.WRITE, "user1", "application", "dataSource1"));
        Assertions.assertTrue(config.isPermitted(Operation.READ, "user1", "application", "dataSource1"));
        Assertions.assertTrue(config.isPermitted(Operation.RW, "user1", "application", "dataSource1"));
    }

    @Test
    public void test_SinglePermissionRule_WO() {
        RbacConfig config = new RbacConfig();
        config.setUsers(List.of(User.builder()
                                    .name("user1")
                                    .permissions(List.of(new Permission(Operation.WRITE, "application", "dataSource1")))
                                    .build()));

        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "application", "dataSource1"));
        Assertions.assertFalse(config.isPermitted(Operation.READ, "user1", "application", "dataSource1"));
        Assertions.assertTrue(config.isPermitted(Operation.RW, "user1", "application", "dataSource1"));
    }

    @Test
    public void test_WildMatchAgainstApplication_StartWith() {
        RbacConfig config = new RbacConfig();
        config.setUsers(List.of(User.builder()
                                    .name("user1")
                                    .permissions(List.of(new Permission(Operation.RW, "app*", "dataSource1")))
                                    .build()));

        Assertions.assertFalse(config.isPermitted(Operation.WRITE, "user1", "ap", "dataSource1"));
        Assertions.assertFalse(config.isPermitted(Operation.WRITE, "user1", "apl", "dataSource1"));
        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "app", "dataSource1"));
        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "appp", "dataSource1"));
    }

    @Test
    public void test_WildMatchAgainstApplication_EndWith() {
        RbacConfig config = new RbacConfig();
        config.setUsers(List.of(User.builder()
                                    .name("user1")
                                    .permissions(List.of(new Permission(Operation.RW, "*app", "dataSource1")))
                                    .build()));

        Assertions.assertFalse(config.isPermitted(Operation.WRITE, "user1", "ap", "dataSource1"));
        Assertions.assertFalse(config.isPermitted(Operation.WRITE, "user1", "apl", "dataSource1"));
        Assertions.assertFalse(config.isPermitted(Operation.WRITE, "user1", "bap", "dataSource1"));
        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "app", "dataSource1"));
        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "bapp", "dataSource1"));
    }

    @Test
    public void test_WildMatchAgainstDataSource_Any() {
        RbacConfig config = new RbacConfig();
        config.setUsers(List.of(User.builder()
                                    .name("user1")
                                    .permissions(List.of(new Permission(Operation.RW, "*", "dataSource1")))
                                    .build()));

        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "ap", "dataSource1"));
        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "apl", "dataSource1"));
    }

    @Test
    public void test_WildMatchAgainstDataSource_Contains() {
        RbacConfig config = new RbacConfig();
        config.setUsers(List.of(User.builder()
                                    .name("user1")
                                    .permissions(List.of(new Permission(Operation.RW, "*bithon*", "dataSource1")))
                                    .build()));

        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "app-bithon-controller", "dataSource1"));
        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "bithon", "dataSource1"));
        Assertions.assertFalse(config.isPermitted(Operation.WRITE, "user1", "app-bitho-controller", "dataSource1"));
    }

    @Test
    public void test_WildMatchAgainstDataSource_StartWith() {
        RbacConfig config = new RbacConfig();
        config.setUsers(List.of(User.builder()
                                    .name("user1")
                                    .permissions(List.of(new Permission(Operation.RW, "app", "data*")))
                                    .build()));

        Assertions.assertFalse(config.isPermitted(Operation.WRITE, "user1", "app", "dat"));
        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "app", "data"));
        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "app", "data1"));
    }

    @Test
    public void test_WildMatchAgainstDataSource_EndWith() {
        RbacConfig config = new RbacConfig();
        config.setUsers(List.of(User.builder()
                                    .name("user1")
                                    .permissions(List.of(new Permission(Operation.RW, "app", "*data")))
                                    .build()));

        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "app", "data"));
        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "app", "adata"));
        Assertions.assertFalse(config.isPermitted(Operation.WRITE, "user1", "app", "dat"));
    }

    @Test
    public void test_WildMatchAgainstApplication_Any() {
        RbacConfig config = new RbacConfig();
        config.setUsers(List.of(User.builder()
                                    .name("user1")
                                    .permissions(List.of(new Permission(Operation.RW, "app", "*")))
                                    .build()));

        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "app", "data1"));
        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "app", "data2"));
    }

    @Test
    public void test_WildMatchAgainstApplication_Null() {
        RbacConfig config = new RbacConfig();
        config.setUsers(List.of(User.builder()
                                    .name("user1")
                                    .permissions(List.of(new Permission(Operation.RW, "app", null)))
                                    .build()));

        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "app", "data1"));
        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "app", "data2"));
    }

    @Test
    public void test_MultipleRules() {
        RbacConfig config = new RbacConfig();
        config.setUsers(List.of(User.builder()
                                    .name("user1")
                                    .permissions(List.of(new Permission(Operation.RW, "app1", "dataSource1"),
                                                         new Permission(Operation.READ, "app2", "dataSource2")))
                                    .build(),
                                User.builder()
                                    .name("user2")
                                    .permissions(List.of(new Permission(Operation.RW, "app1", "dataSource1"),
                                                         new Permission(Operation.READ, "app2", "dataSource2")))
                                    .build()
        ));

        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "app1", "dataSource1"));
        Assertions.assertTrue(config.isPermitted(Operation.READ, "user1", "app2", "dataSource2"));
        Assertions.assertFalse(config.isPermitted(Operation.RW, "user1", "app2", "dataSource1"));
        Assertions.assertFalse(config.isPermitted(Operation.RW, "user1", "app3", "dataSource1"));

        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user2", "app1", "dataSource1"));
        Assertions.assertFalse(config.isPermitted(Operation.WRITE, "user2", "app3", "dataSource1"));

        Assertions.assertFalse(config.isPermitted(Operation.WRITE, "user3", "app3", "dataSource1"));
    }

    @Test
    public void test_AnyUser() {
        RbacConfig config = new RbacConfig();
        config.setUsers(List.of(User.builder()
                                    .name("*")
                                    .permissions(List.of(new Permission(Operation.RW, "application", "dataSource1")))
                                    .build()));

        Assertions.assertTrue(config.isPermitted(Operation.WRITE, "user1", "application", "dataSource1"));
        Assertions.assertTrue(config.isPermitted(Operation.READ, "user2", "application", "dataSource1"));
        Assertions.assertTrue(config.isPermitted(Operation.RW, "user3", "application", "dataSource1"));

        Assertions.assertFalse(config.isPermitted(Operation.WRITE, "user1", "application1", "dataSource1"));
    }
}
