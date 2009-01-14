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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/core/WizardAction.java#2 $
*/

package ariba.ui.wizard.core;

import ariba.ui.wizard.meta.WizardActionMeta;
import ariba.ui.aribaweb.core.AWStringLocalizer;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.util.core.StringUtil;

import java.util.Map;

/**
    The WizardAction class encapsulates an instance of some action, built-in
    or custom, within a particular wizard.  It has holds immutable state like
    name & label, as well as dynamic state like whether the action is
    currently enabled.

    @aribaapi ariba
*/
// subclassed by demoshell/AWXWizardAction.java
public class WizardAction
{
    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/

        // immutable meta-data for this wizard action
    private WizardActionMeta _meta;

        // cached localized values from our meta-data
    private String _label;
    private String _hint;

        // immutable state
    private Wizard  _wizard;
    private boolean _shouldTakeValues;

        // dynamic state
    private boolean _isEnabled;

    /*-----------------------------------------------------------------------
        Constructors
      -----------------------------------------------------------------------*/

    /**
        @aribaapi private
    */
    protected WizardAction (
        WizardActionMeta meta,
        Wizard wizard,
        boolean shouldTakeValues)
    {
            // setup immutable state
        _meta = meta;
        _wizard = wizard;
        _shouldTakeValues = shouldTakeValues;

            // defaults to enabled
        _isEnabled = true;
    }

    /**
        @aribaapi private
    */
    protected WizardAction (WizardActionMeta meta, Wizard wizard)
    {
        this(meta, wizard, true);
    }

    /**
     * Added for use by demoshell
     * @aribaapi private
     */
    public WizardAction (Wizard wizard,
                         String name, String label, String button,
                         String hintKey, boolean ignoreValidation)
    {
        this (new WizardActionMeta (name, label, button, hintKey, ignoreValidation),
                wizard, true);
    }

    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/

    /**
        @aribaapi ariba
    */
    public Wizard getWizard ()
    {
        return _wizard;
    }

    /**
        @aribaapi ariba
    */
    public String getName ()
    {
        return _meta.name();
    }

    /**
        @aribaapi ariba
    */
    public String getTarget ()
    {
        return _meta.target();
    }

    /**
        @aribaapi private
    */
    public String getLabel ()
    {
        if (_label == null) {
            String label = _meta.label();
            _label = getLocalizedString(label);
            if (StringUtil.nullOrEmptyString(_label)) {
                _label = label;
            }
        }

        return _label;
    }

    /**
        @aribaapi private
    */
    public String getHint ()
    {
        if (_hint == null) {
            _hint = getLocalizedString(_meta.hintKey());
            if (StringUtil.nullOrEmptyString(_hint)) {
                _hint = getLabel();
            }
        }
        return _hint;
    }

    private String getLocalizedString (String key)
    {
        String stringTable = _meta.stringTable();
        if (stringTable != null) {
            AWStringLocalizer localizer = AWConcreteApplication.SharedInstance.getStringLocalizer();
            Map map = localizer.getLocalizedStrings(stringTable, _meta.stringsGroup(), _wizard.resourceManager());
            String value = (String)map.get(key);
            return value == null ? key : value;
        }
        return _wizard.localizedStringForKey(key);

    }

    /**
        @aribaapi private
    */
    public boolean shouldTakeValues ()
    {
        return _shouldTakeValues;
    }

    /**
        @aribaapi private
    */
    public boolean ignoreValidation ()
    {
        return _meta.ignoreValidation();
    }

    /**
        @aribaapi private
    */
    public boolean isEnabled ()
    {
        return _isEnabled;
    }

    /**
        @aribaapi private
    */
    public void setEnabled (boolean isEnabled)
    {
        _isEnabled = isEnabled;
    }

    /**
        @aribaapi private
    */
    public boolean isDefault ()
    {
        return (getName().equals(_wizard.getCurrentFrame().getDefaultAction()));
    }
}
