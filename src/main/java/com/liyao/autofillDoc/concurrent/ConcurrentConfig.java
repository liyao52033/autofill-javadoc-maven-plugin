package com.liyao.autofillDoc.concurrent;

/**
 * 并发处理配置
 * 用于控制并发处理行为的各项参数
 */
public class ConcurrentConfig {

    /**
     * 工作线程数（并行解析文件）
     */
    private int workerThreads = 4;

    /**
     * AI 批处理大小（单次 AI 批处理的任务数）
     */
    private int aiBatchSize = 10;

    /**
     * AI 批次间隔（毫秒，用于 API 限流保护）
     */
    private long aiBatchDelayMs = 1000;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 锁超时时间（毫秒）
     */
    private long lockTimeoutMs = 30000;

    /**
     * 是否使用 OS 文件锁（多进程保护）
     */
    private boolean useOsLock = true;

    /**
     * 是否启用检查点（中断恢复）
     */
    private boolean enableCheckpoint = false;

    /**
     * 检查点存储路径
     */
    private String checkpointPath = ".javadoc-checkpoint";

    /**
     * 是否启用并发处理（false 则使用串行处理）
     */
    private boolean enableConcurrent = true;

    /**
     * AI 请求超时时间（秒）
     */
    private long aiTimeoutSeconds = 60;

    /**
     * 重试延迟基数（毫秒，指数退避）
     */
    private long retryDelayBaseMs = 1000;

    // ==================== Getters and Setters ====================
    /**
     * 获取WorkerThreads
     *
     * @return 返回整数值
     */
    public int getWorkerThreads() {
        return workerThreads;
    }

    /**
     * 设置WorkerThreads
     *
     * @param workerThreads 参数 workerThreads 的描述
     */
    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    /**
     * 获取AiBatchSize
     *
     * @return 返回整数值
     */
    public int getAiBatchSize() {
        return aiBatchSize;
    }

    /**
     * 设置AiBatchSize
     *
     * @param aiBatchSize 参数 aiBatchSize 的描述
     */
    public void setAiBatchSize(int aiBatchSize) {
        this.aiBatchSize = aiBatchSize;
    }

    /**
     * 获取AiBatchDelayMs
     *
     * @return 返回整数值
     */
    public long getAiBatchDelayMs() {
        return aiBatchDelayMs;
    }

    /**
     * 设置AiBatchDelayMs
     *
     * @param aiBatchDelayMs 参数 aiBatchDelayMs 的描述
     */
    public void setAiBatchDelayMs(long aiBatchDelayMs) {
        this.aiBatchDelayMs = aiBatchDelayMs;
    }

    /**
     * 获取MaxRetries
     *
     * @return 返回整数值
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * 设置MaxRetries
     *
     * @param maxRetries 参数 maxRetries 的描述
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * 获取LockTimeoutMs
     *
     * @return 返回整数值
     */
    public long getLockTimeoutMs() {
        return lockTimeoutMs;
    }

    /**
     * 设置LockTimeoutMs
     *
     * @param lockTimeoutMs 参数 lockTimeoutMs 的描述
     */
    public void setLockTimeoutMs(long lockTimeoutMs) {
        this.lockTimeoutMs = lockTimeoutMs;
    }

    /**
     * 返回处理结果
     *
     * @return 返回布尔值，true 或 false
     */
    public boolean isUseOsLock() {
        return useOsLock;
    }

    /**
     * 设置UseOsLock
     *
     * @param useOsLock 参数 useOsLock 的描述
     */
    public void setUseOsLock(boolean useOsLock) {
        this.useOsLock = useOsLock;
    }

    /**
     * 返回处理结果
     *
     * @return 返回布尔值，true 或 false
     */
    public boolean isEnableCheckpoint() {
        return enableCheckpoint;
    }

    /**
     * 设置EnableCheckpoint
     *
     * @param enableCheckpoint 参数 enableCheckpoint 的描述
     */
    public void setEnableCheckpoint(boolean enableCheckpoint) {
        this.enableCheckpoint = enableCheckpoint;
    }

    /**
     * 获取CheckpointPath
     *
     * @return 返回字符串
     */
    public String getCheckpointPath() {
        return checkpointPath;
    }

    /**
     * 设置CheckpointPath
     *
     * @param checkpointPath 参数 checkpointPath 的描述
     */
    public void setCheckpointPath(String checkpointPath) {
        this.checkpointPath = checkpointPath;
    }

    /**
     * 返回处理结果
     *
     * @return 返回布尔值，true 或 false
     */
    public boolean isEnableConcurrent() {
        return enableConcurrent;
    }

    /**
     * 设置EnableConcurrent
     *
     * @param enableConcurrent 参数 enableConcurrent 的描述
     */
    public void setEnableConcurrent(boolean enableConcurrent) {
        this.enableConcurrent = enableConcurrent;
    }

    /**
     * 获取AiTimeoutSeconds
     *
     * @return 返回整数值
     */
    public long getAiTimeoutSeconds() {
        return aiTimeoutSeconds;
    }

    /**
     * 设置AiTimeoutSeconds
     *
     * @param aiTimeoutSeconds 参数 aiTimeoutSeconds 的描述
     */
    public void setAiTimeoutSeconds(long aiTimeoutSeconds) {
        this.aiTimeoutSeconds = aiTimeoutSeconds;
    }

    /**
     * 获取RetryDelayBaseMs
     *
     * @return 返回整数值
     */
    public long getRetryDelayBaseMs() {
        return retryDelayBaseMs;
    }

    /**
     * 设置RetryDelayBaseMs
     *
     * @param retryDelayBaseMs 参数 retryDelayBaseMs 的描述
     */
    public void setRetryDelayBaseMs(long retryDelayBaseMs) {
        this.retryDelayBaseMs = retryDelayBaseMs;
    }

    /**
     * 创建默认配置
     */
    public static ConcurrentConfig defaultConfig() {
        return new ConcurrentConfig();
    }

    /**
     * 构建器
     */
    public static class Builder {

        private final ConcurrentConfig config = new ConcurrentConfig();

        /**
         * 返回处理结果
         *
         * @param workerThreads 参数 workerThreads 的描述
         * @return 返回值类型为 Builder 的描述
         */
        public Builder workerThreads(int workerThreads) {
            config.setWorkerThreads(workerThreads);
            return this;
        }

        /**
         * 返回处理结果
         *
         * @param aiBatchSize 参数 aiBatchSize 的描述
         * @return 返回值类型为 Builder 的描述
         */
        public Builder aiBatchSize(int aiBatchSize) {
            config.setAiBatchSize(aiBatchSize);
            return this;
        }

        /**
         * 返回处理结果
         *
         * @param aiBatchDelayMs 参数 aiBatchDelayMs 的描述
         * @return 返回值类型为 Builder 的描述
         */
        public Builder aiBatchDelayMs(long aiBatchDelayMs) {
            config.setAiBatchDelayMs(aiBatchDelayMs);
            return this;
        }

        /**
         * 返回处理结果
         *
         * @param maxRetries 参数 maxRetries 的描述
         * @return 返回值类型为 Builder 的描述
         */
        public Builder maxRetries(int maxRetries) {
            config.setMaxRetries(maxRetries);
            return this;
        }

        /**
         * 返回处理结果
         *
         * @param lockTimeoutMs 参数 lockTimeoutMs 的描述
         * @return 返回值类型为 Builder 的描述
         */
        public Builder lockTimeoutMs(long lockTimeoutMs) {
            config.setLockTimeoutMs(lockTimeoutMs);
            return this;
        }

        /**
         * 返回处理结果
         *
         * @param useOsLock 参数 useOsLock 的描述
         * @return 返回值类型为 Builder 的描述
         */
        public Builder useOsLock(boolean useOsLock) {
            config.setUseOsLock(useOsLock);
            return this;
        }

        /**
         * 返回处理结果
         *
         * @param enableCheckpoint 参数 enableCheckpoint 的描述
         * @return 返回值类型为 Builder 的描述
         */
        public Builder enableCheckpoint(boolean enableCheckpoint) {
            config.setEnableCheckpoint(enableCheckpoint);
            return this;
        }

        /**
         * 返回处理结果
         *
         * @param checkpointPath 参数 checkpointPath 的描述
         * @return 返回值类型为 Builder 的描述
         */
        public Builder checkpointPath(String checkpointPath) {
            config.setCheckpointPath(checkpointPath);
            return this;
        }

        /**
         * 返回处理结果
         *
         * @param enableConcurrent 参数 enableConcurrent 的描述
         * @return 返回值类型为 Builder 的描述
         */
        public Builder enableConcurrent(boolean enableConcurrent) {
            config.setEnableConcurrent(enableConcurrent);
            return this;
        }

        /**
         * 返回处理结果
         *
         * @param aiTimeoutSeconds 参数 aiTimeoutSeconds 的描述
         * @return 返回值类型为 Builder 的描述
         */
        public Builder aiTimeoutSeconds(long aiTimeoutSeconds) {
            config.setAiTimeoutSeconds(aiTimeoutSeconds);
            return this;
        }

        /**
         * 返回处理结果
         *
         * @param retryDelayBaseMs 参数 retryDelayBaseMs 的描述
         * @return 返回值类型为 Builder 的描述
         */
        public Builder retryDelayBaseMs(long retryDelayBaseMs) {
            config.setRetryDelayBaseMs(retryDelayBaseMs);
            return this;
        }

        /**
         * 返回处理结果
         *
         * @return 返回值类型为 ConcurrentConfig 的描述
         */
        public ConcurrentConfig build() {
            return config;
        }
    }
}
