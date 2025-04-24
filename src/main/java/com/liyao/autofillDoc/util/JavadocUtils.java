package com.liyao.autofillDoc.util;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.javadoc.JavadocBlockTag;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Javadoc工具类
 * 提供处理Javadoc的通用方法，包括处理标准和非标准Javadoc标签、泛型类型等
 */
public class JavadocUtils {

    // 用于匹配泛型参数的正则表达式
    private static final Pattern GENERIC_PATTERN = Pattern.compile("<[^<>]*>");

    /**
     * 获取类型关键字
     *
     * @param type 类型声明
     * @return 类型关键字（类、接口或枚举）
     */
    public static String getTypeKeyword(TypeDeclaration<?> type) {
        String typeKeyword = "类";
        if (type instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration classOrInterface = (ClassOrInterfaceDeclaration) type;
            if (classOrInterface.isInterface()) {
                typeKeyword = "接口";
            } else {
                typeKeyword = "类";
            }
        } else if (type.isEnumDeclaration()) {
            typeKeyword = "枚举";
        }
        return typeKeyword;
    }

    /**
     * 查找指定类型和名称的标签
     *
     * @param tags 标签列表
     * @param type 标签类型
     * @param name 标签名称（可为null）
     * @return 找到的标签，如果没有找到则返回null
     */
    public static JavadocBlockTag findBlockTag(List<JavadocBlockTag> tags, JavadocBlockTag.Type type, String name) {
        for (JavadocBlockTag tag : tags) {
            if (tag.getType() == type) {
                if (name == null || (tag.getName().isPresent() && tag.getName().get().equals(name))) {
                    return tag;
                }
            }
        }
        return null;
    }

    /**
     * 清除字符串中的尖括号
     * 使用正则表达式安全地移除文本中的尖括号，避免破坏文本结构
     *
     * @param text 原始文本
     * @return 清除尖括号后的文本
     */
    public static String cleanAngleBrackets(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        // 使用正则表达式替换所有尖括号及其内容
        return GENERIC_PATTERN.matcher(text).replaceAll("");
    }
    
    /**
     * 安全处理泛型类型
     * 保留泛型类型的基本信息，但移除尖括号以避免Javadoc解析问题
     *
     * @param type 包含泛型的类型字符串
     * @return 处理后的类型字符串
     */
    public static String safeGenericType(String type) {
        if (type == null || type.isEmpty()) {
            return "";
        }
        
        if (!type.contains("<")) {
            return type;
        }
        
        // 提取基本类型（泛型前的部分）
        String baseType = type.substring(0, type.indexOf('<'));
        
        // 提取泛型参数
        Matcher matcher = GENERIC_PATTERN.matcher(type);
        if (matcher.find()) {
            String genericPart = matcher.group();
            // 移除尖括号，保留参数
            String params = genericPart.substring(1, genericPart.length() - 1);
            return baseType + "(" + params + ")";
        }
        
        return type;
    }

    /**
     * 生成参数描述
     * 为方法参数生成标准化的描述，处理泛型参数
     *
     * @param paramName 参数名称
     * @return 参数描述
     */
    public static String generateParamDescription(String paramName) {
        if (paramName == null || paramName.isEmpty()) {
            return "参数描述";
        }

        String cleanName = cleanAngleBrackets(paramName);

        // 常见命名优化：比如 userId、fileName、inputStream
        String lowerName = cleanName.toLowerCase();

        if (lowerName.contains("id")) {
            return "ID 标识参数 " + cleanName;
        } else if (lowerName.contains("name")) {
            return "名称参数 " + cleanName;
        } else if (lowerName.contains("list")) {
            return "列表参数 " + cleanName;
        } else if (lowerName.contains("map")) {
            return "映射参数 " + cleanName;
        } else if (lowerName.contains("stream")) {
            return "流参数 " + cleanName;
        } else if (lowerName.contains("input") || lowerName.contains("output")) {
            return "输入输出参数 " + cleanName;
        } else if (lowerName.length() <= 2) {
            return "通用参数 " + cleanName;
        }

        return "参数 " + cleanName + " 的描述";
    }

    /**
     * 生成返回值描述
     * 为方法返回值生成标准化的描述，处理泛型类型
     *
     * @param returnType 返回值类型
     * @return 返回值描述
     */
    public static String generateReturnDescription(String returnType) {
        if (returnType == null || returnType.isEmpty()) {
            return "返回值描述";
        }

        String processedType = safeGenericType(returnType).toLowerCase();

        // 简单的关键词匹配来生成更语义化的描述
        if (processedType.contains("string")) {
            return "返回字符串";
        } else if (processedType.contains("int") || processedType.contains("long")
                || processedType.contains("short") || processedType.contains("integer")) {
            return "返回整数值";
        } else if (processedType.contains("boolean")) {
            return "返回布尔值，true 或 false";
        } else if (processedType.contains("list")) {
            return "返回列表数据，类型为 " + safeGenericType(returnType);
        } else if (processedType.contains("map")) {
            return "返回映射数据，类型为 " + safeGenericType(returnType);
        } else if (processedType.contains("void")) {
            return "无返回值";
        }

        return "返回值类型为 " + safeGenericType(returnType) + " 的描述";
    }

    /**
     * 生成异常描述
     * 为方法抛出的异常生成标准化的描述
     *
     * @param exceptionName 异常名称
     * @return 异常描述
     */
    public static String generateThrowsDescription(String exceptionName) {
        if (exceptionName == null || exceptionName.isEmpty()) {
            return "异常描述";
        }
        // 处理可能包含泛型的异常类型
        String processedName = safeGenericType(exceptionName);
        return "抛出 " + processedName + " 异常的描述";
    }
    
    /**
     * 查找非标准Javadoc标签
     * 支持查找自定义或非标准的Javadoc标签
     *
     * @param tags 标签列表
     * @param tagName 标签名称（如"exception"）
     * @param name 标签参数名称（可为null）
     * @return 找到的标签，如果没有找到则返回null
     */
    public static JavadocBlockTag findCustomBlockTag(List<JavadocBlockTag> tags, String tagName, String name) {
        for (JavadocBlockTag tag : tags) {
            if (tag.getType() == JavadocBlockTag.Type.UNKNOWN && 
                tag.getTagName().equals(tagName)) {
                if (name == null || 
                    (tag.getName().isPresent() && 
                     (tag.getName().get().equals(name) || 
                      tag.getName().get().equals(cleanAngleBrackets(name))))) {
                    return tag;
                }
            }
        }
        return null;
    }
}