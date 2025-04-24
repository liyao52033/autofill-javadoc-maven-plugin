package com.liyao.autofillDoc.exception;

/**
 * Javadoc处理异常类
 * 用于封装Javadoc处理过程中的异常
 */
public class JavadocProcessingException extends RuntimeException {

    /**
     * 构造函数
     * 
     * @param message 异常信息
     */
    public JavadocProcessingException(String message) {
        super(message);
    }

    /**
     * 构造函数
     * 
     * @param message 异常信息
     * @param cause   原始异常
     */
    public JavadocProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建文件处理异常
     * 
     * @param filePath 文件路径
     * @param cause    原始异常
     * @return 文件处理异常
     */
    public static JavadocProcessingException createFileProcessingException(String filePath, Throwable cause) {
        return new JavadocProcessingException("处理文件失败: " + filePath, cause);
    }

    /**
     * 创建方法处理异常
     * 
     * @param methodName 方法名称
     * @param cause      原始异常
     * @return 方法处理异常
     */
    public static JavadocProcessingException createMethodProcessingException(String methodName, Throwable cause) {
        return new JavadocProcessingException("处理方法失败: " + methodName, cause);
    }
}