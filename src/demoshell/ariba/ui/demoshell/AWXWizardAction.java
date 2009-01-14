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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/AWXWizardAction.java#5 $
*/
package ariba.ui.demoshell;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.wizard.core.Wizard;
import ariba.ui.wizard.core.WizardFrame;
import ariba.ui.wizard.core.WizardAction;
import ariba.ui.wizard.component.WizardPage;
import ariba.ui.wizard.component.WizardUtil;

import java.util.Map;
import ariba.util.core.Assert;
import ariba.ui.aribaweb.core.AWTemplate;
import ariba.ui.aribaweb.core.AWElement;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWContainerElement;
import ariba.ui.aribaweb.util.AWUtil;

/** This is intended to sit in a "Step" file to define an action
 *  that belongs in the wizard nav bar.
 *  E.g.: <AWXWizardAction action="$doIt" label="Done" button="foo.gif"
 *                  hintKey="That's a wrap" afterName="Exit"/>
 * The trick is that these need to be registered before the page is rendered.
 */
public class AWXWizardAction extends AWContainerElement
{
    private AWBinding _imageBinding;
    private AWBinding _labelBinding;
    private AWBinding _hintKeyBinding;
    private AWBinding _pageNameBinding;

    public void init (String tagName, Map bindingsHashtable)
    {
        _labelBinding = (AWBinding)bindingsHashtable.remove("label");
        _imageBinding = (AWBinding)bindingsHashtable.remove("image");
        _hintKeyBinding = (AWBinding)bindingsHashtable.remove("hintKey");
        _pageNameBinding = (AWBinding)bindingsHashtable.remove("pageName");

        Assert.that( ((_labelBinding!=null) && (_imageBinding!=null) && (_pageNameBinding!=null)),
                "AWXWizardAction: label, image, and pageName bindings must all be set");
        super.init(tagName, bindingsHashtable);
    }

    public static void initFrameWithTemplate (final WizardFrame frame, AWTemplate template)
    {
        // we need to search to find ourselves as a template child
        AWUtil.iterate(template, new AWUtil.ElementIterator() {
            public Object process(AWElement e) {
                if (e instanceof AWXWizardAction) {
                    ((AWXWizardAction)e).initializeFrame(frame);
                }
                return null; // keep looking
            }
        });
    }

    protected static String stringForBinding (AWBinding binding)
    {
        return (binding == null) ? null : binding.stringValue(null);
    }

    protected void initializeFrame (WizardFrame frame)
    {
        // toss our component reference in the page for a possible later
        // invoke action...

        // don't render anything in the response -- just register
        String name = stringForBinding(_labelBinding);  // must be a constant binding
        Wizard wizard = frame.getWizard();
        WizardAction action = wizard.getActionWithName(name);
        if (action == null) {
            // need to create and register the action
            action = new Action(this, wizard, name, name,
                        stringForBinding(_imageBinding),
                        stringForBinding(_hintKeyBinding), false);
            wizard.addAction(name, action);
        }

        // make sure we're on the frame as well...
        WizardAction[] actions = frame.getActions();
        int len = actions.length;
        int pos = 0;  // default is add to beginning
        for (int i = 0; i < actions.length; i++) {
            if (action == actions[i]) {
                action = null;  // flag that we're done
                break;
            } else if (action.getName().equals(name)) {
                pos = i+1;  // add after this
            }
        }

        if (action != null) {
            // need to register it
            WizardAction[] newActions = new WizardAction[len+1];
            if (pos > 0) {
                System.arraycopy(actions, 0, newActions, 0, pos);
            }
            if (pos < (len+1)) {
                System.arraycopy(actions, pos, newActions, pos+1, len-pos);
            }
            newActions[pos] = action;
            frame.setActions(newActions);
        }
    }

    public AWComponent invoke (AWComponent page)
    {
        // ugh!  How do we evaluate this relative path?  We don't have our parent component!
        String pageName = stringForBinding(_pageNameBinding);
        return AWXHTMLComponentFactory.sharedInstance().createComponentForRelativePath(pageName, page);
    }

    static public class Action extends WizardAction
    {
        protected AWXWizardAction _element;

        public Action (AWXWizardAction element, Wizard wizard,
                         String name, String label, String button,
                         String hintKey, boolean ignoreValidation)
        {
            super (wizard, name, label, button, hintKey, ignoreValidation);
            _element = element;
        }

        public AWComponent invoke ()
        {
            WizardFrame frame = getWizard().getCurrentFrame();
            AWComponent page = (WizardPage)frame.getAttribute(WizardUtil.WizardPageKey);
            return _element.invoke(page);
        }
    }
}
