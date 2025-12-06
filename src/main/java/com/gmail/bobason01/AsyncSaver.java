package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

/**
 * AsyncSaver
 * 비동기 파일 저장을 담당하며, 서버 종료 시 남은 데이터 저장을 보장합니다.
 */
public final class AsyncSaver {

    private static final BlockingQueue<Job> QUEUE = new LinkedBlockingQueue<>();
    private static volatile boolean started = false;
    private static Thread workerThread;

    private AsyncSaver() {}

    public static void enqueue(File file, FileConfiguration config) {
        if (!started) startWorker();
        // 메인 스레드에서 데이터를 복사하여 Job 생성 (Thread-Safety 보장)
        QUEUE.offer(new Job(file, config));
    }

    private static synchronized void startWorker() {
        if (started) return;
        started = true;
        workerThread = new Thread(AsyncSaver::runLoop, "GUIManager-AsyncSaver");
        // Daemon을 false로 설정하여 JVM이 살아있는 동안 최대한 작업을 끝내도록 유도하거나,
        // 아래 shutdown 메서드에서 처리를 보장해야 합니다.
        workerThread.setDaemon(true);
        workerThread.start();
    }

    private static void runLoop() {
        while (started || !QUEUE.isEmpty()) {
            try {
                // 큐에서 작업을 가져옴 (비어있으면 대기)
                Job job = QUEUE.take();
                job.save();
            } catch (InterruptedException e) {
                // 스레드가 인터럽트 되면 루프 종료
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // [핵심] 플러그인 비활성화 시 호출하여 남은 작업을 모두 처리
    public static void shutdown() {
        started = false;
        if (workerThread != null) {
            workerThread.interrupt(); // 대기 중인 스레드 깨우기
        }

        // 큐에 남은 작업들을 메인 스레드에서 즉시 처리 (데이터 손실 방지)
        Job job;
        int count = 0;
        while ((job = QUEUE.poll()) != null) {
            job.save();
            count++;
        }

        if (count > 0) {
            Bukkit.getLogger().info("[GUIManager] Saved " + count + " remaining files during shutdown.");
        }
    }

    private static final class Job {
        private final File file;
        private final YamlConfiguration yaml;

        Job(File file, FileConfiguration config) {
            this.file = file;
            this.yaml = new YamlConfiguration();
            // config의 데이터를 복사 (메인 스레드에서 실행됨)
            // options().copyDefaults(true)는 원본 config 설정에 따라 필요할 수 있음
            this.yaml.options().copyDefaults(config.options().copyDefaults());

            // 모든 키와 값을 복사
            for (String key : config.getKeys(true)) {
                this.yaml.set(key, config.get(key));
            }
        }

        void save() {
            try {
                // 상위 폴더가 없으면 생성
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                yaml.save(file);
            } catch (IOException e) {
                Bukkit.getLogger().log(Level.SEVERE, "[GUIManager] Failed to save file asynchronously: " + file.getName(), e);
            }
        }
    }
}