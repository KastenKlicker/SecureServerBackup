package fr.xephi.authme.util;

/**
 * Utility class for String operations.
 *
 * https://github.com/AuthMe/AuthMeReloaded/blob/8a42a775199c46a884852502219f14fdf75f3709/src/main/java/fr/xephi/authme/util/StringUtils.java#L10
 */
public final class StringUtils {

    // Utility class
    private StringUtils() {
    }

    /**
     * Return whether the given string contains any of the provided elements.
     *
     * @param str    The string to analyze
     * @param pieces The items to check the string for
     *
     * @return True if the string contains at least one of the items
     */
    public static boolean containsAny(String str, Iterable<String> pieces) {
        if (str == null) {
            return false;
        }
        for (String piece : pieces) {
            if (piece != null && str.contains(piece)) {
                return true;
            }
        }
        return false;
    }
}
