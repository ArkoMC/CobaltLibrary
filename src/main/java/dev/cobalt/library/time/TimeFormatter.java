package dev.cobalt.library.time;

import java.util.ArrayList;
import java.util.List;

public class TimeFormatter {

    public static String formatSeconds(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        List<String> parts = new ArrayList<>();

        if (days > 0) parts.add(days + (days == 1 ? " dag" : " dage"));
        if (hours > 0) parts.add(hours + (hours == 1 ? " time" : " timer"));
        if (minutes > 0) parts.add(minutes + (minutes == 1 ? " minut" : " minutter"));
        if (seconds > 0 || parts.isEmpty()) parts.add(seconds + (seconds == 1 ? " sekundt" : " sekunder"));

        return String.join(", ", parts);
    }
}
