package com.liyao.autofillDoc.service;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.google.gson.*;
import okhttp3.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * AI 方法描述生成服务
 * 使用 OkHttp 调用 AI API 生成智能的方法描述
 * 支持所有 OpenAI 兼容的 API 服务（OpenAI、DeepSeek、Moonshot、智谱 AI、Ollama 等）
 */
public class AiMethodDescriptionService {

    private final Logger log;
    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final OkHttpClient httpClient;
    private final Gson gson;

    /**
     * 构造函数
     *
     * @param log     日志对象
     * @param apiKey  AI API 密钥
     * @param apiUrl  AI API 地址
     * @param model   使用的模型名称
     */
    public AiMethodDescriptionService(Logger log, String apiKey, String apiUrl, String model) {
        this.log = log;
        this.apiKey = apiKey;
        this.apiUrl = normalizeApiUrl(apiUrl);
        this.model = model;

        // 配置 HTTP 客户端
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // 配置 Gson
        this.gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }

    /**
     * 标准化 API 基础 URL
     * 确保 URL 格式正确
     *
     * @param apiUrl 原始 API URL
     * @return 标准化后的基础 URL
     */
    private String normalizeApiUrl(String apiUrl) {
        if (apiUrl == null || apiUrl.isEmpty()) {
            return "https://api.openai.com/v1/chat/completions";
        }
        String normalized = apiUrl.trim();
        // 如果 URL 不包含 /chat/completions，添加它
        if (!normalized.endsWith("/chat/completions")) {
            if (normalized.endsWith("/")) {
                normalized = normalized + "chat/completions";
            } else if (normalized.endsWith("/v1")) {
                normalized = normalized + "/chat/completions";
            } else {
                normalized = normalized + "/v1/chat/completions";
            }
        }
        return normalized;
    }

    /**
     * 使用 AI 生成方法描述
     *
     * @param method 方法声明
     * @return AI 生成的方法描述
     */
    public String generateMethodDescription(MethodDeclaration method) {
        try {
            String prompt = buildPrompt(method);
            String response = callAiApi(prompt);
            if (response != null && !response.trim().isEmpty()) {
                return response.trim();
            }
        } catch (Exception e) {
            log.warn("AI 生成方法描述失败，使用默认描述：{}", method.getNameAsString(), e);
        }
        // 降级到默认描述
        return generateDefaultDescription(method);
    }

    /**
     * 构建 AI 提示词
     *
     * @param method 方法声明
     * @return 提示词
     */
    private String buildPrompt(MethodDeclaration method) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个 Java 代码文档生成助手。请为下面的 Java 方法生成一句简短的中文描述（不超过 50 字），");
        prompt.append("描述应该说明这个方法的作用。\n\n");
        prompt.append("方法信息：\n");
        prompt.append("- 方法名：").append(method.getNameAsString()).append("\n");

        // 添加返回类型
        prompt.append("- 返回类型：").append(method.getType().asString()).append("\n");

        // 添加参数信息
        if (!method.getParameters().isEmpty()) {
            prompt.append("- 参数列表：\n");
            for (Parameter param : method.getParameters()) {
                prompt.append("  - ").append(param.getType().asString())
                        .append(" ").append(param.getNameAsString()).append("\n");
            }
        }

        // 添加异常信息
        if (!method.getThrownExceptions().isEmpty()) {
            prompt.append("- 抛出异常：\n");
            method.getThrownExceptions().forEach(ex ->
                    prompt.append("  - ").append(ex.asString()).append("\n"));
        }

        // 添加方法体（如果有）
        if (method.getBody().isPresent()) {
            String methodBody = method.getBody().get().toString();
            // 限制方法体长度，避免 token 过多
            if (methodBody.length() > 1000) {
                methodBody = methodBody.substring(0, 1000) + "\n...（省略）";
            }
            prompt.append("- 方法体代码：\n```java\n").append(methodBody).append("\n```\n");
        }

        prompt.append("\n请直接返回方法描述，不要包含其他内容，不要用引号包裹。");

        return prompt.toString();
    }

    /**
     * 调用 AI API
     *
     * @param prompt 提示词
     * @return AI 返回的响应
     * @throws IOException 网络异常
     */
    private String callAiApi(String prompt) throws IOException {
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", buildMessages(prompt));
        requestBody.put("max_tokens", 150);
        requestBody.put("temperature", 0.3);

        String jsonBody = gson.toJson(requestBody);

        // 创建请求
        RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        // 执行请求
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("AI API 调用失败，状态码：" + response.code() + ", 错误：" + errorBody);
            }

            String responseBody = response.body().string();
            return parseResponse(responseBody);
        }
    }

    /**
     * 构建消息列表
     *
     * @param prompt 用户提示词
     * @return 消息列表
     */
    private List<Map<String, String>> buildMessages(String prompt) {
        List<Map<String, String>> messages = new ArrayList<>();

        // 系统消息
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个专业的 Java 代码文档生成助手，擅长用简洁准确的中文描述 Java 方法的功能。");
        messages.add(systemMessage);

        // 用户消息
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        return messages;
    }

    /**
     * 解析 AI 响应
     *
     * @param responseBody 响应 JSON 字符串
     * @return 提取的内容
     */
    private String parseResponse(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            
            // 检查是否有错误
            if (json.has("error")) {
                JsonObject error = json.getAsJsonObject("error");
                String errorMessage = error.has("message") ? error.get("message").getAsString() : "Unknown error";
                throw new RuntimeException("AI API 返回错误：" + errorMessage);
            }

            // 提取响应内容
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }

            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            JsonObject message = firstChoice.getAsJsonObject("message");
            if (message == null) {
                return null;
            }

            String content = message.get("content").getAsString();
            return content != null ? content.trim() : null;
        } catch (Exception e) {
            log.debug("解析 AI 响应失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 生成默认描述（降级方案）
     *
     * @param method 方法声明
     * @return 默认描述
     */
    private String generateDefaultDescription(MethodDeclaration method) {
        String methodName = method.getNameAsString();

        // 处理 getter/setter
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return "获取" + capitalize(methodName.substring(3)) + "的值";
        }
        if (methodName.startsWith("set") && methodName.length() > 3) {
            return "设置" + capitalize(methodName.substring(3)) + "的值";
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return "判断是否" + capitalize(methodName.substring(2));
        }
        if (methodName.startsWith("has") && methodName.length() > 3) {
            return "检查是否包含" + capitalize(methodName.substring(3));
        }
        if (methodName.startsWith("add") && methodName.length() > 3) {
            return "添加" + capitalize(methodName.substring(3));
        }
        if (methodName.startsWith("remove") && methodName.length() > 6) {
            return "移除" + capitalize(methodName.substring(6));
        }
        if (methodName.startsWith("create") && methodName.length() > 6) {
            return "创建" + capitalize(methodName.substring(6));
        }
        if (methodName.startsWith("delete") && methodName.length() > 6) {
            return "删除" + capitalize(methodName.substring(6));
        }
        if (methodName.startsWith("update") && methodName.length() > 6) {
            return "更新" + capitalize(methodName.substring(6));
        }
        if (methodName.startsWith("find") && methodName.length() > 4) {
            return "查询" + capitalize(methodName.substring(4));
        }
        if (methodName.startsWith("load") && methodName.length() > 4) {
            return "加载" + capitalize(methodName.substring(4));
        }
        if (methodName.startsWith("save") && methodName.length() > 4) {
            return "保存" + capitalize(methodName.substring(4));
        }
        if (methodName.startsWith("build") && methodName.length() > 5) {
            return "构建" + capitalize(methodName.substring(5));
        }
        if (methodName.startsWith("init") && methodName.length() > 4) {
            return "初始化" + capitalize(methodName.substring(4));
        }
        if (methodName.startsWith("validate") && methodName.length() > 8) {
            return "验证" + capitalize(methodName.substring(8));
        }
        if (methodName.startsWith("check") && methodName.length() > 5) {
            return "检查" + capitalize(methodName.substring(5));
        }
        if (methodName.startsWith("parse") && methodName.length() > 5) {
            return "解析" + capitalize(methodName.substring(5));
        }
        if (methodName.startsWith("convert") && methodName.length() > 7) {
            return "转换" + capitalize(methodName.substring(7));
        }
        if (methodName.startsWith("handle") && methodName.length() > 6) {
            return "处理" + capitalize(methodName.substring(6));
        }
        if (methodName.startsWith("process") && methodName.length() > 7) {
            return "处理" + capitalize(methodName.substring(7));
        }
        if (methodName.startsWith("execute") && methodName.length() > 7) {
            return "执行" + capitalize(methodName.substring(7));
        }
        if (methodName.startsWith("calculate") && methodName.length() > 9) {
            return "计算" + capitalize(methodName.substring(9));
        }
        if (methodName.equals("toString")) {
            return "返回对象的字符串表示";
        }
        if (methodName.equals("equals")) {
            return "判断对象是否相等";
        }
        if (methodName.equals("hashCode")) {
            return "返回对象的哈希码";
        }
        if (methodName.equals("clone")) {
            return "克隆当前对象";
        }
        if (methodName.equals("compareTo")) {
            return "比较当前对象与另一个对象";
        }

        return "执行" + methodName + "操作";
    }

    /**
     * 首字母小写
     *
     * @param str 输入字符串
     * @return 首字母小写的字符串
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        // 处理连续大写的情况，如 "ID" -> "ID"
        if (str.length() > 1 && Character.isUpperCase(str.charAt(1))) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}