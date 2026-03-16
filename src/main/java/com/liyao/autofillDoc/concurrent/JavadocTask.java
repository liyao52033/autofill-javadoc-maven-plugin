package com.liyao.autofillDoc.concurrent;

import com.liyao.autofillDoc.concurrent.FileVersionController.VersionInfo;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Javadoc 生成任务
 * 封装单个文件的处理任务，包含状态管理和重试计数
 */
public class JavadocTask implements Comparable<JavadocTask> {

    /**
     * 任务优先级
     */
    public enum Priority {

        // 用户手动触发
        HIGH(1),
        // 常规处理
        NORMAL(2),
        // 后台批量
        LOW(3);

        final int level;

        Priority(int level) {
            this.level = level;
        }
    }

    /**
     * 任务状态
     */
    public enum State {

        // 等待处理
        PENDING,
        // 解析中
        PARSING,
        // AI 处理中
        AI_PROCESSING,
        // 合并中
        MERGING,
        // 写入中
        WRITING,
        // 完成
        COMPLETED,
        // 失败
        FAILED,
        // 冲突待解决
        CONFLICT
    }

    private final String taskId;

    private final File targetFile;

    private final Priority priority;

    private volatile State state = State.PENDING;

    /**
     * 重试计数
     */
    private final AtomicInteger retryCount = new AtomicInteger(0);

    private final int maxRetries;

    /**
     * 版本信息
     */
    private VersionInfo baseVersion;

    /**
     * 原始内容
     */
    private String baseContent;

    /**
     * 生成的内容
     */
    private String generatedContent;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 时间戳
     */
    private final long createdTime;

    private long startTime;

    private long endTime;

    /**
     * 构造函数
     *
     * @param file      目标文件
     * @param priority  优先级
     * @param maxRetries 最大重试次数
     */
    public JavadocTask(File file, Priority priority, int maxRetries) {
        this.taskId = generateTaskId(file);
        this.targetFile = file;
        this.priority = priority;
        this.maxRetries = maxRetries;
        this.createdTime = System.currentTimeMillis();
    }

    /**
     * 生成任务 ID
     */
    private String generateTaskId(File file) {
        return file.getAbsolutePath() + "_" + System.nanoTime();
    }

    /**
     * 根据条件判断返回不同结果
     *
     * @param other 参数 other 的描述
     * @return 返回整数值
     */
    @Override
    public int compareTo(JavadocTask other) {
        // 优先级高的排前面
        int priorityCompare = Integer.compare(this.priority.level, other.priority.level);
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        // 同优先级按创建时间排序（先创建的先处理）
        return Long.compare(this.createdTime, other.createdTime);
    }

    /**
     * 是否可以重试
     */
    public boolean canRetry() {
        return retryCount.get() < maxRetries;
    }

    /**
     * 增加重试计数
     */
    public void incrementRetry() {
        retryCount.incrementAndGet();
    }

    // ==================== Getters and Setters ====================
    /**
     * 获取TaskId
     *
     * @return 返回字符串
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * 获取TargetFile
     *
     * @return 返回值类型为 File 的描述
     */
    public File getTargetFile() {
        return targetFile;
    }

    /**
     * 获取Priority
     *
     * @return 返回值类型为 Priority 的描述
     */
    public Priority getPriority() {
        return priority;
    }

    /**
     * 获取State
     *
     * @return 返回值类型为 State 的描述
     */
    public State getState() {
        return state;
    }

    /**
     * 设置State
     *
     * @param state 参数 state 的描述
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * 获取RetryCount
     *
     * @return 返回整数值
     */
    public int getRetryCount() {
        return retryCount.get();
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
     * 获取BaseVersion
     *
     * @return 返回值类型为 VersionInfo 的描述
     */
    public VersionInfo getBaseVersion() {
        return baseVersion;
    }

    /**
     * 设置BaseVersion
     *
     * @param baseVersion 参数 baseVersion 的描述
     */
    public void setBaseVersion(VersionInfo baseVersion) {
        this.baseVersion = baseVersion;
    }

    /**
     * 获取BaseContent
     *
     * @return 返回字符串
     */
    public String getBaseContent() {
        return baseContent;
    }

    /**
     * 设置BaseContent
     *
     * @param baseContent 参数 baseContent 的描述
     */
    public void setBaseContent(String baseContent) {
        this.baseContent = baseContent;
    }

    /**
     * 获取GeneratedContent
     *
     * @return 返回字符串
     */
    public String getGeneratedContent() {
        return generatedContent;
    }

    /**
     * 设置GeneratedContent
     *
     * @param generatedContent 参数 generatedContent 的描述
     */
    public void setGeneratedContent(String generatedContent) {
        this.generatedContent = generatedContent;
    }

    /**
     * 获取ErrorMessage
     *
     * @return 返回字符串
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 设置ErrorMessage
     *
     * @param errorMessage 参数 errorMessage 的描述
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 获取CreatedTime
     *
     * @return 返回整数值
     */
    public long getCreatedTime() {
        return createdTime;
    }

    /**
     * 获取StartTime
     *
     * @return 返回整数值
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * 设置StartTime
     *
     * @param startTime 参数 startTime 的描述
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * 获取EndTime
     *
     * @return 返回整数值
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * 设置EndTime
     *
     * @param endTime 参数 endTime 的描述
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * 返回处理结果
     *
     * @return 返回字符串
     */
    @Override
    public String toString() {
        return "JavadocTask{" + "taskId='" + taskId + '\'' + ", file=" + targetFile.getName() + ", state=" + state + ", retries=" + retryCount.get() + '}';
    }
}
