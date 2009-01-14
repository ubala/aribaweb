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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWDropContainer.java#10 $
*/
package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.util.core.StringUtil;

import java.util.List;

public final class AWDropContainer extends AWComponent
{
    public static final String DropActionBinding  = "dropAction";
    public static final String DisableDropBinding  = "disableDrop";
    private static final String TypesBinding      = "types";
    private static final String DropClassBinding  = "dropClass";

    private static final String[] SupportedBindingNames = {
        AWBindingNames.tagName, //AWBindingNames.width, AWBindingNames.height,
        AWBindingNames.classBinding, AWBindingNames.style,
        DropClassBinding, DropActionBinding, DisableDropBinding,
        AWBindingNames.type, TypesBinding,
        AWBindingNames.omitTags,
    };

    private static final String DropPrefix=" awDrp_";
    private static final String DropStylePrefix=" awds_";

    private static final String TypesDelimiter =",";

    public AWEncodedString _elementId;

    ///////////////////
    // Bindings Support
    ///////////////////
    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    private static final String DebugStyle="border:1px red solid;";
    public String style ()
    {
        String style = stringValueForBinding(AWBindingNames.style);
        AWSession session = requestContext().session(false);
        if (session != null) {
            Boolean flag = (Boolean)session.dict().get(AWConstants.DropDebugEnabled);
            if (flag != null && flag.booleanValue()) {
                style = (style!=null) ? StringUtil.strcat(style,";",DebugStyle) : DebugStyle;
            }
        }
        return style;
    }

    public String cssClass ()
    {
        String cssClass = stringValueForBinding(AWBindingNames.classBinding);
        String type = stringValueForBinding(AWBindingNames.type);
        List types = (List)valueForBinding(TypesBinding);
        String dropStyle = null;
        if (hasBinding(DropClassBinding)) {
            // if there's a dropClass binding, then take the value, prepend with
            // awds_ and add to the types list to get added to the final class
            // string.  During runtime, we'll look for class names with the format
            // awds_* and if it exists, strip off the awds_, and use the remaining string
            // as the class name for the drop container when the drop container is active
            // (ie there's a valid draggable element being dragged over the drop
            // container).
            dropStyle = StringUtil.strcat(DropStylePrefix,
                                          stringValueForBinding(DropClassBinding));
        }
        return dropTypeToString(cssClass, DropPrefix, type, types, dropStyle);
    }

    public boolean disableDrop ()
    {
        return booleanValueForBinding(DisableDropBinding) || requestContext().isExportMode();
    }

    public static String dropTypeToString (String prefix, String type, Object types)
    {
        return dropTypeToString(null, prefix, type, types, null);
    }

    public static String dropTypeToString (String cssClass, String prefix,
                                           String type, Object types, String dropStyle)
    {
        if (!StringUtil.nullOrEmptyOrBlankString(type)) {
            cssClass = StringUtil.strcat(cssClass == null ? "" : cssClass,prefix,type);
        }

        // check for types and add as appropriate -- must either be a List or a comma
        // delimited String.
        if (types != null) {
            List typesList = null;
            if (types instanceof List) {
                typesList = (List)types;
            }
            else if (types instanceof String) {
                typesList =
                    StringUtil.stringToStringsListUsingBreakChars((String)types,
                                                                  TypesDelimiter);
            }
            else {
                throw new AWGenericException("Types binding must be either a List or a " +
                                             "comma delimited String.");
            }

            cssClass = StringUtil.strcat(cssClass == null ? "" : cssClass, prefix,
                                         StringUtil.fastJoin(typesList, prefix));
        }

        // default to allow any drop
        if (cssClass == null) {
            cssClass = prefix;
        }

        // add dropStyle
        if (dropStyle != null) {
            cssClass= StringUtil.strcat(cssClass,dropStyle);
        }
        return cssClass;
    }

    public void sleep ()
    {
        _elementId = null;
        super.sleep();
    }
}