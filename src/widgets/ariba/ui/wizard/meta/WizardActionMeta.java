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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/meta/WizardActionMeta.java#2 $
*/

package ariba.ui.wizard.meta;

import ariba.util.core.Constants;
import ariba.util.core.WrapperRuntimeException;
import org.w3c.dom.Element;

/**
    A WizardActionMeta is the runtime counterpart to an XML definition of a
    wizard action.  It represents the static information about a custom wizard
    action, i.e. name, label, button image, etc.

    @aribaapi private
*/
public final class WizardActionMeta extends GenericMeta
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

        // names for wizard action XML attributes
    protected static final String ButtonAttr    = "button";
    protected static final String HintAttr      = "hint";
    protected static final String TargetAttr    = "target";

        // should this action allow the user to leave page even with errors?
    protected static final String IgnoreValidationAttr = "ignoreValidation";


    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/
        // base button image name
    private String _button;

        // string table & key for the action label and action hint
    private String _stringTable;
    private String _stringsGroup;
    private String _hintKey;

        // the default target for this action
    private String _target;

        // how should this action handle validation errors?
    private Boolean _ignoreValidation;


    /*-----------------------------------------------------------------------
        Constructor
      -----------------------------------------------------------------------*/

    public WizardActionMeta (String name, String labelKey, String button, String hintKey)
    {
        this(name, labelKey, button, hintKey, false);
    }

    public WizardActionMeta (String name, String labelKey, String button, String hintKey,
                             boolean ignoreValidation)
    {
        super(name, labelKey);

            // store the button name, no target
        _button = button;
        _target = null;

            // we look up the label and hint strings for the built-in actions
            // from a localized string table resource
        _stringTable = WizardMeta.StringsTable;
        _stringsGroup = WizardMeta.stringsGroup();
        _hintKey = hintKey;

            // remember whether this actions allows the user to
            // leave the current page even if it has errors
        _ignoreValidation = Constants.getBoolean(ignoreValidation);
    }

    public WizardActionMeta (WizardMeta wizard, Element actionElement)
    {
            // read the wizard action attributes
        readWizardActionAttrs(actionElement);
    }

    protected void readWizardActionAttrs (Element element)
    {
            // required in both base & extensions
        _name = stringAttrFromElement(element, NameAttr, _name);

            // required in base, optional in extensions
        _label = stringAttrFromElement(element, LabelAttr, _label);

            // optional in both base & extensions
        _hintKey = stringAttrFromElement(element, HintAttr, _hintKey);
        _button  = stringAttrFromElement(element, ButtonAttr, _button);
        _target  = stringAttrFromElement(element, TargetAttr, _target);

            // optional in both base & extensions
        _ignoreValidation =
            booleanAttrFromElement(element, IgnoreValidationAttr, _ignoreValidation);
    }


    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/
    public String target ()
    {
        return _target;
    }

    public boolean ignoreValidation ()
    {
        return _ignoreValidation.booleanValue();
    }

    public String stringTable ()
    {
        return _stringTable;
    }

    public String hintKey ()
    {
        return _hintKey;
    }

    public String stringsGroup ()
    {
        return _stringsGroup;
    }
}
