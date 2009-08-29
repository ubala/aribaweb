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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/core/Wizard.java#4 $
*/

package ariba.ui.wizard.core;

import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.ui.aribaweb.core.AWPageCacheMark;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWMultiLocaleResourceManager;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.ui.aribaweb.util.AWSingleLocaleResourceManager;
import ariba.ui.aribaweb.util.AWSemanticKeyProvider;
import ariba.ui.wizard.component.WizardUtil;
import ariba.ui.wizard.meta.WizardActionMeta;
import ariba.ui.wizard.meta.WizardFrameMeta;
import ariba.ui.wizard.meta.WizardMeta;
import ariba.ui.wizard.meta.WizardStepMeta;
import ariba.util.core.ClassUtil;
import ariba.util.core.Assert;
import ariba.util.core.MapUtil;
import ariba.util.core.ListUtil;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Iterator;

/**
    The Wizard class represents a user's trip through a particular wizard
    instance.  It keeps track of the current step and/or frame, which actions
    are enabled, etc.  It also manages the event handling via the per-frame
    and per-wizard delegate classes provided by the application.

    @aribaapi ariba
*/
public class Wizard implements WizardStepsParent
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

        // generic error messages
    private final static String ErrorDelegateCreation =
        "error creating wizard delegate of type '%s'";
    private final static String NoVisibleStep =
        "no visible frame in current step '%s'";
    private final static String NullCurrentStep =
        "current step should never be null";

        // action error messages
    private final static String InvalidStepFrameAction = "invalid action for step frame";
    private final static String InvalidDialogAction = "invalid action for dialog frame";
    private final static String InvalidActionResult =
        "null action target for action '%s'";

    /** @aribaapi private */
    public final WizardAction next =
        new WizardAction(WizardMeta.NextActionMeta, this, true);

    /** @aribaapi private */
    public final WizardAction prev =
        new WizardAction(WizardMeta.PrevActionMeta, this, true);

    /** @aribaapi private */
    public final WizardAction exit =
        new WizardAction(WizardMeta.ExitActionMeta, this, true);

    /** @aribaapi private */
    public final WizardAction ok =
        new WizardAction(WizardMeta.OkActionMeta, this, true);

    /** @aribaapi private */
    public final WizardAction cancel =
        new WizardAction(WizardMeta.CancelActionMeta, this, false);

    /** @aribaapi private */
    public final WizardAction refresh =
        new WizardAction(WizardMeta.RefreshActionMeta, this, true);


    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/

    private String            _label;
    private String            _commandBar;
    private Object            _context;
    private WizardMeta        _meta;
    private WizardDelegate    _delegate;
    private Map               _actions;
    private List              _stepList;
    private Map               _frames;
    private Map               _steps;
    private WizardFrame       _selectionsFrame;
    private AWResourceManager _resourceManager;
    private Map               _attributes;
    private WizardFrame       _exitFrame;


        // fields that track the runtime state of the wizard
    private WizardActionTarget _currentActionTarget;
    private boolean            _topLevelChanged = true;
    private int                _numVisibleTopLevelSteps;

        // these shouldn't be here since they're UI-related...
    private AWPageCacheMark _pageCacheMark;
    private boolean         _terminated = false;

    /*-----------------------------------------------------------------------
        Static State & Initialization
      -----------------------------------------------------------------------*/
    static
    {
        AWMultiLocaleResourceManager resourceManager = multiLocaleResourceManager();
        String packageName = ClassUtil.stripClassFromClassName(WizardUtil.class.getName());
        resourceManager.registerPackageName(packageName, true);
        AWSemanticKeyProvider.registerClassExtension(WizardAction.class,
                new SemanticKeyProvider_WizardAction());
        AWSemanticKeyProvider.registerClassExtension(WizardStep.class,
                new SemanticKeyProvider_WizardStep());
    }

    private static AWMultiLocaleResourceManager multiLocaleResourceManager ()
    {
        AWConcreteServerApplication application =
            (AWConcreteServerApplication)AWConcreteServerApplication.SharedInstance;

        AWMultiLocaleResourceManager resourceManager = application.resourceManager();
        return resourceManager;
    }


    /*-----------------------------------------------------------------------
        Constructors
      -----------------------------------------------------------------------*/

    /**
        Constructor
        @aribaapi ariba
    */
    public Wizard (String name,
                   Object context,
                   AWResourceManager resourceManager)
    {
        this(name, context, resourceManager, null);
    }

    /**
        Constructor
        @aribaapi private
    */
    public Wizard (String name,
                   Object context,
                   AWResourceManager resourceManager,
                   String extensionDirectory)
    {
            // initialize
        init(name, context, resourceManager, extensionDirectory);
    }

    protected Wizard ()
    {
    }

    protected void init (String name,
                         Object context,
                         AWResourceManager resourceManager,
                         String extensionDirectory)
    {
            // get the resource manager from the session
        if (extensionDirectory != null) {

                // get the resource manager from the session
            AWMultiLocaleResourceManager extensionResourceManager =
                multiLocaleResourceManager();
            extensionResourceManager.registerResourceDirectory(extensionDirectory, null);

            AWMultiLocaleResourceManager multiResMgr =
                ((AWSingleLocaleResourceManager)resourceManager).multiLocaleResourceManager(); // OK

            extensionResourceManager.setNextResourceManager(multiResMgr);
            Locale locale = resourceManager.locale();
            resourceManager = extensionResourceManager.resourceManagerForLocale(locale);
        }
        _resourceManager = resourceManager;
        _commandBar = null;
        _context = context;

            // check to see if debug is turned on, read the meta info
        boolean debug = AWConcreteServerApplication.SharedInstance.isDebuggingEnabled();
        _meta = WizardMeta.loadWizardMeta(name, resourceManager, debug);

            // initialize our delegate
        String delegateName = _meta.delegate();
        if (delegateName != null) {
            _delegate = instantiateWizardDelegate(delegateName);
            Assert.that(_delegate != null, ErrorDelegateCreation, delegateName);
        }

            // initialize our runtime state
        _actions = MapUtil.map();
        _frames  = MapUtil.map();
        _steps   = MapUtil.map();

            // create instances of built-in and custom actions
        buildActions();

            // create runtime representation of the steps
        _stepList = buildSteps(_meta.steps(), this);

            // create dialog frames
        Iterator dialogs = _meta.dialogs();
        while (dialogs.hasNext()) {
            WizardFrameMeta frameMeta = (WizardFrameMeta)dialogs.next();
            WizardFrame frame = new WizardFrame(frameMeta, null, this);
            _frames.put(frame.getName(), frame);
        }

            // create a selections frame if given in the meta
        if (_meta.selectionsFrame() != null) {
            _selectionsFrame = getFrameWithName(_meta.selectionsFrame().name());
            _frames.put(_selectionsFrame.getName(), _selectionsFrame);
        }

            // cache the exit frame
        _exitFrame = new WizardFrame(_meta.exitFrame(), null, this);
    }

    /**
        create action instances for both built-in and
    */
    private void buildActions ()
    {
        _actions.put(next.getName(), next);
        _actions.put(prev.getName(), prev);
        _actions.put(exit.getName(), exit);
        _actions.put(ok.getName(), ok);
        _actions.put(cancel.getName(), cancel);
        _actions.put(refresh.getName(), refresh);

        Iterator actionMetas = _meta.actions();
        while (actionMetas.hasNext()) {
            WizardActionMeta actionMeta = (WizardActionMeta)actionMetas.next();
            WizardAction action = new WizardAction(actionMeta, this);
            _actions.put(action.getName(), action);
        }
    }

    /**
        @aribaapi private
    */
    protected List buildSteps (List stepMetas, WizardStepsParent parent)
    {
        List stepList = ListUtil.list(stepMetas.size());
        WizardStep previousStep = null;
        for (int index = 0; index < stepMetas.size(); index++) {
            WizardStepMeta stepMeta = (WizardStepMeta)stepMetas.get(index);
            WizardStep step = new WizardStep(stepMeta, parent, this);
            _steps.put(step.getName(), step);
            stepList.add(step);
            step.setPreviousSibling(previousStep);
            if (previousStep != null) {
                previousStep.setNextSibling(step);
            }
            WizardFrame frame = step.getFrame();
            if (frame != null) {
                _frames.put(frame.getName(), frame);
            }
            previousStep = step;
        }

        return stepList;
    }


    /*-----------------------------------------------------------------------
        Public Methods - Customization
      -----------------------------------------------------------------------*/
    /**
        @aribaapi ariba
    */
    public void setCommandBar (String commandBar)
    {
        _commandBar = commandBar;
    }

    /**
        @aribaapi ariba
    */
    public String getCommandBar ()
    {
        return _commandBar;
    }


    /*-----------------------------------------------------------------------
        Public Methods - Lookup
      -----------------------------------------------------------------------*/

    /**
        @aribaapi ariba
    */
    public WizardAction getActionWithName (String actionName)
    {
        WizardAction action = (WizardAction)_actions.get(actionName);
        return action;
    }

    /**
        added for use by demoshell
        @aribaapi private
    */
    public void addAction (String name, WizardAction action)
    {
        _actions.put(name, action);
    }

    /**
        @aribaapi ariba
    */
    public WizardFrame getFrameWithName (String frameName)
    {
        WizardFrame frame = (WizardFrame)_frames.get(frameName);
        return frame;
    }

    /**
        @aribaapi ariba
    */
    public WizardStep getStepWithName (String stepName)
    {
        WizardStep step = (WizardStep)_steps.get(stepName);
        return step;
    }

    public WizardFrame getExitFrame ()
    {
        return _exitFrame;
    }

    public void setExitFrame (WizardFrame frame)
    {
        _exitFrame = frame;
    }

    /*-----------------------------------------------------------------------
        Public Methods - State
      -----------------------------------------------------------------------*/
    /**
        @aribaapi ariba
    */
    public Object getContext ()
    {
        return _context;
    }

    /**
        @aribaapi private
    */
    public String getLabel ()
    {
        if (_label == null) {
            _label = localizedStringForKey(_meta.label());
        }
        return _label;
    }

    /**
        @aribaapi private
    */
    public void setLabel (String label)
    {
        _label = label;
    }

    /**
        @aribaapi ariba
    */
    public WizardDelegate getDelegate ()
    {
        return _delegate;
    }

    /**
        @aribaapi private
    */
    public String getSummarySource ()
    {
        return _meta.summary();
    }

    public String getPreTocSource ()
    {
        return _meta.preToc();
    }

    public String getPostTocSource ()
    {
        return _meta.postToc();
    }

    /**
        @aribaapi ariba
    */
    public WizardStep getCurrentStep ()
    {
        WizardFrame frame = getCurrentFrame();
        WizardStep  step = frame.getStep();
        while (step == null && frame != null) {
            frame = frame.getBackFrame();
            step = frame.getStep();
        }
        Assert.that(step != null, NullCurrentStep);
        return step;
    }

    /**
        @aribaapi private
    */
    public String getSelectionsLabel ()
    {
        return _meta.selectionsLabel();
    }

    /**
        @aribaapi private
    */
    public String getSelectionsIcon ()
    {
        return _meta.selectionsIcon();
    }

    /**
        @aribaapi private
    */
    public WizardFrame getSelectionsFrame ()
    {
        return _selectionsFrame;
    }

    /**
        @aribaapi ariba
    */
    public WizardActionTarget getCurrentActionTarget ()
    {
        return _currentActionTarget;
    }

    /**
        @aribaapi ariba
    */
    public WizardFrame getCurrentFrame ()
    {
        return _currentActionTarget != null ?
            _currentActionTarget.getOriginatingFrame():  null;
    }

    /**
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
        @aribaapi ariba
    */
    public Object getAttribute (Object key)
    {
        if (_attributes != null) {
            return _attributes.get(key);
        }
        return null;
    }

    /*-----------------------------------------------------------------------
        Public Methods - Actions
      -----------------------------------------------------------------------*/

    /**
        @aribaapi ariba
    */
    public void start ()
    {
        if (_delegate != null) {
            _delegate.initialize(this);
        }
            // default to starting at the first step, no validation
        if (getCurrentActionTarget() == null) {
            WizardStep firstStep =  (WizardStep)ListUtil.firstElement(_stepList);
            WizardFrame firstFrame = firstStep.getFirstFrameToDisplay();
            if (firstFrame == null) {
                firstStep = firstStep.getNextStepToDisplay();
                Assert.that(firstStep != null, NoVisibleStep);
                firstFrame = firstStep.getFrame();
            }
            setCurrentActionTarget(firstFrame);
        }
    }

    /**
        This take place after the take values phase. So the validation result
        has already been stored in the current frame

        @aribaapi private
    */
    public WizardActionTarget invokeAction (
        WizardAction       action,
        AWRequestContext   requestContext)
    {
        WizardFrame currentFrame = getCurrentFrame();

        if (!currentFrame.isValid() && !action.ignoreValidation()) {
            currentFrame.setShowErrors(true);
            return currentFrame;
        }

        if (currentFrame.isValid()) {
            currentFrame.setShowErrors(false);
        }
        WizardFrameDelegate frameDelegate = currentFrame.getDelegate();
        WizardActionTarget actionTarget = null;

            // see if the frame delegate wants to tell us where to go
        if (frameDelegate != null) {
            actionTarget = frameDelegate.targetForAction(action);
        }

            // see if the wizard delegate wants to tell us where to go
        if (actionTarget == null && _delegate != null) {
            actionTarget = _delegate.targetForAction(action);
        }

            // do default navigation for the actions we know about
        if (actionTarget == null) {
            if (action == next) {
                Assert.that(!currentFrame.isDialogFrame(), InvalidStepFrameAction);
                WizardStep nextStep = getCurrentStep().getNextStepToDisplay();
                Assert.that(nextStep != null, NoVisibleStep);
                actionTarget = nextStep.getFrame();
            }
            else if (action == prev) {
                Assert.that(!currentFrame.isDialogFrame(), InvalidStepFrameAction);
                WizardStep prevStep = getCurrentStep().getPreviousStepToDisplay();
                Assert.that(prevStep != null, NoVisibleStep);
                actionTarget = prevStep.getFrame();
            }
            else if (action == ok || action == cancel) {
                Assert.that(currentFrame.isDialogFrame(), InvalidDialogAction);
                actionTarget = currentFrame.getBackFrame();
            }
            else if (action == exit) {
                actionTarget = _exitFrame;
            }
            else if (action == refresh) {
                actionTarget = currentFrame;
            }
            else {
                String targetFrameName = action.getTarget();
                if (targetFrameName != null) {
                    actionTarget = getFrameWithName(targetFrameName);
                }
            }
        }

        Assert.that(actionTarget != null, InvalidActionResult, action.getName());
        setCurrentActionTarget(actionTarget);
        return actionTarget;
    }

    /**
        Tells the wizard to go to a particular step. It enforces validation.
        In other words, the user will be forced to stay on the current frame
        if there is any error.

        @aribaapi ariba
    */
    public WizardFrame gotoStep (WizardStep step)
    {
        return gotoFrame(step.getFirstFrameToDisplay());
    }

    /**
        Tells the wizard to go to a particular frame. It enforces validation.
        In other words, the user will be forced to stay on the current frame
        if there is any error.

        @aribaapi ariba
    */
    public WizardFrame gotoFrame (WizardFrame frame)
    {
        WizardFrame currentFrame = getCurrentFrame();
        if (currentFrame != null ) {
            if (!currentFrame.isValid()) {
                currentFrame.setShowErrors(true);
                frame = currentFrame;
            }
            else {
                currentFrame.setShowErrors(false);
            }
        }

        setCurrentActionTarget(frame);
        return frame;
    }

    /**
        @aribaapi private
    */
    public void cleanup ()
    {
        _terminated = true;
        // remove wizard frame delegate
        for (Iterator enum_Itr = _frames.values().iterator(); enum_Itr.hasNext();) {
            WizardFrame frame = (WizardFrame)enum_Itr.next();
            frame.cleanup();
        }
        // null out all  wizard ivars
        _delegate = null;
        _actions = null;
        _attributes = null;
        _stepList = null;
        _steps = null;
        _selectionsFrame = null;
        _resourceManager = null;
        _exitFrame = null;
        _currentActionTarget = null;
        _pageCacheMark = null;
        _label = null;
        _commandBar = null;
        _context = null;
        _meta = null;
        _frames = null;
    }

    /**
        @aribaapi private
    */
    public boolean isTerminated ()
    {
        return _terminated;
    }


    /*-----------------------------------------------------------------------
        Public Methods - Dynamic Step/Frame creation
        Implements the WizardStepsParent interface
      -----------------------------------------------------------------------*/
    /**
        @aribaapi ariba
    */
    public List getSteps ()
    {
        return _stepList;
    }

    /**
        @aribaapi ariba
    */
    public void insertStepBefore (WizardStep step, WizardStep beforeStep)
    {
        WizardStepsParent parent = beforeStep.getParent();
        List steps = parent.getSteps();
        insertStepAt(step, steps, steps.indexOf(beforeStep), parent);
    }

    /**
        @aribaapi ariba
    */
    public void insertStepAfter (WizardStep step, WizardStep afterStep)
    {
        WizardStepsParent parent = afterStep.getParent();
        List steps = parent.getSteps();
        insertStepAt(step, steps, steps.indexOf(afterStep)+ 1, parent);
    }

    /**
        @aribaapi ariba
    */
    public void insertStepAt (WizardStep step, int index)
    {
        insertStepAt(step, _stepList, index, this);
    }

    public void updateChildrenVisible () {
        // no-op since Wizard class is always visible.
    }

    private void addStepToCache (WizardStep step)
    {
        _steps.put(step.getName(), step);
        WizardFrame frame = step.getFrame();
        if (frame != null) {
            _frames.put(frame.getName(), frame);
        }
        List substeps = step.getSteps();
        if (substeps != null) {
            int size = substeps.size();
            for (int i = 0; i < size; i++) {
                addStepToCache((WizardStep)substeps.get(i));
            }
        }
    }

    /**
        @aribaapi private
    */
    protected void removeStepFromCache (WizardStep step)
    {
        _steps.remove(step.getName());
        WizardFrame frame = step.getFrame();
        if (frame != null) {
            frame.setStep(null);
            _frames.remove(frame.getName());
        }
        List substeps = step.getSteps();
        if (substeps != null) {
            int size = substeps.size();
            for (int i = 0; i < size; i++) {
                removeStepFromCache((WizardStep)substeps.get(i));
            }
        }
    }

    /**
        @aribaapi private
    */
    protected void insertStepAt (WizardStep step, List steps, int index,
                                 WizardStepsParent parent)
    {
            // insert the new step
        Assert.that(index >= 0 && index <= steps.size(), "invalid index in insertStepAt");
        steps.add(index, step);
        step.setParent(parent);

            // register the steps and frames if the current step is
            // hooked up to the wizard
        WizardStepsParent ancestor = parent;
        while (ancestor != this &&  ancestor != null) {
            ancestor = ((WizardStep)ancestor).getParent();
        }
        if (ancestor == this) {
            addStepToCache(step);
        }

            // make sure next/prev pointers are still correct
        WizardStep stepBefore = null;
        WizardStep stepAfter = null;
        if (index > 0) {
            stepBefore = (WizardStep)steps.get(index-1);
            stepBefore.setNextSibling(step);
            step.setPreviousSibling(stepBefore);
        }
        int indexAfter = index + 1;
        if (indexAfter < steps.size()) {
            stepAfter = (WizardStep)steps.get(indexAfter);
            stepAfter.setPreviousSibling(step);
            step.setNextSibling(stepAfter);
        }

        if (parent == this) {
            setTopLevelChanged(true);
        }
    }

    /**
        @aribaapi ariba
    */
    public void removeStep (WizardStep step)
    {
        WizardStepsParent parent = step.getParent();
        List steps = parent.getSteps();

        int stepIndex = steps.indexOf(step);
        if (stepIndex != -1) {
            int indexBefore = stepIndex - 1;
            int indexAfter  = stepIndex + 1;
            WizardStep stepBefore = null;
            WizardStep stepAfter = null;
            if (indexBefore >= 0) {
                stepBefore = (WizardStep)steps.get(indexBefore);
            }
            if (indexAfter < steps.size()) {
                stepAfter = (WizardStep)steps.get(indexAfter);
                stepAfter.setPreviousSibling(stepBefore);
            }
            if (stepBefore != null) {
                stepBefore.setNextSibling(stepAfter);
            }

            steps.remove(stepIndex);
            removeStepFromCache(step);
        }

        if (parent == this) {
            setTopLevelChanged(true);
        }
    }
    /*-----------------------------------------------------------------------
        Private/Protected Utility Methods
      -----------------------------------------------------------------------*/
    /**
        @aribaapi private
    */
    public AWPageCacheMark getPageCacheMark ()
    {
        return _pageCacheMark;
    }

    /**
        @aribaapi private
    */
    public void setPageCacheMark (AWPageCacheMark pageCacheMark)
    {
        _pageCacheMark = pageCacheMark;
    }

    /**
        @aribaapi private
    */
    protected AWResourceManager resourceManager ()
    {
        return _resourceManager;
    }

    /**
        @aribaapi private
    */
    protected WizardMeta meta ()
    {
        return _meta;
    }

    /**
        @aribaapi private
    */
    protected String localizedStringForKey (String key)
    {
        if (_delegate != null && key != null) {
            return _delegate.localizedStringForKey(key, this);
        }
        return key;
    }

    /**
        @aribaapi private
    */
    protected String urlForResourceNamed (String resource)
    {
        return _resourceManager.urlForResourceNamed(resource);
    }

    /**
        @aribaapi private
    */
    public void setCurrentActionTarget (WizardActionTarget target)
    {
        if (target == _currentActionTarget) {
            return;
        }

        if (target instanceof WizardFrame) {
            WizardFrame frameTarget = (WizardFrame)target;
            if (_currentActionTarget != null) {
                WizardFrame currentFrame =  getCurrentFrame();
                if (shouldSetBackFrame(currentFrame, frameTarget)) {
                    frameTarget.setBackFrame(currentFrame);
                }
            }
            WizardStep step = frameTarget.getStep();
            if (step != null) {
                step.setHasBeenVisited(true);
                prev.setEnabled(step.getPreviousStepToDisplay() != null);
                next.setEnabled(step.getNextStepToDisplay() != null);
            }
        }
        _currentActionTarget = target;
    }

    private boolean shouldSetBackFrame (WizardFrame currentFrame,
                                        WizardFrame targetFrame)
    {
        if (!targetFrame.isDialogFrame()) {
            return false;
        }

        if (!currentFrame.isDialogFrame()) {
            return true;
        }

        WizardFrame backFrame = currentFrame;
        while (backFrame != null) {
            if (backFrame == targetFrame) {
                return false;
            }
            backFrame = backFrame.getBackFrame();
        }
        return true;
    }

    /**
        @aribaapi private
    */
    protected void assignTopLevelStepIndexes ()
    {
        if (_topLevelChanged) {
            int numSteps = _stepList.size();
            int stepIndex = 0;
            for (int i = 0; i < numSteps; i++) {
                WizardStep step = (WizardStep)_stepList.get(i);
                if (step.isVisible()) {
                    step.setIndex(stepIndex);
                stepIndex ++;
                }
                else {
                    step.setIndex(-1);
                }
            }
            _numVisibleTopLevelSteps = stepIndex;
            _topLevelChanged = false;
        }
    }

    /**
        @aribaapi private
    */
    protected void setTopLevelChanged (boolean changed)
    {
        _topLevelChanged = changed;
    }

    /**
        @aribaapi private
    */
    public int getVisibleTopLevelStepSize ()
    {
        assignTopLevelStepIndexes();
        return _numVisibleTopLevelSteps;
    }

    protected WizardDelegate instantiateWizardDelegate
        (String delegateName)
    {
        return (WizardDelegate)ClassUtil.newInstance(delegateName, false);
    }

    protected WizardFrameDelegate instantiateWizardFrameDelegate
        (String frameDelegateName)
    {
        return (WizardFrameDelegate)ClassUtil.newInstance(frameDelegateName, false);
    }

    public boolean allowsClickableSteps ()
    {
        return meta().allowsClickableSteps();
    }

    public boolean showSteps ()
    {
        return meta().showSteps();
    }

    public String getName ()
    {
        return meta().name();
    }
}
