package com.gmail.bobason01;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * AsyncSaver
 * Single worker async file writer for YAML configs
 */
public final class AsyncSaver {

    private static final BlockingQueue<Job> QUEUE = new LinkedBlockingQueue<>();
    private static volatile boolean started = false;

    private AsyncSaver() {}

    // 정적(static) 메서드
    public static void enqueue(File file, FileConfiguration config) {
        if (!started) startWorker();
        QUEUE.offer(new Job(file, config));
    }

    private static synchronized void startWorker() {
        if (started) return;
        started = true;
        Thread t = new Thread(AsyncSaver::runLoop, "GUIManager-AsyncSaver");
        t.setDaemon(true);
        t.start();
    }

    private static void runLoop() {
        while (true) {
            try {
                Job job = QUEUE.take();
                job.save();
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static final class Job {
        private final File file;
        private final YamlConfiguration yaml;

        Job(File file, FileConfiguration config) {
            this.file = file;
            this.yaml = new YamlConfiguration();
            this.yaml.setDefaults(config);
            this.yaml.options().copyDefaults(true);
            config.getKeys(true).forEach(k -> this.yaml.set(k, config.get(k)));
        }

        void save() {
            try {
                yaml.save(file);
            } catch (IOException ignored) {
            }
        }
    }
}
