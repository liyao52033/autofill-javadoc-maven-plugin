package com.liyao.autofillDoc;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Mojo(name = "autofill", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class JavadocAutofillMojo extends AbstractMojo {

    /**
     * 源代码目录
     */
    @org.apache.maven.plugins.annotations.Parameter(property = "sourceDir", defaultValue = "${project.build.sourceDirectory}", required = true)
    private File sourceDir;

    /**
     * 执行插件
     */
    @Override
    public void execute() {
        if (!sourceDir.exists()) {
            getLog().warn("源代码目录不存在: " + sourceDir);
            return;
        }

        try (Stream<Path> paths = Files.walk(sourceDir.toPath())) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> processJavaFile(path.toFile()));
        } catch (IOException e) {
            getLog().error("遍历 Java 文件失败", e);
        }
    }

    /**
     * 生成基于 AST 的方法描述
     *
     * @param method 方法
     * @return 方法描述
     */
    private String generateAstBasedMethodDescription(MethodDeclaration method) {
        if (method.getNameAsString().startsWith("get")) {
            return "获取" + method.getNameAsString().substring(3);
        }
        if (method.getNameAsString().startsWith("set")) {
            return "设置" + method.getNameAsString().substring(3);
        }

        List<MethodCallExpr> methodCalls = method.findAll(MethodCallExpr.class);
        List<IfStmt> ifStmts = method.findAll(IfStmt.class);
        List<ForStmt> forStmts = method.findAll(ForStmt.class);
        List<WhileStmt> whileStmts = method.findAll(WhileStmt.class);
        List<ReturnStmt> returnStmts = method.findAll(ReturnStmt.class);
        List<ThrowStmt> throwStmts = method.findAll(ThrowStmt.class);

        boolean hasDbCall = methodCalls.stream().anyMatch(mc ->
                mc.getNameAsString().toLowerCase().contains("save") ||
                        mc.getNameAsString().toLowerCase().contains("update") ||
                        mc.getNameAsString().toLowerCase().contains("delete") ||
                        mc.getNameAsString().toLowerCase().contains("insert"));

        boolean hasLogCall = methodCalls.stream().anyMatch(mc ->
                mc.getNameAsString().toLowerCase().contains("log") ||
                        mc.getNameAsString().toLowerCase().contains("debug") ||
                        mc.getNameAsString().toLowerCase().contains("info"));

        if (!methodCalls.isEmpty() && methodCalls.stream().anyMatch(mc -> mc.getNameAsString().contains("find"))) {
            return "查询并返回相关数据";
        }

        if (hasDbCall) {
            return "执行数据库操作";
        }

        if (!ifStmts.isEmpty() && !returnStmts.isEmpty()) {
            return "根据条件判断返回不同结果";
        }

        if (!forStmts.isEmpty() || !whileStmts.isEmpty()) {
            return "遍历并处理集合数据";
        }

        if (!returnStmts.isEmpty()) {
            return "返回处理结果";
        }

        if (!throwStmts.isEmpty()) {
            return "执行操作并抛出异常";
        }

        if (hasLogCall) {
            return "记录日志操作";
        }

        return "执行" + method.getNameAsString() + "操作";
    }


   /**
     * 处理java文件生成注释
     *
     * @param file Java 文件
     */
    private void processJavaFile(File file) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);

            boolean isEmpty = cu.getTypes().isEmpty() ||
                    cu.getTypes().stream().allMatch(t -> t.getMembers().isEmpty());
            if (isEmpty) {
                getLog().info("跳过空类文件: " + file.getPath());
                return;
            }

            // === 新增：为类/接口/枚举添加缺失的类注释 ===
            boolean[] fileModified = {false}; // 使用数组作为可变引用
            
            cu.getTypes().forEach(type -> {
                if (type.getJavadoc().isEmpty()) {
                    String typeKeyword = "类";
                    if (type instanceof ClassOrInterfaceDeclaration classOrInterface) {
                        if (classOrInterface.isInterface()) {
                            typeKeyword = "接口";
                        } else {
                            typeKeyword = "类";
                        }
                    } else if (type.isEnumDeclaration()) {
                        typeKeyword = "枚举";
                    }

                 //   String doc = "/**\n * " + type.getNameAsString() + " " + typeKeyword + "的描述\n */";
                    String doc = type.getNameAsString() + " " + typeKeyword + "的描述\n";
                    type.setJavadocComment(doc);
                    getLog().debug("添加类注释: " + type.getNameAsString());
                    fileModified[0] = true;
                }
            });

            // === 为方法自动补充 Javadoc 参数 ===
            cu.findAll(CallableDeclaration.class).forEach(decl -> {
                try {
                    // 如果是 MethodDeclaration，则可以修改 Javadoc
                    if (decl instanceof MethodDeclaration method) {
                        Javadoc javadoc;
                        // 如果方法没有 Javadoc 注释，创建新的 Javadoc 注释
                        if (method.getJavadoc().isPresent()) {
                            javadoc = method.getJavadoc().get();
                        } else {
                            // 如果没有 Javadoc，初始化一个空的并添加方法描述
                            javadoc = new Javadoc(JavadocDescription.parseText(generateAstBasedMethodDescription(method)));
                            fileModified[0] = true;
                        }

                        List<JavadocBlockTag> tags = javadoc.getBlockTags();
                        boolean methodModified = false;
                        
                        // 处理参数标签
                        for (Parameter param : method.getParameters()) {
                            String paramName = param.getNameAsString();
                            // 清除参数名中的<>符号用于描述
                            String cleanParamName = paramName.replaceAll("[<>]", "");
                            
                            // 查找现有的参数标签
                            JavadocBlockTag existingTag = null;
                            for (JavadocBlockTag tag : tags) {
                                if (tag.getType() == JavadocBlockTag.Type.PARAM && 
                                    tag.getName().isPresent() && 
                                    tag.getName().get().equals(paramName)) {
                                    existingTag = tag;
                                    break;
                                }
                            }
                            
                            // 如果标签存在但内容为空，或者标签不存在，添加新标签
                            if (existingTag == null || existingTag.getContent().isEmpty()) {
                                // 如果存在空标签，先移除它
                                if (existingTag != null) {
                                    tags.remove(existingTag);
                                }
                                // 添加新标签
                                String paramDescription = "参数 " + cleanParamName + " 的描述";
                                javadoc.addBlockTag("param", paramName, paramDescription);
                                methodModified = true;
                            }
                        }

                        // 处理返回值标签
                        if (!method.getType().isVoidType()) {

                            // 查找现有的返回标签
                            JavadocBlockTag existingReturnTag = null;
                            for (JavadocBlockTag tag : tags) {
                                if (tag.getType() == JavadocBlockTag.Type.RETURN) {
                                    existingReturnTag = tag;
                                    break;
                                }
                            }
                            
                            // 如果返回标签存在但内容为空，或者标签不存在，添加新标签
                            if (existingReturnTag == null || existingReturnTag.getContent().isEmpty()) {
                                // 如果存在空标签，先移除它
                                if (existingReturnTag != null) {
                                    tags.remove(existingReturnTag);
                                }
                                // 添加新标签
                                String returnDescription = "返回值类型为 " + method.getType() + " 的描述";
                                String cleanDescription = returnDescription.replaceAll("[<>]", "");
                                javadoc.addBlockTag("return", cleanDescription);
                                methodModified = true;
                            }  else {
                                // 如果返回标签存在且内容不为空，替换其中的 <> 符号
                                String existingContent = existingReturnTag.getContent().toText();
                                String cleanContent = existingContent.replaceAll("[<>]", "");
                                if (!existingContent.equals(cleanContent)) {
                                    // 移除旧标签
                                    tags.remove(existingReturnTag);
                                    // 添加新标签
                                    javadoc.addBlockTag("return", cleanContent);
                                    methodModified = true;
                                }
                            }
                        }

                        // 处理异常标签
                        List<ReferenceType> thrownExceptions = method.getThrownExceptions();
                        for (ReferenceType exception : thrownExceptions) {
                            String exceptionName = exception.toString();
                            // 查找现有的异常标签
                            JavadocBlockTag existingThrowsTag = null;
                            for (JavadocBlockTag tag : tags) {
                                if (tag.getType() == JavadocBlockTag.Type.THROWS &&
                                        tag.getName().isPresent() &&
                                        tag.getName().get().equals(exceptionName)) {
                                    existingThrowsTag = tag;
                                    break;
                                }
                            }

                            // 如果标签存在但内容为空，或者标签不存在，添加新标签
                            if (existingThrowsTag == null || existingThrowsTag.getContent().isEmpty()) {
                                // 如果存在空标签，先移除它
                                if (existingThrowsTag != null) {
                                    tags.remove(existingThrowsTag);
                                }
                                // 添加新标签
                                String throwsDescription = "抛出 " + exceptionName + " 异常的描述";
                                javadoc.addBlockTag("throws", exceptionName, throwsDescription);
                                methodModified = true;
                            }
                        }


                        // 只有当方法被修改时才更新 Javadoc
                        if (methodModified) {
                            method.setJavadocComment(javadoc.toText());
                            fileModified[0] = true;
                            getLog().debug("处理方法: " + method.getNameAsString());
                        }
                    }
                } catch (Exception e) {
                    getLog().warn("处理方法失败: " + decl.getNameAsString(), e);
                }
            });
            
            // 只有当文件被修改时才写入文件
            if (fileModified[0]) {
                Files.writeString(file.toPath(), cu.toString());
                getLog().info("处理完成: " + file.getPath());
            }
        } catch (Exception e) {
            getLog().error("处理失败: " + file.getPath(), e);
        }
    }
}
