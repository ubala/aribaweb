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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/meta/WizardStepMeta.java#3 $
*/

package ariba.ui.wizard.meta;

import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import ariba.ui.widgets.XMLUtil;
import org.w3c.dom.Element;
import java.util.List;

/**
    A WizardStepMeta is the runtime counterpart to an XML definition of a
    wizard step.  It represents the static information about a step in a
    wizard flow, i.e. name, label, list of frames, etc.

    @aribaapi private
*/
public final class WizardStepMeta extends GenericMeta
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

        // generic wizard step XML attributes
    private static final String NumberedAttr = "numbered";

        // extension wizard step XML elements
    private static final String StepsElement   = WizardMeta.StepsElement;
    private static final String StepElement    = WizardMeta.StepElement;
    private static final String InFrameElement = WizardMeta.InFrameElement;
    private static final String FrameElement   = WizardMeta.FrameElement;

        // extension attribute error messages
    protected static final String UnknownFrameMsg = WizardMeta.UnknownFrameMsg;

        // validation error messages
    private static final String InvalidStepMsg =
        "step '%s' in wizard '%s' must have either a frame or sub-steps (or both)";


    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/

        // our main wizard meta info
    private WizardMeta _wizard;

        // should this step be numbered?
    private Boolean _numbered = Boolean.TRUE;

        // list of sub-step definitions
    private List _substeps;

        // frame to show for a leaf step
    private WizardFrameMeta _frame;


    /*-----------------------------------------------------------------------
        Constructors
      -----------------------------------------------------------------------*/

    /**
        Creates a new WizardStepMeta from static information, as opposed
        to reading in this information from XML.
    */
    public WizardStepMeta (WizardMeta wizard, String name, String label)
    {
            // initialize our core state
        super(name, label);
        _wizard = wizard;
    }

    /**
        Creates a new WizardStepMeta by reading in data from a wizard XML
        document.  This is the normal case, as opposed to instances which are
        created via application code (indirectly via the WizardStep
        constructor).
    */
    protected WizardStepMeta (WizardMeta wizard, Element stepElement)
    {
            // cache our top-level meta info
        _wizard = wizard;

            // read the wizard step attributes
        readWizardStepAttrs(stepElement);

            // read the frame for this step if given
        _frame = wizard.readWizardFrames(stepElement, this, null);

            // read any sub-steps that are given
        _substeps = wizard.readWizardSteps(stepElement, this, false);
    }


    /*-----------------------------------------------------------------------
        Initialization
      -----------------------------------------------------------------------*/

    protected void readInStepSubElements (Element inStepElement)
    {
            // read step attributes
        readWizardStepAttrs(inStepElement);

            // get the first (and only) child element
        Element child = XMLUtil.getFirstChildElement(inStepElement);
        String childName = elementName(child);

            // recursively handle 'steps' sub-element
        if (childName.equals(StepsElement)) {
            _wizard.applyWizardStepsExtensions(inStepElement, this, _substeps);
        }
            // handle 'inFrame' sub-element to change source
        else if (childName.equals(InFrameElement)) {
            String fn = stringAttrFromElement(child, NameAttr);
            _wizard.assertion(_frame.name().equals(fn), UnknownFrameMsg, fn, StepElement);
            _frame = _wizard.readWizardFrames(inStepElement, this, null);
        }
            // handle 'frame' sub-element to allow full replacement
        else if (childName.equals(FrameElement)) {
            // todo: does this method call have side effects, or can we eliminate?
            stringAttrFromElement(child, NameAttr);
            _frame = _wizard.readWizardFrames(inStepElement, this, null);
        }
    }

    private void readWizardStepAttrs (Element stepElement)
    {
            // required in both base & extensions
        _name  = stringAttrFromElement(stepElement, NameAttr, _name);

            // required in base, optional in extensions
        _label = stringAttrFromElement(stepElement, LabelAttr, _label);

            // optional in both base & extensions
        _numbered = booleanAttrFromElement(stepElement, NumberedAttr, _numbered);
    }

    /**
        Checks that the final form of our step meta-data is valid.  Asserts
        this step and each of its sub-steps has either a frame or a non-empty
        list of sub-steps (or both).  Returns true if this step or any of its
        sub-steps has a frame.
    */
    private boolean validate ()
    {
            // check that we have either a frame or sub-steps
        boolean valid = ((_frame != null) || !ListUtil.nullOrEmptyList(_substeps));
        Assert.that(valid, InvalidStepMsg, _name, _wizard.name());

            // validate our frame (if we have one)
        if (_frame != null) {
            _frame.validate();
        }

            // recurse and return true if at least one step frame found
        return validate(_substeps, (_frame != null));
    }

    /**
        @aribaapi private
    */
    protected static boolean validate (List steps, boolean hasStepFrame)
    {
        for (int index = steps.size() - 1; index >= 0; index--) {
            WizardStepMeta stepMeta = (WizardStepMeta)steps.get(index);
            hasStepFrame = (stepMeta.validate() || hasStepFrame);
        }
        return hasStepFrame;
    }


    /*-----------------------------------------------------------------------
        Field Accessors
      -----------------------------------------------------------------------*/

    public WizardMeta wizard ()
    {
        return _wizard;
    }

    public boolean numbered ()
    {
        return _numbered.booleanValue();
    }

    public List substeps ()
    {
        return _substeps;
    }

    public WizardFrameMeta frame ()
    {
        return _frame;
    }

    public void setFrame (WizardFrameMeta frame)
    {
        _frame = frame;
    }


    /*-----------------------------------------------------------------------
        Protected Methods
      -----------------------------------------------------------------------*/

    protected boolean resourcesHaveChanged ()
    {
        if (_frame != null) {
            return _frame.resourcesHaveChanged();
        }

        for (int i = 0, count = _substeps.size(); i < count; i++) {
            if (((WizardStepMeta)_substeps.get(i)).resourcesHaveChanged()) {
                return true;
            }
        }

        return false;
    }
}
