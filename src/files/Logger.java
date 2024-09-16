package files;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public abstract class Logger {
    private static final StringBuilder LOG_BUILDER = new StringBuilder();
    private static final SimpleDateFormat date_format = new SimpleDateFormat("[HH:mm:ss.SSS] - ");
    public static void log(String txt) {
        log(txt, false, '\n');
    }
    public static void log(String txt, boolean error) {
        log(txt, error, '\n');
    }

    public static void log(String txt, boolean error, char end) {
        LOG_BUILDER.append(error? "! " : "  ")
                .append(current_time())
                .append(txt)
                .append(end);
    }

    public static String get_log() {
        return LOG_BUILDER.toString();
    }

    private static String current_time() {
        Calendar c_time = Calendar.getInstance();
        return date_format.format(c_time.getTime());
    }
}
