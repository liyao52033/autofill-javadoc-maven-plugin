package com.liyao.autofillDoc.concurrent;

import org.slf4j.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 文件锁管理器
 * 支持 JVM 内锁 + OS 文件锁双重保护，防止多进程并发修改冲突
 */
public class FileLockManager {

    private final Logger log;

    /**
     * JVM 内锁：防止同一 JVM 内并发
     */
    private final Map<String, ReentrantLock> jvmLocks = new ConcurrentHashMap<>();

    /**
     * 锁超时配置（毫秒）
     */
    private final long lockTimeoutMs;

    /**
     * 是否使用 OS 文件锁
     */
    private final boolean useOsLock;

    /**
     * 构造函数
     *
     * @param log            日志对象
     * @param lockTimeoutMs  锁超时时间（毫秒）
     * @param useOsLock      是否使用 OS 文件锁
     */
    public FileLockManager(Logger log, long lockTimeoutMs, boolean useOsLock) {
        this.log = log;
        this.lockTimeoutMs = lockTimeoutMs;
        this.useOsLock = useOsLock;
    }

    /**
     * 锁句柄
     */
    public static class LockHandle {

        private final String filePath;

        private final ReentrantLock jvmLock;

        private FileChannel channel;

        private FileLock osLock;

        public LockHandle(String filePath, ReentrantLock jvmLock) {
            this.filePath = filePath;
            this.jvmLock = jvmLock;
        }

        /**
         * 获取FilePath
         *
         * @return 返回字符串
         */
        public String getFilePath() {
            return filePath;
        }

        /**
         * 返回处理结果
         *
         * @return 返回布尔值，true 或 false
         */
        public boolean isLocked() {
            return jvmLock != null && jvmLock.isHeldByCurrentThread();
        }

        /**
         * 设置Channel
         *
         * @param channel 参数 channel 的描述
         */
        void setChannel(FileChannel channel) {
            this.channel = channel;
        }

        /**
         * 设置OsLock
         *
         * @param osLock 参数 osLock 的描述
         */
        void setOsLock(FileLock osLock) {
            this.osLock = osLock;
        }

        /**
         * 获取Channel
         *
         * @return 返回值类型为 FileChannel 的描述
         */
        FileChannel getChannel() {
            return channel;
        }

        /**
         * 获取OsLock
         *
         * @return 返回值类型为 FileLock 的描述
         */
        FileLock getOsLock() {
            return osLock;
        }

        /**
         * 获取JvmLock
         *
         * @return 返回值类型为 ReentrantLock 的描述
         */
        ReentrantLock getJvmLock() {
            return jvmLock;
        }
    }

    /**
     * 尝试获取文件锁
     *
     * @param file 目标文件
     * @return LockHandle 或 null（获取失败）
     */
    public LockHandle tryLock(File file) {
        String filePath = file.getAbsolutePath();
        // 1. 获取 JVM 内锁
        ReentrantLock jvmLock = jvmLocks.computeIfAbsent(filePath, k -> new ReentrantLock());
        try {
            if (!jvmLock.tryLock(lockTimeoutMs, TimeUnit.MILLISECONDS)) {
                log.debug("JVM 内锁获取超时：{}", file.getName());
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("JVM 内锁获取被中断：{}", file.getName());
            return null;
        }
        LockHandle handle = new LockHandle(filePath, jvmLock);
        // 2. 获取 OS 文件锁（可选，用于多进程保护）
        if (useOsLock) {
            try {
                FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
                FileLock osLock = channel.tryLock();
                if (osLock == null) {
                    jvmLock.unlock();
                    channel.close();
                    log.debug("OS 文件锁获取失败（其他进程持有）：{}", file.getName());
                    return null;
                }
                handle.setChannel(channel);
                handle.setOsLock(osLock);
                log.debug("成功获取文件锁：{}", file.getName());
            } catch (IOException e) {
                jvmLock.unlock();
                log.warn("OS 文件锁获取异常：{}", file.getName(), e);
                return null;
            }
        }
        return handle;
    }

    /**
     * 释放锁
     *
     * @param handle 锁句柄
     */
    public void unlock(LockHandle handle) {
        if (handle == null) {
            return;
        }
        // 释放 OS 锁
        if (handle.getOsLock() != null) {
            try {
                handle.getOsLock().release();
            } catch (IOException ignored) {
            }
        }
        if (handle.getChannel() != null) {
            try {
                handle.getChannel().close();
            } catch (IOException ignored) {
            }
        }
        // 释放 JVM 锁
        if (handle.getJvmLock() != null && handle.getJvmLock().isHeldByCurrentThread()) {
            handle.getJvmLock().unlock();
        }
        log.debug("释放文件锁：{}", handle.getFilePath());
    }

    /**
     * 检查文件是否被锁定
     *
     * @param file 目标文件
     * @return 是否被锁定
     */
    public boolean isLocked(File file) {
        String filePath = file.getAbsolutePath();
        ReentrantLock lock = jvmLocks.get(filePath);
        return lock != null && lock.isLocked();
    }

    /**
     * 清理所有锁（谨慎使用）
     */
    public void clearAll() {
        jvmLocks.clear();
    }
}
