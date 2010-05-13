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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/core/WizardFrame.java#3 $
*/

package ariba.ui.wizard.core;

import ariba.ui.widgets.Widgets;
import ariba.ui.widgets.WidgetsDelegate;
import ariba.ui.wizard.meta.WizardActionMeta;
import ariba.ui.wizard.meta.WizardFrameMeta;
import ariba.util.core.Assert;
import ariba.util.core.StringUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import java.util.Map;
import java.util.List;
import java.util.Iterator;

/**
    @aribaapi ariba
*/
public final class WizardFrame implements WizardActionTarget
{
    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/

    public final static String ActionParamKey = "wzrd_actionParam";

        // error messages
    private final static String ErrorDelegateCreation =
        "error creating wizard delegate of type '%s'";
    private static final String UnknownActionMsg =
        "unknown action '%s' in %s frame";

    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/

        // the main wizard instance
    private Wizard _wizard;

        // immutable meta-data for this wizard frame
    private WizardFrameMeta _meta;

        // delegate for action handling, etc.
    private WizardFrameDelegate _delegate;

        // cached localized values from our meta-data
    private String _label;

        // our parent step (null for dialog frames)
    private WizardStep _step;

        // the list of actions to show
    private WizardAction[] _actions;

        // dynamic state
    private boolean      _visible;
    private Map          _attributes;
    private boolean      _isValid;
    private WizardFrame  _backFrame;

        // if this flag is set and the frame is invalid,
        // the frame should display all error messages.
        // This flag is turned on at the first time the
        // frame is invoked by an enforce-validation action
        // and it stays on.
    private boolean      _showErrors;

    /*-----------------------------------------------------------------------
        Constructors
      -----------------------------------------------------------------------*/

    /**
        Constructor
        @aribaapi ariba
    */
    public WizardFrame (Wizard wizard, String name,
                        String label, String source)
    {
        this(wizard, name, label, source, null, null, null);
    }

    /**
        Constructor
        @aribaapi ariba
    */
    public WizardFrame (Wizard wizard, String name,
                        String label, String source,
                        String formEncoding,
                        WizardFrameDelegate delegate,
                        String[] actionNames)
    {
        String delegateName = null;
        if (delegate != null) {
            delegateName = delegate.getClass().getName();
            _delegate = delegate;
        }
            // not reading from XML, so we must create our own meta
        WizardFrameMeta meta = new WizardFrameMeta(
            wizard.meta(), name, label,
            source, delegateName, null,
            formEncoding, actionNames);
        init(meta, null, wizard);
    }

    /**
        @aribaapi private
    */
    public WizardFrame (WizardFrameMeta frameMeta, WizardStep step, Wizard wizard)
    {
        init(frameMeta, step, wizard);
    }

    private void init (WizardFrameMeta frameMeta, WizardStep step, Wizard wizard)
    {
        _wizard = wizard;

        _meta = frameMeta;
        _step = step;
        _isValid = true;
        _visible = true;
        if (_delegate == null) {
                // ask wizard delegate for the frame delegate first
            WizardDelegate wizardDelegate = wizard.getDelegate();
            if (wizardDelegate != null) {
                _delegate = wizardDelegate.delegateForFrame(this);
            }

                // otherwise, defer to the XML declaration
            if (_delegate == null) {
                String delegateName = _meta.delegate();
                if (delegateName != null) {
                    _delegate = wizard.instantiateWizardFrameDelegate(delegateName);
                    Assert.that(_delegate != null, ErrorDelegateCreation, delegateName);
                }
            }
        }

    }

    private WizardAction[] buildActions ()
    {
            // set up the list of actions to show for this frame
        List actionList = ListUtil.list();
        boolean isDialog = isDialogFrame();

            // next & previous are first for non-dialog frames
        if (!isDialog) {
            actionList.add(_wizard.prev);
            actionList.add(_wizard.next);
        }

            // add the actions specified in the meta for this frame
            // (this happens for both dialog and non-dialog frames)
        Iterator actionEnum = _meta.actions();
        while (actionEnum.hasNext()) {
            WizardActionMeta actionMeta = (WizardActionMeta)actionEnum.next();
            WizardAction action = _wizard.getActionWithName(actionMeta.name());
            if (action == null) {
                Assert.that(false, UnknownActionMsg,
                                    actionMeta.name(), _meta.name());
            }
            ListUtil.addElementIfAbsent(actionList, action);
        }

            // exit is the last action for non-dialog frames
        if (!isDialog) {
            actionList.add(_wizard.exit);
        }

            // provide default ok and cancel actions for a dialog frame
            // that has no other actions given
        if (isDialog && ListUtil.nullOrEmptyList(actionList)) {
            actionList.add(_wizard.ok);
            actionList.add(_wizard.cancel);
        }

        WizardAction[] actions = new WizardAction[actionList.size()];
        actionList.toArray(actions);
        return actions;
    }

    /**
        here for demoshell
        @aribaapi private
    */
    public void setActions (WizardAction[] actions)
    {
        _actions = actions;
    }

    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/

    /**
        Returns the frame name
        @aribaapi ariba
    */
    public String getName ()
    {
        return _meta.name();
    }

    /**
        Returns the wizard
        @aribaapi ariba
    */
    public Wizard getWizard ()
    {
        return _wizard;
    }
    /**
        Returns the wizard frame delegate instance. It could either
        be returned by WizardDelegate.getFrameDelegate() or instantiated
        from the delegate class name specified in the frame xml if the
        WizardDelegate returns null.
        @aribaapi ariba
    */
    public WizardFrameDelegate getDelegate ()
    {
        return _delegate;
    }

    /**
        @aribaapi private
    */
    public void setDelegate (WizardFrameDelegate delegate)
    {
        _delegate = delegate;
    }

    /**
        Returns the class name of the default frame delegate as specified in the xml
        @aribaapi ariba
    */
    public String getDefaultDelegateName ()
    {
        return _meta.delegate();
    }
    /**
        Returns the label
        @aribaapi ariba
    */
    public String getLabel ()
    {
        if (_label == null) {
            _label = _wizard.localizedStringForKey(_meta.label());
        }
        return _label;
    }

    /**
	    overrides the default value for the wizard frame label

	    @param localizedLabel localized string used as labe for wizard frame
	    @aribaapi ariba
	*/
    public void setLabel (String localizedLabel)
    {
        _label = localizedLabel;
    }

    /**
        Returns the default action name
        @aribaapi ariba
    */
    public String getDefaultAction ()
    {
        String defaultActionName = _meta.defaultAction();
        if (defaultActionName == null) {
            defaultActionName = _wizard.next.getName();
        }
        return defaultActionName;
    }

    /**
        Returns the form encoding type
        @aribaapi ariba
    */
    public String getFormEncoding ()
    {
        return _meta.formEncoding();
    }

    public boolean getSubmitFormDefault ()
    {
        return _meta.submitFormDefault();
    }
    /**
        @aribaapi ariba
    */
    public String getType ()
    {
        return _meta.type();
    }

    /**
        Returns the resource of the frame
        @aribaapi ariba
    */
    public String getSource ()
    {
        return _meta.source();
    }

    /**
        Returns the list of actions including both built-in and custom actions
        @aribaapi ariba
    */
    public WizardAction[] getActions ()
    {
        if (_actions == null) {
            _actions = buildActions();
        }
        return _actions;
    }

    /**
        Returns the enclosing step for this frame.  Returns null for
        dialog frames.
        @aribaapi private
    */
    public WizardStep getStep ()
    {
        return _step;
    }

    /**
        @aribaapi private
    */
    public void setStep (WizardStep step)
    {
            // set up all the right pointers,
        _step = step;
    }

    /**
        Returns true if the this is a dialog frame.
        Also see getStep()
        @aribaapi private
    */
    public boolean isDialogFrame ()
    {
        return (_step == null);
    }

    /**
        sets an user-defined attribute on this frame
        @aribaapi ariba
    */
    public void setAttribute (Object key, Object value)
    {
        if (_attributes == null) {
            _attributes = MapUtil.map();
        }
        _attributes.put(key, value);
    }

    /**
        Returns the value for the attribute with the given key.  If the attribute
        had not been set, it would return null.
        @aribaapi ariba
    */
    public Object getAttribute (Object key)
    {
        return _attributes == null ? null : _attributes.get(key);
    }

    /**
        Removes a particular attribute from this frame
        @aribaapi ariba
    */
    public void removeAttribute (Object key)
    {
        if (_attributes != null) {
            _attributes.remove(key);
        }
    }

    /**
        Returns true if this frame is visible.  By default, all frames are visible.
        The visibility can be changed by wizard frame delegate, usually inside the
        targetForAction() method.  See also setVisible()
        @aribaapi ariba
    */
    public boolean isVisible ()
    {
        return _visible;
    }

    /**
        Changes the visibility of this frame.  See also isVisible()
        @aribaapi ariba
    */
    public void setVisible (boolean visible)
    {
        _visible = visible;
    }

    /**
        @aribaapi private
    */
    protected WizardFrameMeta meta ()
    {
        return _meta;
    }

    /**
        @aribaapi public
        set the validation result for this frame.
    */
    public boolean isValid ()
    {
        return _isValid;
    }

    /**
        Allows the WizardFrameDelegate to validate the values associated with
        the current frame.  Return true if all frame values are valid
        according to the application's business logic, or false if there are
        invalid values.  It is left up to the application to manage the
        display of these invalid values within the frame content.

        @aribaapi ariba
    */
    public void setValid (boolean valid)
    {
        _isValid = valid;
    }

    /**
        Returns the frame set the previously visited frame.  This is usefu
        for dialog frames
        @aribaapi ariba
    */
    public WizardFrame getBackFrame ()
    {
        return _backFrame;
    }

    /**
        Sets the back frame pointer
        @aribaapi private
    */
    protected void setBackFrame (WizardFrame backFrame)
    {
        _backFrame = backFrame;
    }

    /**
        @aribaapi public
    */
    public boolean shouldShowErrors ()
    {
        return _showErrors;
    }

    /**
        @aribaapi private
    */
    public void setShowErrors (boolean showErrors)
    {
        _showErrors =  showErrors;
    }

    /*--------------------------------------------
        Implements WizardActionTarget interface
    --------------------------------------------*/
    /**
        @aribaapi private
    */
    public WizardFrame getOriginatingFrame ()
    {
        return this;
    }

    /**
        @aribaapi private
    */
    public boolean terminatesWizard ()
    {
        return false;
    }

    protected void cleanup ()
    {
        _delegate = null;
    }
}
