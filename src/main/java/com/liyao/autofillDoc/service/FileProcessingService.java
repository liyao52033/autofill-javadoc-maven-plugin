package com.liyao.autofillDoc.service;

import com.liyao.autofillDoc.config.JavadocAutofillConfig;
import com.liyao.autofillDoc.concurrent.ConcurrentConfig;
import com.liyao.autofillDoc.concurrent.ConcurrentJavadocManager;
import com.liyao.autofillDoc.concurrent.JavadocTask;
import com.liyao.autofillDoc.exception.JavadocProcessingException;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * 文件处理服务
 * 负责遍历源代码目录并处理 Java 文件
 * 支持并发处理和并发冲突检测
 */
public class FileProcessingService {

    private final Logger log;
    private final JavadocAutofillConfig config;
    private final JavadocProcessor javadocProcessor;

    /**
     * 构造函数
     *
     * @param log    日志对象
     * @param config 配置对象
     */
    public FileProcessingService(Logger log, JavadocAutofillConfig config) {
        this.log = log;
        this.config = config;
        this.javadocProcessor = new JavadocProcessor(log, config);
    }

    /**
     * 处理源代码目录
     *
     * @return 处理的文件数量
     * @throws JavadocProcessingException 处理异常
     */
    public int processSourceDirectory() {
        File sourceDir = config.getSourceDir();

        if (!sourceDir.exists()) {
            log.warn("源代码目录不存在：{}", sourceDir);
            return 0;
        }

        AtomicInteger processedCount = new AtomicInteger(0);
        List<String> excludePatterns = config.getExcludePatterns();

        log.info("开始处理 Java 文件，排除模式数量：{}", excludePatterns != null ? excludePatterns.size() : 0);

        if (!config.isIncludePrivateMethods()) {
            log.info("isIncludePrivateMethods 设置为 false, 跳过私有方法处理");
        }

        if (!config.isAddReturnJavadoc()) {
            log.info("addReturnJavadoc 设置为 false, 跳过返回值注释处理");
        }
        
        if (!config.isAddThrowsJavadoc()) {
            log.info("addThrowsJavadoc 设置为 false, 跳过抛出异常注释处理");
        }

        if (!config.isAddParamJavadoc()) {
            log.info("addParamJavadoc 设置为 false, 跳过参数注释处理");
        }

        if (!config.isAddClassJavadoc()) {
            log.info("addClassJavadoc 设置为 false, 跳过类注释处理");
        }

        if (!config.isAddMethodJavadoc()) {
            log.info("addMethodJavadoc 设置为 false, 跳过方法注释处理");
        }

        // 根据配置选择处理模式
        if (config.isEnableConcurrent()) {
            processedCount.set(processConcurrently(sourceDir));
        } else {
            processedCount.set(processSequentially(sourceDir));
        }

        if (processedCount.get() == 0) {
            log.info("未找到需要处理的 Java 文件");
        }
         
        return processedCount.get();
    }

    /**
     * 并发处理文件
     *
     * @param sourceDir 源目录
     * @return 处理的文件数量
     */
    private int processConcurrently(File sourceDir) {
        log.info("使用并发模式处理，线程数：{}", config.getWorkerThreads());

        // 创建并发配置
        ConcurrentConfig concurrentConfig = new ConcurrentConfig.Builder()
                .workerThreads(config.getWorkerThreads())
                .aiBatchSize(config.getAiBatchSize())
                .aiBatchDelayMs(config.getAiBatchDelayMs())
                .maxRetries(config.getMaxRetries())
                .lockTimeoutMs(config.getLockTimeoutMs())
                .useOsLock(config.isUseOsLock())
                .aiTimeoutSeconds(config.getAiTimeoutSeconds())
                .build();

        // 创建并发管理器
        ConcurrentJavadocManager manager = new ConcurrentJavadocManager(log, config, concurrentConfig);

        try {
            // 收集所有 Java 文件并提交任务
            try (Stream<Path> paths = Files.walk(sourceDir.toPath())) {
                paths.filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> {
                            manager.submitTask(path.toFile(), JavadocTask.Priority.NORMAL);
                        });
            } catch (IOException e) {
                log.error("遍历 Java 文件失败", e);
                throw new JavadocProcessingException("遍历源代码目录失败：" + sourceDir, e);
            }

            // 启动处理
            manager.start();

            // 等待处理完成（最长等待 30 分钟）
            boolean completed = false;
            try {
                completed = manager.awaitCompletion(30, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("并发处理被中断");
            }
            if (!completed) {
                log.warn("并发处理超时，部分任务可能未完成");
            }

            // 获取统计信息
            java.util.Map<String, Integer> stats = manager.getStatistics();
            log.info("并发处理完成 - 成功: {}, 失败: {}, 冲突: {}", 
                    stats.get("processed"), stats.get("failed"), stats.get("conflict"));

            return stats.get("processed");

        } finally {
            manager.shutdown();
        }
    }

    /**
     * 串行处理文件（原有逻辑）
     *
     * @param sourceDir 源目录
     * @return 处理的文件数量
     */
    private int processSequentially(File sourceDir) {
        log.info("使用串行模式处理");

        AtomicInteger processedCount = new AtomicInteger(0);

        try (Stream<Path> paths = Files.walk(sourceDir.toPath())) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            boolean fileModified = processJavaFile(path.toFile());
                            if (fileModified) {
                                processedCount.incrementAndGet();
                            }
                        } catch (JavadocProcessingException e) {
                            // 记录异常但继续处理其他文件
                            log.error(e.getMessage(), e.getCause());
                        }
                    });
        } catch (IOException e) {
            log.error("遍历 Java 文件失败", e);
            throw new JavadocProcessingException("遍历源代码目录失败：" + sourceDir, e);
        }

        return processedCount.get();
    }

    /**
     * 处理单个 Java 文件
     *
     * @param file Java 文件
     * @return 是否成功处理文件
     * @throws JavadocProcessingException 处理异常
     */
    private boolean processJavaFile(File file) {
        try {
            return javadocProcessor.processJavaFile(file);
        } catch (JavadocProcessingException e) {
            // 直接抛出 JavadocProcessingException 异常
            throw e;
        } catch (Exception e) {
            // 包装其他异常为 JavadocProcessingException
            throw JavadocProcessingException.createFileProcessingException(file.getPath(), e);
        }
    }
}