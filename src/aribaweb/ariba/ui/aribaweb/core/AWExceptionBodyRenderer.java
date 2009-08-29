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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWExceptionBodyRenderer.java#10 $
*/
package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWGenericException.ParsedException;
import ariba.util.core.SystemUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.ListUtil;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;


public class AWExceptionBodyRenderer extends AWComponent
{
    public Throwable _exception;
    public String _stackTrace;
    public List<AWGenericException.ParsedException> _parseList;
    ParsedException _parsedException;
    public AWGenericException.FrameInfo _currentFrameInfo;
    public int _frameIndex;
    public String _exType, _exTitle;
    String _lastMessage, _lastTitle;
    private static String _testExceptionInfoComponent;
    static Pattern _ExceptionTypePattern = Pattern.compile("^([\\w\\.]+)\\:\\s*(.*)");
    static Pattern _ParenthesizedPattern = Pattern.compile("\\((.+?)\\)");
    static Pattern _HeadingPattern = Pattern.compile("(?m)^-- ([\\w\\s]+\\:)");

    public void sleep ()
    {
        _exception = null;
        _stackTrace = null;
        _parseList = null;
        _parsedException = null;
        _currentFrameInfo = null;
        _exType = null;
        _exTitle = null;
        _lastMessage = null;
        _lastTitle = null;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component) {
        _exception = (Throwable)valueForBinding(AWBindingNames.exception);
        _stackTrace = (_exception != null) ? SystemUtil.stackTrace(_exception) : "No stack trace available.";
        _parseList = AWGenericException.parseException(_exception);

        setParsedException(ListUtil.lastElement(_parseList));
        String title = ListUtil.firstElement(_parseList).title;
        setTitle(title);

        super.renderResponse(requestContext, component);
    }

    public ParsedException parsedException()
    {
        return _parsedException;
    }

    public void setParsedException(ParsedException parsedException)
    {
        if (_parsedException != null) {
            _lastMessage = _parsedException.additionalMessage;
            _lastTitle = _parsedException.title;
        }

        _parsedException = parsedException;

        setTitle(_parsedException.title);
    }

    private void setTitle (String title)
    {
        _exTitle = title;
        Matcher m = _ExceptionTypePattern.matcher(_exTitle);
        if (m.matches()) {
            _exType = m.group(1);
            _exTitle = m.group(2);
        }
    }

    static boolean areEqual (String a, String b)
    {
        a = StringUtil.nullOrEmptyOrBlankString(a) ? "" : a;
        b = StringUtil.nullOrEmptyOrBlankString(b) ? "" : b;
        return a.equals(b);
    }

    public boolean showCurrentTitle ()
    {
        return (_parsedException != ListUtil.firstElement(_parseList))
            && !areEqual(_parsedException.title, _lastTitle);
    }

    public boolean showCurrentMessage ()
    {
        return (_parsedException != ListUtil.lastElement(_parseList))
                && !areEqual(_parsedException.additionalMessage, _lastMessage)
                && !StringUtil.nullOrEmptyOrBlankString(_parsedException.additionalMessage);
    }

    public String additionalMessage ()
    {
        String msg = _parsedException.additionalMessage;
        if (msg != null) {
            msg = _HeadingPattern.matcher(msg).replaceAll("<b><u>$1</u></b>");
            msg = AWUtil.replaceLeadingSpacesWithNbsps(msg);
            msg = msg.replace("\n", "<br/>");
            msg = _ParenthesizedPattern.matcher(msg).replaceAll("<span class=\"paren\">(<span class=\"ro\" bh=\"_RO\">$1</span>)</span>");
        }
        return msg;
    }

    // We suppress the innards of reflection and groovy dispatch, since these are
    // usually noise
    public boolean shouldDisplayCurrentFrame ()
    {
        String filename = _currentFrameInfo.file;
        return _frameIndex == 0 ||
               !(   filename.startsWith("org.codehaus.groovy.runtime.")
                 || filename.startsWith("org.codehaus.groovy.reflection.")
                 || filename.startsWith("groovy.lang.Meta")
                 || filename.startsWith("ariba.util.fieldvalue.Reflection")
                 || filename.startsWith("sun.reflect.")
                 || filename.startsWith("java.lang.reflect.")
               );
    }

    public String wrappedMethodString ()
    {
        return StringUtil.wrap(_currentFrameInfo.method, "<br/>", 60);
    }

    // a hidden component from which the test framework can extract the exception information
    public static void registerTestExceptionInfoComponent (String componentName)
    {
        _testExceptionInfoComponent = componentName;
    }

    public boolean hasTestExceptionInfoComponent ()
    {
        return _testExceptionInfoComponent != null;
    }

    public String testExceptionInfoComponent ()
    {
        return _testExceptionInfoComponent;
    }
}
