package com.liyao.autofillDoc.concurrent;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Javadoc 智能合并器
 * 基于 AST 的三路合并，解决并发修改冲突
 */
public class JavadocMerger {

    private final Logger log;

    /**
     * 构造函数
     *
     * @param log 日志对象
     */
    public JavadocMerger(Logger log) {
        this.log = log;
    }

    /**
     * 三路合并
     *
     * @param baseContent    原始内容（处理前的文件内容）
     * @param ourContent     我们生成的 Javadoc 内容
     * @param currentContent 当前文件内容（可能已被外部修改）
     * @return 合并后的内容，如果合并失败返回 null
     */
    public String merge(String baseContent, String ourContent, String currentContent) {
        try {
            CompilationUnit baseCu = StaticJavaParser.parse(baseContent);
            CompilationUnit ourCu = StaticJavaParser.parse(ourContent);
            CompilationUnit theirCu = StaticJavaParser.parse(currentContent);

            // 构建我们的方法 Javadoc 映射
            Map<String, String> ourJavadocs = extractJavadocs(ourCu);

            // 遍历当前文件的方法，应用我们的 Javadoc
            theirCu.findAll(MethodDeclaration.class).forEach(theirMethod -> {
                String signature = getMethodSignature(theirMethod);

                // 如果我们的生成结果中有该方法的 Javadoc
                if (ourJavadocs.containsKey(signature)) {
                    // 只有当外部修改没有添加 Javadoc 时才应用我们的
                    if (!theirMethod.getJavadoc().isPresent()) {
                        String javadocText = ourJavadocs.get(signature);
                        theirMethod.setJavadocComment(javadocText);
                        log.debug("合并 Javadoc：{}", theirMethod.getNameAsString());
                    } else {
                        log.debug("保留外部 Javadoc：{}", theirMethod.getNameAsString());
                    }
                }
            });

            return theirCu.toString();

        } catch (Exception e) {
            log.warn("Javadoc 合并失败，保留当前文件内容：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 提取所有方法的 Javadoc
     *
     * @param cu 编译单元
     * @return 方法签名 -> Javadoc 文本映射
     */
    private Map<String, String> extractJavadocs(CompilationUnit cu) {
        Map<String, String> javadocs = new HashMap<>();

        cu.findAll(MethodDeclaration.class).forEach(method -> {
            if (method.getJavadoc().isPresent()) {
                String signature = getMethodSignature(method);
                String javadocText = method.getJavadoc().get().toText();
                javadocs.put(signature, javadocText);
            }
        });

        return javadocs;
    }

    /**
     * 获取方法签名（用于匹配方法）
     *
     * @param method 方法声明
     * @return 方法签名
     */
    private String getMethodSignature(MethodDeclaration method) {
        StringBuilder signature = new StringBuilder();
        signature.append(method.getNameAsString());
        signature.append("(");
        method.getParameters().forEach(param -> {
            signature.append(param.getType().asString());
            signature.append(",");
        });
        if (!method.getParameters().isEmpty()) {
            signature.setLength(signature.length() - 1); // 移除最后的逗号
        }
        signature.append(")");
        return signature.toString();
    }

    /**
     * 检测两个内容是否有 Javadoc 冲突
     *
     * @param baseContent     原始内容
     * @param currentContent  当前内容
     * @return 是否存在冲突
     */
    public boolean hasConflict(String baseContent, String currentContent) {
        try {
            CompilationUnit baseCu = StaticJavaParser.parse(baseContent);
            CompilationUnit currentCu = StaticJavaParser.parse(currentContent);

            // 提取基础版本的 Javadoc
            Map<String, String> baseJavadocs = extractJavadocs(baseCu);
            Map<String, String> currentJavadocs = extractJavadocs(currentCu);

            // 检查是否有 Javadoc 被修改或添加
            for (Map.Entry<String, String> entry : currentJavadocs.entrySet()) {
                String signature = entry.getKey();
                String currentJavadoc = entry.getValue();

                // 如果当前版本有 Javadoc，但基础版本没有，说明外部添加了
                if (!baseJavadocs.containsKey(signature)) {
                    return true;
                }

                // 如果 Javadoc 内容不同，说明外部修改了
                String baseJavadoc = baseJavadocs.get(signature);
                if (!baseJavadoc.equals(currentJavadoc)) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            log.warn("冲突检测失败：{}", e.getMessage());
            return true; // 解析失败时保守认为有冲突
        }
    }

    /**
     * 智能合并策略：优先保留外部修改，追加缺失的 Javadoc
     *
     * @param baseContent     原始内容
     * @param generatedResult 生成的结果（CompilationUnit）
     * @param currentContent  当前内容
     * @return 合并后的内容
     */
    public String smartMerge(String baseContent, CompilationUnit generatedResult, String currentContent) {
        try {
            CompilationUnit baseCu = StaticJavaParser.parse(baseContent);
            CompilationUnit currentCu = StaticJavaParser.parse(currentContent);

            // 提取各版本的 Javadoc
            Map<String, String> baseJavadocs = extractJavadocs(baseCu);
            Map<String, String> generatedJavadocs = extractJavadocs(StaticJavaParser.parse(generatedResult.toString()));
            Map<String, String> currentJavadocs = extractJavadocs(currentCu);

            // 遍历当前文件，应用智能合并
            currentCu.findAll(MethodDeclaration.class).forEach(method -> {
                String signature = getMethodSignature(method);
                String generatedJavadoc = generatedJavadocs.get(signature);
                String currentJavadoc = currentJavadocs.get(signature);
                String baseJavadoc = baseJavadocs.get(signature);

                // 策略：
                // 1. 如果当前没有 Javadoc，且生成版本有 -> 应用生成的
                // 2. 如果当前有 Javadoc，但和基础版本相同 -> 应用生成的（更新）
                // 3. 如果当前有 Javadoc，且和基础版本不同 -> 保留当前的（外部修改）
                if (generatedJavadoc != null) {
                    if (currentJavadoc == null) {
                        // 当前没有 Javadoc，应用生成的
                        method.setJavadocComment(generatedJavadoc);
                        log.debug("应用生成的 Javadoc：{}", method.getNameAsString());
                    } else if (currentJavadoc.equals(baseJavadoc)) {
                        // 当前 Javadoc 未被外部修改，更新为生成的
                        method.setJavadocComment(generatedJavadoc);
                        log.debug("更新 Javadoc：{}", method.getNameAsString());
                    }
                    // 其他情况保留当前的 Javadoc
                }
            });

            return currentCu.toString();

        } catch (Exception e) {
            log.warn("智能合并失败：{}", e.getMessage());
            return null;
        }
    }
}
