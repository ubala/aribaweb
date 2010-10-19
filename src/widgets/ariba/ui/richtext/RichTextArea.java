/*
    Copyright 1996-2010 Ariba, Inc.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/richtext/RichTextArea.java#26 $
*/

package ariba.ui.richtext;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWInputId;
import ariba.ui.aribaweb.core.AWErrorManager;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.html.BindingNames;
import ariba.util.core.HTML;
import ariba.util.core.Fmt;
import java.util.regex.Pattern;

public class RichTextArea extends AWComponent
{
    public static final String ClassName = RichTextArea.class.getName();

    private static final String[] SupportedBindingNames = {
         BindingNames.value,
         BindingNames.emptyStringValue,
         BindingNames.errorKey,
         BindingNames.editable,
         "rows",
         "cols",
         "readonly"
    };

    public AWEncodedString _areaName;
    private Object _errorKey;
    // display value
    private String _displayValue;

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    public boolean isStateless ()
    {
        return false;
    }

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        _areaName = AWInputId.getAWInputId(requestContext());
        _displayValue = (String)valueForBinding(BindingNames.value);
        if (_displayValue != null) {
            /* dfinlay 3.6.09: we always filter before rendering even though we we filter
               on setting (see setDisplayValue) becuase the text might be coming from
               another input field that didn't filter on entry 
               (as most of our fields don't) */
            _displayValue = HTML.filterUnsafeHTML(_displayValue);
            _displayValue = _displayValue.replaceAll("\n", "<br/>");
        }
        super.renderResponse(requestContext, component);
    }

    public void setAreaName (AWEncodedString areaName)
    {
        if (_areaName == null) {
            _areaName = areaName;
        }
    }

    private Object errorKey ()
    {
        if (_errorKey == null) {
            _errorKey = AWErrorManager.getErrorKeyForComponent(this);
        }
        if (_errorKey == null) {
            _errorKey = _areaName;
        }

        return _errorKey;
    }

    public String displayValue ()
    {
        return _displayValue;
    }

    public void setDisplayValue (String displayValue)
    {
        String value = convertWhitespaces(displayValue);
        value = insertLinkTarget(value);

        String safeHTML = HTML.filterUnsafeHTML(value);
        safeHTML = convertEmptyPTags(safeHTML);
        safeHTML = HTML.filterMargins(safeHTML);

        if (value != null && !safeHTML.equals(value.trim())) {
            // this results in an error message that (should)
            // be displayed to the user and the submission
            // will fail
            recordError();
        }
        // We don't want to edit the HTML further here because we want
        // our changes to result in a notification to the end-user so that
        // they can verify that everything still appears correctly.

        setValueForBinding(safeHTML, BindingNames.value);
    }

    // For dimensions we need to specify *both* in terms of row/cols, and in terms
    // of style width/height (in "em"s).  The style will take precendent, except
    // when area is sized in a panel, in which case Xinha will use rows/cols as width/height
    // in ems -- so, we need to adjust for the size different between ems and the
    // "average character size" traditionally used by rows/cols.

    /**
     * In EM.
     * @return
     */
    public int rows ()
    {
        int rows = intValueForBinding("rows");
        double emsPerRow = 11.0 / 7.0;
        int maxRows = 8;
        double ems = Math.max(rows, maxRows) * emsPerRow;
        return (int)ems;
    }

    /**
     * In EM.
     * @return
     */
    public int cols ()
    {
        int columns = intValueForBinding("cols");
        double emsPerColumn = 32.0 / 50.0;
        int maxColumns = 69;
        double ems = Math.max(columns, maxColumns) * emsPerColumn;
        return (int)ems;
    }

    public String style ()
    {
        return Fmt.S("width:%sem;height:%sem", cols(), rows());
    }

    private static final Pattern RemoveNewlinePattern =
        Pattern.compile("\r?\n", Pattern.MULTILINE);
    private static final Pattern RemoveUnbalancedPattern =
        Pattern.compile("^ *</[^>]*>");
    private static final Pattern RemoveMouseAttrPattern =
        Pattern.compile(" awmouse(up|down)=\"true\"", Pattern.MULTILINE);
    private static final Pattern RemoveLeadingLISpacePattern =
        Pattern.compile(" *<li>", Pattern.MULTILINE);
    private static final Pattern RemovePTagPattern =
        Pattern.compile("<p>(.*?)</p>", Pattern.MULTILINE);
    private static final Pattern ConvertNBSPPattern =
        Pattern.compile("&nbsp;", Pattern.MULTILINE);
    private static final Pattern ConvertBRPattern =
        Pattern.compile("<br ?/>", Pattern.MULTILINE);
    private static final Pattern RemoveTralingSpacesPattern =
        Pattern.compile("\\s*$");

    private String convertWhitespaces (String string)
    {
        string = RemoveNewlinePattern.matcher(string).replaceAll("");
        // 1-79ISX1 - remove leading unbalanced closing tags
        string = RemoveUnbalancedPattern.matcher(string).replaceAll("");
        // remove script added attributes by Firefox
        string = RemoveMouseAttrPattern.matcher(string).replaceAll("");
        string = RemoveLeadingLISpacePattern.matcher(string).replaceAll("<li>");
        string = RemovePTagPattern.matcher(string).replaceAll("$1\n");
        string = ConvertNBSPPattern.matcher(string).replaceAll(" ");
        string = ConvertBRPattern.matcher(string).replaceAll("\n");
        string = RemoveTralingSpacesPattern.matcher(string).replaceAll("");
        return string;
    }

    private static final Pattern ConvertEmptyPTagPattern =
        Pattern.compile("<p[^>]*>\\s*</p>", Pattern.MULTILINE);

    /**
     * This converts empty p tags (even with attributes) to \ns.
     * We have this function because when you paste into the Xinha RTE from
     * MSWord the blank lines will be represented by empty paragraphs.
     * In our CSS we set the p.margin to 0 so empty paragraphs produce no
     * whitespace.  This function will convert these empty paragraphs to
     * end of line characters so that they will be displayed to the user.
     * @param input
     * @return
     */
    private String convertEmptyPTags (String input)
    {
        return ConvertEmptyPTagPattern.matcher(input).replaceAll("\n");
    }

    private static final Pattern RemoveLinkTargetPattern =
        Pattern.compile("<a (.*?) target=\"_blank\"([^>]*)>", Pattern.MULTILINE);
    private static final Pattern InsertLinkTargetPattern =
        Pattern.compile("<a ([^>]*)>", Pattern.MULTILINE);

    private String insertLinkTarget (String string)
    {
        string = RemoveLinkTargetPattern.matcher(string).replaceAll("<a $1$2>");
        return InsertLinkTargetPattern.matcher(string).replaceAll(
            "<a target=\"_blank\" $1>");
    }

    public static String convertToPlainText (String richText)
    {
        if (richText == null) {
            return null;
        }
        return HTML.convertToPlainText(richText);
    }

    private void recordError ()
    {
        recordValidationError(errorKey(),
            localizedJavaString(1,
                "Content that you pasted into this page contained errors. " +
                "The invalid text has been removed" +
                "--check your entries and resubmit."), "");
    }

}

