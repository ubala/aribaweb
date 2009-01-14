/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/io/TokenGenerator.java#6 $
*/

package ariba.util.io;

import ariba.util.core.Fmt;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
    TokenGenerator is a private class that transform an ASCII byte stream in
    tokens. This is class is used by the Deserializer.

    @aribaapi private
*/
public class TokenGenerator extends FilterReader
{
    /*
        Token types
    */
    public static final char NULL_TOKEN                     = 0x0;
    public static final char STRING_TOKEN                   = 0x1;
    public static final char ARRAY_BEGIN_TOKEN              = 0x2;
    public static final char ARRAY_END_TOKEN                = 0x3;
    public static final char VECTOR_BEGIN_TOKEN             = 0x4;
    public static final char VECTOR_END_TOKEN               = 0x5;
    public static final char HASHTABLE_BEGIN_TOKEN          = 0x6;
    public static final char HASHTABLE_KEY_VALUE_SEP_TOKEN  = 0x7;
    public static final char HASHTABLE_KEY_VALUE_END_TOKEN  = 0x8;
    public static final char HASHTABLE_END_TOKEN            = 0x9;
    public static final char GENERIC_SEP_TOKEN              = 0xa;
    public static final char NULL_VALUE_TOKEN               = 0xb;
    public static final char LAST_TOKEN_TYPE                = 0xb;

    static char[] tokenToAscii;
    static char[] asciiToToken;


    static {
        tokenToAscii = new char[LAST_TOKEN_TYPE+1];
        tokenToAscii[NULL_TOKEN]                     = 0; /* Error !*/
        tokenToAscii[STRING_TOKEN]                   = 0; /* Error !*/
        tokenToAscii[ARRAY_BEGIN_TOKEN]              = '[';
        tokenToAscii[ARRAY_END_TOKEN]                = ']';
        tokenToAscii[VECTOR_BEGIN_TOKEN]             = '(';
        tokenToAscii[VECTOR_END_TOKEN]               = ')';
        tokenToAscii[HASHTABLE_BEGIN_TOKEN]          = '{';
        tokenToAscii[HASHTABLE_KEY_VALUE_SEP_TOKEN]  = '=';
        tokenToAscii[HASHTABLE_KEY_VALUE_END_TOKEN]  = ';';
        tokenToAscii[HASHTABLE_END_TOKEN]            = '}';
        tokenToAscii[GENERIC_SEP_TOKEN]              = ',';
        tokenToAscii[NULL_VALUE_TOKEN]               = '@';

        asciiToToken = new char[127];
        int i;
        for (i=0;i<=' ';i++) {
            asciiToToken[i] = NULL_TOKEN;
        }
        for (i=' '+1 ; i < 127 ; i++) {
            asciiToToken[i] = STRING_TOKEN;
        }

        asciiToToken['['] = ARRAY_BEGIN_TOKEN;
        asciiToToken[']'] = ARRAY_END_TOKEN;
        asciiToToken['('] = VECTOR_BEGIN_TOKEN;
        asciiToToken[')'] = VECTOR_END_TOKEN;
        asciiToToken['{'] = HASHTABLE_BEGIN_TOKEN;
        asciiToToken['='] = HASHTABLE_KEY_VALUE_SEP_TOKEN;
        asciiToToken[';'] = HASHTABLE_KEY_VALUE_END_TOKEN;
        asciiToToken['}'] = HASHTABLE_END_TOKEN;
        asciiToToken[','] = GENERIC_SEP_TOKEN;
        asciiToToken['@'] = NULL_VALUE_TOKEN;
        asciiToToken['/'] = NULL_TOKEN; /* Comment begin/end */
    }

    static final int CHARACTER_COUNT_PER_ARRAY = 128;
    static final int CCPA_BIT_COUNT = 7;
    static final int CCPA_MASK      = 0x7F;

    static final int PARSING_NONE_STATE           =0;
    static final int PARSING_STRING_STATE         =1;
    static final int PARSING_QUOTED_STRING_STATE  =2;
    static final int PARSING_COMMENT_STATE        =3;
    static final int PARSING_C_STYLE_COMMENT_STATE=4;
    static final int PARSING_C_PLUS_PLUS_STYLE_COMMENT_STATE=5;

    private char input[][];  /* List of array of chars */

    private int    nextAvailableByteIndex;
    private int    markedByteIndex;
    private int    nextFreeByteSlotIndex;

    private char[]    charsForCurrentToken;
    private int       currentToken;
    private int       lastToken;

    private int       currentLineNumber;

    private boolean previousCharacterWasBackslash=false;
    private boolean starFound=false; /* used while parsing C-style comments */
    private int     parserState;

    public TokenGenerator (Reader in)
    {
        super(in);
        input = new char[1][];
        input[0] = new char[CHARACTER_COUNT_PER_ARRAY];
        nextAvailableByteIndex=0;
        nextFreeByteSlotIndex =0;
        currentLineNumber=0;
        parserState = PARSING_NONE_STATE;
    }

    private final void markCurrentCharacter ()
    {
        markedByteIndex=nextAvailableByteIndex;
    }

    private final void markPreviousCharacter ()
    {
        markedByteIndex=nextAvailableByteIndex-1;
    }

    private final void growInputBuffer ()
    {
        char newInput[][] = new char[input.length+1][];
        System.arraycopy(input, 0, newInput, 0, input.length);
        newInput[input.length] = new char[CHARACTER_COUNT_PER_ARRAY];
        input = newInput;
    }

    private final void readMoreCharacters ()
      throws IOException
    {
        int length;
        int currentArrayIndex = nextFreeByteSlotIndex >> CCPA_BIT_COUNT;

        if (currentArrayIndex >= input.length) {
            growInputBuffer();
        }

        length = read(input[currentArrayIndex],
                      nextFreeByteSlotIndex & CCPA_MASK,
                      CHARACTER_COUNT_PER_ARRAY -
                      (nextFreeByteSlotIndex & CCPA_MASK));
        if (length != -1) {
            nextFreeByteSlotIndex += length;
        }
        else {
            return;
        }

        /*
            Make sure we read at least CHARCTER_COUNT_PER_ARRAY characters
            this is necessary to keep a constant number of cached characters
        */
        if (ready() && length < CHARACTER_COUNT_PER_ARRAY) {
            currentArrayIndex = nextFreeByteSlotIndex >> CCPA_BIT_COUNT;

            if (currentArrayIndex >= input.length) {
                growInputBuffer();
            }

            length = read(input[currentArrayIndex],
                          nextFreeByteSlotIndex & CCPA_MASK,
                          CHARACTER_COUNT_PER_ARRAY -
                          (nextFreeByteSlotIndex & CCPA_MASK));
            if (length != -1) {
                nextFreeByteSlotIndex += length;
            }
        }
    }

    private final boolean hasMoreCharacters ()
      throws IOException
    {
        if (nextAvailableByteIndex < nextFreeByteSlotIndex) {
            return true;
        }
        else {
            readMoreCharacters();
            if (nextAvailableByteIndex < nextFreeByteSlotIndex) {
                return true;
            }
        }
        return false;
    }

    private final char peekNextCharacter ()
      throws IOException
    {
        char result = 0;
        if (nextAvailableByteIndex >= nextFreeByteSlotIndex) {
            readMoreCharacters();
        }

        if (nextAvailableByteIndex < nextFreeByteSlotIndex) {
            result =
                input[nextAvailableByteIndex >> CCPA_BIT_COUNT]
                [nextAvailableByteIndex & CCPA_MASK];
            nextAvailableByteIndex++;
        }
        return result;
    }

    private final void rewindToMarkedCharacter ()
    {
        nextAvailableByteIndex = markedByteIndex;
    }

    /* Warning this method is "inlined" manually in parseOneToken */
    private final void deletePeekedCharacters ()
    {
        markedByteIndex=-1;
        while ((nextAvailableByteIndex >> CCPA_BIT_COUNT) > 0) {
            char tmp[] = input[0];
            int i;
            int c;
            for (i = 0, c = input.length-1; i < c ; i++) {
                input[i] = input[i+1];
            }
            input[input.length-1] = tmp;
            nextAvailableByteIndex -= CHARACTER_COUNT_PER_ARRAY;
            nextFreeByteSlotIndex  -= CHARACTER_COUNT_PER_ARRAY;
        }
    }

    private final void deletePeekedCharactersMinusOne ()
    {
        markedByteIndex=-1;
        while (((nextAvailableByteIndex-1) >> CCPA_BIT_COUNT) > 0) {
            char tmp[] = input[0];
            int i;
            int c;
            for (i = 0, c = input.length-1; i < c ; i++) {
                input[i] = input[i+1];
            }
            input[input.length-1] = tmp;
            nextAvailableByteIndex -= CHARACTER_COUNT_PER_ARRAY;
            nextFreeByteSlotIndex  -= CHARACTER_COUNT_PER_ARRAY;
        }
    }

    private final char[] getAndDeletePeekedCharacters ()
    {
        int length = nextAvailableByteIndex - markedByteIndex;
        char result[] = new char[length];
        int i;
        int c;
        /* arraycopy is slower than this */
        for (i = markedByteIndex, c = markedByteIndex+length; i < c ; i++) {
            result[i-markedByteIndex] =
                input[i >> CCPA_BIT_COUNT][i & CCPA_MASK];
        }
        deletePeekedCharacters();
        markedByteIndex=-1;
        return result;
    }

    private final char[] getAndDeletePeekedCharactersMinusOne ()
    {
        int length = nextAvailableByteIndex - markedByteIndex - 1;
        char result[] = new char[length];
        int i;
        int c;
        /* arraycopy is slower than this */
        for (i = markedByteIndex, c = markedByteIndex+length; i < c ; i++) {
            result[i-markedByteIndex] =
                input[i >> CCPA_BIT_COUNT][i & CCPA_MASK];
        }
        deletePeekedCharactersMinusOne();
        markedByteIndex=-1;
        return result;
    }

    private final void parseOneToken ()
      throws DeserializationException, IOException
    {
        char ch;
        while (currentToken == NULL_TOKEN) {
            if (nextAvailableByteIndex >= nextFreeByteSlotIndex) {
                readMoreCharacters();
                if (nextAvailableByteIndex >= nextFreeByteSlotIndex) {
                    break;
                }
            }

            if (markedByteIndex == -1) {
                markedByteIndex=nextAvailableByteIndex; /* markCurrentCharacter(); */

            }
            /* This is peekNextCharcter */
            ch =
                input[nextAvailableByteIndex >> CCPA_BIT_COUNT]
                [nextAvailableByteIndex & CCPA_MASK];
            nextAvailableByteIndex++;
            if (ch == '\n') {
                currentLineNumber++;
            }
            if (parserState == PARSING_QUOTED_STRING_STATE) {
                if (!previousCharacterWasBackslash && ch=='"') {
                    currentToken = STRING_TOKEN;
                    charsForCurrentToken = getAndDeletePeekedCharacters();
                    parserState = PARSING_NONE_STATE;
                    markedByteIndex=nextAvailableByteIndex; /* markCurrentCharacter(); */
                    previousCharacterWasBackslash = false;
                }
                else if (ch == '\\') {
                    previousCharacterWasBackslash =
                        !previousCharacterWasBackslash;
                }
                else {
                    previousCharacterWasBackslash = false;
                }
            }
            else {
                int token;
                if (ch >= 0 && ch < 127) {
                    token = asciiToToken[ch];
                }
                else {
                    token = NULL_TOKEN;
                }
                if (parserState == PARSING_STRING_STATE) {
                    if (ch != '"' && token == STRING_TOKEN) {
                        continue;
                    }
                    else {
                        currentToken = STRING_TOKEN;
                        charsForCurrentToken = getAndDeletePeekedCharactersMinusOne();
                        parserState = PARSING_NONE_STATE;
                            // markedByteIndex = markPreviousCharacter();
                        markedByteIndex = nextAvailableByteIndex-1;
                        rewindToMarkedCharacter();
                    }
                }
                else if (parserState == PARSING_COMMENT_STATE) {
                    if (ch == '*') {
                        parserState = PARSING_C_STYLE_COMMENT_STATE;
                    }
                    else if (ch == '/') {
                        parserState = PARSING_C_PLUS_PLUS_STYLE_COMMENT_STATE;
                    }
                    else {
                        throw new DeserializationException(
                            Fmt.S("Syntax error at line %s",
                                  lineForLastToken()),
                            lineForLastToken());
                    }
                }
                else if (parserState == PARSING_C_STYLE_COMMENT_STATE) {
                    if (starFound && ch == '/') {
                        starFound = false;
                        parserState = PARSING_NONE_STATE;
                        continue;
                    }
                    if (ch == '*') {
                        starFound = true;
                    }
                    else {
                        starFound = false;
                    }
                }
                else if (parserState == PARSING_C_PLUS_PLUS_STYLE_COMMENT_STATE) {
                    if (ch == '\n') {
                        parserState = PARSING_NONE_STATE;
                        continue;
                    }

                }
                else {
                    if (ch == '/') {
                        parserState = PARSING_COMMENT_STATE;
                        continue;
                    }
                    if (token == NULL_TOKEN) {
                        continue;
                    }
                    else if (token == STRING_TOKEN) {
                        if (ch == '"') {
                            parserState = PARSING_QUOTED_STRING_STATE;
                        }
                        else {
                            parserState = PARSING_STRING_STATE;
                        }

                        deletePeekedCharactersMinusOne();
                            // markedByteIndex = markPreviousCharacter();
                        markedByteIndex = nextAvailableByteIndex-1;
                    }
                    else {
                        currentToken = token;
                        charsForCurrentToken = null;
                        /* This is deletePeekedCharacter() */
                        markedByteIndex=-1;
                        while ((nextAvailableByteIndex >> CCPA_BIT_COUNT) > 0) {
                            char tmp[] = input[0];
                            int i;
                            int c;
                            for (i = 0, c = input.length-1; i < c ; i++) {
                                input[i] = input[i+1];
                            }
                            input[input.length-1] = tmp;
                            nextAvailableByteIndex -= CHARACTER_COUNT_PER_ARRAY;
                            nextFreeByteSlotIndex  -= CHARACTER_COUNT_PER_ARRAY;
                        }
                            // markedByteIndex = markCurrentCharacter();
                        markedByteIndex = nextAvailableByteIndex;
                    }
                }
            }
        }

        if (currentToken == NULL_TOKEN && !hasMoreCharacters()) {
            switch (parserState) {
              case PARSING_NONE_STATE:
                break;
              case PARSING_STRING_STATE:
                currentToken = STRING_TOKEN;
                charsForCurrentToken=getAndDeletePeekedCharacters();
                parserState = PARSING_NONE_STATE;
                previousCharacterWasBackslash=false;
                break;
              case PARSING_QUOTED_STRING_STATE:
                /* Syntax error unterminated String with quote */
                parserState = PARSING_NONE_STATE;
                previousCharacterWasBackslash=false;
                throw new DeserializationException(
                    Fmt.S("Unterminated string at line %s",
                          lineForLastToken()),
                    lineForLastToken());
              case PARSING_COMMENT_STATE:
                parserState = PARSING_NONE_STATE;
                throw new DeserializationException(
                    Fmt.S("Syntax error at line %s", lineForLastToken()),
                    lineForLastToken());
              case PARSING_C_STYLE_COMMENT_STATE:
                parserState = PARSING_NONE_STATE;
                starFound=false;
                throw new DeserializationException(
                    Fmt.S("Unterminated comment at line %s",
                          lineForLastToken()),
                    lineForLastToken());
              case PARSING_C_PLUS_PLUS_STYLE_COMMENT_STATE:
                parserState = PARSING_NONE_STATE;
                break;
            }
        }
    }



    /* Return true if the TokenGenerator has more token to return */
    public final boolean hasMoreTokens ()
      throws DeserializationException, IOException
    {
        if (currentToken != NULL_TOKEN) {
            return true;
        }
        parseOneToken();
        if (currentToken != NULL_TOKEN) {
            return true;
        }
        return false;
    }

    /* Return the next token */
    public final int nextToken ()
      throws DeserializationException, IOException
    {
        int result = NULL_TOKEN;
        if (currentToken == NULL_TOKEN) {
            parseOneToken();
        }
        if (currentToken != NULL_TOKEN) {
            result = currentToken;
            lastToken = currentToken;
            currentToken = NULL_TOKEN;
        }
        return result;
    }

    /* Return the next available token but does not remove it */
    public final int peekNextToken ()
      throws DeserializationException, IOException
    {
        hasMoreTokens();
        lastToken = currentToken;
        return currentToken;
    }

    /**
        Return the bytes parsed to generate the last returned token.
    */
    public final char[] charsForLastToken ()
    {
        if (lastToken == STRING_TOKEN) {
            return charsForCurrentToken;
        }
        char c[] = new char[1];
        c[0] = tokenToAscii[lastToken];
        return c;
    }

    /**
        Returns the ASCII byte that matches a given token. Use this method
        when the token is a separator (everything but a string).
    */
    public char charForLastToken ()
    {
        return tokenToAscii[lastToken];
    }

    /**
        Return the line number for the last returned token
    */
    public int lineForLastToken ()
    {
        return currentLineNumber+1;
    }
}

