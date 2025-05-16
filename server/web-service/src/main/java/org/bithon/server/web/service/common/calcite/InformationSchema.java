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

package org.bithon.server.web.service.common.calcite;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.apache.calcite.DataContext;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Linq4j;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Statistic;
import org.apache.calcite.schema.Statistics;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 1/3/23 3:00 pm
 */
public class InformationSchema extends AbstractSchema {

    private final SchemaPlus rootSchema;
    private final ImmutableMap<String, Table> tableMap;

    public InformationSchema(SchemaPlus rootSchema) {
        this.rootSchema = rootSchema;
        this.tableMap = ImmutableMap.of(
            "SCHEMATA", new SchemataTable(),
            "TABLES", new TablesTable(),
            "COLUMNS", new ColumnsTable()
        );
    }

    @Override
    protected Map<String, Table> getTableMap() {
        return tableMap;
    }

    private class SchemataTable extends AbstractTable implements ScannableTable {
        private RelDataType type;

        @Override
        public Enumerable<Object[]> scan(final DataContext root) {
            final FluentIterable<Object[]> results = FluentIterable
                .from(rootSchema.getSubSchemaNames())
                .transform(
                    schemaName -> {
                        final SchemaPlus subSchema = rootSchema.getSubSchema(schemaName);
                        return new Object[]{
                            subSchema.getName(), // CATALOG_NAME
                            subSchema.getName(), // SCHEMA_NAME
                            null, // SCHEMA_OWNER
                            null, // DEFAULT_CHARACTER_SET_CATALOG
                            null, // DEFAULT_CHARACTER_SET_SCHEMA
                            null, // DEFAULT_CHARACTER_SET_NAME
                            null  // SQL_PATH
                        };
                    }
                );

            return Linq4j.asEnumerable(results);
        }

        @Override
        public RelDataType getRowType(final RelDataTypeFactory typeFactory) {
            if (type == null) {
                type = new RelDataTypeFactory.Builder(typeFactory)
                    .add("CATALOG_NAME", SqlTypeName.VARCHAR)
                    .add("SCHEMA_NAME", SqlTypeName.VARCHAR)
                    .add("SCHEMA_OWNER", SqlTypeName.VARCHAR)
                    .add("DEFAULT_CHARACTER_SET_CATALOG", SqlTypeName.VARCHAR)
                    .add("DEFAULT_CHARACTER_SET_SCHEMA", SqlTypeName.VARCHAR)
                    .add("DEFAULT_CHARACTER_SET_NAME", SqlTypeName.VARCHAR)
                    .add("SQL_PATH", SqlTypeName.VARCHAR)
                    .build();
            }
            return type;
        }

        @Override
        public Statistic getStatistic() {
            return Statistics.UNKNOWN;
        }

        @Override
        public TableType getJdbcTableType() {
            return TableType.SYSTEM_TABLE;
        }
    }

    private class TablesTable extends AbstractTable implements ScannableTable {
        private RelDataType type;

        @Override
        public Enumerable<Object[]> scan(final DataContext root) {
            final FluentIterable<Object[]> results = FluentIterable
                .from(rootSchema.getSubSchemaNames())
                .transformAndConcat(
                    schemaName -> {
                        final SchemaPlus subSchema = rootSchema.getSubSchema(schemaName);
                        return FluentIterable.from(subSchema.getTableNames()).transform(
                            tableName -> {
                                final Table table = subSchema.getTable(tableName);

                                return new Object[]{
                                    schemaName, // TABLE_CATALOG
                                    schemaName, // TABLE_SCHEMA
                                    tableName, // TABLE_NAME
                                    table.getJdbcTableType().toString(), // TABLE_TYPE
                                    "NO", // IS_JOINABLE
                                    "NO"  // BROADCAST
                                };
                            }
                        );
                    }
                );

            return Linq4j.asEnumerable(results);
        }

        @Override
        public RelDataType getRowType(final RelDataTypeFactory typeFactory) {
            if (type == null) {
                type = new RelDataTypeFactory.Builder(typeFactory)
                    .add("TABLE_CATALOG", SqlTypeName.VARCHAR)
                    .add("TABLE_SCHEMA", SqlTypeName.VARCHAR)
                    .add("TABLE_NAME", SqlTypeName.VARCHAR)
                    .add("TABLE_TYPE", SqlTypeName.VARCHAR)
                    .add("IS_JOINABLE", SqlTypeName.VARCHAR)
                    .add("IS_BROADCAST", SqlTypeName.VARCHAR)
                    .build();
            }
            return type;
        }

        @Override
        public Statistic getStatistic() {
            return Statistics.UNKNOWN;
        }

        @Override
        public TableType getJdbcTableType() {
            return TableType.SYSTEM_TABLE;
        }
    }

    private class ColumnsTable extends AbstractTable implements ScannableTable {
        private RelDataType type;

        @Override
        public Enumerable<Object[]> scan(final DataContext root) {
            final FluentIterable<Object[]> results = FluentIterable
                .from(rootSchema.getSubSchemaNames())
                .transformAndConcat(
                    schemaName -> {
                        final SchemaPlus subSchema = rootSchema.getSubSchema(schemaName);
                        final JavaTypeFactoryImpl typeFactory = new JavaTypeFactoryImpl();
                        return Iterables.concat(FluentIterable.from(subSchema.getTableNames())
                                                              .transform(tableName -> getColumns(
                                                                  schemaName,
                                                                  tableName,
                                                                  subSchema.getTable(tableName),
                                                                  typeFactory
                                                              )));
                    }
                );

            return Linq4j.asEnumerable(results);
        }

        @Override
        public RelDataType getRowType(final RelDataTypeFactory typeFactory) {
            if (type == null) {
                type = new RelDataTypeFactory.Builder(typeFactory)
                    .add("TABLE_CATALOG", SqlTypeName.VARCHAR)
                    .add("TABLE_SCHEMA", SqlTypeName.VARCHAR)
                    .add("TABLE_NAME", SqlTypeName.VARCHAR)
                    .add("COLUMN_NAME", SqlTypeName.VARCHAR)
                    .add("ORDINAL_POSITION", SqlTypeName.VARCHAR)
                    .add("COLUMN_DEFAULT", SqlTypeName.VARCHAR)
                    .add("IS_NULLABLE", SqlTypeName.VARCHAR)
                    .add("DATA_TYPE", SqlTypeName.VARCHAR)
                    .add("CHARACTER_MAXIMUM_LENGTH", SqlTypeName.VARCHAR)
                    .add("CHARACTER_OCTET_LENGTH", SqlTypeName.VARCHAR)
                    .add("NUMERIC_PRECISION", SqlTypeName.VARCHAR)
                    .add("NUMERIC_PRECISION_RADIX", SqlTypeName.VARCHAR)
                    .add("NUMERIC_SCALE", SqlTypeName.VARCHAR)
                    .add("DATETIME_PRECISION", SqlTypeName.VARCHAR)
                    .add("CHARACTER_SET_NAME", SqlTypeName.VARCHAR)
                    .add("COLLATION_NAME", SqlTypeName.VARCHAR)
                    .add("JDBC_TYPE", SqlTypeName.INTEGER)
                    .build();
            }
            return type;
        }

        @Override
        public Statistic getStatistic() {
            return Statistics.UNKNOWN;
        }

        @Override
        public TableType getJdbcTableType() {
            return TableType.SYSTEM_TABLE;
        }

        @Nullable
        private Iterable<Object[]> getColumns(
            final String schemaName,
            final String tableName,
            final Table table,
            final RelDataTypeFactory typeFactory
        ) {
            if (table == null) {
                return null;
            }

            return FluentIterable
                .from(table.getRowType(typeFactory).getFieldList())
                .transform(
                    field -> {
                        final RelDataType type = field.getType();
                        boolean isNumeric = SqlTypeName.NUMERIC_TYPES.contains(type.getSqlTypeName());
                        boolean isCharacter = SqlTypeName.CHAR_TYPES.contains(type.getSqlTypeName());
                        boolean isDateTime = SqlTypeName.DATETIME_TYPES.contains(type.getSqlTypeName());
                        final String typeName = type.getSqlTypeName().toString();

                        return new Object[]{
                            schemaName, // TABLE_CATALOG
                            schemaName, // TABLE_SCHEMA
                            tableName, // TABLE_NAME
                            field.getName(), // COLUMN_NAME
                            String.valueOf(field.getIndex()), // ORDINAL_POSITION
                            "", // COLUMN_DEFAULT
                            type.isNullable() ? "YES" : "NO",
                            typeName, // DATA_TYPE
                            null, // CHARACTER_MAXIMUM_LENGTH
                            null, // CHARACTER_OCTET_LENGTH
                            isNumeric ? String.valueOf(type.getPrecision()) : null, // NUMERIC_PRECISION
                            isNumeric ? "10" : null, // NUMERIC_PRECISION_RADIX
                            isNumeric ? String.valueOf(type.getScale()) : null, // NUMERIC_SCALE
                            isDateTime ? String.valueOf(type.getPrecision()) : null, // DATETIME_PRECISION
                            isCharacter ? type.getCharset().name() : null, // CHARACTER_SET_NAME
                            isCharacter ? type.getCollation().getCollationName() : null, // COLLATION_NAME
                            type.getSqlTypeName().getJdbcOrdinal() // JDBC_TYPE (Druid extension)
                        };
                    }
                );
        }
    }
}
