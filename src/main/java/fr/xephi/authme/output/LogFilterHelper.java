package fr.xephi.authme.output;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service class for the log filters.
 * https://github.com/AuthMe/AuthMeReloaded/blob/8a42a775199c46a884852502219f14fdf75f3709/src/main/java/fr/xephi/authme/output/LogFilterHelper.java
 */
final class LogFilterHelper {

    @VisibleForTesting
    static final List<String> COMMANDS_TO_SKIP = withAndWithoutPrefix(
            "login ", "setlogin ", "addserver ", " removeserver");

    private static final String ISSUED_COMMAND_TEXT = "issued server command:";

    private LogFilterHelper() {
        // Util class
    }

    /**
     * Validate a message and return whether the message contains a sensitive command.
     *
     * @param message The message to verify
     *
     * @return True if it is a sensitive command, false otherwise
     */
    static boolean isSensitiveCommand(String message) {
        if (message == null) {
            return false;
        }
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains(ISSUED_COMMAND_TEXT) && StringUtils.containsAny(lowerMessage, COMMANDS_TO_SKIP);
    }

    private static List<String> withAndWithoutPrefix(String... commands) {
        List<String> commandList = new ArrayList<>(commands.length * 2);
        for (String command : commands) {
            commandList.add(command);
            commandList.add(command.substring(0, 1) + "ssr:" + command.substring(1));
        }
        return Collections.unmodifiableList(commandList);
    }
}