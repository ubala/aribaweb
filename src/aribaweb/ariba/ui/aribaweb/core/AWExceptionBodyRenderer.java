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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWExceptionBodyRenderer.java#6 $
*/
package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWGenericException;
import ariba.util.core.SystemUtil;
import ariba.util.core.StringUtil;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class AWExceptionBodyRenderer extends AWComponent
{
    private Throwable _exception;
    public String _stackTrace;
    public AWGenericException.ParsedException _parsedException;
    public AWGenericException.FrameInfo _currentFrameInfo;
    public String _exType, _exTitle;

    static Pattern _ExceptionTypePattern = Pattern.compile("^([\\w\\.]+)\\:\\s*(.*)");
    static Pattern _ParenthesizedPattern = Pattern.compile("\\((.+?)\\)");

    public void renderResponse(AWRequestContext requestContext, AWComponent component) {
        _exception = (Throwable)valueForBinding(AWBindingNames.exception);
        _stackTrace = (_exception != null) ? SystemUtil.stackTrace(_exception) : "No stack trace available.";
        _parsedException = AWGenericException.parseException(_exception);

        _exTitle = _parsedException.title;
        Matcher m = _ExceptionTypePattern.matcher(_exTitle);
        if (m.matches()) {
            _exType = m.group(1);
            _exTitle = m.group(2);
        }
        super.renderResponse(requestContext, component);
    }

    public String additionalMessage ()
    {
        String msg = null;
        if (_exception instanceof AWGenericException) {
            msg = ((AWGenericException)_exception).additionalMessage();
            if (msg != null) {
                msg = msg.replace("\n", "<br/>");
                msg = _ParenthesizedPattern.matcher(msg).replaceAll("<span class=\"paren\">($1)</span>");
            }
        }
        return msg;
    }

    public void sleep ()
    {
        _exception = null;
        _parsedException = null;
        _stackTrace = null;
        _currentFrameInfo = null;
        _exType = null;
        _exTitle = null;
    }

    public String wrappedMethodString ()
    {
        return StringUtil.wrap(_currentFrameInfo.method, "<br/>", 60);
    }
}
