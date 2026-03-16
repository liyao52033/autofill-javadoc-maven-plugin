package com.liyao.autofillDoc.concurrent;

import com.liyao.autofillDoc.concurrent.JavadocTask.Priority;
import com.liyao.autofillDoc.concurrent.JavadocTask.State;
import com.liyao.autofillDoc.config.JavadocAutofillConfig;
import com.liyao.autofillDoc.service.JavadocProcessor;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并发 Javadoc 处理管理器
 * 简化版本：移除复杂文件锁定，仅保留 AI 限流机制
 */
public class ConcurrentJavadocManager {

    private final Logger log;
    private final JavadocAutofillConfig config;
    private final ConcurrentConfig concurrentConfig;
    private final JavadocProcessor javadocProcessor;
    
    /**
     * AI 限流信号量（控制同时进行的 AI 请求数）
     */
    private final Semaphore aiSemaphore;
    
    /**
     * 优先级任务队列
     */
    private final PriorityBlockingQueue<JavadocTask> taskQueue;
    
    /**
     * 任务注册表
     */
    private final ConcurrentHashMap<String, JavadocTask> taskRegistry;
    
    /**
     * 线程池
     */
    private final ExecutorService executorService;
    
    /**
     * 运行状态
     */
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    /**
     * 统计信息
     */
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    private final AtomicInteger conflictCount = new AtomicInteger(0);

    /**
     * 构造函数
     *
     * @param log              日志对象
     * @param config           插件配置
     * @param concurrentConfig 并发配置
     */
    public ConcurrentJavadocManager(Logger log, JavadocAutofillConfig config, ConcurrentConfig concurrentConfig) {
        this.log = log;
        this.config = config;
        this.concurrentConfig = concurrentConfig;
        this.javadocProcessor = new JavadocProcessor(log, config);

        // AI 限流：最多同时 2 个 AI 请求
        this.aiSemaphore = new Semaphore(Math.min(2, concurrentConfig.getAiBatchSize()));
        
        this.taskQueue = new PriorityBlockingQueue<>();
        this.taskRegistry = new ConcurrentHashMap<>();
        
        this.executorService = Executors.newFixedThreadPool(concurrentConfig.getWorkerThreads());
    }

    /**
     * 启动处理
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            // 启动工作线程
            for (int i = 0; i < concurrentConfig.getWorkerThreads(); i++) {
                executorService.submit(this::workerLoop);
            }

            log.info("并发处理管理器已启动，工作线程数：{}，AI 并发限制：{}", 
                    concurrentConfig.getWorkerThreads(), aiSemaphore.availablePermits());
        }
    }

    /**
     * 停止处理（优雅关闭）
     */
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            executorService.shutdown();

            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }

            log.info("并发处理管理器已关闭，处理：{}，失败：{}，冲突：{}", 
                    processedCount.get(), failedCount.get(), conflictCount.get());
        }
    }

    /**
     * 提交任务
     *
     * @param file     目标文件
     * @param priority 优先级
     */
    public void submitTask(File file, Priority priority) {
        JavadocTask task = new JavadocTask(file, priority, concurrentConfig.getMaxRetries());
        taskRegistry.put(task.getTaskId(), task);
        taskQueue.offer(task);
    }

    /**
     * 提交任务（默认优先级）
     *
     * @param file 目标文件
     */
    public void submitTask(File file) {
        submitTask(file, Priority.NORMAL);
    }

    /**
     * 等待所有任务完成
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否全部完成
     */
    public boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        long startTime = System.nanoTime();
        long timeoutNanos = unit.toNanos(timeout);

        while (System.nanoTime() - startTime < timeoutNanos) {
            if (taskQueue.isEmpty()) {
                Thread.sleep(100);
                if (taskQueue.isEmpty()) {
                    return true;
                }
            }
            Thread.sleep(100);
        }
        return false;
    }

    /**
     * 获取统计信息
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new ConcurrentHashMap<>();
        stats.put("processed", processedCount.get());
        stats.put("failed", failedCount.get());
        stats.put("conflict", conflictCount.get());
        stats.put("pending", taskQueue.size());
        return stats;
    }

    /**
     * 工作线程循环
     */
    private void workerLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                JavadocTask task = taskQueue.poll(1, TimeUnit.SECONDS);
                if (task == null) {
                    continue;
                }

                processTask(task);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 处理单个任务
     */
    private void processTask(JavadocTask task) {
        File file = task.getTargetFile();
        task.setStartTime(System.currentTimeMillis());

        try {
            // 使用 JavadocProcessor 处理文件
            boolean fileModified = javadocProcessor.processJavaFile(file);

            if (fileModified) {
                task.setState(State.COMPLETED);
                processedCount.incrementAndGet();
                log.info("处理完成：{}", file.getAbsolutePath());
            } else {
                task.setState(State.COMPLETED);
                processedCount.incrementAndGet();
                log.debug("文件无需修改：{}", file.getName());
            }

            task.setEndTime(System.currentTimeMillis());

        } catch (Exception e) {
            handleTaskError(task, e);
        }
    }

    /**
     * 读取文件内容
     */
    private String readFileContent(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), "UTF-8");
    }

    /**
     * 任务错误处理
     */
    private void handleTaskError(JavadocTask task, Exception e) {
        if (task.canRetry()) {
            task.incrementRetry();
            task.setState(State.PENDING);
            taskQueue.offer(task);
            
            log.warn("任务失败，重试 {}/{}：{} - {}",
                    task.getRetryCount(),
                    task.getMaxRetries(),
                    task.getTargetFile().getName(),
                    e.getMessage());
        } else {
            task.setState(State.FAILED);
            task.setErrorMessage(e.getMessage());
            failedCount.incrementAndGet();
            
            log.error("任务最终失败：{}", task.getTargetFile().getName(), e);
        }
    }
}