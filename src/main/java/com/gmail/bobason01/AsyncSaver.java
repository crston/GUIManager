package com.gmail.bobason01;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

public final class AsyncSaver {

    private static final BlockingQueue<Job> QUEUE = new LinkedBlockingQueue<>();
    private static volatile boolean started = false;
    private static Thread workerThread;

    private AsyncSaver() {}

    public static void enqueue(File file, FileConfiguration config) {
        if (!started) startWorker();
        QUEUE.offer(new Job(file, config));
    }

    private static synchronized void startWorker() {
        if (started) return;
        started = true;
        workerThread = new Thread(AsyncSaver::runLoop, "GUIManagerAsyncSaver");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    private static void runLoop() {
        while (started || !QUEUE.isEmpty()) {
            try {
                Job job = QUEUE.take();
                job.save();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public static void shutdown() {
        started = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }

        Job job;
        int count = 0;
        while ((job = QUEUE.poll()) != null) {
            job.save();
            count++;
        }

        if (count > 0) {
            Bukkit.getLogger().info("Saved remaining files during shutdown");
        }
    }

    private static final class Job {
        private final File file;
        private final String yamlString;

        Job(File file, FileConfiguration config) {
            this.file = file;
            this.yamlString = config.saveToString();
        }

        void save() {
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.loadFromString(this.yamlString);
                yaml.save(file);
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to save file asynchronously", e);
            }
        }
    }
}