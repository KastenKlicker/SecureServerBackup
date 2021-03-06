package fr.xephi.authme.output;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

/**
 * Implements a filter for Log4j to skip sensitive AuthMe commands.
 *
 * @author Xephi59
 *
 * https://github.com/AuthMe/AuthMeReloaded/blob/8a42a775199c46a884852502219f14fdf75f3709/src/main/java/fr/xephi/authme/output/Log4JFilter.java
 */
public class Log4JFilter extends AbstractFilter {

    /**
     * Validates a Message instance and returns the {@link Result} value
     * depending on whether the message contains sensitive data.
     *
     * @param message The Message object to verify
     *
     * @return The Result value
     */
    private static Result validateMessage(Message message) {
        if (message == null) {
            return Result.NEUTRAL;
        }
        return validateMessage(message.getFormattedMessage());
    }

    /**
     * Validates a message and returns the {@link Result} value depending
     * on whether the message contains sensitive data.
     *
     * @param message The message to verify
     *
     * @return The Result value
     */
    private static Result validateMessage(String message) {
        return LogFilterHelper.isSensitiveCommand(message)
                ? Result.DENY
                : Result.NEUTRAL;
    }

    @Override
    public Result filter(LogEvent event) {
        Message candidate = null;
        if (event != null) {
            candidate = event.getMessage();
        }
        return validateMessage(candidate);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return validateMessage(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        return validateMessage(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        String candidate = null;
        if (msg != null) {
            candidate = msg.toString();
        }
        return validateMessage(candidate);
    }
}
