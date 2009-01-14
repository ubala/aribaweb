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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWGenericException.java#8 $
*/

package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.core.AWBindableElement;
import ariba.ui.aribaweb.core.AWIncludeContent;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWComponentReference;
import ariba.ui.aribaweb.core.AWConcreteTemplate;
import ariba.ui.aribaweb.core.AWBaseElement;
import ariba.util.core.ListUtil;
import ariba.util.core.WrapperRuntimeException;
import ariba.util.core.StringUtil;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class AWGenericException extends WrapperRuntimeException
{
    private static HashSet<String> _FilterClasses = new HashSet<String>();
    static{
        _FilterClasses.add("ariba.ui.aribaweb.core.AWConcreteTemplate");
        _FilterClasses.add("ariba.ui.aribaweb.core.AWGenericContainer");
        _FilterClasses.add("ariba.ui.aribaweb.core.AWContainerElement");
        _FilterClasses.add("ariba.ui.aribaweb.core.AWIf$AWIfBlock");

        _FilterClasses.add("ariba.util.fieldvalue.ReflectionMethodGetter");
        _FilterClasses.add("ariba.util.fieldvalue.FieldValue_Object");
        _FilterClasses.add("sun.reflect.NativeMethodAccessorImpl");
        _FilterClasses.add("sun.reflect.DelegatingMethodAccessorImpl");
    }
    private String _additionalMessage;
    private List<AWBaseElement> _componentStack = ListUtil.list();

    public AWGenericException (String exceptionMessage)
    {
        super(exceptionMessage);
        _additionalMessage = null;
    }

    public AWGenericException (Exception exception)
    {
        super(exception);
        _additionalMessage = null;
    }

    public AWGenericException (String message, Exception exception)
    {
        super(exception);
        _additionalMessage = message;
    }

    public static AWGenericException augmentedExceptionWithMessage (String message, Exception exception)
    {
        if (exception instanceof AWGenericException) {
            ((AWGenericException)exception).addMessage(message);
            return ((AWGenericException)exception);
        }
        return new AWGenericException(message, exception);
    }
    
    public String additionalMessage ()
    {
        return _additionalMessage;
    }

    public void addMessage (String message)
    {
        if (_additionalMessage != null) {
            message = StringUtil.strcat(_additionalMessage, "\n",message);
        }
        _additionalMessage = message;
    }

    // Must be called for all traversed IncludeContent and ComponentReference elements
    public void addReferenceElement(AWBaseElement comRef)
    {
        _componentStack.add(comRef);
    }

    public void printStackTrace (PrintStream printStream)
    {
        printStackTrace(new PrintWriter(new OutputStreamWriter(printStream)), true);
    }

    public void printStackTrace (PrintWriter printWriter)
    {
        printStackTrace(printWriter, true);
    }

    public void printStackTrace (PrintWriter printWriter, boolean smart)
    {
        if (!smart || _componentStack == null) {
            if (_additionalMessage != null) {
                printWriter.println(_additionalMessage);
            }
            super.printStackTrace(printWriter);
            return;
        }

        Throwable curr = getCause() != null ? getCause() : this;

        printWriter.println(curr);

        if (_additionalMessage != null) {
            printWriter.println(_additionalMessage);
        }
        printWriter.println();
        
        StackTraceElement[] trace = curr.getStackTrace();
        int pos = 0, ilimit = trace.length - 3;
        // We have componentStack entries of templates for ComponentReference and IncludeContent elements
        for (int i = 0; i < trace.length; i++) {
            if (pos < _componentStack.size()
                && (i < ilimit)
                && trace[i + 2].getClassName().equals(AWComponentReference.class.getName()))
            {
                // Suppress the ConcreteTemplate frame (if any)
                if (!trace[i].getClassName().equals(AWConcreteTemplate.class.getName()))
                {
                    printWriter.println("\tat " + trace[i]);
                }
                AWBaseElement cref = _componentStack.get(pos++);
                StackTraceElement compElement = trace[i + 1];
                String tName = cref.templateName();
                tName = (tName == null) ? "null" :
                    tName.substring(tName.lastIndexOf('/') + 1);

                printWriter.print("\tat " + compElement.getClassName());
                if (compElement.getClassName().equals(AWComponent.class.getName())
                        && (cref instanceof AWBindableElement))
                {
                    printWriter.print("(" + ((AWBindableElement)cref).tagName() + ")");
                }
                printWriter.println("." + compElement.getMethodName() + "("
                    + compElement.getFileName() + ":" + compElement.getLineNumber()
                    + ")<" + tName + ":" + cref.lineNumber() + ">");

                i += 2;
            }
            else if (pos < _componentStack.size()
                && trace[i].getClassName().equals(AWIncludeContent.class.getName()))
            {
                AWBaseElement cref = _componentStack.get(pos++);
                String tName = cref.templateName();
                tName = (tName == null) ? "null"
                    : tName.substring(tName.lastIndexOf('/') + 1);

                printWriter.println("\tat " + trace[i] + "<" + tName + ":"
                    + cref.lineNumber() + ">");
            }
            else if (!_FilterClasses.contains(trace[i].getClassName())) {
                printWriter.println("\tat " + trace[i]);
            }
        }

        printCause(printWriter, curr);
    }
    
    private void printCause (PrintWriter printWriter, Throwable curr)
    {
        if (curr.getCause() != null) {
            StackTraceElement[] currTrace = curr.getStackTrace();
            StackTraceElement[] causedTrace = curr.getCause().getStackTrace();
            int m = currTrace.length - 1, n = causedTrace.length - 1;
            while (m >= 0 && n >= 0 && currTrace[m].equals(causedTrace[n])) {
                m--; n--;
            }

            int framesInCommon = causedTrace.length - 1 - n;

            printWriter.println("\nCaused by: " + curr.getCause());
            for (int i = 0; i <= m; i++)
                printWriter.println("\tat " + causedTrace[i]);
            if (framesInCommon != 0) {
                printWriter.println("\t...  more");
            }

            printCause(printWriter, curr.getCause());
        }
    }

    public static class ParsedException
    {
        public String title;
        public String additionalMessage;
        public List <FrameInfo>frames;
    }

    public static class FrameInfo
    {
        public String method, file, template;
    }

    private static final Pattern _FramePattern
            = Pattern.compile("^\\s*at\\s+([\\w\\.\\$\\(\\)]+)\\s*\\(([\\w\\.\\:]+)\\)(?:\\<([\\w\\.\\:]+)\\>)?");
    private static final Pattern _PackageClassMethodPattern
            = Pattern.compile("(.+\\.)([\\w\\$\\(\\)]+\\.\\w+)");

    public static ParsedException parseException (Throwable e)
    {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String string = stringWriter.toString();
        Scanner scanner = new Scanner(string);
        ParsedException parse = new ParsedException();
        parse.frames = new ArrayList();

        parse.title = scanner.nextLine();
        StringBuffer addMsgBuf = new StringBuffer();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Matcher m = _FramePattern.matcher(line);
            if (m.matches()) {
                FrameInfo fi = new FrameInfo();
                fi.method = m.group(1);
                fi.file = m.group(2);
                fi.template = m.group(3);
                
                // see if we can move the package to the file
                m = _PackageClassMethodPattern.matcher(fi.method);
                if (m.matches()) {
                    fi.file = m.group(1).concat(fi.file);
                    fi.method = m.group(2);
                }

                parse.frames.add(fi);
            } else {
                addMsgBuf.append(line);
                addMsgBuf.append('\n');
            }
        }
        parse.additionalMessage = addMsgBuf.toString();

        return parse;
    }
}
