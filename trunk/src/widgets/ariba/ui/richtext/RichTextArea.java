/*
    Copyright (c) 1996-2008 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/ui/widgets/ariba/ui/richtext/RichTextArea.java#19 $

    Responsible: kngan

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

    public boolean isStateless()
    {
        return false;
    }

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        _areaName = AWInputId.getAWInputId(requestContext());
        _displayValue = (String)valueForBinding(BindingNames.value);
        if (_displayValue != null) {
            _displayValue = _displayValue.replaceAll("\n", "<br/>");
        }
        super.renderResponse(requestContext, component);
    }

    public void setAreaName (AWEncodedString areaName)
    {
        if (_areaName == null) _areaName = areaName;
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

        if (!safeHTML.equals(value)) {
            recordError(); 
        }

        setValueForBinding(safeHTML, BindingNames.value);
    }

    // For dimensions we need to specify *both* in terms of row/cols, and in terms
    // of style width/height (in "em"s).  The style will take precendent, except
    // when area is sized in a panel, in which case Xinha will use rows/cols as width/height
    // in ems -- so, we need to adjust for the size different between ems and the
    // "average character size" traditionally used by rows/cols.
    
    public int rows ()
    {
        int rows = intValueForBinding("rows");
        return Math.max(rows, 8) * 11 / 7;
    }

    public int cols ()
    {
        int rows = intValueForBinding("cols");
        return Math.max(rows, 69) * 32 / 50;
    }

    public String style ()
    {
        return Fmt.S("width:%sem;height:%sem", cols(), rows());
    }

    private static final Pattern RemoveNewlinePattern =
        Pattern.compile("\r?\n", Pattern.MULTILINE);
    private static final Pattern RemoveUnbalancedPattern =
        Pattern.compile("^ *</[^>]*>", Pattern.MULTILINE);
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
        Pattern.compile("\\s*$", Pattern.MULTILINE);

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

    private static final Pattern RemoveLinkTargetPattern =
        Pattern.compile("<a (.*?) target=\"_blank\"([^>]*)>", Pattern.MULTILINE);
    private static final Pattern InsertLinkTargetPattern =
        Pattern.compile("<a ([^>]*)>", Pattern.MULTILINE);

    private String insertLinkTarget (String string)
    {
        string = RemoveLinkTargetPattern.matcher(string).replaceAll("<a $1$2>");
        return InsertLinkTargetPattern.matcher(string).replaceAll("<a target=\"_blank\" $1>");
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
            localizedJavaString(1, "Content that you pasted into this page contained errors. The invalid text has been removed--check your entries and resubmit."), "");
    }

}

