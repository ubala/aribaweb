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

    $Id: //ariba/platform/util/core/ariba/util/io/IndentWriter.java#4 $
*/

package ariba.util.io;

import ariba.util.core.Fmt;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
    This class tracks an indent level to prepend output lines

    @aribaapi private
*/
public class IndentWriter extends PrintWriter
{
    public static String [] indents;
    private static final int indentsStored = 20;
    static {
        indents = new String[indentsStored];
        indents[0] = "";
        for (int i=1; i<indentsStored; i++) {
            indents[i] = Fmt.S("%s    ", indents[i-1]);
        }
    }
    /**
        Each level adds an additional 4 spaces to the indent.
    */
    public static final String indentForLevel (int level)
    {
        if (level<indentsStored) {
            return indents[level];
        }
        int arraySize = level*4;
        char[] indent = new char[arraySize];
        for (int i=0; i<arraySize; i++) {
            indent[i] = ' ';
        }
        return new String(indent);
    }
    /**
        legacy use by other classes. doesn't track ident of IndentWriter
    */
    public static final String Indent = "    ";

    private IndentFilter indentFilter;

    /**
        Create an IndentWriter around a Writer such as a
        BufferedWriter or StringWriter
    */
    public IndentWriter (Writer writer)
    {
        this(new IndentFilter(writer), false);
    }

    public IndentWriter (Writer writer, boolean autoFlush)
    {
        this(new IndentFilter(writer), autoFlush);
    }

    private IndentWriter (IndentFilter indentFilter, boolean autoFlush)
    {
        super(indentFilter, autoFlush);
        this.indentFilter = indentFilter;
    }


    public void indent ()
    {
        flush();
        indentFilter.indent();
    }

    public void outdent ()
    {
        flush();
        indentFilter.outdent();
    }

    public int getIndent ()
    {
        return indentFilter.getIndent();
    }

    public int indentLevel ()
    {
        return indentFilter.indentLevel();
    }

    public void setIndentLevel (int n)
    {
        indentFilter.setIndentLevel(n);
    }

}

class IndentFilter extends FilterWriter
{
    /**
        Use 4 space indent by default
    */
    private static final int DefaultIndent = 4;

    /**
        Whether the next output should be preceded by an indent
    */
    private boolean indentPending;

    /**
        The level of nesting
    */
    private int indent;

    /**
        The indent size in spaces
    */
    private int indentLevel;

    /**
        The characters to output for indenting
    */
    private char[] indentChars;

    private Writer writer;

    IndentFilter (Writer writer)
    {
        super(writer);
        this.writer = writer;
        setIndentLevel(DefaultIndent);
    }

    public void indent ()
    {
        indent++;
    }

    public void outdent ()
    {
        indent--;
    }

    public int getIndent ()
    {
        return indent;
    }

    public int indentLevel ()
    {
        return indentLevel;
    }

    public void setIndentLevel (int n)
    {
        indentLevel = n;
        indentChars = new char[indentLevel];
        for (int i = 0; i < indentLevel; i++) {
            indentChars[i] = ' ';
        }
    }

    /**
        When writing a newline prepend it with the indent
    */
    public void write (int c) throws IOException
    {
        writeIndent();
        super.write(c);
        if (c == '\n') {
            indentPending = true;
        }
    }

    /**
        Turn char[] writes into char writes. Slower but it simplier
    */
    public void write (char c[], int off, int len) throws IOException
    {
        int stop = off + len;
        for (int i = off; i < stop; i++) {
            write(c[i]);
        }
    }

    /**
        Turn String writes into char writes. Slower but it simplier
    */
    public void write (String s, int off, int len) throws IOException
    {
        int stop = off + len;
        for (int i = off; i < stop; i++) {
            write(s.charAt(i));
        }
    }

    private void writeIndent () throws IOException
    {
        if (!indentPending) {
            return;
        }

        for (int i = 0; i < indent; i++) {
            super.write(indentChars, 0, indentChars.length);
        }
        indentPending = false;
    }

}
