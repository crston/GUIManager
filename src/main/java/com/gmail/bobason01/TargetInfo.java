package com.gmail.bobason01;

import java.util.Objects;

public final class TargetInfo {

    private final String command;
    private final GUIManager.ExecutorType executor;

    public TargetInfo(String command, GUIManager.ExecutorType executor) {
        this.command = command;
        this.executor = executor;
    }

    public String command() {
        return command;
    }

    public GUIManager.ExecutorType executor() {
        return executor;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TargetInfo that = (TargetInfo) obj;
        return Objects.equals(command, that.command) && executor == that.executor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(command, executor);
    }

    @Override
    public String toString() {
        return "TargetInfo[" +
                "command='" + command + '\'' +
                ", executor=" + executor +
                ']';
    }
}