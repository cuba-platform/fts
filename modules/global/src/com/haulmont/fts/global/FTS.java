/*
 * Copyright (c) 2008-2019 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
