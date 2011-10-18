package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.MapUtil;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: pdanella
 * Date: May 17, 2011
 * Time: 4:55:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class AWExampleApi extends AWContainerElement implements AWHtmlTemplateParser.FilterBody
{
    AWEncodedString _text;

    public String Name = "name", Component = "component";
    private Map _examples;
    public boolean isInline;
    public String _includeExampleName;

    public void init (String tagName, Map bindingsHashtable)
    {
        _examples = MapUtil.cloneMap(bindingsHashtable);
        super.init();
    }

    //Old AWEx

    public String filterBody(String body)
    {
        _text = AWUtil.escapeHtml(trimLeadingWhitespace(body));
        return body;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        AWResponse r = requestContext.response();

        r.appendContent("<div class='quoteSample'>");
        super.renderResponse(requestContext, component);
        r.appendContent("</div>");

        r.appendContent("<div class='quoteCode'><pre class='prettyprint'><code>");
        r.appendContent(_text);
        r.appendContent("</code></pre></div>");
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

    //Old AWExampleApi

    public String toString(){
        return ("name: " + name());
    }

    private AWBinding getBinding(String key, boolean required){
        AWBinding binding = (AWBinding)_examples.get(key);
        if (required && binding == null) {
            throw new RuntimeException("AWExampleApi missing required binding specification \"" + key + "\"");
        }
        return binding;
    }

    public String name()
    {
        AWBinding binding = getBinding(Name, true);
        return binding.stringValue(null);
    }

    public String componentName ()
    {
       AWBinding binding = getBinding(Component, false);
       return binding == null ? null : binding.stringValue(null);
    }

    public void setIsInline (boolean inline)
    {
        isInline = inline;
    }

    public boolean getIsInline ()
    {
        return isInline;
    }
}