/*
    Copyright 1996-2009 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/io/Deserializer.java#8 $
*/

package ariba.util.io;

import ariba.util.core.Constants;
import ariba.util.core.MapUtil;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.OrderedHashtable;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

/**
    FilterInputStream subclass that can deserialize Maps,
    Lists, arrays, and Strings from an ASCII stream. The
    serialization format is very similar to the output of Map's
    and List's <b>toString()</b> methods, except strings with
    non-alphanumeric characters are quoted and special characters are
    escaped so that the output can be unambiguously deserialized.

    @see Serializer

    @aribaapi private
*/
public class Deserializer extends FilterReader
{
    private TokenGenerator tokenGenerator;


    /* static methods */


    /**
        Convenience method for creating a Deserializer taking its
        input from <b>reader</b>. This method only returns
        <b>null</b> on error instead of throwing an exception.
    */
    public static Object readObject (Reader reader)
    {
        Object object;
        Deserializer deserializer;

        try {
            deserializer = new Deserializer(reader);
            object = deserializer.readObject();
        }
        catch (IOException e) {
            object = null;
        }
        catch (DeserializationException e) {
            object = null;
        }
        return object;
    }

    private boolean useOrderedHashtable = false;

    /* constructors */


    /**
        Constructs a Deserializer that takes its input from <b>reader</b>.
    */
    public Deserializer (Reader reader)
    {
        super(reader);
        tokenGenerator = new TokenGenerator(reader);
    }

    public boolean useOrderedHashtable ()
    {
        return useOrderedHashtable;
    }

    public void setUseOrderedHashtable (boolean useOrderedHashtable)
    {
        this.useOrderedHashtable = useOrderedHashtable;
    }

    /**
        Convenience method for deserializing from the string
        <b>serialization</b>. Returns <b>null</b> on error.
    */
    public static Object deserializeObject (String serialization)
    {
        StringReader in;

        if (serialization == null) {
            return null;
        }

        in = new StringReader(serialization);
        return readObject(in);
    }

    /**
        Deserializes the next Dictionary, array, List, or String
        from the current reader.
    */
    public Object readObject ()
      throws IOException, DeserializationException
    {
        return readObjectInternal();
    }

    /**
        Deserializes the next object from the current reader.
        The result goes *into* the passed in object as opposed to
        creating a brand new instance.

        NOTE: Currently, this is only wired up for hashtables and lists.
        See readObjectInternal(Object instance) for the actual implementation.
    */
    public void readObject (Object instance)
      throws IOException, DeserializationException
    {
        readObjectInternal(instance);
    }

    private final void readObjectInternal (Object instance)
      throws IOException, DeserializationException
    {
        int token;

        if (!tokenGenerator.hasMoreTokens()) {
            return;
        }

        token = tokenGenerator.nextToken();
        switch (token) {
            case TokenGenerator.STRING_TOKEN:
            case TokenGenerator.ARRAY_BEGIN_TOKEN:
                return;
            case TokenGenerator.VECTOR_BEGIN_TOKEN:
                readIntoList((List)instance);
                return;
            case TokenGenerator.HASHTABLE_BEGIN_TOKEN:
                readIntoMap((Map)instance);
                return;
            default:
                syntaxError();
                return;
        }
    }

    private final Object readObjectInternal ()
      throws IOException, DeserializationException
    {
        int token;

        if (!tokenGenerator.hasMoreTokens()) {
            return null;
        }

        token = tokenGenerator.nextToken();
        switch (token) {
            case TokenGenerator.STRING_TOKEN:
                return stringForToken();
            case TokenGenerator.ARRAY_BEGIN_TOKEN:
                return readArray();
            case TokenGenerator.VECTOR_BEGIN_TOKEN:
                return readList();
            case TokenGenerator.HASHTABLE_BEGIN_TOKEN:
                return readMap();
            default:
                syntaxError();
                return null;
        }
    }

    private final void readKeyValuePair (Map result)
      throws IOException, DeserializationException
    {
        Object key;
        Object value;
        int token;
        int nextToken;

        key = readObjectInternal();
        if (key == null) {
            unterminatedExpression();
        }

        if (!tokenGenerator.hasMoreTokens()) {
            unterminatedExpression();
        }

        token = tokenGenerator.nextToken();
        if (token != TokenGenerator.HASHTABLE_KEY_VALUE_SEP_TOKEN) {
            syntaxError();
        }

        if (!tokenGenerator.hasMoreTokens()) {
            unterminatedExpression();
        }
        
        // This code is here and not in readObjectInternal() because we
        // only want to allow null in arrays and maps.  It should be a syntax error
        // for the null token to appear elsewhere.

        nextToken = tokenGenerator.peekNextToken();
        if (nextToken == TokenGenerator.NULL_VALUE_TOKEN) {
            tokenGenerator.nextToken();
            value = null;
        }
        else {
            value = readObjectInternal();
            if (value == null) {
                unterminatedExpression();
            }
        }
        result.put(key, value);

        if (!tokenGenerator.hasMoreTokens()) {
            unterminatedExpression();
        }

        token = tokenGenerator.peekNextToken();
        if (token == TokenGenerator.HASHTABLE_KEY_VALUE_END_TOKEN ||
            token == TokenGenerator.GENERIC_SEP_TOKEN)
            tokenGenerator.nextToken();
    }

    private final Map readMap ()
      throws IOException, DeserializationException
    {
        Map result = null;

        if (useOrderedHashtable()) {
            result = new OrderedHashtable();
        }
        else {
            result = MapUtil.map();
        }
        readIntoMap(result);
        return result;
    }

    private final void readIntoMap (Map target)
      throws IOException, DeserializationException
    {
        int token;

        while (true) {
            if (!tokenGenerator.hasMoreTokens()) {
                unterminatedExpression();
            }

            token = tokenGenerator.peekNextToken();
            if (token == TokenGenerator.HASHTABLE_END_TOKEN) {
                tokenGenerator.nextToken();
                return;
            }

            readKeyValuePair(target);
        }
    }

    private final List readList ()
      throws IOException, DeserializationException
    {
        List result = ListUtil.list();
        int token;
        boolean justAddedObject=false;
        Object object;

        while (true) {
            if (!tokenGenerator.hasMoreTokens()) {
                unterminatedExpression();
            }

            token = tokenGenerator.peekNextToken();
            if (token == TokenGenerator.VECTOR_END_TOKEN) {
                tokenGenerator.nextToken();
                return result;
            }

            if (token == TokenGenerator.GENERIC_SEP_TOKEN) {
                tokenGenerator.nextToken();
                if (justAddedObject) {
                    justAddedObject = false;
                }
                else {
                    syntaxError();
                }
            }
            else if (justAddedObject) {
                syntaxError();
            }

            object = readObjectInternal();
            if (object != null) {
                result.add(object);
                justAddedObject = true;
            }
        }
    }

    private final void readIntoList (List target)
      throws IOException, DeserializationException
    {
        List list = readList();
        target.addAll(list);
    }

    private final Object[] readArray ()
      throws IOException, DeserializationException
    {
        Object buf[] = new Object[16];
        int bufIndex = 0;
        Object obj;
        int nextToken;
        boolean justAddedObject=false;

        while (true) {
            if (!tokenGenerator.hasMoreTokens()) {
                unterminatedExpression();
            }

            nextToken = tokenGenerator.peekNextToken();
            if (nextToken == TokenGenerator.ARRAY_END_TOKEN) {
                tokenGenerator.nextToken();
                Object result[] = new Object[bufIndex];
                System.arraycopy(buf, 0, result, 0, bufIndex);
                return result;
            }
            else if (nextToken == TokenGenerator.GENERIC_SEP_TOKEN) {
                tokenGenerator.nextToken();
                if (justAddedObject) {
                    justAddedObject=false;
                }
                else {
                    syntaxError();
                }
            }
            else if (justAddedObject) {
                syntaxError();
            }

            // This code is here and not in readObjectInternal() because we
            // only want to allow null in arrays.  It should be a syntax error
            // for the null token to appear elsewhere.

            nextToken = tokenGenerator.peekNextToken();
            if (nextToken == TokenGenerator.NULL_VALUE_TOKEN) {
                tokenGenerator.nextToken();
                obj = null;
            }
            else {
                obj = readObjectInternal();
            }

            buf[bufIndex++] = obj;
            if (bufIndex == buf.length) {
                Object newBuf[] = new Object[buf.length * 2];
                System.arraycopy(buf, 0, newBuf, 0, buf.length);
                buf = newBuf;
            }
            justAddedObject = true;
        }
    }

    private final String stringForToken () throws DeserializationException
    {
        char str[] = tokenGenerator.charsForLastToken();

        if (str == null || str.length == 0) {
            internalInconsistency("empty string");
        }

        if (str[0] == '"') {
            char ch;
            char charBuf[] = new char[32];
            int charBufIndex = 0;
            int i;
            int c;

            for (i = 1, c = str.length-1; i < c; i++) {
                ch = str[i];
                if (ch == '\\') {
                    char nextChar = 0;
                    if (++i < c) {
                        nextChar = str[i];
                    }
                    else {
                        malformedString();
                    }

                    switch (nextChar) {
                      case '"': {
                          nextChar = '\"';
                          break;
                      }
                      case 't': {
                          nextChar = '\t';
                          break;
                      }
                      case 'n': {
                          nextChar = '\n';
                          break;
                      }
                      case 'r': {
                          nextChar = '\r';
                          break;
                      }
                      case '\\': {
                          nextChar = '\\';
                          break;
                      }
                      case 'u': {
                          char one   = 0;
                          char two   = 0;
                          char three = 0;
                          char four  = 0;

                          if (++i < c) {
                              one = str[i];
                          }
                          else {
                              malformedString();
                          }

                          if (++i < c) {
                              two = str[i];
                          }
                          else {
                              malformedString();
                          }

                          if (++i < c) {
                              three = str[i];
                          }
                          else {
                              malformedString();
                          }

                          if (++i < c) {
                              four = str[i];
                          }
                          else {
                              malformedString();
                          }

                          if (isHexa(one)   &&
                              isHexa(two)   &&
                              isHexa(three) &&
                              isHexa(four))
                          {
                              nextChar = (char)
                                  ((asciiToFourBits(one)   << 12) |
                                   (asciiToFourBits(two)   <<  8) |
                                   (asciiToFourBits(three) <<  4) |
                                   (asciiToFourBits(four)  <<  0));
                              break;
                          }
                          else {
                              malformedString();
                          }
                          break;
                      }

                      default: {
                          char up     = 0;
                          char middle = 0;
                          char low    = 0;

                          up = nextChar;
                          if (++i < c) {
                              middle = str[i];
                          }
                          else {
                              i--;
                          }

                          if (++i < c) {
                              low = str[i];
                          }
                          else {
                              i--;
                          }

                          if (up >= '0' && up <= '7' &&
                              middle >= '0' && middle <= '7' &&
                              low >= '0' && low <= '7') {
                              nextChar = (char)(((up - '0') << 6) |
                                                ((middle - '0') << 3) |
                                                (low - '0'));
                              break;
                          }
                          else {
                              malformedString();
                          }

                          break;
                      }
                    }

                    charBuf[charBufIndex++] = nextChar;
                    if (charBufIndex == charBuf.length) {
                        char newCharBuf[]  = new char[charBuf.length * 2];
                        System.arraycopy(charBuf, 0, newCharBuf, 0,
                                         charBuf.length);
                        charBuf = newCharBuf;
                    }
                }
                else {
                    charBuf[charBufIndex++] = ch;
                    if (charBufIndex == charBuf.length) {
                        char newCharBuf[]  = new char[charBuf.length * 2];
                        System.arraycopy(charBuf, 0, newCharBuf, 0,
                                         charBuf.length);
                        charBuf = newCharBuf;
                    }
                }
            }

            return new String(charBuf, 0, charBufIndex);
        }
        else {
            return new String(str);
        }
    }

    private final boolean isHexa (char c)
    {
        return ((c >= '0' && c <= '9') ||
                (c >= 'a' && c <= 'f') ||
                (c >= 'A' && c <= 'F'));
    }

    private final byte asciiToFourBits (char c)
    {
        if (c >= '0' && c <= '9') {
            return (byte)(c - '0');
        }
        if (c >= 'a' && c <= 'f') {
            return (byte)(c - 'a'+0xa);
        }
        /* We assume that char c makes sense... Caller should test it */
        return (byte)(c - 'A'+0xa);
    }

    private void malformedString () throws DeserializationException
    {
        int line = tokenGenerator.lineForLastToken();

        throw new DeserializationException(
            Fmt.S("%s%s:%s",
                  "Malformed string at line ",
                  Constants.getInteger(line),
                  new String(tokenGenerator.charsForLastToken())),
            line);
    }

    private void syntaxError () throws DeserializationException
    {
        int line = tokenGenerator.lineForLastToken();
        String lineStr = "[not available]";
        try {
            lineStr = String.valueOf(tokenGenerator.charsForLastToken());
        }
        catch (Throwable th) {
            // suppress it in case there is bad data and token string cannot be obtained
        }
        throw new DeserializationException(Fmt.S("Syntax error at line %s. Current token: \"%s\"", line,
            lineStr), line);
    }

    private void internalInconsistency (String type) throws
      DeserializationException {
        int line = tokenGenerator.lineForLastToken();

        throw new DeserializationException(
            Fmt.S("%s%s %s",
                  "Internal inconsistency exception. Please report this problem. ",
                  type, Constants.getInteger(line)), line);
    }

    private void unterminatedExpression () throws DeserializationException
    {
        int line = tokenGenerator.lineForLastToken();

        throw new DeserializationException(
            Fmt.S("Unterminated expression at line %s", line),
            line);
    }
}
