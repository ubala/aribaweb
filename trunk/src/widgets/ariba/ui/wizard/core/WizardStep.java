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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/core/WizardStep.java#2 $
*/

package ariba.ui.wizard.core;

import ariba.ui.wizard.meta.WizardFrameMeta;
import ariba.ui.wizard.meta.WizardStepMeta;
import ariba.util.core.ListUtil;

import java.util.List;

/**
    A WizardStep represents the runtime state of a particular step in a wizard
    flow.  It reflects both the immutable meta-data specified in the XML or
    given by the application, as well as the dynamic state of the step for a
    particular trip through the wizard.

    @aribaapi ariba
*/
public final class WizardStep implements WizardStepsParent
{
    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/

        // immutable meta-data for this wizard step
    private WizardStepMeta _meta;

        // main wizard instance
    private Wizard _wizard;

        // our parent
    private WizardStepsParent _parent;

        // cached localized values from our meta-data
    private String _label;

        // our position within our parent's list
    private int         _index;
    private WizardStep  _nextStep;
    private WizardStep  _prevStep;

        // list of sub-steps, or our single frame
    private List      _substeps;
    private WizardFrame _frame;

        // visibility, visited state
    private boolean _visible;
    private boolean _childrenVisible;
    private boolean _hasBeenVisited;


    /*-----------------------------------------------------------------------
        Constructors
      -----------------------------------------------------------------------*/

    /**
        @aribaapi ariba
    */
    public WizardStep (Wizard wizard, String name, String label)
    {
            // not reading from XML, so we must create our own meta
        WizardStepMeta meta = new WizardStepMeta(wizard.meta(), name, label);
        init(meta, null, wizard);
    }

    public WizardStep (Wizard wizard, String name, String label, WizardStep parent)
    {
            // not reading from XML, so we must create our own meta
        WizardStepMeta meta = new WizardStepMeta(wizard.meta(), name, label);
        init(meta, parent, wizard);
    }

    /**
        @aribaapi private
    */
    protected WizardStep (WizardStepMeta meta, WizardStepsParent parent, Wizard wizard)
    {
        init(meta, parent, wizard);
    }

    private void init (WizardStepMeta meta, WizardStepsParent parent, Wizard wizard)
    {
        _wizard = wizard;
        _parent = parent;
        _meta = meta;

            // if this step's meta-data comes from XML, it will already have a
            // frame and/or a list of sub-steps provided
        WizardFrameMeta frameMeta = _meta.frame();
        if (frameMeta != null) {
            _frame = new WizardFrame(frameMeta, this, wizard);
        }

        List substepsMeta = _meta.substeps();
        if (substepsMeta != null) {
            _substeps = wizard.buildSteps(substepsMeta, this);
        }
        else {
            _substeps = ListUtil.list();
        }

            // initially visible and unvisited
        _visible = true;
        _childrenVisible = true;
        _hasBeenVisited = false;
    }


    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/

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
    public Wizard getWizard ()
    {
        return _wizard;
    }

    /**
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
        Overrides the default label for the wizard step
        @param  localizedLabel localized string to use as label for wizard step
        @aribaapi ariba
    */

    public void setLabel (String localizedLabel)
    {
        _label = localizedLabel;
    }

    /**
        @aribaapi ariba
    */
    public boolean isNumbered ()
    {
        return _meta.numbered();
    }

    /**
        @aribaapi ariba
    */
    public WizardFrame getFrame ()
    {
        return _frame;
    }

    /**
        Sets the frame that is directly contained by this step.<br>

        If the step already contains a frame, it will be replace by the
        given frame.  The old frame will be a dialog frame with no containing
        step and can be obtained by calling Wizard.getFrameWithName().<br>

        If the parameter is null, the current frame will be removed from the step
        the step will be left with no frame.  It is the responsiblity of the
        application to ensure that there is at least one visible frame under
        each top level step, either contained directly by the step or one of the
        substeps. If not, the step should be remove or runtime exception will be thrown.

        @aribaapi ariba
    */
    public void setFrame (WizardFrame frame)
    {
        if (_frame != null) {
            _frame.setStep(null);
        }
        if (frame != null) {
            frame.setStep(this);
        }
        _frame = frame;
    }

    /**
        Removes all the substeps contained by the step
        @aribaapi ariba
    */
    public void removeAllSteps ()
    {
        if (_substeps != null) {
            int lastStepIndex = _substeps.size() - 1;
            for (int index = lastStepIndex; index >= 0; index--) {
                WizardStep step = (WizardStep)_substeps.remove(index);
                _wizard.removeStepFromCache(step);
            }
        }
    }
    /**
        @aribaapi ariba
    */
    public List getSteps ()
    {
        return _substeps;
    }

    /**
        @aribaapi ariba
    */
    public WizardStepsParent getParent ()
    {
        return _parent;
    }

    /**
        @aribaapi ariba
    */
    protected void setParent (WizardStepsParent parent)
    {
        _parent = parent;
    }

    /**
        @aribaapi ariba
    */
    public void insertStepBefore (WizardStep step, WizardStep beforeStep)
    {
        _wizard.insertStepBefore(step, beforeStep);
    }

    /**
        @aribaapi ariba
    */
    public void insertStepAfter (WizardStep step, WizardStep afterStep)
    {
        _wizard.insertStepAfter(step, afterStep);
    }

    /**
        @aribaapi ariba
    */
    public void insertStepAt (WizardStep step, int index)
    {
        _wizard.insertStepAt(step, getSteps(), index, this);
    }

    /**
        @aribaapi ariba
    */
    public void removeStep (WizardStep step)
    {
        _wizard.removeStep(step);
    }

    /**
        @aribaapi ariba
    */
    public boolean isVisible ()
    {
        if ((_substeps == null) || (_frame != null)) {
            return _visible;
        }
        else {
            return (_visible && _childrenVisible);
        }
    }

    /**
        @aribaapi ariba
    */
    public void setVisible (boolean visible)
    {
        if (visible != _visible && isTopLevelStep()) {
            _wizard.setTopLevelChanged(true);
        }
        _visible = visible;

        // update the parent step's children visibility based on the new value for this step's visibility
        if (_parent != null) {
            _parent.updateChildrenVisible();
        }
    }

    public void updateChildrenVisible ()
    {
        if (_substeps == null) {
            _childrenVisible = false;
        }
        else {
            for (int i = 0; i < _substeps.size(); i++) {
                _childrenVisible = false;
                WizardStep subStep = (WizardStep)_substeps.get(i);
                if (subStep.isVisible()) {
                    _childrenVisible =  true;
                }
            }
        }
    }

    /**
        @aribaapi ariba
    */
    public boolean hasBeenVisited ()
    {
        return _hasBeenVisited;
    }

    /**
        @aribaapi private
    */
    public void setHasBeenVisited (boolean hasBeenVisited)
    {
        _hasBeenVisited = hasBeenVisited;
    }

    /**
        @aribaapi private
    */
    protected WizardStep getNextStepToDisplay ()
    {
        // first, hide the frame that is directly contained by this step if one exists before
        // getting the next frame to display

        boolean isFrameVisible = false;

        if (_frame != null) {
            isFrameVisible = _frame.isVisible();
            _frame.setVisible(false);
        }
        WizardFrame nextFrame = getNextFrameToDisplay();
        if (_frame != null) {
            _frame.setVisible(isFrameVisible);
        }
        return nextFrame == null ? null : nextFrame.getStep();
    }

    /**
        @aribaapi private
    */
    public WizardStep getNextVisibleSibling ()
    {
        if (_nextStep != null) {
            return _nextStep.isVisible() ? _nextStep : _nextStep.getNextVisibleSibling();
        }
        return null;
    }

    /**
        @aribaapi private
    */
    public WizardStep getNextSibling ()
    {
        return _nextStep;
    }

    /**
        @aribaapi private
    */
    protected void setNextSibling (WizardStep step)
    {
        _nextStep = step;
    }

    /**
        @aribaapi private
    */
    public int index ()
    {
        _wizard.assignTopLevelStepIndexes();
        return _index;
    }

    /**
        @aribaapi private
    */
    protected void setIndex (int index)
    {
        _index = index;
    }

    /**
        @aribaapi private
    */
    protected WizardStep getPreviousStepToDisplay ()
    {
        WizardFrame previousFrame = getPreviousFrameToDisplay();
        return previousFrame == null ? null : previousFrame.getStep();
    }

    /**
        @aribaapi private
    */
    public WizardStep getPreviousVisibleSibling ()
    {
        if (_prevStep != null) {
            return _prevStep.isVisible() ?
                _prevStep : _prevStep.getPreviousVisibleSibling();
        }
        return null;
    }

    /**
        @aribaapi private
    */
    public WizardStep getPreviousSibling ()
    {
        return _prevStep;
    }

    /**
        @aribaapi private
    */
    protected void setPreviousSibling (WizardStep step)
    {
        _prevStep = step;
    }

    /**
        @aribaapi private
    */
    public WizardStepMeta meta ()
    {
        return _meta;
    }

    /**
        @aribaapi private
    */
    protected WizardFrame getFirstFrameToDisplay ()
    {
        WizardFrame firstFrame = null;

        if (_visible) {
            if (_frame != null && _frame.isVisible()) {
                firstFrame = _frame;
            }
            else if (_substeps != null && !_substeps.isEmpty()) {
                int size = _substeps.size();
                for (int i = 0; i < size; i++) {
                    WizardStep substep = (WizardStep)_substeps.get(i);
                    firstFrame = substep.getFirstFrameToDisplay();
                    if (firstFrame != null) {
                        break;
                    }
                }
            }
        }
        return firstFrame;
    }

    private WizardFrame getLastFrameToDisplay ()
    {
        WizardFrame resultFrame = null;

        if (_visible) {
            if (_substeps != null && !_substeps.isEmpty()) {
                int size = _substeps.size();
                for (int index = size -1 ; index >= 0; index--) {
                    WizardStep substep = (WizardStep)_substeps.get(index);
                    resultFrame = substep.getLastFrameToDisplay();
                    if (resultFrame != null) {
                        break;
                    }
                }
            }

            if (resultFrame == null  && _frame != null && _frame.isVisible()) {
                resultFrame = _frame;
            }
        }
        return resultFrame;
    }

    private WizardFrame getNextFrameToDisplay ()
    {
        WizardFrame targetFrame = getFirstFrameToDisplay();
        if (targetFrame == null) {
            WizardStep nextSibling = getNextSibling();
            while (nextSibling != null) {
                targetFrame = nextSibling.getFirstFrameToDisplay();
                if (targetFrame != null) {
                    break;
                }
                nextSibling = nextSibling.getNextSibling();
            }
            if (targetFrame == null && _parent instanceof WizardStep) {
                WizardStep parentSibling = ((WizardStep)_parent).getNextSibling();
                if (parentSibling != null) {
                    targetFrame = parentSibling.getNextFrameToDisplay();
                }
            }
        }
        return targetFrame;
    }


    private WizardFrame getPreviousFrameToDisplay ()
    {
        WizardFrame targetFrame = null;

        WizardStep prevSibling = getPreviousSibling();
        while (prevSibling != null) {
            targetFrame = prevSibling.getLastFrameToDisplay();
            if (targetFrame != null) {
                break;
            }
            prevSibling = prevSibling.getPreviousSibling();
        }

        if (targetFrame == null && _parent instanceof WizardStep) {
            WizardStep parentStep = (WizardStep)_parent;
            targetFrame = parentStep.getFrame();
            if (targetFrame == null || !targetFrame.isVisible()) {
                targetFrame = parentStep.getPreviousFrameToDisplay();
            }
        }
        return targetFrame;
    }

    /**
        @aribaapi private
    */
    public boolean isTopLevelStep ()
    {
        return (_parent == _wizard);
    }
}
