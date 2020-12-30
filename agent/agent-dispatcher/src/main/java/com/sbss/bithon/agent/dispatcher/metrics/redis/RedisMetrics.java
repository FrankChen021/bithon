package com.sbss.bithon.agent.dispatcher.metrics.redis;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Description : redis 计数器存储
 * <br>Date: 17/11/1
 *
 * @author 马至远
 */
public class RedisMetrics {
    /**
     * redis host + port
     */
    private String hostPort;
    /**
     * 数据库编号
     */
    @Deprecated
    private int db;
    /**
     * 客户端请求发送总耗时 (连接建立+发送命令)
     */
    private AtomicLong writeCostTime = new AtomicLong(0);
    /**
     * 客户端响应接受总耗时
     */
    private AtomicLong readCostTime = new AtomicLong(0);
    /**
     * 总请求数
     */
    private AtomicInteger count = new AtomicInteger(0);
    /**
     * 总失败数
     */
    private AtomicInteger failureCount = new AtomicInteger(0);
    /**
     * 读入字节
     */
    private AtomicLong bytesIn = new AtomicLong(0);
    /**
     * 写出字节
     */
    private AtomicLong bytesOut = new AtomicLong(0);

    public RedisMetrics(String hostPort) {
        this.hostPort = hostPort;
    }

    /**
     * 记录add客户端请求耗时
     *
     * @param writeCostTime 向server发送请求的时间
     */
    public void addWrite(long writeCostTime, int failureCount) {
        this.writeCostTime.addAndGet(writeCostTime);
        this.failureCount.addAndGet(failureCount);
        this.count.incrementAndGet();
    }

    /**
     * 记录add客户端接受server响应的耗时
     *
     * @param readCostTime 接收server响应的时间
     * @param failureCount 产生异常数, 无异常 0, 有异常 1
     */
    public void addRead(long readCostTime, int failureCount) {
        this.readCostTime.addAndGet(readCostTime);
        this.failureCount.addAndGet(failureCount);
    }

    public AtomicLong getWriteCostTime() {
        return writeCostTime;
    }

    public AtomicLong getReadCostTime() {
        return readCostTime;
    }

    public AtomicInteger getCount() {
        return count;
    }

    public AtomicInteger getFailureCount() {
        return failureCount;
    }

    public String getHostPort() {
        return hostPort;
    }

    /**
     * 以后移除掉db这个维度, 多余
     *
     * @return constant -1
     */
    public int getDb() {
        return -1;
    }

    public AtomicLong getBytesIn() {
        return bytesIn;
    }

    public AtomicLong getBytesOut() {
        return bytesOut;
    }

    public void addBytesIn(int bytesIn) {
        this.bytesIn.addAndGet(bytesIn);
    }

    public void addBytesOut(int bytesOut) {
        this.bytesOut.addAndGet(bytesOut);
    }
}
