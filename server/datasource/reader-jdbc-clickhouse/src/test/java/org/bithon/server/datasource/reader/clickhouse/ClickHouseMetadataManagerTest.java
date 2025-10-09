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

package org.bithon.server.datasource.reader.clickhouse;

import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Frank Chen
 * @date 29/1/24 11:26 am
 */
public class ClickHouseMetadataManagerTest {

    private DSLContext dslContext;
    private ClickHouseMetadataManager metadataManager;

    @BeforeEach
    public void setup() {
        dslContext = mock(DSLContext.class);
        metadataManager = new ClickHouseMetadataManager(dslContext);
    }

    @Test
    public void testGetOrderByExpression_SimpleColumn() {
        // Mock the database response
        Record record = mock(Record.class);
        when(record.get(0, String.class)).thenReturn("MergeTree() ORDER BY (timestamp)");

        Result<Record> result = mock(Result.class);
        when(result.isEmpty()).thenReturn(false);
        when(result.get(0)).thenReturn(record);

        when(dslContext.fetch(ArgumentMatchers.anyString())).thenReturn(result);

        // Test
        List<IExpression> expressions = metadataManager.getOrderByExpression("test_db", "test_table");

        // Verify
        Assertions.assertEquals(1, expressions.size());
        Assertions.assertEquals("timestamp", expressions.get(0).serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testGetOrderByExpression_MultipleColumns() {
        // Mock the database response
        Record record = mock(Record.class);
        when(record.get(0, String.class)).thenReturn("MergeTree() ORDER BY (appName, timestamp)");

        Result<Record> result = mock(Result.class);
        when(result.isEmpty()).thenReturn(false);
        when(result.get(0)).thenReturn(record);

        when(dslContext.fetch(ArgumentMatchers.anyString())).thenReturn(result);

        // Test
        List<IExpression> expressions = metadataManager.getOrderByExpression("test_db", "test_table");

        // Verify
        Assertions.assertEquals(2, expressions.size());
        Assertions.assertEquals("appName", expressions.get(0).serializeToText(IdentifierQuotaStrategy.NONE));
        Assertions.assertEquals("timestamp", expressions.get(1).serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testGetOrderByExpression_WithFunction() {
        // Mock the database response
        Record record = mock(Record.class);
        when(record.get(0, String.class)).thenReturn("MergeTree() ORDER BY ( toStartOfMinute( timestamp ), appName)");

        Result<Record> result = mock(Result.class);
        when(result.isEmpty()).thenReturn(false);
        when(result.get(0)).thenReturn(record);

        when(dslContext.fetch(ArgumentMatchers.anyString())).thenReturn(result);

        // Test
        List<IExpression> expressions = metadataManager.getOrderByExpression("test_db", "test_table");

        // Verify
        Assertions.assertEquals(2, expressions.size());
        Assertions.assertEquals("toStartOfMinute(timestamp)", expressions.get(0).serializeToText(IdentifierQuotaStrategy.NONE));
        Assertions.assertEquals("appName", expressions.get(1).serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testGetOrderByExpression_DistributedTable() {
        // Mock the database response for distributed table
        Record distributedRecord = mock(Record.class);
        when(distributedRecord.get(0, String.class))
            .thenReturn("Distributed(my_cluster, 'local_db', 'local_table', rand())");

        Result<Record> distributedResult = mock(Result.class);
        when(distributedResult.isEmpty()).thenReturn(false);
        when(distributedResult.get(0)).thenReturn(distributedRecord);

        // Mock the database response for local table
        Record localRecord = mock(Record.class);
        when(localRecord.get(0, String.class)).thenReturn("MergeTree() ORDER BY (timestamp, appName)");

        Result<Record> localResult = mock(Result.class);
        when(localResult.isEmpty()).thenReturn(false);
        when(localResult.get(0)).thenReturn(localRecord);

        // Setup mock to return different results based on the SQL query
        // First call queries the distributed table
        when(dslContext.fetch("SELECT engine_full FROM system.tables WHERE database = 'dist_db' AND name = 'dist_table'"))
            .thenReturn(distributedResult);
        // Second call queries the local table
        when(dslContext.fetch("SELECT engine_full FROM system.tables WHERE database = 'local_db' AND name = 'local_table'"))
            .thenReturn(localResult);

        // Test
        List<IExpression> expressions = metadataManager.getOrderByExpression("dist_db", "dist_table");

        // Verify - should have queried twice (distributed then local)
        verify(dslContext, times(2)).fetch(ArgumentMatchers.anyString());
        Assertions.assertEquals(2, expressions.size());
        Assertions.assertEquals("timestamp", expressions.get(0).serializeToText(IdentifierQuotaStrategy.NONE));
        Assertions.assertEquals("appName", expressions.get(1).serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testGetOrderByExpression_DistributedTable_WithQuotedNames() {
        // Test distributed table with single-quoted database and table names
        Record distributedRecord = mock(Record.class);
        when(distributedRecord.get(0, String.class))
            .thenReturn("Distributed(my_cluster, 'metrics_db', 'events_local', rand())");

        Result<Record> distributedResult = mock(Result.class);
        when(distributedResult.isEmpty()).thenReturn(false);
        when(distributedResult.get(0)).thenReturn(distributedRecord);

        // Mock the local table
        Record localRecord = mock(Record.class);
        when(localRecord.get(0, String.class))
            .thenReturn("ReplicatedMergeTree('/clickhouse/tables/{shard}/events_local', '{replica}') ORDER BY (timestamp, instanceName, traceId)");

        Result<Record> localResult = mock(Result.class);
        when(localResult.isEmpty()).thenReturn(false);
        when(localResult.get(0)).thenReturn(localRecord);

        // Setup mocks
        when(dslContext.fetch("SELECT engine_full FROM system.tables WHERE database = 'metrics' AND name = 'events_dist'"))
            .thenReturn(distributedResult);
        when(dslContext.fetch("SELECT engine_full FROM system.tables WHERE database = 'metrics_db' AND name = 'events_local'"))
            .thenReturn(localResult);

        // Test
        List<IExpression> expressions = metadataManager.getOrderByExpression("metrics", "events_dist");

        // Verify
        verify(dslContext, times(2)).fetch(ArgumentMatchers.anyString());
        Assertions.assertEquals(3, expressions.size());
        Assertions.assertEquals("timestamp", expressions.get(0).serializeToText(IdentifierQuotaStrategy.NONE));
        Assertions.assertEquals("instanceName", expressions.get(1).serializeToText(IdentifierQuotaStrategy.NONE));
        Assertions.assertEquals("traceId", expressions.get(2).serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testGetOrderByExpression_DistributedTable_WithoutQuotes() {
        // Test distributed table without quotes
        Record distributedRecord = mock(Record.class);
        when(distributedRecord.get(0, String.class))
            .thenReturn("Distributed(my_cluster, local_db, local_table, rand())");

        Result<Record> distributedResult = mock(Result.class);
        when(distributedResult.isEmpty()).thenReturn(false);
        when(distributedResult.get(0)).thenReturn(distributedRecord);

        // Mock the local table
        Record localRecord = mock(Record.class);
        when(localRecord.get(0, String.class))
            .thenReturn("MergeTree() PARTITION BY toYYYYMM(timestamp) ORDER BY (appName, timestamp DESC)");

        Result<Record> localResult = mock(Result.class);
        when(localResult.isEmpty()).thenReturn(false);
        when(localResult.get(0)).thenReturn(localRecord);

        // Setup mocks
        when(dslContext.fetch("SELECT engine_full FROM system.tables WHERE database = 'production' AND name = 'metrics_dist'"))
            .thenReturn(distributedResult);
        when(dslContext.fetch("SELECT engine_full FROM system.tables WHERE database = 'local_db' AND name = 'local_table'"))
            .thenReturn(localResult);

        // Test
        List<IExpression> expressions = metadataManager.getOrderByExpression("production", "metrics_dist");

        // Verify
        verify(dslContext, times(2)).fetch(ArgumentMatchers.anyString());
        Assertions.assertEquals(2, expressions.size());
        Assertions.assertEquals("appName", expressions.get(0).serializeToText(IdentifierQuotaStrategy.NONE));
        // Note: DESC is stripped during parsing
        Assertions.assertEquals("timestamp", expressions.get(1).serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testGetOrderByExpression_TableNotFound() {
        // Mock empty result
        Result<Record> result = mock(Result.class);
        when(result.isEmpty()).thenReturn(true);

        when(dslContext.fetch(ArgumentMatchers.anyString())).thenReturn(result);

        // Test & Verify
        RuntimeException exception = Assertions.assertThrows(
            RuntimeException.class,
            () -> metadataManager.getOrderByExpression("test_db", "non_existent_table")
        );

        Assertions.assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    public void testGetOrderByExpression_NoOrderBy() {
        // Mock the database response without ORDER BY
        Record record = mock(Record.class);
        when(record.get(0, String.class)).thenReturn("MergeTree()");

        Result<Record> result = mock(Result.class);
        when(result.isEmpty()).thenReturn(false);
        when(result.get(0)).thenReturn(record);

        when(dslContext.fetch(ArgumentMatchers.anyString())).thenReturn(result);

        // Test & Verify
        RuntimeException exception = Assertions.assertThrows(
            RuntimeException.class,
            () -> metadataManager.getOrderByExpression("test_db", "test_table")
        );

        Assertions.assertTrue(exception.getMessage().contains("No ORDER BY"));
    }

    @Test
    public void testGetOrderByExpression_CachingWorks() {
        // Mock the database response
        Record record = mock(Record.class);
        when(record.get(0, String.class)).thenReturn("MergeTree() ORDER BY (timestamp)");

        Result<Record> result = mock(Result.class);
        when(result.isEmpty()).thenReturn(false);
        when(result.get(0)).thenReturn(record);

        when(dslContext.fetch(ArgumentMatchers.anyString())).thenReturn(result);

        // Test - call twice with same parameters
        List<IExpression> expressions1 = metadataManager.getOrderByExpression("test_db", "test_table");
        List<IExpression> expressions2 = metadataManager.getOrderByExpression("test_db", "test_table");

        // Verify - should only query database once due to caching
        verify(dslContext, times(1)).fetch(ArgumentMatchers.anyString());
        Assertions.assertEquals(expressions1.size(), expressions2.size());
    }
}

