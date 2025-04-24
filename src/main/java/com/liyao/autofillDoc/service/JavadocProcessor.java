package com.liyao.autofillDoc.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.liyao.autofillDoc.config.JavadocAutofillConfig;
import com.liyao.autofillDoc.exception.JavadocProcessingException;
import com.liyao.autofillDoc.util.JavadocUtils;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Javadoc处理器
 * 负责处理Java文件的Javadoc生成和更新
 */
public class JavadocProcessor {

    private final Log log;
    private final JavadocAutofillConfig config;
    private final MethodDescriptionService methodDescriptionService;

    /**
     * 构造函数
     *
     * @param log    日志对象
     * @param config 配置对象
     */
    public JavadocProcessor(Log log, JavadocAutofillConfig config) {
        this.log = log;
        this.config = config;
        this.methodDescriptionService = new MethodDescriptionService();
    }

    /**
     * 处理枚举类型的JavaDoc注释
     * 此方法遍历给定的编译单元中的所有类型，识别出枚举类型，并为其添加JavaDoc注释
     * 如果枚举类型或其常量缺少JavaDoc注释，则会自动生成一个简单的描述性注释
     *
     * @param cu           编译单元，代表一个Java源文件
     * @param fileModified 单元素数组，用于指示文件是否已被修改
     */
    private void processEnumJavadoc(CompilationUnit cu, boolean[] fileModified) {
        // 遍历编译单元中的所有类型，过滤出枚举类型
        cu.getTypes().stream()
                .filter(BodyDeclaration::isEnumDeclaration)
                .forEach(enumType -> {
                    // 如果枚举类型缺少JavaDoc注释，则生成并设置一个简单的描述性注释
                    if (!enumType.getJavadoc().isPresent()) {
                        String doc = enumType.getNameAsString() + " 枚举的描述\n";
                        enumType.setJavadocComment(doc);
                        log.debug("添加枚举注释: " + enumType.getNameAsString());
                        fileModified[0] = true;
                    }
                    // 如果枚举类型是EnumDeclaration的实例，则为其每个常量添加JavaDoc注释
                    if (enumType instanceof EnumDeclaration) {
                        ((EnumDeclaration) enumType).getEntries().forEach(entry -> {
                            // 如果枚举常量缺少JavaDoc注释，则生成并设置一个简单的描述性注释
                            if (!entry.getJavadoc().isPresent()) {
                                String entryDoc = entry.getNameAsString() + " 枚举常量的描述\n";
                                entry.setJavadocComment(entryDoc);
                                log.debug("添加枚举常量注释: " + entry.getNameAsString());
                                fileModified[0] = true;
                            }
                        });
                    }
                });
    }

    /**
     * 处理Java文件，根据配置添加或修改JavaDoc注释
     *
     * @param file 待处理的Java文件
     * @return 如果文件被处理并成功修改，则返回true；否则返回false
     * @throws JavadocProcessingException 如果文件处理过程中发生错误
     */
    public boolean processJavaFile(File file) {
        try {
            // 检查文件是否符合排除条件
            if (shouldExcludeFile(file)) {
                log.info("根据排除模式跳过文件: " + file.getPath());
                return false;
            }

            // 解析Java文件内容
            CompilationUnit cu = StaticJavaParser.parse(file);

            // 初始化标志，用于指示文件是否被修改
            boolean[] fileModified = { false };

            // 根据配置处理类的JavaDoc注释
            if (config.isAddClassJavadoc()) {
                processTypeJavadoc(cu, fileModified);
            }

            // 添加对枚举注释的处理
            processEnumJavadoc(cu, fileModified);

            // 根据配置处理方法的JavaDoc注释
            if (config.isAddMethodJavadoc() || config.isAddParamJavadoc() ||
                    config.isAddReturnJavadoc() || config.isAddThrowsJavadoc()) {
                processMethodJavadoc(cu, fileModified);
            }

            // 如果文件被修改，尝试写入修改内容
            if (fileModified[0]) {
                try {
                    String newContent = cu.toString();
                    String oldContent = new String(Files.readAllBytes(file.toPath()));

                    // 只有当内容实际发生变化时才写入文件并打印日志
                    if (!newContent.equals(oldContent)) {
                        Files.write(file.toPath(), newContent.getBytes());
                        log.info("处理完成: " + file.getPath());
                        return true;
                    } else {
                        return false;
                    }
                } catch (IOException e) {
                    throw JavadocProcessingException.createFileProcessingException(file.getPath(), e);
                }
            }

            // 如果文件未被修改，返回false
            return false;
        } catch (Exception e) {
            log.error("处理失败: " + file.getPath(), e);
            throw JavadocProcessingException.createFileProcessingException(file.getPath(), e);
        }
    }

    /**
     * 处理类型的Javadoc注释
     *
     * @param cu           编译单元
     * @param fileModified 文件是否被修改的标记
     */
    private void processTypeJavadoc(CompilationUnit cu, boolean[] fileModified) {
        cu.getTypes().forEach(type -> {
            if (type.getJavadoc().isPresent()) {
                // 移除非标准标签
                Javadoc javadoc = type.getJavadoc().get();
                List<JavadocBlockTag> tags = javadoc.getBlockTags();
                boolean typeModified = removeNonStandardTags(tags);
                if (typeModified) {
                    type.setJavadocComment(javadoc.toText());
                    fileModified[0] = true;
                }
            } else {
                String typeKeyword = JavadocUtils.getTypeKeyword(type);
                String doc = type.getNameAsString() + " " + typeKeyword + "的描述\n";
                type.setJavadocComment(doc);
                fileModified[0] = true;
            }
        });
    }

    /**
     * 检查文件是否被排除
     *
     * @param file 要检查的文件
     * @return 如果文件应该被排除则返回true，否则返回false
     */
    private boolean shouldExcludeFile(File file) {
        String filePath = file.getPath();
        // 如果排除模式列表为空，则不排除任何文件
        if (config.getExcludePatterns() == null || config.getExcludePatterns().isEmpty()) {
            return false;
        }

        // 检查文件路径是否匹配任何排除模式
        for (String pattern : config.getExcludePatterns()) {
            if (filePath.matches(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 处理方法的Javadoc注释
     * 此方法遍历编译单元中的所有可调用声明，特别是方法声明，并根据配置和现有注释添加或修改Javadoc
     *
     * @param cu           编译单元，代表一个源文件及其包含的代码结构
     * @param fileModified 一个布尔数组，用于指示文件是否已被修改
     */
    private void processMethodJavadoc(CompilationUnit cu, boolean[] fileModified) {
        // 处理普通方法声明
        processMethodDeclarations(cu, fileModified);

        // 处理注解成员声明
        processAnnotationMembers(cu, fileModified);
    }

    /**
     * 处理方法声明
     * 负责处理方法级别的Javadoc生成和更新
     *
     * @param cu           编译单元
     * @param fileModified 文件修改标记
     */
    private void processMethodDeclarations(CompilationUnit cu, boolean[] fileModified) {
        cu.findAll(CallableDeclaration.class).forEach(decl -> {
            try {
                if (decl instanceof MethodDeclaration) {
                    MethodDeclaration method = (MethodDeclaration) decl;
                    if (!config.isIncludePrivateMethods() && method.isPrivate()) {
                        return; // 跳过私有方法
                    }

                    // 初始化或获取现有Javadoc
                    Javadoc javadoc = initOrGetMethodJavadoc(method);
                    if (javadoc == null)
                        return;

                    // 处理各个部分注释
                    boolean methodModified = processMethodJavadocParts(method, javadoc);

                    if (methodModified) {
                        method.setJavadocComment(javadoc.toText());
                        fileModified[0] = true;
                        log.debug("处理方法: " + method.getNameAsString());
                    }
                }
            } catch (Exception e) {
                log.warn("处理方法失败: " + decl.getNameAsString(), e);
            }
        });
    }

    /**
     * 处理注解成员
     * 负责处理注解成员级别的Javadoc生成和更新
     *
     * @param cu           编译单元
     * @param fileModified 文件修改标记
     */
    private void processAnnotationMembers(CompilationUnit cu, boolean[] fileModified) {
        cu.findAll(AnnotationMemberDeclaration.class).forEach(annoMember -> {
            try {
                Javadoc javadoc = annoMember.getJavadoc().orElse(new Javadoc(new JavadocDescription()));
                List<JavadocBlockTag> tags = javadoc.getBlockTags(); // ✅ 使用原始引用
                boolean modified = false;

                if (config.isAddReturnJavadoc()) {
                    modified = processAnnotationReturnTags(annoMember, javadoc, tags);
                }

                if (modified) {
                    annoMember.setJavadocComment(javadoc.toText());
                    fileModified[0] = true;
                    log.debug("处理注解成员: " + annoMember.getNameAsString());
                }
            } catch (Exception e) {
                log.warn("处理注解成员失败: " + annoMember.getNameAsString(), e);
            }
        });
    }

    /**
     * 初始化或获取方法的Javadoc
     * 根据配置决定是否创建新的Javadoc或使用现有的
     *
     * @param method 方法声明
     * @return Javadoc对象，如果不需要处理则返回null
     */
    private Javadoc initOrGetMethodJavadoc(MethodDeclaration method) {
        if (method.getJavadoc().isPresent()) {
            return method.getJavadoc().get();
        } else if (config.isAddMethodJavadoc()) {
            return new Javadoc(JavadocDescription.parseText(
                    methodDescriptionService.generateMethodDescription(method)));
        }
        return null;
    }

    /**
     * 处理方法Javadoc的各个部分
     * 包括参数、返回值和异常注释
     *
     * @param method  方法声明
     * @param javadoc Javadoc对象
     * @return 是否修改了Javadoc
     */
    private boolean processMethodJavadocParts(MethodDeclaration method, Javadoc javadoc) {
        List<JavadocBlockTag> tags = javadoc.getBlockTags();
        boolean methodModified = false;

        methodModified |= removeNonStandardTags(tags);
        methodModified |= processMethodParams(method, javadoc, tags);
        methodModified |= processMethodReturn(method, javadoc, tags);
        methodModified |= processMethodThrows(method, javadoc, tags);

        return methodModified;
    }

    /**
     * 处理方法参数注释
     *
     * @param method  方法声明
     * @param javadoc Javadoc对象
     * @param tags    标签列表
     * @return 是否修改了Javadoc
     */
    private boolean processMethodParams(MethodDeclaration method, Javadoc javadoc, List<JavadocBlockTag> tags) {
        return config.isAddParamJavadoc() && processParamTags(method, javadoc, tags);
    }

    /**
     * 处理方法返回值注释
     *
     * @param method  方法声明
     * @param javadoc Javadoc对象
     * @param tags    标签列表
     * @return 是否修改了Javadoc
     */
    private boolean processMethodReturn(MethodDeclaration method, Javadoc javadoc, List<JavadocBlockTag> tags) {
        if (!config.isAddReturnJavadoc())
            return false;

        if (!method.getType().isVoidType()) {
            return processReturnTag(method, javadoc, tags);
        } else {
            JavadocBlockTag existingReturnTag = findBlockTag(tags, JavadocBlockTag.Type.RETURN, null);
            if (existingReturnTag != null) {
                removeAllReturnTags(tags);
                return true;
            }
        }
        return false;
    }

    /**
     * 处理方法异常注释
     *
     * @param method  方法声明
     * @param javadoc Javadoc对象
     * @param tags    标签列表
     * @return 是否修改了Javadoc
     */
    private boolean processMethodThrows(MethodDeclaration method, Javadoc javadoc, List<JavadocBlockTag> tags) {
        return config.isAddThrowsJavadoc() && processThrowsTags(method, javadoc, tags);
    }

    /**
     * 处理参数标签
     * 为方法参数添加Javadoc标签，并正确处理泛型参数
     * 若标签存在但内容为空，则自动补全标准描述
     *
     * @param method  方法声明
     * @param javadoc Javadoc对象
     * @param tags    标签列表
     * @return 是否修改了Javadoc
     */
    private boolean processParamTags(MethodDeclaration method, Javadoc javadoc, List<JavadocBlockTag> tags) {
        boolean modified = false;

        // 1. 处理泛型类型参数 @param <T>
        for (TypeParameter typeParam : method.getTypeParameters()) {
            String paramName = typeParam.getNameAsString();

            // 查找现有的泛型参数标签
            JavadocBlockTag existingTag = findBlockTag(tags, JavadocBlockTag.Type.PARAM, "<" + paramName + ">");

            if (existingTag == null || existingTag.getContent().toText().trim().isEmpty()) {
                if (existingTag != null) {
                    tags.remove(existingTag);
                }

                String description = "泛型类型参数 " + paramName;
                javadoc.addBlockTag("param", "<" + paramName + ">", description);
                modified = true;
            }
        }

        // 2. 处理普通参数
        for (Parameter param : method.getParameters()) {
            String paramName = param.getNameAsString();
            String paramType = param.getType().asString();

            JavadocBlockTag existingTag = findBlockTag(tags, JavadocBlockTag.Type.PARAM, paramName);

            if (existingTag == null || existingTag.getContent().toText().trim().isEmpty()) {
                if (existingTag != null) {
                    tags.remove(existingTag);
                }

                String paramDescription = JavadocUtils.generateParamDescription(paramName);
                if (paramType.contains("<")) {
                    paramDescription += "，类型为 " + JavadocUtils.cleanAngleBrackets(paramType);
                }

                javadoc.addBlockTag("param", paramName, paramDescription);
                modified = true;
            } else if (existingTag.getContent().toText().contains("<")
                    || existingTag.getContent().toText().contains(">")) {
                String cleanContent = JavadocUtils.cleanAngleBrackets(existingTag.getContent().toText());
                tags.remove(existingTag);
                javadoc.addBlockTag("param", paramName, cleanContent);
                modified = true;
            }
        }

        return modified;
    }

    /**
     * 处理返回值标签
     * 为非void方法添加返回值标签，并处理泛型类型
     * 对于void方法，移除任何现有的@return标签
     * 若@return标签存在但内容为空，则自动补全标准描述
     *
     * @param node    节点
     * @param javadoc Javadoc对象
     * @param tags    标签列表
     * @return 是否修改了Javadoc
     */
    private boolean processReturnTag(Node node, Javadoc javadoc, List<JavadocBlockTag> tags) {
        String returnType;

        if (node instanceof MethodDeclaration) {
            MethodDeclaration method = (MethodDeclaration) node;
            if (method.getType().isVoidType()) {
                // void 方法，不需要 @return，清理旧的
                removeAllReturnTags(tags);
                return true;
            }
            returnType = method.getType().asString();
        } else if (node instanceof AnnotationMemberDeclaration) {
            AnnotationMemberDeclaration annoMember = (AnnotationMemberDeclaration) node;
            returnType = annoMember.getType().asString();
        } else {
            return false;
        }

        // 删除所有旧的 @return 标签（防止出现多个）
        removeAllReturnTags(tags);

        String returnDescription = JavadocUtils.generateReturnDescription(returnType);
        javadoc.addBlockTag("return", returnDescription);
        return true;
    }

    /**
     * 处理抛出异常标签
     * 为方法添加抛出异常的Javadoc标签，并处理泛型异常
     * 若@throws标签存在但内容为空，则自动补全标准描述
     */
    private void removeAllReturnTags(List<JavadocBlockTag> tags) {
        Iterator<JavadocBlockTag> iterator = tags.iterator();
        while (iterator.hasNext()) {
            JavadocBlockTag tag = iterator.next();
            if (tag.getType() == JavadocBlockTag.Type.RETURN) {
                // 打印或记录被移除的标签信息
                log.debug("移除的标签: " + tag.toString());
                iterator.remove();
            }
        }
    }

    /**
     * 处理注解成员声明的返回标签
     * 该方法主要用于检查注解成员的文档注释中是否缺少返回标签（@return），如果缺少，则自动生成并添加
     * 如果存在返回标签但内容为空或包含不合适的字符（如尖括号），则进行清理或更新
     *
     * @param annoMember 注解成员声明对象
     * @param javadoc    Javadoc对象
     * @param tags       Javadoc块标签列表
     * @return boolean 表示是否对文档进行了修改
     */
    private boolean processAnnotationReturnTags(AnnotationMemberDeclaration annoMember, Javadoc javadoc,
            List<JavadocBlockTag> tags) {
        boolean modified = false;

        // 获取注解成员返回类型
        String returnType = annoMember.getType().asString();
        JavadocBlockTag existingReturnTag = findBlockTag(tags, JavadocBlockTag.Type.RETURN, null);

        // 如果没有 @return 标签或者现有标签内容为空，移除现有标签并添加新标签
        if (existingReturnTag == null || existingReturnTag.getContent().toText().trim().isEmpty()) {
            if (existingReturnTag != null) {
                removeAllReturnTags(tags);
            }

            String returnDescription = JavadocUtils.generateReturnDescription(returnType);
            javadoc.addBlockTag("return", returnDescription);
            modified = true;
        } else {
            // 如果已有 @return 标签且内容包含尖括号，清理内容
            String content = existingReturnTag.getContent().toText();
            if (content.contains("<") || content.contains(">")) {
                removeAllReturnTags(tags);
                String cleaned = JavadocUtils.cleanAngleBrackets(content);
                javadoc.addBlockTag("return", cleaned);
                modified = true;
            }
        }

        return modified;
    }

    /**
     * 处理异常标签
     * 为方法声明的异常添加Javadoc标签，并正确处理泛型异常类型
     * 支持标准的@throws标签和非标准的@exception标签
     *
     * @param method  方法声明
     * @param javadoc Javadoc对象
     * @param tags    标签列表
     * @return 是否修改了Javadoc
     */
    private boolean processThrowsTags(MethodDeclaration method, Javadoc javadoc, List<JavadocBlockTag> tags) {
        boolean modified = false;

        List<ReferenceType> thrownExceptions = method.getThrownExceptions();
        for (ReferenceType exception : thrownExceptions) {
            String exceptionName = exception.toString();
            String cleanExceptionName = JavadocUtils.cleanAngleBrackets(exceptionName);

            // 查找现有的标准@throws标签
            JavadocBlockTag existingThrowsTag = findBlockTag(tags, JavadocBlockTag.Type.THROWS, exceptionName);
            if (existingThrowsTag == null) {
                // 尝试查找使用清理后的异常名称的标签
                existingThrowsTag = findBlockTag(tags, JavadocBlockTag.Type.THROWS, cleanExceptionName);
            }

            // 使用工具类查找非标准的@exception标签
            if (existingThrowsTag == null) {
                existingThrowsTag = JavadocUtils.findCustomBlockTag(tags, "exception", exceptionName);
                if (existingThrowsTag == null) {
                    existingThrowsTag = JavadocUtils.findCustomBlockTag(tags, "exception", cleanExceptionName);
                }
            }

            // 如果标签存在但内容为空，或者标签不存在，添加新标签
            if (existingThrowsTag == null || existingThrowsTag.getContent().isEmpty()) {
                // 如果存在空标签，先移除它
                if (existingThrowsTag != null) {
                    tags.remove(existingThrowsTag);
                }
                // 使用工具类生成异常描述
                String throwsDescription = JavadocUtils.generateThrowsDescription(cleanExceptionName);
                javadoc.addBlockTag("throws", cleanExceptionName, throwsDescription);
                modified = true;
            } else if (existingThrowsTag.getContent().toText().contains("<")
                    || existingThrowsTag.getContent().toText().contains(">")) {
                // 如果现有标签包含尖括号，清理它们
                String cleanContent = JavadocUtils.cleanAngleBrackets(existingThrowsTag.getContent().toText());
                tags.remove(existingThrowsTag);
                javadoc.addBlockTag("throws", cleanExceptionName, cleanContent);
                modified = true;
            }
        }

        return modified;
    }

    /**
     * 查找指定类型和名称的标签
     *
     * @param tags 标签列表
     * @param type 标签类型
     * @param name 标签名称（可为null）
     * @return 找到的标签，如果没有找到则返回null
     */
    private JavadocBlockTag findBlockTag(List<JavadocBlockTag> tags, JavadocBlockTag.Type type, String name) {
        return JavadocUtils.findBlockTag(tags, type, name);
    }

    /**
     * 移除Javadoc中的非标准标签，仅保留标准标签
     *
     * @param tags 标签列表
     * @return 是否有标签被移除
     */
    private boolean removeNonStandardTags(List<JavadocBlockTag> tags) {
        boolean modified = false;
        // 标准Javadoc标签集合
        final List<String> standardTags = Arrays.asList(
                "author", "deprecated", "exception", "param", "return", "see", "serial", "serialData", "serialField",
                "since", "throws", "version");
        // 使用迭代器安全移除
        Iterator<JavadocBlockTag> iterator = tags.iterator();
        while (iterator.hasNext()) {
            JavadocBlockTag tag = iterator.next();
            String tagName = tag.getTagName();
            if (!standardTags.contains(tagName)) {
                iterator.remove();
                modified = true;
            }
        }
        return modified;
    }
}