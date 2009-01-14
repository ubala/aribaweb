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

    $Id: //ariba/platform/util/core/ariba/util/io/HTMLTemplateParser.java#6 $
*/

package ariba.util.io;

import ariba.util.core.Fmt;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import java.io.PrintWriter;

/**
    An HTMLTemplateParser is a subclass of TemplateParser which processes
    HTML template files.

    @see TemplateParser

    @aribaapi private
*/
public class HTMLTemplateParser extends TemplateParser
{

    private static final String StringTable = "ariba.util.core";
    /*-----------------------------------------------------------------------
        Private Constants
      -----------------------------------------------------------------------*/

        // resource tags for localized strings
    private static final String TemplateParserPageTitleKey =
        "TemplateParserPageTitle";
    private static final String TemplateParserTitleKey     =
        "TemplateParserTitle";

    /*-----------------------------------------------------------------------
        Constructors
      -----------------------------------------------------------------------*/

    public HTMLTemplateParser (TagHandler handler)
    {
        super(handler);
    }


    /*-----------------------------------------------------------------------
        Protected Methods
      -----------------------------------------------------------------------*/

    protected void signalError (PrintWriter out, String message)
    {
        out.println("<HTML>");
        out.println("<HEAD>");
        out.print("<META HTTP-EQUIV=\"Content-Type\" ");
        out.println("CONTENT=" + MIME.ContentTypeTextHTML +
                    "; CHARSET=" + MIME.CharSetUTF);
        out.println("<TITLE>");
        out.println(ResourceService.getString(StringTable, TemplateParserPageTitleKey));
        out.println("</TITLE></HEAD>");

        out.print("<BODY>\n<H2>");
        out.print(ResourceService.getString(StringTable, TemplateParserTitleKey));
        out.print("</H2>");
        Fmt.F(out,"<PRE>%s</PRE><BR>\n</BODY>\n", message);

        out.println("</HTML>");
    }
}
