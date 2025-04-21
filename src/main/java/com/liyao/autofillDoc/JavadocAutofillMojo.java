package com.liyao.autofillDoc;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
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

    @org.apache.maven.plugins.annotations.Parameter(property = "sourceDir", defaultValue = "${project.build.sourceDirectory}", required = true)
    private File sourceDir;

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

                    String doc = "/**\n * " + type.getNameAsString() + " " + typeKeyword + "的描述\n */";
                    type.setJavadocComment(doc);
                    getLog().info("添加类注释: " + type.getNameAsString());
                }
            });

            // === 为方法自动补充 Javadoc 参数 ===
            cu.findAll(CallableDeclaration.class).forEach(decl -> {
                try {
                    // 如果是 MethodDeclaration，则可以修改 Javadoc
                    if (decl instanceof MethodDeclaration method) {
                     //   Optional<Javadoc> javadocOpt = method.getJavadoc();
                        Javadoc javadoc;
                        // 如果方法没有 Javadoc 注释，创建新的 Javadoc 注释
                        if (method.getJavadoc().isPresent()) {
                            javadoc = method.getJavadoc().get();
                        } else {
                            // 如果没有 Javadoc，初始化一个空的并添加方法描述
                            javadoc = new Javadoc(JavadocDescription.parseText(generateAstBasedMethodDescription(method)));
                        }

                       // Javadoc javadoc = javadocOpt.get();
                        List<JavadocBlockTag> tags = javadoc.getBlockTags();

                        // 是否已包含全部参数注释
                        boolean allParamsDocumented = method.getParameters().stream().allMatch(param ->
                                tags.stream()
                                        .filter(tag -> tag.getType() == JavadocBlockTag.Type.PARAM)
                                        .anyMatch(tag -> tag.getName().isPresent() && tag.getName().get().equals(param.getNameAsString()))
                        );

                        // 是否已包含 return 注释
                        boolean hasReturn = method.getType().isVoidType()
                                || tags.stream().anyMatch(tag -> tag.getType() == JavadocBlockTag.Type.RETURN);

                        // 如果都已经注释了，就跳过
                        if (allParamsDocumented && hasReturn) {
                            return;
                        }

                        // 添加缺失的 @param
                        for (Parameter param : method.getParameters()) {
                            String paramName = param.getNameAsString();
                            boolean exists = tags.stream()
                                    .filter(tag -> tag.getType() == JavadocBlockTag.Type.PARAM)
                                    .anyMatch(tag -> tag.getName().isPresent() && tag.getName().get().equals(paramName));
                            if (!exists) {
                                String paramDescription = "参数 " + paramName + " 的描述";
                                javadoc.addBlockTag("param", paramName, paramDescription);
                            }
                        }

                        // 动态生成 @return 注释
                        if (!method.getType().isVoidType()) {
                            boolean returnTagExists = tags.stream()
                                    .anyMatch(tag -> tag.getType() == JavadocBlockTag.Type.RETURN);
                            if (!returnTagExists) {
                                String returnDescription = "返回值类型为 " + method.getType() + " 的描述";
                                javadoc.addBlockTag("return", returnDescription);
                            }
                        }

                        // 使用 setJavadocComment 修改 Javadoc
                        method.setJavadocComment(javadoc.toText());
                        Files.writeString(file.toPath(), cu.toString());
                        getLog().info("处理完成: " + file.getPath());
                    }
                } catch (Exception e) {
                    getLog().warn("处理方法失败: " + decl.getNameAsString(), e);
                }
            });
        } catch (Exception e) {
            getLog().error("处理失败: " + file.getPath(), e);
        }
    }
}
