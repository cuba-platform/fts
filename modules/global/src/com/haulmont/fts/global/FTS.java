/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */
package com.haulmont.fts.global;

public class FTS {

    public static final String FILE_CONT_PROP = "fileContent";

    public static final String FIELD_START = "^^";

    public static final String FIELD_START_RE = "\\^\\^";

    public static final String FIELD_SEP = "^";

    public static final int HIT_CONTEXT_PAD = 10;

    public static boolean isTokenChar(int c) {
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
