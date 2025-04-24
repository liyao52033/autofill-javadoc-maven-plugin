package com.liyao.autofillDoc.service;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;

import java.util.List;

/**
 * 方法描述生成服务
 * 负责根据方法的AST结构生成适当的方法描述
 */
public class MethodDescriptionService {

    /**
     * 生成基于AST的方法描述
     *
     * @param method 方法声明
     * @return 方法描述
     */
    public String generateMethodDescription(MethodDeclaration method) {
        // 处理getter和setter方法
        if (method.getNameAsString().startsWith("get")) {
            return "获取" + method.getNameAsString().substring(3);
        }
        if (method.getNameAsString().startsWith("set")) {
            return "设置" + method.getNameAsString().substring(3);
        }

        // 分析方法中的各种语句和表达式
        List<MethodCallExpr> methodCalls = method.findAll(MethodCallExpr.class);
        List<IfStmt> ifStmts = method.findAll(IfStmt.class);
        List<ForStmt> forStmts = method.findAll(ForStmt.class);
        List<WhileStmt> whileStmts = method.findAll(WhileStmt.class);
        List<ReturnStmt> returnStmts = method.findAll(ReturnStmt.class);
        List<ThrowStmt> throwStmts = method.findAll(ThrowStmt.class);

        // 检查是否包含数据库操作
        boolean hasDbCall = hasDataBaseOperation(methodCalls);

        // 检查是否包含日志操作
        boolean hasLogCall = hasLogOperation(methodCalls);

        // 根据方法特征生成描述
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

        // 默认描述
        return "执行" + method.getNameAsString() + "操作";
    }

    /**
     * 检查方法调用中是否包含数据库操作
     *
     * @param methodCalls 方法调用列表
     * @return 是否包含数据库操作
     */
    private boolean hasDataBaseOperation(List<MethodCallExpr> methodCalls) {
        return methodCalls.stream().anyMatch(mc -> mc.getNameAsString().toLowerCase().contains("save") ||
                mc.getNameAsString().toLowerCase().contains("update") ||
                mc.getNameAsString().toLowerCase().contains("delete") ||
                mc.getNameAsString().toLowerCase().contains("insert"));
    }

    /**
     * 检查方法调用中是否包含日志操作
     *
     * @param methodCalls 方法调用列表
     * @return 是否包含日志操作
     */
    private boolean hasLogOperation(List<MethodCallExpr> methodCalls) {
        return methodCalls.stream().anyMatch(mc -> mc.getNameAsString().toLowerCase().contains("log") ||
                mc.getNameAsString().toLowerCase().contains("debug") ||
                mc.getNameAsString().toLowerCase().contains("info"));
    }
}