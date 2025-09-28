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

package org.bithon.agent.observability.utils;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark to compare the performance of original vs optimized cleanupConnectionString method
 * 
 * @author frank.chen021@outlook.com
 * @date 2021/3/21 22:28
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class MiscUtilsBenchmark {

    private String[] testData;

    @Setup
    public void setup() {
        // Create various test cases that represent real-world connection strings
        testData = new String[]{
            "jdbc:mysql://localhost:3306/mydb?user=root&password=secret&useSSL=false",
            "jdbc:mysql://localhost:3306/mydb;user=root;password=secret;useSSL=false",
            "jdbc:mysql://localhost:3306/mydb?user=root&password=secret;useSSL=false",
            "jdbc:postgresql://localhost:5432/testdb?user=admin&password=admin123",
            "jdbc:oracle:thin:@localhost:1521:xe;user=scott;password=tiger",
            "jdbc:mysql://user:pass@host1:3306,host2:3307/database?useSSL=true&serverTimezone=UTC;autoReconnect=true",
            "jdbc:mysql://localhost:3306/mydb",
            "jdbc:postgresql://localhost:5432/testdb",
            "jdbc:oracle:thin:@localhost:1521:xe",
            "jdbc:sqlserver://localhost:1433;databaseName=testdb;user=sa;password=password123",
            "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "jdbc:mysql://localhost:3306/mydb?characterEncoding=UTF-8&useUnicode=true&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true",
            "jdbc:postgresql://localhost:5432/testdb?user=admin&password=admin123&ssl=true&sslmode=require",
            "jdbc:oracle:thin:@localhost:1521:xe;user=scott;password=tiger;oracle.net.CONNECT_TIMEOUT=10000",
            "jdbc:mysql://localhost:3306/mydb?user=root&password=secret&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8"
        };
    }

    /**
     * Original implementation using split() method
     */
    public static String cleanupConnectionStringOriginal(String connectionString) {
        return connectionString.split("\\?")[0].split(";")[0];
    }

    /**
     * Optimized implementation using indexOf() method
     */
    public static String cleanupConnectionStringOptimized(String connectionString) {
        if (connectionString == null || connectionString.isEmpty()) {
            return connectionString;
        }
        
        // Find the first occurrence of '?' or ';' and truncate at the earlier position
        int questionMarkIndex = connectionString.indexOf('?');
        int semicolonIndex = connectionString.indexOf(';');
        
        // If neither character is found, return the original string
        if (questionMarkIndex == -1 && semicolonIndex == -1) {
            return connectionString;
        }
        
        // Find the minimum index (earliest occurrence)
        int minIndex = questionMarkIndex == -1 ? semicolonIndex : 
                      semicolonIndex == -1 ? questionMarkIndex : 
                      Math.min(questionMarkIndex, semicolonIndex);
        
        return connectionString.substring(0, minIndex);
    }

    @Benchmark
    public String benchmarkOriginal() {
        StringBuilder result = new StringBuilder();
        for (String connectionString : testData) {
            result.append(cleanupConnectionStringOriginal(connectionString));
        }
        return result.toString();
    }

    @Benchmark
    public String benchmarkOptimized() {
        StringBuilder result = new StringBuilder();
        for (String connectionString : testData) {
            result.append(cleanupConnectionStringOptimized(connectionString));
        }
        return result.toString();
    }

    @Benchmark
    public String benchmarkCurrent() {
        StringBuilder result = new StringBuilder();
        for (String connectionString : testData) {
            result.append(MiscUtils.cleanupConnectionString(connectionString));
        }
        return result.toString();
    }

    /**
     * JMH Runner method for proper benchmarking
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MiscUtilsBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(5)
                .measurementIterations(10)
                .timeUnit(TimeUnit.NANOSECONDS)
                .build();

        new Runner(opt).run();
    }
}
