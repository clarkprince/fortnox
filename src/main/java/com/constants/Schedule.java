package com.constants;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Schedule {
    TWO_MINUTES("2 Minutes", 120), FIVE_MINUTES("5 Minutes", 300), FIFTEEN_MINUTES("15 Minutes", 900), THIRTY_MINUTES("30 Minutes", 1800),
    ONE_HOUR("1 Hour", 3600), THREE_HOURS("3 Hours", 10800), SIX_HOURS("6 Hours", 21600), TWELVE_HOURS("12 Hours", 43200), DAILY("Daily", 86400);

    private final String displayName;
    private final int seconds;

    Schedule(String displayName, int seconds) {
        this.displayName = displayName;
        this.seconds = seconds;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSeconds() {
        return seconds;
    }

    public static List<String> getDisplayNames() {
        return Arrays.stream(Schedule.values()).map(Schedule::getDisplayName).collect(Collectors.toList());
    }

    public static Schedule fromDisplayName(String displayName) {
        return Arrays.stream(Schedule.values()).filter(schedule -> schedule.getDisplayName().equals(displayName)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid schedule display name: " + displayName));
    }

    public static Schedule fromSeconds(int seconds) {
        return Arrays.stream(Schedule.values()).filter(schedule -> schedule.getSeconds() == seconds).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid schedule seconds: " + seconds));
    }
}
