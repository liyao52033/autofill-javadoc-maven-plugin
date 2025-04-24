package com.liyao.autofillDoc.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Javadoc自动填充配置类
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
         * 构建配置对象
         * 
         * @return 配置对象
         */
        public JavadocAutofillConfig build() {
            return new JavadocAutofillConfig(this);
        }
    }
}