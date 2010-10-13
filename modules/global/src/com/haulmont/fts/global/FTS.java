/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 29.07.2010 13:42:22
 *
 * $Id$
 */
package com.haulmont.fts.global;

public class FTS {

    public static final String FILE_CONT_PROP = "fileContent";

    public static final String FIELD_START = "^^";

    public static final String FIELD_START_RE = "\\^\\^";

    public static final String FIELD_SEP = "^";

    public static final int HIT_CONTEXT_PAD = 10;

    public static boolean isTokenChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '/' || c == '\\' || c == '$' || c == '^';
    }

    public static class Tokenizer {

        private String input;
        private int maxPos;
        private int currentPos;
        private int tokenStart;
        private int tokenEnd;

        public Tokenizer(String input) {
            this.input = input;
            maxPos = input.length() - 1;
        }

        public String nextToken() {
            skipDelimiters();

            tokenStart = currentPos;
            int pos = currentPos;
            while (pos <= maxPos && isTokenChar(input.charAt(pos))) {
                pos++;
            }
            if (pos > currentPos) {
                String token = input.substring(currentPos, pos);
                currentPos = pos;
                tokenEnd = pos;
                return token;
            }
            return null;
        }

        public boolean hasMoreTokens() {
            skipDelimiters();
            return currentPos <= maxPos;
        }

        public int getTokenStart() {
            return tokenStart;
        }

        public int getTokenEnd() {
            return tokenEnd;
        }

        private void skipDelimiters() {
            int pos = currentPos;
            while (pos <= maxPos && !isTokenChar(input.charAt(pos))) {
                pos++;
            }
            currentPos = pos;
        }
    }
}
