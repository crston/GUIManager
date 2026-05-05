package com.gmail.bobason01;
public class TargetInfo {
    private final String command;
    private final GUIManager.ExecutorType executor;
    public TargetInfo(String command, GUIManager.ExecutorType executor) {
        this.command = command;
        this.executor = executor;
    }
    public String getCommand() { return command; }
    public GUIManager.ExecutorType getExecutor() { return executor; }
}