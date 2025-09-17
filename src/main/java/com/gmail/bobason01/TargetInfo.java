package com.gmail.bobason01;

import java.util.Objects;

public final class TargetInfo {

    private final String command;

    public TargetInfo(String command) {
        this.command = command;
    }

    public String command() {
        return command;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TargetInfo that = (TargetInfo) obj;
        return Objects.equals(command, that.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command);
    }

    @Override
    public String toString() {
        return "TargetInfo[" +
                "command='" + command + '\'' +
                ']';
    }
}