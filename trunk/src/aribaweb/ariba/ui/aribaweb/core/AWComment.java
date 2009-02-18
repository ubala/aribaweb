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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWComment.java#2 $
*/
package ariba.ui.aribaweb.core;

/**
    Is an XML / HTML comment. <p/>
    That is, whatever is inside the comment is bracketed by
        <code>&lt;!--</code>
    and
        <code>--></code>
    and rendered into the output. The comment body is not interpreted, so it's safe
    to put all sorts of markup in there. It is not safe to place open or close comment
    markers (i.e. <code>&lt;!--</code> or <code>--></code>) in the body, however.
 
    @aribaapi ariba
*/
public class AWComment extends AWContainerElement implements AWHtmlTemplateParser.LiteralBody
{
    public static final String Open = "<!--";
    public static final String Close = "-->";

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        AWResponse response = requestContext.response();
        response.appendContent(Open);
        super.renderResponse(requestContext, component);
        response.appendContent(Close);
    }
}

