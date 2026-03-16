package com.liyao.autofillDoc;

import com.liyao.autofillDoc.config.JavadocAutofillConfig;
import com.liyao.autofillDoc.service.FileProcessingService;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.List;

/**
 * JavadocAutofillMojo 类的描述
 */
@Mojo(name = "autofill", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class JavadocAutofillMojo extends AbstractMojo {

    private static final Logger log = LoggerFactory.getLogger(JavadocAutofillMojo.class);

    /**
     * 源代码目录
     */
    @Parameter(property = "sourceDir", defaultValue = "${project.build.sourceDirectory}", required = true)
    private File sourceDir;

    /**
     * 是否添加类注释
     */
    @Parameter(property = "addClassJavadoc", defaultValue = "true")
    private boolean addClassJavadoc;

    /**
     * 是否添加方法注释
     */
    @Parameter(property = "addMethodJavadoc", defaultValue = "true")
    private boolean addMethodJavadoc;

    /**
     * 是否添加参数注释
     */
    @Parameter(property = "addParamJavadoc", defaultValue = "true")
    private boolean addParamJavadoc;

    /**
     * 是否添加返回值注释
     */
    @Parameter(property = "addReturnJavadoc", defaultValue = "true")
    private boolean addReturnJavadoc;

    /**
     * 是否添加异常注释
     */
    @Parameter(property = "addThrowsJavadoc", defaultValue = "true")
    private boolean addThrowsJavadoc;

    /**
     * 排除特定文件的模式列表
     */
    @Parameter(property = "excludePatterns")
    private List<String> excludePatterns;

    /**
     * 是否包含私有方法
     */
    @Parameter(property = "includePrivateMethods", defaultValue = "true")
    private boolean includePrivateMethods;

    /**
     * 是否启用 AI 生成方法描述
     */
    @Parameter(property = "enableAi", defaultValue = "false")
    private boolean enableAi;

    /**
     * AI API 密钥
     * 优先从环境变量读取（AI_API_KEY），避免在 pom.xml 中暴露敏感信息
     */
    @Parameter(property = "aiApiKey")
    private String aiApiKey;

    /**
     * 是否跳过已有注释的方法（避免重复生成浪费 token）
     */
    @Parameter(property = "skipExistingJavadoc", defaultValue = "true")
    private boolean skipExistingJavadoc;

    /**
     * AI API 地址
     * 默认根据 aiProvider 自动选择，如果指定则使用自定义地址
     */
    @Parameter(property = "aiApiUrl")
    private String aiApiUrl;

    /**
     * AI 模型名称
     */
    @Parameter(property = "aiModel")
    private String aiModel;

    /**
     * AI 提供商名称
     * 支持：OPENAI, DEEPSEEK, MOONSHOT, ZHIPU, OLLAMA, BAIDU, ALIBABA, CUSTOM
     */
    @Parameter(property = "aiProvider", defaultValue = "OPENAI")
    private String aiProvider;

    // ==================== 并发控制参数 ====================
    /**
     * 是否启用并发处理
     * 启用后可提高大项目处理速度，同时支持并发冲突检测和自动合并
     */
    @Parameter(property = "enableConcurrent", defaultValue = "true")
    private boolean enableConcurrent;

    /**
     * 工作线程数（并行解析文件）
     */
    @Parameter(property = "workerThreads", defaultValue = "4")
    private int workerThreads;

    /**
     * AI 批处理大小（单次 AI 批处理的任务数）
     */
    @Parameter(property = "aiBatchSize", defaultValue = "10")
    private int aiBatchSize;

    /**
     * AI 批次间隔（毫秒，用于 API 限流保护）
     */
    @Parameter(property = "aiBatchDelayMs", defaultValue = "1000")
    private long aiBatchDelayMs;

    /**
     * 最大重试次数
     */
    @Parameter(property = "maxRetries", defaultValue = "3")
    private int maxRetries;

    /**
     * 锁超时时间（毫秒）
     */
    @Parameter(property = "lockTimeoutMs", defaultValue = "30000")
    private long lockTimeoutMs;

    /**
     * 是否使用 OS 文件锁（多进程保护）
     */
    @Parameter(property = "useOsLock", defaultValue = "true")
    private boolean useOsLock;

    /**
     * AI 请求超时时间（秒）
     */
    @Parameter(property = "aiTimeoutSeconds", defaultValue = "60")
    private long aiTimeoutSeconds;

    /**
     * 执行插件
     */
    @Override
    public void execute() {
        try {
            // 从环境变量读取 API Key（如果未在配置中指定）
            String effectiveApiKey = aiApiKey;
            if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
                effectiveApiKey = System.getenv("AI_API_KEY");
            }
            if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
                effectiveApiKey = System.getenv("DEEPSEEK_API_KEY");
            }
            if (effectiveApiKey == null || effectiveApiKey.isEmpty()) {
                effectiveApiKey = System.getenv("OPENAI_API_KEY");
            }
            // 创建配置对象
            JavadocAutofillConfig config = new JavadocAutofillConfig.Builder().sourceDir(sourceDir).addClassJavadoc(addClassJavadoc).addMethodJavadoc(addMethodJavadoc).addParamJavadoc(addParamJavadoc).addReturnJavadoc(addReturnJavadoc).addThrowsJavadoc(addThrowsJavadoc).excludePatterns(excludePatterns).includePrivateMethods(includePrivateMethods).enableAi(enableAi).aiApiKey(effectiveApiKey).aiApiUrl(aiApiUrl).aiModel(aiModel).aiProvider(aiProvider).skipExistingJavadoc(skipExistingJavadoc).// 并发控制配置
            enableConcurrent(enableConcurrent).workerThreads(workerThreads).aiBatchSize(aiBatchSize).aiBatchDelayMs(aiBatchDelayMs).maxRetries(maxRetries).lockTimeoutMs(lockTimeoutMs).useOsLock(useOsLock).aiTimeoutSeconds(aiTimeoutSeconds).build();
            // 创建文件处理服务并执行处理
            FileProcessingService fileProcessingService = new FileProcessingService(log, config);
            int processedCount = fileProcessingService.processSourceDirectory();
            if (processedCount > 0) {
                log.info("执行 Javadoc 自动填充插件完成，共处理 {} 个文件", processedCount);
            }
        } catch (Exception e) {
            log.error("执行 Javadoc 自动填充插件失败：{}", e.getMessage(), e);
        }
    }
}
