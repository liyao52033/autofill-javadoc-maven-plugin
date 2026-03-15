# AGENTS.md - autofill-javadoc-maven-plugin 知识库

**生成时间**: 2026-03-15  
**版本**: 1.2.0  
**分支**: 主分支

## 项目概述

Maven 插件，自动为 Java 方法生成 Javadoc 注释（参数、返回值、异常）。支持 Java 8 和 Java 17+，可选 AI 增强生成方法描述。

## 核心结构

```
autofill-javadoc-maven-plugin/
├── pom.xml                           # Maven 配置，插件坐标和发布配置
├── AGENTS.md                         # 本文档
└── src/main/java/com/liyao/autofillDoc/
    ├── JavadocAutofillMojo.java      # 插件入口（@Mojo）
    ├── config/
    │   └── JavadocAutofillConfig.java # 配置类 (Builder 模式 + AI 提供商枚举)
    ├── exception/
    │   └── JavadocProcessingException.java # 自定义运行时异常
    ├── service/                      # 核心业务逻辑 (4 个类)
    │   ├── FileProcessingService.java    # 文件遍历和协调
    │   ├── JavadocProcessor.java         # Javadoc 生成核心逻辑
    │   ├── MethodDescriptionService.java # 基于 AST 规则生成方法描述
    │   └── AiMethodDescriptionService.java # AI 生成方法描述 (可选)
    └── util/
        └── JavadocUtils.java             # Javadoc 工具方法
```

## 快速命令

```bash
# 构建插件
mvn clean install

# 打包不运行测试
mvn clean package

# 跳过测试
mvn clean install -DskipTests

# 运行插件
mvn autofill:autofill

# 带 AI 参数运行
mvn autofill:autofill -DenableAi=true -DaiProvider=DEEPSEEK -DaiApiKey=xxx

# 生成 Javadoc
mvn javadoc:javadoc
```

## 配置选项

### 基本配置 (pom.xml)

```xml
<configuration>
    <sourceDir>src/main/java</sourceDir>
    <addClassJavadoc>true</addClassJavadoc>
    <addMethodJavadoc>true</addMethodJavadoc>
    <addParamJavadoc>true</addParamJavadoc>
    <addReturnJavadoc>true</addReturnJavadoc>
    <addThrowsJavadoc>true</addThrowsJavadoc>
    <excludePatterns>
        <excludePattern>.*\/generated\/.*</excludePattern>
        <excludePattern>.*Test\.java</excludePattern>
    </excludePatterns>
    <includePrivateMethods>true</includePrivateMethods>
</configuration>
```

### AI 配置

```xml
<configuration>
    <enableAi>true</enableAi>
    <aiApiKey>your-api-key</aiApiKey>
    <aiProvider>DEEPSEEK</aiProvider>  <!-- OPENAI, DEEPSEEK, MOONSHOT, ZHIPU, OLLAMA, BAIDU, ALIBABA -->
</configuration>
```

| 提供商 | aiProvider 值 | 默认模型 | API 地址 |
|--------|---------------|----------|----------|
| DeepSeek | `DEEPSEEK` | deepseek-chat | https://api.deepseek.com |
| OpenAI | `OPENAI` | gpt-3.5-turbo | https://api.openai.com |
| Moonshot (Kimi) | `MOONSHOT` | moonshot-v1-8k | https://api.moonshot.cn |
| 智谱 AI | `ZHIPU` | glm-4 | https://open.bigmodel.cn |
| Ollama (本地) | `OLLAMA` | mistral | http://localhost:11434 |
| 百度文心 | `BAIDU` | ernie-bot-4 | https://aip.baidubce.com |
| 阿里通义 | `ALIBABA` | qwen-turbo | https://dashscope.aliyuncs.com |

## 代码约定

### Java 版本
- **目标**: Java 8 (`<release>8</release>`)
- **编码**: UTF-8

### 命名
- **类**: PascalCase (`JavadocProcessor`)
- **方法/变量**: camelCase (`processJavaFile`, `fileModified`)
- **包**: lowercase (`com.liyao.autofillDoc.service`)

### 导入顺序
1. Java 标准库 (`java.io.*`, `java.util.*`)
2. 第三方库 (`com.github.javaparser.*`, `org.apache.maven.*`)
3. 项目内部 (`com.liyao.autofillDoc.*`)

### 格式化
- **缩进**: 4 空格
- **无自动格式化**: 不使用 Spotless/Checkstyle

### Javadoc
- **语言**: 中文
- **范围**: 所有 public classes/methods
- **格式**:
  ```java
  /**
   * 方法描述
   *
   * @param paramName 参数描述
   * @return 返回值描述
   * @throws ExceptionType 异常描述
   */
  ```

### 类型使用
- **基本类型优先**: `boolean` 而非 `Boolean` (标志位)
- **集合接口**: `List<String>` 而非具体实现 (方法签名)
- **文件**: `java.io.File`
- **Optional**: JavaParser 的 `Optional` (`method.getJavadoc().isPresent()`)

### 错误处理
- **异常**: 继承 `RuntimeException` (`JavadocProcessingException`)
- **日志**: `org.apache.maven.plugin.logging.Log` 通过构造函数注入
- **级别**:
  - `log.error()` - 致命错误，停止处理
  - `log.warn()` - 非致命问题 (单个方法失败)
  - `log.info()` - 重要操作
  - `log.debug()` - 调试详情
- **策略**: 捕获具体异常，记录后继续 (非致命)

## 设计模式

| 模式 | 应用 |
|------|------|
| **Builder** | `JavadocAutofillConfig.Builder` (配置类) |
| **Service Layer** | 业务逻辑在服务类，Mojo 为入口 |
| **Utility** | 静态方法在 `util` 包 |

## 依赖版本

| 依赖 | 版本 | 用途 |
|------|------|------|
| JavaParser | 3.25.4 | AST 解析和 Javadoc 操作 |
| Maven Plugin API | 3.8.1 | 插件开发 |
| Maven Plugin Annotations | 3.6.0 | Mojo 注解处理 |
| OkHttp | 4.12.0 | AI API HTTP 请求 |
| Gson | 2.10.1 | JSON 解析 |

## 核心原则

1. **单一职责**: 每个服务类处理一个方面
2. **不可变配置**: Builder 模式 + final 字段
3. **优雅降级**: AI 失败自动降级到规则生成
4. **去重**: 公共逻辑提取到工具类
5. **Stream 优先**: Java 8 streams 过滤和迭代
6. **资源管理**: 无文件句柄泄漏

## 新增功能流程

1. 配置选项 → `JavadocAutofillConfig.java`
2. 业务逻辑 → 对应 service 类
3. Maven 参数 → `JavadocAutofillMojo.java` 暴露
4. 手动测试 → 示例 Java 项目

## 注意事项

- 包名 (`com.liyao.autofillDoc`) 未从 groupId (`io.github.liyao52033`) 派生 (个人命名风格)
- 无测试文件 (当前版本)
- AI 为可选功能，默认禁用
