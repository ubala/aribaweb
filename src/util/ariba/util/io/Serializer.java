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

    $Id: //ariba/platform/util/core/ariba/util/io/Serializer.java#8 $
*/

package ariba.util.io;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
    Object subclass that can serialize a fixed set of data types
    (Dictionaries, arrays, Lists, and Strings) to an ASCII writer.
    If the object passed in is not one of these types, or contains an
    object that is not one of these types, the Serializer converts the
    object to a string via the object's <b>toString()</b> method. The
    serialization format is very similar to the output of Map's
    and List's <b>toString()</b> methods, except that strings with
    non-alphanumeric characters are quoted and special characters are
    escaped, so that the output can be unambiguously deserialized.
    Serializer produces an ASCII representation with few, if any,
    spaces separating components. To get a more readable
    representation, use the OutputSerializer class.

    @see Deserializer
    @note 1.0 Added several unsafe characters that will always be quoted
    (fixed problem with archiving the @ symbol)

    @aribaapi private
*/
public class Serializer extends FilterWriter
{
    private static boolean unsafeChars[];
    private static final int SMALL_STRING_LIMIT=40;
    private static final int BUF_LEN = 128;
    private char buf[] = new char[BUF_LEN];
    private int bufIndex=0;

    static {
        unsafeChars = new boolean[127];
        for (int i=0;i<' ';i++)
            unsafeChars[i]=true;

        unsafeChars[' '] = true; /* Token separator*/
        unsafeChars['"'] = true; /* Strings */
        unsafeChars['['] = true; /* Arrays  */
        unsafeChars[']'] = true;
        unsafeChars[','] = true;
        unsafeChars['('] = true; /* Lists */
        unsafeChars[')'] = true;
        unsafeChars['{'] = true; /* Dictionaries*/
        unsafeChars['}'] = true;
        unsafeChars['='] = true;
        unsafeChars[';'] = true;
        unsafeChars['/'] = true; /* Comment */
        unsafeChars['@'] = true; /* Null */

        unsafeChars['!'] = true; /* Reserved */
        unsafeChars['#'] = true;
        unsafeChars['$'] = true;
        unsafeChars['%'] = true;
        unsafeChars['&'] = true;
        unsafeChars['\''] = true;
        unsafeChars[':'] = true;
        unsafeChars['<'] = true;
        unsafeChars['>'] = true;
        unsafeChars['?'] = true;
        unsafeChars['\\'] = true;
        unsafeChars['^'] = true;
        unsafeChars['`'] = true;
        unsafeChars['|'] = true;
        unsafeChars['~'] = true;
    }

    /** Constructs a Serializer that writes its output to <b>writer</b>.
      */
    public Serializer (Writer writer)
    {
        super(writer);
    }

    private void flushBuffer () throws IOException
    {
        if (bufIndex > 0) {
            this.write(buf, 0, bufIndex);
            bufIndex=0;
        }
    }

    final void writeOutput (int character) throws IOException
    {
        if (bufIndex >= BUF_LEN) {
            flushBuffer();
        }
        buf[bufIndex++] = (char)character;
    }

    private final void serializeMap (Map h) throws IOException
    {
        Iterator e=h.keySet().iterator();
        Object key;
        Object value;
        writeOutput('{');
        while (e.hasNext()) {
            key = e.next();
            value = h.get(key);
                // Serialize the key. Test if it is a string to avoid one
                // recursion in the common case.
            if (key instanceof String) {
                serializeString((String)key);
            }
            else {
                serializeObjectInternal(key);
            }

            writeOutput('=');
            if (value instanceof String) {
                serializeString((String)value);
            }
            else {
                serializeObjectInternal(value);
            }
            writeOutput(';');
        }
        writeOutput('}');
    }

    private final void serializeArray (Object a[]) throws IOException
    {
        writeOutput('[');
        for (int i = 0, c = a.length; i < c; i++) {
            Object o = a[i];
            if (o instanceof String) {
                serializeString((String)o);
            }
            else {
                serializeObjectInternal(o);
            }
            if (i < (c-1)) {
                writeOutput(',');
            }
        }
        writeOutput(']');
    }

    private final void serializeList (List v) throws IOException
    {
        Object o;
        int i;
        int c;
        writeOutput('(');
        for (i = 0, c = v.size(); i < c; i++) {
            o = v.get(i);
            if (o instanceof String) {
                serializeString((String)o);
            }
            else {
                serializeObjectInternal(o);
            }
            if (i < (c-1)) {
                writeOutput(',');
            }
        }
        writeOutput(')');
    }

    final boolean stringRequiresQuotes(String s) {
        char ch;
        int i;
        int c;
        for (i = 0, c = s.length(); i < c; i++) {
            ch = s.charAt(i);
            if (ch >= 127) {
                return true;
            }
            if (unsafeChars[ch]) {
                return true;
            }
        }
        return false;
    }

    private final boolean stringRequiresQuotes (char str[])
    {
        char ch;
        int i;
        int c;
        for (i = 0, c = str.length; i < c; i++) {
            ch = str[i];
            if (ch >= 127) {
                return true;
            }
            if (unsafeChars[ch]) {
                return true;
            }
        }
        return false;
    }

    private final int fourBitToAscii (int n)
    {
        if (n < 0xa) {
            return '0' + n;
        }
        return 'A' + (n-0xa);
    }

    /**
        JP: The only reason this method exists is to get around a class
        verifier error in IE 3.02 in the method serializeString.  IE 3.02
        didn't like the "ch = str[i]" line being in serializeString.
    */
    private void serializeCharacters (char[]  str,
                                      String  s,
                                      int     length,
                                      boolean shouldUseArray)
      throws IOException
    {
        for (int i=0; i < length ; i++) {
            char ch;
            if (shouldUseArray) {
                ch = str[i];
            }
            else {
                ch = s.charAt(i);
            }
            if (ch < 0xff) {
                    // ASCII
                if (ch >= '#' && ch <= '~' && ch != '\\') {
                    writeOutput(ch);
                }
                else {
                    switch (ch) {
                      case ' ':
                      case '!':
                        writeOutput(ch);
                        break;
                      case '"':
                        writeOutput('\\');
                        writeOutput('"');
                        break;
                      case '\t':
                        writeOutput('\\');
                        writeOutput('t');
                        break;
                      case '\n':
                        writeOutput('\\');
                        writeOutput('n');
                        break;
                      case '\r':
                        writeOutput('\\');
                        writeOutput('r');
                        break;
                      case '\\':
                        writeOutput('\\');
                        writeOutput('\\');
                        break;
                      default:
                        writeOutput(ch);
                        break;
                    }
                }
            }
            else {
                writeOutput(ch);
            }

        }
    }

    void serializeString (String s) throws IOException
    {
        if (s == null) {
            writeOutput('"');
            writeOutput('"');
            return;
        }
        int length = s.length();
        if (length == 0) {
            writeOutput('"');
            writeOutput('"');
            return;
        }

        boolean shouldUseArray = (length > 8);
        char str[];
        if (shouldUseArray) {
            str = s.toCharArray();
        }
        else {
            str = null;
        }

        /*
            If the string is bigger than SMALL_STRING_LIMIT, don't
            bother searching for unsafe character. The probably to
            have a space is high enough to add '"' automatically.
        */
        boolean shouldUseQuote;
        if (length > SMALL_STRING_LIMIT) {
            shouldUseQuote = true;
        }
        else {
            if (shouldUseArray) {
                shouldUseQuote = stringRequiresQuotes(str);
            }
            else {
                shouldUseQuote = stringRequiresQuotes(s);
            }
        }

        if (shouldUseQuote) {
            writeOutput('"');
        }

        serializeCharacters(str, s, length, shouldUseArray);

        if (shouldUseArray) {
            str = null; /* Pretty please!*/
        }
        if (shouldUseQuote) {
            writeOutput('"');
        }
    }

    final void serializeNull () throws IOException
    {
        // We have our own magic null token!  This should only happen
        // in arrays.
        writeOutput('@');
    }

    private final void serializeObjectInternal (Object anObject) throws
                                                                IOException {
        if (anObject instanceof String) {
            serializeString((String)anObject);
        }
        else if (anObject instanceof Map) {
            serializeMap((Map)anObject);
        }
        else if (anObject instanceof Object[]) {
            serializeArray((Object[])anObject);
        }
        else if (anObject instanceof List) {
            serializeList((List)anObject);
        }
        else if (anObject == null) {
            serializeNull();
        }
        else {
            serializeString(anObject.toString());
        }
    }

    /**
        Flushes the Serializer's output to its writer.
    */
    public void flush () throws IOException
    {
        flushBuffer();
        super.flush();
    }

    /**
        Serializes <b>anObject</b> to its writer
    */
    public void writeObject (Object anObject) throws IOException
    {
        serializeObjectInternal(anObject);
    }


    /* conveniences */



    /**
        Convenience method for generating <b>anObject</b>'s ASCII
        serialization. Returns <b>null</b> on error.
    */
    public static String serializeObject (Object anObject)
    {
        String result = null;
        if (anObject == null) {
            result = null;
        }
        else {
            StringWriter memory = new StringWriter();
            Serializer serializer = new Serializer(memory);

            try {
                serializer.writeObject(anObject);
                serializer.flush();
            }
            catch (IOException e) {
            }


            result = memory.toString();
            try {
                serializer.close();
                memory.close();
            }
            catch (IOException e) {
            }
            memory=null;
            serializer=null;
        }
        return result;
    }

    /**
        Convenience method for writing <b>anObject</b's ASCII
        serialization to <b>writer</b>. Returns <b>true</b> if
        the serialization and writing succeeds, rather than throwing
        an exception.
    */
    public static boolean writeObject (Writer writer,
                                       Object anObject)
    {
        Serializer serializer;

        try {
            serializer = new Serializer(writer);
            serializer.writeObject(anObject);
            serializer.flush();
        }
        catch (IOException e) {
            return false;
        }
        return true;
    }
}
