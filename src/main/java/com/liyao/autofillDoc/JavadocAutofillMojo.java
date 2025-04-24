package com.liyao.autofillDoc;

import com.liyao.autofillDoc.config.JavadocAutofillConfig;
import com.liyao.autofillDoc.service.FileProcessingService;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import java.io.File;
import java.util.List;

@Mojo(name = "autofill", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class JavadocAutofillMojo extends AbstractMojo {

    /**
     * 源代码目录
     */
    @Parameter(property = "sourceDir", defaultValue = "${project.build.sourceDirectory}", required = true)
    private File sourceDir;

    /**
     * 是否添加类注释
     */
    @Parameter(property = "addClassJavadoc", defaultValue = "true")
    private boolean addClassJavadoc;

    /**
     * 是否添加方法注释
     */
    @Parameter(property = "addMethodJavadoc", defaultValue = "true")
    private boolean addMethodJavadoc;

    /**
     * 是否添加参数注释
     */
    @Parameter(property = "addParamJavadoc", defaultValue = "true")
    private boolean addParamJavadoc;

    /**
     * 是否添加返回值注释
     */
    @Parameter(property = "addReturnJavadoc", defaultValue = "true")
    private boolean addReturnJavadoc;

    /**
     * 是否添加异常注释
     */
    @Parameter(property = "addThrowsJavadoc", defaultValue = "true")
    private boolean addThrowsJavadoc;

    /**
     * 排除特定文件的模式列表
     */
    @Parameter(property = "excludePatterns")
    private List<String> excludePatterns;

    /**
     * 是否包含私有方法
     */
    @Parameter(property = "includePrivateMethods", defaultValue = "true")
    private boolean includePrivateMethods;

    /**
     * 执行插件
     */
    @Override
    public void execute() {
        try {
            // 创建配置对象
            JavadocAutofillConfig config = new JavadocAutofillConfig.Builder()
                    .sourceDir(sourceDir)
                    .addClassJavadoc(addClassJavadoc)
                    .addMethodJavadoc(addMethodJavadoc)
                    .addParamJavadoc(addParamJavadoc)
                    .addReturnJavadoc(addReturnJavadoc)
                    .addThrowsJavadoc(addThrowsJavadoc)
                    .excludePatterns(excludePatterns)
                    .includePrivateMethods(includePrivateMethods)
                    .build();

            // 创建文件处理服务并执行处理
            FileProcessingService fileProcessingService = new FileProcessingService(getLog(), config);
            int processedCount = fileProcessingService.processSourceDirectory();
            

            if (processedCount > 0) {
                getLog().info("执行Javadoc自动填充插件完成, 共处理 " + processedCount + " 个文件");
            }
         
        } catch (Exception e) {
            getLog().error("执行Javadoc自动填充插件失败: " + e.getMessage(), e);
        }
    }

}
