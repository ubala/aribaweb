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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/AWXXMLComponent.java#2 $
*/
package ariba.ui.demoshell;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWTemplateParser;
import ariba.ui.aribaweb.core.AWHtmlTemplateParser;
import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.ui.aribaweb.core.AWApplication;
import java.util.Map;

public class AWXXMLComponent extends AWComponent
{
    private static AWTemplateParser _DefaultTemplateParser;
    private boolean _templateParserSet = false;

    public AWTemplateParser templateParser ()
    {
        if (_templateParserSet) {
            return super.templateParser();
        }
        else {
            return defaultTemplateParser();
        }
    }

    public void setTemplateParser (AWTemplateParser templateParser)
    {
        _templateParserSet = (templateParser != null);
        super.setTemplateParser(templateParser);
    }

    public static AWTemplateParser defaultTemplateParser ()
    {
        if (_DefaultTemplateParser == null) {
            _DefaultTemplateParser = new TemplateParser();
            _DefaultTemplateParser.init((AWApplication)AWConcreteServerApplication.sharedInstance());
        }
        return _DefaultTemplateParser;
    }

    public static void setDefaultTemplateParser (AWTemplateParser templateParser)
    {
        _DefaultTemplateParser = templateParser;
    }

    public boolean shouldCloseElements ()
    {
        return true;
    }

    public boolean useXmlEscaping ()
    {
        return true;
    }

    /**

        An extension of the <code>AWHtmlTemplateParser</code>, that would
        treat every tag as a <code>AWGenericElement/Container</code>,
        instead of a <code>AWBareString</code>.
        @aribaapi private
    */
    public static class TemplateParser
        extends AWHtmlTemplateParser
    {
        private static final String _QuestionMark = "?";

        /**
            Returns the element class for a given openTagName.

            Overrides the default behavior in AWHtmlTemplateParser,
            which is to create AWBaseStrings for any tags that don't
            coorespond to a template.  That is an optimization which
            does not work in the XML decoding space.

            @aribaapi private
        */
        protected Class elementClassForNameAndAttributes (
            String openTagName,
            Map tagAttributes,
            boolean isElementTag)
        {
                // Forces all tags to become AWGenericElement/AWGenericComponent
            Class elementClass = super.elementClassForNameAndAttributes(
                                    openTagName,
                                    tagAttributes,
                                    isElementTag);
            if (elementClass == null) {
                    // Escape <? tags, as they are not balanced
                if (!openTagName.startsWith(_QuestionMark) && !openTagName.startsWith("!")) {
                    elementClass = isElementTag ? AWGenericElementClass
                                                : AWGenericContainerClass;
                }
            }

            return elementClass;
        }
    }
}
