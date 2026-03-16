package com.liyao.autofillDoc.concurrent;

import org.slf4j.Logger;
import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件版本控制器
 * 基于内容哈希和修改时间双重校验，确保并发修改时的数据一致性
 */
public class FileVersionController {

    private final Logger log;

    /**
     * 文件版本缓存：filePath -> VersionInfo
     */
    private final Map<String, VersionInfo> versionCache = new ConcurrentHashMap<>();

    /**
     * 版本信息
     */
    public static class VersionInfo {

        private final String contentHash;

        private final long lastModified;

        private final long fileSize;

        private final String baseContent;

        public VersionInfo(String contentHash, long lastModified, long fileSize, String baseContent) {
            this.contentHash = contentHash;
            this.lastModified = lastModified;
            this.fileSize = fileSize;
            this.baseContent = baseContent;
        }

        /**
         * 获取ContentHash
         *
         * @return 返回字符串
         */
        public String getContentHash() {
            return contentHash;
        }

        /**
         * 获取LastModified
         *
         * @return 返回整数值
         */
        public long getLastModified() {
            return lastModified;
        }

        /**
         * 获取FileSize
         *
         * @return 返回整数值
         */
        public long getFileSize() {
            return fileSize;
        }

        /**
         * 获取BaseContent
         *
         * @return 返回字符串
         */
        public String getBaseContent() {
            return baseContent;
        }
    }

    /**
     * 构造函数
     *
     * @param log 日志对象
     */
    public FileVersionController(Logger log) {
        this.log = log;
    }

    /**
     * 注册文件版本（处理前调用）
     *
     * @param file 目标文件
     * @return 当前版本信息
     * @throws Exception 读取文件异常
     */
    public VersionInfo registerVersion(File file) throws Exception {
        String filePath = file.getAbsolutePath();
        byte[] content = Files.readAllBytes(file.toPath());
        String hash = computeHash(content);
        long lastModified = file.lastModified();
        long fileSize = file.length();
        String baseContent = new String(content);
        VersionInfo version = new VersionInfo(hash, lastModified, fileSize, baseContent);
        versionCache.put(filePath, version);
        log.debug("注册文件版本：{}, hash={}, size={}", file.getName(), hash.substring(0, 8), fileSize);
        return version;
    }

    /**
     * 校验文件版本（写入前调用）
     *
     * @param file 目标文件
     * @return true = 版本一致，可写入；false = 已被修改，需要处理冲突
     */
    public boolean validateVersion(File file) {
        String filePath = file.getAbsolutePath();
        VersionInfo registered = versionCache.get(filePath);
        if (registered == null) {
            return true;
        }
        // 快速检查：修改时间和大小
        if (file.lastModified() != registered.lastModified || file.length() != registered.fileSize) {
            // 需要精确校验
            try {
                byte[] currentContent = Files.readAllBytes(file.toPath());
                String currentHash = computeHash(currentContent);
                boolean valid = currentHash.equals(registered.contentHash);
                if (!valid) {
                    log.debug("文件版本校验失败（内容已变更）：{}", file.getName());
                }
                return valid;
            } catch (Exception e) {
                log.warn("文件版本校验异常：{}", file.getName(), e);
                return false;
            }
        }
        return true;
    }

    /**
     * 获取注册的版本信息
     *
     * @param filePath 文件路径
     * @return 版本信息，如果未注册则返回 null
     */
    public VersionInfo getVersion(String filePath) {
        return versionCache.get(filePath);
    }

    /**
     * 清除版本缓存
     *
     * @param filePath 文件路径
     */
    public void clearVersion(String filePath) {
        versionCache.remove(filePath);
    }

    /**
     * 清除所有版本缓存
     */
    public void clearAll() {
        versionCache.clear();
    }

    /**
     * 计算内容哈希
     *
     * @param content 文件内容
     * @return MD5 哈希值
     * @throws Exception 计算异常
     */
    private String computeHash(byte[] content) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(content);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
