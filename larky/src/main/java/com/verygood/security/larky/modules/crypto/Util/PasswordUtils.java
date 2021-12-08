package com.verygood.security.larky.modules.crypto.Util;

import java.util.Arrays;

//TODO: move to modules.codecs?
public class PasswordUtils {

    /**
     * Blank out a character array
     *
     * @param pwd the character array
     */
    public static void blankOut(char[] pwd) {
        if (pwd != null)
            Arrays.fill(pwd, ' ');
    }

}
