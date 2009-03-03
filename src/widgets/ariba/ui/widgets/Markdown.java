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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/Markdown.java#6 $
*/
package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWHtmlTemplateParser;
import ariba.ui.aribaweb.core.AWContainerElement;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.Fmt;
import com.petebevin.markdown.MarkdownProcessor;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;

/**
    Support for the Markdown text->html format.
        See http://daringfireball.net/projects/markdown/ for info on the format, and
        http://sourceforge.net/projects/markdownj/ for info on the java library we're
        wrapping here.

    This provides an AW tag that can be use as either:
        {@code
        <w:Markdown>
            some text
        </w:Markdown>
        }
    Or:
        {@code
        <w:Markdown value="$someText"/>
        }
    Our filtering of markdown is enhanced in the following ways:
        -  leading indentation is stripped
             (i.e. the indentation level of the first non-whitespace line is used
              to determine the indentation level, and all lines are de-indented by
              that amount).

        - Code blocks are prettified
            (i.e. wrapped with {@code <div class='quoteCode'><pre class='prettyprint'> })

        - Convenient inner-document linking to headers
            - All headers are given an id matching their contents (with ws converted to _)
            - The format [Heading Text]# creates a hyperlink linking to that heading
                (i.e. it is converted to [Heading Text](#Heading_Text], which is then
                 converted to {@code <a href="#Heading_Text">Heading Text</a>}
 */
public class Markdown extends AWContainerElement
        implements AWHtmlTemplateParser.FilterBody, AWHtmlTemplateParser.SupressesEmbeddedKeyPaths
{
    AWBinding _value;

    public static String translateMarkdown (String text)
    {
        MarkdownProcessor markdown = new MarkdownProcessor();
        String value = trimLeadingWhitespace(text);
        value = preprocessLocalLinks(value);

        value = markdown.markdown(value);

        value = tweakCodeBlocks(value);
        value = idHeadings(value);

        return value;
    }

    public void init (String tagName, Map bindingsHashtable)
    {
        _value = (AWBinding)bindingsHashtable.remove(AWBindingNames.value);
        super.init(tagName, bindingsHashtable);
    }

    public String filterBody(String body)
    {
        return translateMarkdown(body);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        super.renderResponse(requestContext, component);
        if (_value != null) {
            String value = _value.stringValue(component);
            if (value != null) {
                requestContext.response().appendContent(translateMarkdown(value));
            }
        }
    }

    static final Pattern _LeadingWS = Pattern.compile("(?m)^([^\\n\\S]*)\\S");
    static final Pattern _CodeBlocks = Pattern.compile("(?s)\\<pre\\>\\<code\\>(.+?)\\<\\/code\\>\\<\\/pre\\>");
    static final Pattern _Headings = Pattern.compile("\\<(h\\d)\\>(.+?)\\<\\/h\\d\\>");
    static final Pattern _LocalAnchors = Pattern.compile("\\[(.+?)\\]\\#(\\w+)?");

    static String trimLeadingWhitespace (String content)
    {
        Matcher m = _LeadingWS.matcher(content);
        if (m.find()) {
            String ws = m.group(1);
            content = content.replaceAll("(?m)^" + ws, "");
        }
        return content;
    }

    // Transform [A ref]# to [A ref](#A_Ref)
    static String preprocessLocalLinks (String content)
    {
        Matcher m = _LocalAnchors.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String title = m.group(1);
            String id = title.replaceAll("[^\\w]+", "_").toLowerCase();
            String replacement = (m.group(2) != null)
                            ? Fmt.S("[%s](/%s/%s)", title, m.group(2), title)
                            : Fmt.S("[%s](#%s)", title, id);
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static String tweakCodeBlocks (String content)
    {
        Matcher m = _CodeBlocks.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String codeBody = trimLeadingWhitespace(m.group(1)).replace("$", "\\$");
            String replacement = Fmt.S("<div class='quoteCode'><pre class='prettyprint'><code>%s</pre></code></div>",
                                    codeBody);
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static String idHeadings (String content)
    {
        Matcher m = _Headings.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String tag = m.group(1);
            String title = m.group(2);
            String id = title.replaceAll("[^\\w]+", "_").toLowerCase();
            String replacement = Fmt.S("<%s id=\"%s\">%s</%s>",
                                    tag, id, title, tag);
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
