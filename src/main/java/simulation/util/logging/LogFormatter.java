package simulation.util.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * LogFormatter class used by {@code Logger}.
 */
public class LogFormatter extends Formatter {


    @Override
    public String format(LogRecord record) {
        return record.getMessage() + "\n";
    }
}
