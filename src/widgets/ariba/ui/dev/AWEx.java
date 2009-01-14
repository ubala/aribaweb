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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWEx.java#3 $
*/
package ariba.ui.dev;

import ariba.ui.aribaweb.core.AWHtmlTemplateParser;
import ariba.ui.aribaweb.core.AWContainerElement;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWEncodedString;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class AWEx extends AWContainerElement
        implements AWHtmlTemplateParser.FilterBody
{
    AWEncodedString _text;

    public String filterBody(String body)
    {
        _text = AWUtil.escapeHtml(trimLeadingWhitespace(body));
        return body;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        AWResponse r = requestContext.response();
        r.appendContent("<div class='quoteCode'><pre class='prettyprint'><code>");
        r.appendContent(_text);
        r.appendContent("</code></pre></div>");
        r.appendContent("<div class='quoteSample'>");
        super.renderResponse(requestContext, component);
        r.appendContent("</div>");
    }

    static final Pattern _LeadingWS = Pattern.compile("(?m)^([^\\n\\S]*)\\S");

    static String trimLeadingWhitespace (String content)
    {
        Matcher m = _LeadingWS.matcher(content);
        if (m.find()) {
            String ws = m.group(1);
            content = content.replaceAll("(?m)^" + ws, "");
        }
        return content;
    }
}