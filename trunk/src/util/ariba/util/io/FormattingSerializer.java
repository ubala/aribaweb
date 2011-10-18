/*
    Copyright (c) 1996-2011 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/io/FormattingSerializer.java#10 $
*/

package ariba.util.io;

import ariba.util.core.DebugState;
import ariba.util.core.Fmt;
import ariba.util.core.OrderedHashtable;
import ariba.util.core.Sort;
import ariba.util.core.StringCompare;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
    Serializer subclass that formats and indents the ASCII generated
    by the Serializer. This class makes it possible to insert comments
    into a serialization.

    @aribaapi private
*/
public class FormattingSerializer extends Serializer
{
    private final int MAX_SIZE_FOR_SMALL_EXPRESSION = 80;

    private int indentationLength;
    private int tabLevel;
    private int nextCharIndex;

    /** Constructs a FormattingSerializer that writes to <b>writer</b>.
      */
    public FormattingSerializer (Writer writer)
    {
        super(writer);
        indentationLength = 4;
        tabLevel = 0;
        nextCharIndex = 0;
    }

    /**
        Sets the number of spaces to indent an expression. The default
        value is 4.
    */
    public void setIndentationLength (int numberOfSpaces)
    {
        indentationLength = numberOfSpaces;
    }

    /**
        Returns the number of spaces currently used to indent an expression.
    */
    public int indentationLength ()
    {
        return indentationLength;
    }

    /**
        Writes <b>aComment</b> to the FormattingSerializer's writer.
        Ignores all non-ASCII characters. If <b>cStyle</b> is
        <b>true</b>, the FormattingSerializer will use C-style
        delimiters, otherwise it will use C++-style delimiters ("//").
        <b>aComment</b> should not include comment delimiters.
    */
    public void writeComment (String aComment, boolean cStyle)
      throws IOException
    {
        int length = aComment.length();

        int delimiterSize;
        if (cStyle) {
            delimiterSize = 7;
        }
        else {
            delimiterSize = 4;
        }
        /* Does it fits on one line ? */
        if ((length + delimiterSize) <= (MAX_SIZE_FOR_SMALL_EXPRESSION -
                                         nextCharIndex) &&
           aComment.indexOf('\n') == -1)
        {
            if (cStyle) {
                writeCommentCharacter('/');
                writeCommentCharacter('*');
                writeCommentCharacter(' ');
            }
            else {
                writeCommentCharacter('/');
                writeCommentCharacter('/');
                writeCommentCharacter(' ');
            }
            for (int i=0; i < length ; i++) {
                writeCommentCharacter(aComment.charAt(i));
            }
            if (cStyle) {
                writeCommentCharacter(' ');
                writeCommentCharacter('*');
                writeCommentCharacter('/');
                writeCommentCharacter('\n');
            }
            else {
                writeCommentCharacter('\n');
            }
        }
        else {
            char ch;
            if (cStyle) {
                writeCommentCharacter('/');
                writeCommentCharacter('*');
                writeCommentCharacter('\n');
                writeCommentCharacter(' ');
                writeCommentCharacter('*');
                writeCommentCharacter(' ');
            }
            else {
                writeCommentCharacter('/');
                writeCommentCharacter('/');
                writeCommentCharacter(' ');
            }
            for (int i=0;i < length ; i++) {
                ch = aComment.charAt(i);
                if (ch == '\n') {
                    writeCommentCharacter('\n');
                    if (cStyle) {
                        writeCommentCharacter(' ');
                        writeCommentCharacter('*');
                        writeCommentCharacter(' ');
                    }
                    else {
                        writeCommentCharacter('/');
                        writeCommentCharacter('/');
                        writeCommentCharacter(' ');
                    }
                }
                else {
                    writeCommentCharacter(ch);
                }
            }
            if (cStyle) {
                writeCommentCharacter('\n');
                writeCommentCharacter(' ');
                writeCommentCharacter('*');
                writeCommentCharacter('/');
                writeCommentCharacter('\n');
            }
            else {
                writeCommentCharacter('\n');
            }
        }
    }

    private final void increaseTabLevel ()
    {
        tabLevel++;
    }

    private final void decreaseTabLevel ()
    {
        tabLevel--;
    }

    private final void insertNewLine () throws IOException
    {
        writeCharacter('\n');
        for (int i = 0, c = tabLevel * indentationLength(); i < c ; i++)
            writeCharacter(' ');
    }

    private final void writeCharacter (int c) throws IOException
    {
        super.writeOutput(c);
        if (c == '\n') {
            nextCharIndex=0;
        }
        else {
            nextCharIndex++;
        }
    }

    private final void writeCommentCharacter (int ch) throws IOException
    {
        if (ch >= 0x20 && ch < 0x7f) {
            writeCharacter(ch);
        }
        else {
            switch (ch) {
              case '\n':
              case '\t':
              case '\r':
                writeCharacter(ch);
            }
        }
    }

    private final int serializedStringFitsIn (String str, int maxSize)
    {
        if (str == null || str.length() == 0) {
            return 2;
        }

        int c = str.length();
        if (c > maxSize) {
            /*
                Ok in fact it is even more but it is greater than
                maxSize anyway
            */
            return c;
        }

        int length;
        if (stringRequiresQuotes(str)) {
            length = 2;
        }
        else {
            length = 0;
        }

        for (int i=0 ; i < c ; i++) {
            char ch = str.charAt(i);
            if (ch < 0xff) {
                if (ch >= '#' && ch <= '~' && ch != '\\') {
                    length++;
                }
                else switch (ch) {
                  case ' ':
                  case '!':
                    length++;
                    break;
                  case '"':
                  case '\t':
                  case '\n':
                  case '\r':
                  case '\\':
                    length+=2;
                    break;
                  default:
                    length+=4; /* Octal representation */
                }
            }
            else {
                /* Unicode */
                length += 6;
            }
            if (length > maxSize) {
                return length;
            }
        }
        return length;
    }

    private final int serializedMapFitsIn (Map h, int maxSize)
    {
        int length=2; /* for {
            and
        }
        */
        Iterator e = h.keySet().iterator();
        Object key;
        while (e.hasNext()) {
            key = e.next();
            length++; /* space */

            length += serializedObjectFitsIn(key, maxSize-length);
            if (length > maxSize) {
                return length;
            }

            length += 3; /* space=space */

            length += serializedObjectFitsIn(h.get(key), maxSize-length);
            length++; /* ; */
            if (length > maxSize) {
                return length;
            }
        }
        return length;
    }

    private final int serializedArrayFitsIn (Object a[], int maxSize)
    {
        int length = 3; /* [ space ] */
        for (int i = 0, c = a.length; i < c; i++) {
            length++; /* space */
            length += serializedObjectFitsIn(a[i], maxSize-length);
            if (length > maxSize) {
                return length;
            }
            if (i<(c-1)) { /* , */
                length++;
            }
        }
        return length;
    }

    private final int serializedListFitsIn (List v, int maxSize)
    {
        int length = 3; /* (space) */
        for (int i = 0, c = v.size(); i < c; i++) {
            length++; /* space */
            length += serializedObjectFitsIn(v.get(i), maxSize-length);
            if (length > maxSize) {
                return length;
            }
            if (i<(c-1)) { /* , */
                length++;
            }
        }
        return length;
    }

    private final int serializedNullFitsIn ()
    {
        return 1;
    }

    private final int serializedObjectFitsIn (Object anObject, int maxSize)
    {
        if (anObject instanceof String) {
            return serializedStringFitsIn((String)anObject, maxSize);
        }
        if (anObject instanceof Map) {
            return serializedMapFitsIn((Map)anObject, maxSize);
        }
        if (anObject instanceof Object[]) {
            return serializedArrayFitsIn((Object[])anObject, maxSize);
        }
        if (anObject instanceof List) {
            return serializedListFitsIn((List)anObject, maxSize);
        }
        if (anObject == null) {
            return serializedNullFitsIn();
        }
        return serializedStringFitsIn(anObject.toString(), maxSize);
    }

    private final boolean canFitExpressionOnLine (Object anObject)
    {
        int maxLength = MAX_SIZE_FOR_SMALL_EXPRESSION - nextCharIndex;
        return !(serializedObjectFitsIn(anObject, maxLength) > maxLength);
    }

    private final void formatList (List aList) throws IOException
    {
        int count = aList.size();
        if (canFitExpressionOnLine(aList)) {
            writeCharacter('(');
            for (int i=0 ; i < count ; i++) {
                writeCharacter(' ');
                formatObject(aList.get(i));
                if (i < (count-1)) {
                    writeCharacter(',');
                }
            }
            writeCharacter(' ');
            writeCharacter(')');
        }
        else {
            writeCharacter('(');
            increaseTabLevel();
            for (int i=0 ; i < count ; i++) {
                insertNewLine();
                formatObject(aList.get(i));
                if (i < (count-1)) {
                    writeCharacter(',');
                }
            }
            decreaseTabLevel();
            insertNewLine();
            writeCharacter(')');
        }
    }

    private final void formatArray (Object anArray[]) throws IOException
    {
        int count = anArray.length;
        if (canFitExpressionOnLine(anArray)) {
            writeCharacter('[');
            for (int i=0 ; i < count ; i++) {
                writeCharacter(' ');
                formatObject(anArray[i]);
                if (i < (count-1)) {
                    writeCharacter(',');
                }
            }
            writeCharacter(' ');
            writeCharacter(']');
        }
        else {
            writeCharacter('[');
            increaseTabLevel();
            for (int i=0 ; i < count ; i++) {
                insertNewLine();
                formatObject(anArray[i]);
                if (i < (count-1)) {
                    writeCharacter(',');
                }
            }
            decreaseTabLevel();
            insertNewLine();
            writeCharacter(']');
        }
    }

    private final void formatMap (Map h) throws IOException
    {
        int nonStringKeysCount=0;
        int stringCount=0;
        int count = h.size();
        String[] keys = new String[count];
        Object[] nonStringKeys = new Object[count];
        Iterator e = h.keySet().iterator();
        for (int i = 0; i < count; i++) {
            Object s = e.next();
            if (s instanceof String) {
                keys[stringCount++] = (String)s;
            }
            else {
                nonStringKeys[nonStringKeysCount++] = s;
            }
        }

        if (stringCount > 0) {
            if (!(h instanceof OrderedHashtable)) {
                Sort.objects(keys, null, null, 0, stringCount, StringCompare.self);
            }
            System.arraycopy(keys,
                             0,
                             nonStringKeys,
                             nonStringKeysCount,
                             stringCount);
        }
            // common code moved from then and else to here D107127
        writeCharacter('{');
        if (canFitExpressionOnLine(h)) {
            for (int i=0 ; i < count ; i++) {
                writeCharacter(' ');
                Object key = nonStringKeys[i];
                Object value = h.get(key);
                formatObject(key);
                writeCharacter(' '); writeCharacter('='); writeCharacter(' ');
                formatObject(value);
                writeCharacter(';');
            }
            writeCharacter('}');
        }
        else {
            increaseTabLevel();
            for (int i=0 ; i < count ; i++) {
                Object key = nonStringKeys[i];
                Object value = h.get(key);

                insertNewLine();
                formatObject(key);

                writeCharacter(' '); writeCharacter('='); writeCharacter(' ');

                formatObject(value);
                writeCharacter(';');
            }
            decreaseTabLevel();
            insertNewLine();
            writeCharacter('}');
        }
    }

    private final void formatObject (Object anObject) throws IOException
    {
        if (anObject instanceof String) {
            serializeString((String)anObject);
        }
        else if (anObject instanceof Map) {
            formatMap((Map)anObject);
        }
        else if (anObject instanceof Object[]) {
            formatArray((Object[])anObject);
        }
        else if (anObject instanceof List) {
            formatList((List)anObject);
        }
        else if (anObject instanceof DebugState) {
                // print the debug state if any. However if the debug
                // state is the object itself, just toString it.
            DebugState debugState = (DebugState)anObject;
            Object newObj = debugState.debugState();
            if (debugState == newObj) {
                serializeString(anObject.toString());
            }
            else {
                formatObject(newObj);
            }
        }
        else if (anObject == null) {
            serializeNull();
        }
        else {
            String s = objectToString(anObject);
            serializeString(s);
        }
    }

    private static String objectToString (Object anObject)
    {
        String s;
        try {
            s = anObject.toString();
        }
        catch (Exception e) { // OK
            // if object is marked as "released from the cache" as a result of transactionRollback(),
            // object.toString() asserts with ID2811 
            Object baseIdString = getBaseIdString(anObject);
            String className = anObject.getClass().getName();
            s = Fmt.S("[%s %s]", className, baseIdString);
        }
        return s;
    }

    private static String getBaseIdString (Object anObject)
    {
        String result;
        try {
            Class<? extends Object> clazz = anObject.getClass();
            Method method = clazz.getMethod("getBaseId", (Class<?>[])null);
            Object baseId = method.invoke(anObject, (Object[])null);
            result = (baseId == null) ? "[BaseId=null]" : baseId.toString();
        }
        catch (Throwable t) { // OK
            result = "[BaseId is N/A]";
        }
        return result;
    }

    /** Overridden to produce a formatted serialization of <b>anObject</b>.
      */
    public void writeObject (Object anObject) throws IOException
    {
        formatObject(anObject);
    }

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
            FormattingSerializer serializer = new FormattingSerializer(memory);

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
            memory = null;
            serializer = null;
        }
        return result;
    }

    /**
        Convenience method for generating <b>anObject</b>'s ASCII serialization.
        This version will truncate the serialized object to about the maxLength.
        It will also add a warning message when truncation occurs.
        Returns <b>null</b> on empty input.
    */
    public static String serializeObject (Object anObject, int length)
    {
        String result = null;
        if (anObject == null) {
            result = null;
        }
        else {
            StringWriter memory = new TruncatingStringWriter(length);
            FormattingSerializer serializer = new FormattingSerializer(memory);

            try {
                serializer.writeObject(anObject);
                serializer.flush();
            }
            catch (TruncationException te) {
                // Truncation has occurred due to our size limit
                // The string already contains a warning that this happened.
                // Just ignore and continue.
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
            memory = null;
            serializer = null;
        }
        return result;
    }

    /**
        Convenience method to format any ASCII serialization produced by a
        Serializer. Returns <b>null</b> on error.
    */
    public static String formatString (String input)
    {
        StringReader in = new StringReader(input);
        Object o;

        o = Deserializer.readObject(in);
        if (o != null) {
            StringWriter out = new StringWriter();
            FormattingSerializer serializer = new FormattingSerializer(out);
            try {
                serializer.writeObject(o);
                serializer.flush();
            }
            catch (IOException e) {
                return null;
            }
            return out.toString();
        }
        return null;
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
        FormattingSerializer serializer;

        try {
            serializer = new FormattingSerializer(writer);
            serializer.writeObject(anObject);
            serializer.flush();
        }
        catch (IOException e) {
            return false;
        }
        return true;
    }
}
