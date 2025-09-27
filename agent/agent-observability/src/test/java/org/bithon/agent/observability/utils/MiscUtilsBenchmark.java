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
import org.openjdk.jmh.runner.RunnerException;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark to compare the performance of original vs optimized cleanupConnectionString method
 * 
 * @author frank.chen021@outlook.com
 * @date 2025/09/27 21:28
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

    /**
     * Vectorized implementation processing 4 characters at a time
     */
    public static String cleanupConnectionStringVectorized(String connectionString) {
        if (connectionString == null || connectionString.isEmpty()) {
            return connectionString;
        }
        
        // Vectorized search for '?' or ';' - process 4 characters at a time
        int len = connectionString.length();
        int i = 0;
        
        // Process 4 characters at a time for vectorization
        for (; i < len - 3; i += 4) {
            char c1 = connectionString.charAt(i);
            char c2 = connectionString.charAt(i + 1);
            char c3 = connectionString.charAt(i + 2);
            char c4 = connectionString.charAt(i + 3);
            
            if (c1 == '?' || c1 == ';') {
                return connectionString.substring(0, i);
            }
            if (c2 == '?' || c2 == ';') {
                return connectionString.substring(0, i + 1);
            }
            if (c3 == '?' || c3 == ';') {
                return connectionString.substring(0, i + 2);
            }
            if (c4 == '?' || c4 == ';') {
                return connectionString.substring(0, i + 3);
            }
        }
        
        // Handle remaining characters (less than 4)
        for (; i < len; i++) {
            char c = connectionString.charAt(i);
            if (c == '?' || c == ';') {
                return connectionString.substring(0, i);
            }
        }
        
        // If neither character is found, return the original string
        return connectionString;
    }

    @Benchmark
    public String benchmarkOriginal() {
        String result = "";
        for (String connectionString : testData) {
            result += cleanupConnectionStringOriginal(connectionString);
        }
        return result;
    }

    @Benchmark
    public String benchmarkOptimized() {
        String result = "";
        for (String connectionString : testData) {
            result += cleanupConnectionStringOptimized(connectionString);
        }
        return result;
    }

    @Benchmark
    public String benchmarkCurrent() {
        String result = "";
        for (String connectionString : testData) {
            result += MiscUtils.cleanupConnectionString(connectionString);
        }
        return result;
    }

    @Benchmark
    public String benchmarkVectorized() {
        String result = "";
        for (String connectionString : testData) {
            result += cleanupConnectionStringVectorized(connectionString);
        }
        return result;
    }

    public static void main(String[] args) throws RunnerException {
        // Simple performance comparison without JMH runner
        MiscUtilsBenchmark benchmark = new MiscUtilsBenchmark();
        benchmark.setup();
        
        System.out.println("=== Performance Comparison: cleanupConnectionString ===");
        System.out.println("Warming up...");
        
        // Warmup
        for (int i = 0; i < 10000; i++) {
            benchmark.benchmarkOriginal();
            benchmark.benchmarkOptimized();
            benchmark.benchmarkCurrent();
            benchmark.benchmarkVectorized();
        }
        
        System.out.println("Running benchmarks...");
        
        // Benchmark original implementation
        long startTime = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            benchmark.benchmarkOriginal();
        }
        long originalTime = System.nanoTime() - startTime;
        
        // Benchmark optimized implementation
        startTime = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            benchmark.benchmarkOptimized();
        }
        long optimizedTime = System.nanoTime() - startTime;
        
        // Benchmark current implementation
        startTime = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            benchmark.benchmarkCurrent();
        }
        long currentTime = System.nanoTime() - startTime;
        
        // Benchmark vectorized implementation
        startTime = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            benchmark.benchmarkVectorized();
        }
        long vectorizedTime = System.nanoTime() - startTime;
        
        System.out.println("\nResults (100,000 iterations):");
        System.out.println(String.format(Locale.ROOT, "Original implementation:  %d ns (%.2f ms)", originalTime, originalTime / 1_000_000.0));
        System.out.println(String.format(Locale.ROOT, "Optimized implementation: %d ns (%.2f ms)", optimizedTime, optimizedTime / 1_000_000.0));
        System.out.println(String.format(Locale.ROOT, "Current implementation:   %d ns (%.2f ms)", currentTime, currentTime / 1_000_000.0));
        System.out.println(String.format(Locale.ROOT, "Vectorized implementation: %d ns (%.2f ms)", vectorizedTime, vectorizedTime / 1_000_000.0));
        
        double improvement = (double) originalTime / optimizedTime;
        double currentImprovement = (double) originalTime / currentTime;
        double vectorizedImprovement = (double) originalTime / vectorizedTime;
        System.out.println(String.format(Locale.ROOT, "\nPerformance improvement: %.2fx faster (indexOf)", improvement));
        System.out.println(String.format(Locale.ROOT, "Performance improvement: %.2fx faster (current)", currentImprovement));
        System.out.println(String.format(Locale.ROOT, "Performance improvement: %.2fx faster (vectorized)", vectorizedImprovement));
        
        // Verify results are identical
        System.out.println("\nVerifying correctness...");
        boolean allCorrect = true;
        for (String testString : benchmark.testData) {
            String original = cleanupConnectionStringOriginal(testString);
            String optimized = cleanupConnectionStringOptimized(testString);
            String current = MiscUtils.cleanupConnectionString(testString);
            String vectorized = cleanupConnectionStringVectorized(testString);
            
            if (!original.equals(optimized) || !original.equals(current) || !original.equals(vectorized)) {
                System.err.println(String.format(Locale.ROOT, "Mismatch for input: %s", testString));
                System.err.println(String.format(Locale.ROOT, "  Original:   %s", original));
                System.err.println(String.format(Locale.ROOT, "  Optimized:  %s", optimized));
                System.err.println(String.format(Locale.ROOT, "  Current:    %s", current));
                System.err.println(String.format(Locale.ROOT, "  Vectorized: %s", vectorized));
                allCorrect = false;
            }
        }
        
        if (allCorrect) {
            System.out.println("✓ All implementations produce identical results");
        } else {
            System.out.println("✗ Implementation mismatch detected!");
        }
    }
}
