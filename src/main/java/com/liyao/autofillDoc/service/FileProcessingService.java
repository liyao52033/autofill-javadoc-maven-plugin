package com.liyao.autofillDoc.service;

import com.liyao.autofillDoc.config.JavadocAutofillConfig;
import com.liyao.autofillDoc.exception.JavadocProcessingException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * 文件处理服务
 * 负责遍历源代码目录并处理Java文件
 */
public class FileProcessingService {

    private final Log log;
    private final JavadocAutofillConfig config;
    private final JavadocProcessor javadocProcessor;

    /**
     * 构造函数
     *
     * @param log    日志对象
     * @param config 配置对象
     */
    public FileProcessingService(Log log, JavadocAutofillConfig config) {
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
            log.warn("源代码目录不存在: " + sourceDir);
            return 0;
        }

        AtomicInteger processedCount = new AtomicInteger(0);
        List<String> excludePatterns = config.getExcludePatterns();

        log.info("开始处理Java文件, 排除模式数量: " + (excludePatterns != null ? excludePatterns.size() : 0));

        if (!config.isIncludePrivateMethods()) {
            log.info("isIncludePrivateMethods设置为false, 跳过私有方法处理");
        }

        if (!config.isAddReturnJavadoc()) {
            log.info("addReturnJavadoc设置为false, 跳过返回值注释处理");
        }
        
        if (!config.isAddThrowsJavadoc()) {
            log.info("addThrowsJavadoc设置为false, 跳过抛出异常注释处理");
        }

        if (!config.isAddParamJavadoc()) {
            log.info("addParamJavadoc设置为false, 跳过参数注释处理");
        }

        if (!config.isAddClassJavadoc()) {
            log.info("addClassJavadoc设置为false, 跳过类注释处理");
        }

        if (!config.isAddMethodJavadoc()) {
            log.info("addMethodJavadoc设置为false, 跳过方法注释处理");
        }

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
            log.error("遍历Java文件失败", e);
            throw new JavadocProcessingException("遍历源代码目录失败: " + sourceDir, e);
        }

        if (processedCount.get() == 0) {
            log.info("未找到需要处理的Java文件");
        }
         
        return processedCount.get();
    }

    /**
     * 处理单个Java文件
     *
     * @param file Java文件
     * @return 是否成功处理文件
     * @throws JavadocProcessingException 处理异常
     */
    private boolean processJavaFile(File file) {
        try {
            return javadocProcessor.processJavaFile(file);
        } catch (JavadocProcessingException e) {
            // 直接抛出JavadocProcessingException异常
            throw e;
        } catch (Exception e) {
            // 包装其他异常为JavadocProcessingException
            throw JavadocProcessingException.createFileProcessingException(file.getPath(), e);
        }
    }
}