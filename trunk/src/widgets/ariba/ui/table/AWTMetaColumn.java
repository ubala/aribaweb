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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTMetaColumn.java#14 $
*/
package ariba.ui.table;

import ariba.util.fieldvalue.FieldPath;
import java.util.Map;
import ariba.util.core.StringUtil;
import ariba.ui.validation.AWVFormatterFactory;
import org.w3c.dom.*;

public final class AWTMetaColumn extends AWTDataTable.Column
{
    public static final String ImageType = "image";
    public static final String TargetAttribute = "target";
    public static final String ActionAttribute = "action";
    public static final String TypeAttribute = "type";
    public static final String AlignAttribute = "align";
    public static final String StyleAttribute = "style";

    protected FieldPath _fieldPath;
    protected String _label;
    protected Element _columnMeta;
    protected FieldPath _actionFieldPath;
    protected Object _formatter; // AWFormatting
    protected String _align;
    protected String _style;
    protected boolean _isImage;
    protected FieldPath _actionTargetFieldPath;
    protected boolean _sessionless;

    public String rendererComponentName ()
    {
        return AWTMetaColumnRenderer.class.getName();
    }

    public void initializeColumn (AWTDataTable table)
    {
        // we expect to get created and registered by the AWTMetaContent
    }

    public boolean isValueColumn ()
    {
        return !_isImage;
    }

    public boolean isOptional (AWTDataTable sender)
    {
        return true;    // meta-data specified?
    }

    public boolean initiallyVisible ()
    {
        String val = _columnMeta.getAttribute("initiallyVisible");
        return StringUtil.nullOrEmptyString(val) || val.equals("true");
    }

    public String align ()
    {
        return _align;
    }

    public String style ()
    {
       return _style;
    }

    public void init (String tagName, Map bindingsHashtable)
    {
        super.init(tagName, bindingsHashtable);
    }

    /** Convenience for initing a dynamic column */
     public void init (AWTDataTable table,
                       String tagPath,
                       String label,
                       Element columnMeta,
                       boolean sessionless,
                       Map formatters,
                       boolean useXMLFieldPath)
    {
        String keyPath = (useXMLFieldPath) ? tagPath + ".text" : tagPath;
        _fieldPath = FieldPath.sharedFieldPath(keyPath);

        String actionPath = StringUtil.strcat(tagPath + ".", ActionAttribute);
        _actionFieldPath = FieldPath.sharedFieldPath(actionPath);

        String actionTargetPath = StringUtil.strcat(tagPath + ".", TargetAttribute);
        _actionTargetFieldPath = FieldPath.sharedFieldPath(actionTargetPath);

        _label = (label != null) ? label : tagPath;
        _columnMeta = columnMeta;

        String type = _columnMeta.hasAttribute(TypeAttribute) ? _columnMeta.getAttribute(TypeAttribute) : null;
        if (type != null) {
            if (ImageType.equals(type)) {
                _isImage = true;
            }
            else if (formatters != null) {
                Map xmlFormatters = (Map)formatters.get(AWVFormatterFactory.XMLFormattersKey);
                _formatter = xmlFormatters == null ? null : xmlFormatters.get(type);
            }
            if (AWVFormatterFactory.IntegerFormatterKey.equals(type) ||
                    AWVFormatterFactory.MoneyFormatterKey.equals(type) ||
                    AWVFormatterFactory.BigDecimalFormatterKey.equals(type)) {
                // default alignment
                _align = "right";
            }
        }
        if (_columnMeta.hasAttribute(AlignAttribute)) {
            _align = _columnMeta.getAttribute(AlignAttribute);
        }

        if (_columnMeta.hasAttribute(StyleAttribute)) {
            _style = _columnMeta.getAttribute(StyleAttribute);
        }
        _sessionless = sessionless;
    }

    public FieldPath fieldPath ()
    {
        return _fieldPath;
    }

    public String keyPathString ()
    {
        return (_fieldPath != null) ? _fieldPath.fieldPathString() : null;
    }

    public String label ()
    {
        return _label;
    }

    public FieldPath actionTargetFieldPath ()
    {
        return _actionTargetFieldPath;
    }

    public FieldPath actionFieldPath ()
    {
        return _actionFieldPath;
    }

    public Element columnMeta ()
    {
        return _columnMeta;
    }

    public Object formatter ()
    {
        return _formatter;
    }

    public boolean isImage ()
    {
        return _isImage;
    }
}
