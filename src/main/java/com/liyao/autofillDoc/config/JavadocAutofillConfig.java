package com.liyao.autofillDoc.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Javadoc 自动填充配置类
 * 用于存储插件的配置参数
 */
public class JavadocAutofillConfig {

    /**
     * 源代码目录
     */
    private final File sourceDir;

    /**
     * 是否添加类注释
     */
    private final boolean addClassJavadoc;

    /**
     * 是否添加方法注释
     */
    private final boolean addMethodJavadoc;

    /**
     * 是否添加参数注释
     */
    private final boolean addParamJavadoc;

    /**
     * 是否添加返回值注释
     */
    private final boolean addReturnJavadoc;

    /**
     * 是否添加异常注释
     */
    private final boolean addThrowsJavadoc;

    /**
     * 排除特定文件的模式列表
     */
    private final List<String> excludePatterns;

    /**
     * 是否包含私有方法
     */
    private final boolean includePrivateMethods;

    /**
     * 是否启用 AI 生成方法描述
     */
    private final boolean enableAi;

    /**
     * AI API 密钥
     */
    private final String aiApiKey;

    /**
     * AI API 地址
     */
    private final String aiApiUrl;

    /**
     * AI 模型名称
     */
    private final String aiModel;

    /**
     * AI 提供商名称（用于快速选择预设配置）
     * 支持：OPENAI, DEEPSEEK, MOONSHOT, ZHIPU, OLLAMA, AZURE, ANTHROPIC, GEMINI
     */
    private final String aiProvider;

    /**
     * 是否跳过已有注释的方法（避免重复生成浪费 token）
     */
    private final boolean skipExistingJavadoc;

    // ==================== 并发控制配置 ====================
    /**
     * 是否启用并发处理
     */
    private final boolean enableConcurrent;

    /**
     * 工作线程数
     */
    private final int workerThreads;

    /**
     * AI 批处理大小
     */
    private final int aiBatchSize;

    /**
     * AI 批次间隔（毫秒）
     */
    private final long aiBatchDelayMs;

    /**
     * 最大重试次数
     */
    private final int maxRetries;

    /**
     * 锁超时时间（毫秒）
     */
    private final long lockTimeoutMs;

    /**
     * 是否使用 OS 文件锁
     */
    private final boolean useOsLock;

    /**
     * AI 请求超时时间（秒）
     */
    private final long aiTimeoutSeconds;

    /**
     * 构造函数
     *
     * @param builder 构建器
     */
    private JavadocAutofillConfig(Builder builder) {
        this.sourceDir = builder.sourceDir;
        this.addClassJavadoc = builder.addClassJavadoc;
        this.addMethodJavadoc = builder.addMethodJavadoc;
        this.addParamJavadoc = builder.addParamJavadoc;
        this.addReturnJavadoc = builder.addReturnJavadoc;
        this.addThrowsJavadoc = builder.addThrowsJavadoc;
        this.excludePatterns = builder.excludePatterns;
        this.includePrivateMethods = builder.includePrivateMethods;
        this.enableAi = builder.enableAi;
        this.aiApiKey = builder.aiApiKey;
        this.aiApiUrl = builder.aiApiUrl;
        this.aiModel = builder.aiModel;
        this.aiProvider = builder.aiProvider;
        this.skipExistingJavadoc = builder.skipExistingJavadoc;
        // 并发控制配置
        this.enableConcurrent = builder.enableConcurrent;
        this.workerThreads = builder.workerThreads;
        this.aiBatchSize = builder.aiBatchSize;
        this.aiBatchDelayMs = builder.aiBatchDelayMs;
        this.maxRetries = builder.maxRetries;
        this.lockTimeoutMs = builder.lockTimeoutMs;
        this.useOsLock = builder.useOsLock;
        this.aiTimeoutSeconds = builder.aiTimeoutSeconds;
    }

    /**
     * 获取源代码目录
     *
     * @return 源代码目录
     */
    public File getSourceDir() {
        return sourceDir;
    }

    /**
     * 是否添加类注释
     *
     * @return 是否添加类注释
     */
    public boolean isAddClassJavadoc() {
        return addClassJavadoc;
    }

    /**
     * 是否添加方法注释
     *
     * @return 是否添加方法注释
     */
    public boolean isAddMethodJavadoc() {
        return addMethodJavadoc;
    }

    /**
     * 是否添加参数注释
     *
     * @return 是否添加参数注释
     */
    public boolean isAddParamJavadoc() {
        return addParamJavadoc;
    }

    /**
     * 是否添加返回值注释
     *
     * @return 是否添加返回值注释
     */
    public boolean isAddReturnJavadoc() {
        return addReturnJavadoc;
    }

    /**
     * 是否添加异常注释
     *
     * @return 是否添加异常注释
     */
    public boolean isAddThrowsJavadoc() {
        return addThrowsJavadoc;
    }

    /**
     * 获取排除特定文件的模式列表
     *
     * @return 排除模式列表
     */
    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    /**
     * 是否包含私有方法
     *
     * @return 是否包含私有方法
     */
    public boolean isIncludePrivateMethods() {
        return includePrivateMethods;
    }

    /**
     * 是否启用 AI 生成方法描述
     *
     * @return 是否启用 AI
     */
    public boolean isEnableAi() {
        return enableAi;
    }

    /**
     * 获取 AI API 密钥
     *
     * @return AI API 密钥
     */
    public String getAiApiKey() {
        return aiApiKey;
    }

    /**
     * 获取 AI API 地址
     *
     * @return AI API 地址
     */
    public String getAiApiUrl() {
        return aiApiUrl;
    }

    /**
     * 获取 AI 模型名称
     *
     * @return AI 模型名称
     */
    public String getAiModel() {
        return aiModel;
    }

    /**
     * 获取 AI 提供商名称
     *
     * @return AI 提供商名称
     */
    public String getAiProvider() {
        return aiProvider;
    }

    /**
     * 获取 AI 提供商的预设 API 地址
     *
     * @return API 地址
     */
    public String getProviderApiUrl() {
        if (aiApiUrl != null && !aiApiUrl.isEmpty()) {
            return aiApiUrl;
        }
        // 根据提供商返回预设地址
        return AiProvider.getApiUrl(aiProvider);
    }

    /**
     * 获取 AI 提供商的预设模型名称
     *
     * @return 模型名称
     */
    public String getProviderModel() {
        if (aiModel != null && !aiModel.isEmpty()) {
            return aiModel;
        }
        // 根据提供商返回预设模型
        return AiProvider.getDefaultModel(aiProvider);
    }

    /**
     * 是否跳过已有注释的方法
     *
     * @return 是否跳过
     */
    public boolean isSkipExistingJavadoc() {
        return skipExistingJavadoc;
    }

    // ==================== 并发控制配置 Getter ====================
    /**
     * 是否启用并发处理
     *
     * @return 是否启用并发处理
     */
    public boolean isEnableConcurrent() {
        return enableConcurrent;
    }

    /**
     * 获取工作线程数
     *
     * @return 工作线程数
     */
    public int getWorkerThreads() {
        return workerThreads;
    }

    /**
     * 获取 AI 批处理大小
     *
     * @return AI 批处理大小
     */
    public int getAiBatchSize() {
        return aiBatchSize;
    }

    /**
     * 获取 AI 批次间隔（毫秒）
     *
     * @return AI 批次间隔
     */
    public long getAiBatchDelayMs() {
        return aiBatchDelayMs;
    }

    /**
     * 获取最大重试次数
     *
     * @return 最大重试次数
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * 获取锁超时时间（毫秒）
     *
     * @return 锁超时时间
     */
    public long getLockTimeoutMs() {
        return lockTimeoutMs;
    }

    /**
     * 是否使用 OS 文件锁
     *
     * @return 是否使用 OS 文件锁
     */
    public boolean isUseOsLock() {
        return useOsLock;
    }

    /**
     * 获取 AI 请求超时时间（秒）
     *
     * @return AI 请求超时时间
     */
    public long getAiTimeoutSeconds() {
        return aiTimeoutSeconds;
    }

    /**
     * AI 提供商枚举
     * 包含主流 AI 服务提供商的预设配置
     */
    public enum AiProvider {

        OPENAI("https://api.openai.com/v1/chat/completions", "gpt-3.5-turbo"),
        DEEPSEEK("https://api.deepseek.com/v1/chat/completions", "deepseek-chat"),
        MOONSHOT("https://api.moonshot.cn/v1/chat/completions", "moonshot-v1-8k"),
        ZHIPU("https://open.bigmodel.cn/api/paas/v4/chat/completions", "glm-4"),
        OLLAMA("http://localhost:11434/v1/chat/completions", "mistral"),
        AZURE("https://api.azure.com/v1/chat/completions", "gpt-4"),
        ANTHROPIC("https://api.anthropic.com/v1/messages", "claude-3-sonnet-20240229"),
        GEMINI("https://generativelanguage.googleapis.com/v1beta/models", "gemini-pro"),
        BAIDU("https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions", "ernie-bot-4"),
        ALIBABA("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", "qwen-turbo"),
        CUSTOM("", "gpt-3.5-turbo");

        private final String defaultUrl;

        private final String defaultModel;

        AiProvider(String defaultUrl, String defaultModel) {
            this.defaultUrl = defaultUrl;
            this.defaultModel = defaultModel;
        }

        /**
         * 获取DefaultUrl
         *
         * @return 返回字符串
         */
        public String getDefaultUrl() {
            return defaultUrl;
        }

        /**
         * 获取DefaultModel
         *
         * @return 返回字符串
         */
        public String getDefaultModel() {
            return defaultModel;
        }

        /**
         * 根据名称获取提供商
         *
         * @param name 提供商名称
         * @return AI 提供商枚举
         */
        public static AiProvider fromName(String name) {
            if (name == null || name.isEmpty()) {
                return CUSTOM;
            }
            try {
                return valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                return CUSTOM;
            }
        }

        /**
         * 获取提供商的默认 API 地址
         *
         * @param name 提供商名称
         * @return API 地址
         */
        public static String getApiUrl(String name) {
            return fromName(name).getDefaultUrl();
        }

        /**
         * 获取提供商的默认模型
         *
         * @param name 提供商名称
         * @return 模型名称
         */
        public static String getDefaultModel(String name) {
            return fromName(name).getDefaultModel();
        }
    }

    /**
     * 构建器类
     */
    public static class Builder {

        private File sourceDir;

        private boolean addClassJavadoc = true;

        private boolean addMethodJavadoc = true;

        private boolean addParamJavadoc = true;

        private boolean addReturnJavadoc = true;

        private boolean addThrowsJavadoc = true;

        private List<String> excludePatterns = new ArrayList<>();

        private boolean includePrivateMethods = false;

        private boolean enableAi = false;

        private String aiApiKey = "";

        private String aiApiUrl = "";

        private String aiModel = "";

        private String aiProvider = "OPENAI";

        private boolean skipExistingJavadoc = true;

        // 并发控制配置默认值
        private boolean enableConcurrent = true;

        private int workerThreads = 4;

        private int aiBatchSize = 10;

        private long aiBatchDelayMs = 1000;

        private int maxRetries = 3;

        private long lockTimeoutMs = 30000;

        private boolean useOsLock = true;

        private long aiTimeoutSeconds = 60;

        /**
         * 设置源代码目录
         *
         * @param sourceDir 源代码目录
         * @return 构建器
         */
        public Builder sourceDir(File sourceDir) {
            this.sourceDir = sourceDir;
            return this;
        }

        /**
         * 设置是否添加类注释
         *
         * @param addClassJavadoc 是否添加类注释
         * @return 构建器
         */
        public Builder addClassJavadoc(boolean addClassJavadoc) {
            this.addClassJavadoc = addClassJavadoc;
            return this;
        }

        /**
         * 设置是否添加方法注释
         *
         * @param addMethodJavadoc 是否添加方法注释
         * @return 构建器
         */
        public Builder addMethodJavadoc(boolean addMethodJavadoc) {
            this.addMethodJavadoc = addMethodJavadoc;
            return this;
        }

        /**
         * 设置是否添加参数注释
         *
         * @param addParamJavadoc 是否添加参数注释
         * @return 构建器
         */
        public Builder addParamJavadoc(boolean addParamJavadoc) {
            this.addParamJavadoc = addParamJavadoc;
            return this;
        }

        /**
         * 设置是否添加返回值注释
         *
         * @param addReturnJavadoc 是否添加返回值注释
         * @return 构建器
         */
        public Builder addReturnJavadoc(boolean addReturnJavadoc) {
            this.addReturnJavadoc = addReturnJavadoc;
            return this;
        }

        /**
         * 设置是否添加异常注释
         *
         * @param addThrowsJavadoc 是否添加异常注释
         * @return 构建器
         */
        public Builder addThrowsJavadoc(boolean addThrowsJavadoc) {
            this.addThrowsJavadoc = addThrowsJavadoc;
            return this;
        }

        /**
         * 设置排除特定文件的模式列表
         *
         * @param excludePatterns 排除模式列表
         * @return 构建器
         */
        public Builder excludePatterns(List<String> excludePatterns) {
            this.excludePatterns = excludePatterns;
            return this;
        }

        /**
         * 设置是否包含私有方法
         *
         * @param includePrivateMethods 是否包含私有方法
         * @return 构建器
         */
        public Builder includePrivateMethods(boolean includePrivateMethods) {
            this.includePrivateMethods = includePrivateMethods;
            return this;
        }

        /**
         * 设置是否启用 AI 生成方法描述
         *
         * @param enableAi 是否启用 AI
         * @return 构建器
         */
        public Builder enableAi(boolean enableAi) {
            this.enableAi = enableAi;
            return this;
        }

        /**
         * 设置 AI API 密钥
         *
         * @param aiApiKey AI API 密钥
         * @return 构建器
         */
        public Builder aiApiKey(String aiApiKey) {
            this.aiApiKey = aiApiKey;
            return this;
        }

        /**
         * 设置 AI API 地址
         *
         * @param aiApiUrl AI API 地址
         * @return 构建器
         */
        public Builder aiApiUrl(String aiApiUrl) {
            this.aiApiUrl = aiApiUrl;
            return this;
        }

        /**
         * 设置 AI 模型名称
         *
         * @param aiModel AI 模型名称
         * @return 构建器
         */
        public Builder aiModel(String aiModel) {
            this.aiModel = aiModel;
            return this;
        }

        /**
         * 设置 AI 提供商名称
         *
         * @param aiProvider AI 提供商名称 (OPENAI, DEEPSEEK, MOONSHOT, ZHIPU, OLLAMA 等)
         * @return 构建器
         */
        public Builder aiProvider(String aiProvider) {
            this.aiProvider = aiProvider;
            return this;
        }

        /**
         * 设置是否跳过已有注释的方法
         *
         * @param skipExistingJavadoc 是否跳过
         * @return 构建器
         */
        public Builder skipExistingJavadoc(boolean skipExistingJavadoc) {
            this.skipExistingJavadoc = skipExistingJavadoc;
            return this;
        }

        // ==================== 并发控制配置 Builder ====================
        /**
         * 设置是否启用并发处理
         *
         * @param enableConcurrent 是否启用并发处理
         * @return 构建器
         */
        public Builder enableConcurrent(boolean enableConcurrent) {
            this.enableConcurrent = enableConcurrent;
            return this;
        }

        /**
         * 设置工作线程数
         *
         * @param workerThreads 工作线程数
         * @return 构建器
         */
        public Builder workerThreads(int workerThreads) {
            this.workerThreads = workerThreads;
            return this;
        }

        /**
         * 设置 AI 批处理大小
         *
         * @param aiBatchSize AI 批处理大小
         * @return 构建器
         */
        public Builder aiBatchSize(int aiBatchSize) {
            this.aiBatchSize = aiBatchSize;
            return this;
        }

        /**
         * 设置 AI 批次间隔（毫秒）
         *
         * @param aiBatchDelayMs AI 批次间隔
         * @return 构建器
         */
        public Builder aiBatchDelayMs(long aiBatchDelayMs) {
            this.aiBatchDelayMs = aiBatchDelayMs;
            return this;
        }

        /**
         * 设置最大重试次数
         *
         * @param maxRetries 最大重试次数
         * @return 构建器
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * 设置锁超时时间（毫秒）
         *
         * @param lockTimeoutMs 锁超时时间
         * @return 构建器
         */
        public Builder lockTimeoutMs(long lockTimeoutMs) {
            this.lockTimeoutMs = lockTimeoutMs;
            return this;
        }

        /**
         * 设置是否使用 OS 文件锁
         *
         * @param useOsLock 是否使用 OS 文件锁
         * @return 构建器
         */
        public Builder useOsLock(boolean useOsLock) {
            this.useOsLock = useOsLock;
            return this;
        }

        /**
         * 设置 AI 请求超时时间（秒）
         *
         * @param aiTimeoutSeconds AI 请求超时时间
         * @return 构建器
         */
        public Builder aiTimeoutSeconds(long aiTimeoutSeconds) {
            this.aiTimeoutSeconds = aiTimeoutSeconds;
            return this;
        }

        /**
         * 构建配置对象
         *
         * @return 配置对象
         */
        public JavadocAutofillConfig build() {
            return new JavadocAutofillConfig(this);
        }
    }
}
