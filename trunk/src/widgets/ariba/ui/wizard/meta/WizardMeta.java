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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/meta/WizardMeta.java#3 $
*/

package ariba.ui.wizard.meta;

import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.util.core.ArrayUtil;
import ariba.util.core.Assert;
import ariba.util.core.MultiKeyHashtable;
import ariba.util.core.StringUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.ui.widgets.XMLUtil;
import java.io.File;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
    WizardMeta is the runtime counterpart to an XML definition of a wizard.
    It represents all the static information about a particular wizard flow,
    i.e. the name of the wizard, the steps and frames used in the process,
    custom actions, etc.

    @aribaapi private
*/
public final class WizardMeta extends GenericMeta
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

        // file extensions for XML files
    public final static String WizardBaseExtension      = ".awz";
    public final static String WizardExtExtension       = ".awx";
    public final static String WizardFrameBaseExtension = ".afr";
    public final static String WizardFrameExtExtension  = ".afx";

        // generic wizard XML elements
    protected static final String WizardElement     = "wizard";
    protected static final String ActionsElement    = "actions";
    protected static final String ActionElement     = "action";
    protected static final String StepsElement      = "steps";
    protected static final String StepElement       = "step";
    protected static final String DialogsElement    = "dialogs";
    protected static final String SelectionsElement = "selections";
    protected static final String ExitElement       = "exit";
    protected static final String FrameElement      = "frame";

        // generic wizard XML attributes
    protected static final String DelegateAttr  = "delegate";
    protected static final String SummaryAttr   = "summary";
    protected static final String PreTocAttr    = "preToc";
    protected static final String PostTocAttr   = "postToc";
    protected static final String IconAttr      = "icon";
    protected static final String FrameAttr     = "frame";
    protected static final String AllowsClickableStepsAttr = "allowsClickableSteps";
    protected static final String ShowStepsAttr = "showSteps";

        // extension wizard XML elements
    protected static final String InWizardElement     = "inWizard";
    protected static final String InActionElement     = "inAction";
    protected static final String DeleteActionElement = "deleteAction";
    protected static final String InStepElement       = "inStep";
    protected static final String BeforeStepElement   = "beforeStep";
    protected static final String AfterStepElement    = "afterStep";
    protected static final String DeleteStepElement   = "deleteStep";
    protected static final String InFrameElement      = "inFrame";
    protected static final String DeleteFrameElement  = "deleteFrame";

        // general purpose names
    protected static final String TrueName = "true";

        // general XML parsing error messages
    protected static final String FileErrorMsg  = "couldn't open wizard file '%s'";
    protected static final String ParseErrorMsg = "error parsing wizard file '%s'";

        // extension attribute error messages
    protected static final String ExtNameMismatchMsg =
        "wizard extension name '%s' doesn't match base name '%s' in %s";
    protected static final String DuplicateActionMsg =
        "action '%s' redeclared in %s";
    protected static final String UnknownActionMsg =
        "unknown action '%s' in %s";
    protected static final String DuplicateFrameMsg =
        "frame '%s' redeclared within '%s' in %s";
    protected static final String UnknownFrameMsg =
        "unknown frame '%s' within '%s' in %s";
    protected static final String UnknownStepMsg =
        "unknown step '%s' within '%s' in %s";

        // validation error messages
    private static final String NoStepFrameMsg =
        "wizard '%s' must have at least one step frame";

        // file and keys for localized string resources
    protected final static String StringsTable        = "ariba.ui.wizard";
    protected final static String StringsGroup        = "WizardMeta";
    protected final static String NextActionHintKey   = "NextActionHint";
    protected final static String PrevActionHintKey   = "PrevActionHint";
    protected final static String ExitActionHintKey   = "ExitActionHint";
    protected final static String OkActionHintKey     = "OkActionHint";
    protected final static String CancelActionHintKey = "CancelActionHint";
    protected final static String NextActionKey       = "NextAction";
    protected final static String PreviousActionKey   = "PreviousAction";
    protected final static String ExitActionKey       = "ExitAction";
    protected final static String OkActionKey         = "OkAction";
    protected final static String CancelActionKey     = "CancelAction";
    protected final static String RefreshActionKey    = "RefreshAction";
    public final static String RefreshActionName = "refresh";


        // static action metas for built-in actions
    public final static WizardActionMeta NextActionMeta =
        new WizardActionMeta("next",
                             NextActionKey,
                             "widg/btn_next",
                             NextActionHintKey);
    public final static WizardActionMeta PrevActionMeta =
        new WizardActionMeta("prev",
                             PreviousActionKey,
                             "widg/btn_prev",
                             PrevActionHintKey,
                             true);
    public final static WizardActionMeta ExitActionMeta =
        new WizardActionMeta("exit",
                             ExitActionKey,
                             "widg/btn_exit",
                             ExitActionHintKey,
                             true);
    public final static WizardActionMeta OkActionMeta =
        new WizardActionMeta("ok",
                             OkActionKey,
                             "widg/btn_ok",
                             OkActionHintKey);
    public final static WizardActionMeta CancelActionMeta =
        new WizardActionMeta("cancel",
                             CancelActionKey,
                             "widg/btn_cancel",
                             CancelActionHintKey,
                             true);
    public final static WizardActionMeta RefreshActionMeta =
        new WizardActionMeta(RefreshActionName, RefreshActionKey, null, null, true);

    private static final Map BuiltInActionMetas = MapUtil.map();

    static {
        BuiltInActionMetas.put(NextActionMeta.name(),    NextActionMeta);
        BuiltInActionMetas.put(PrevActionMeta.name(),    PrevActionMeta);
        BuiltInActionMetas.put(ExitActionMeta.name(),    ExitActionMeta);
        BuiltInActionMetas.put(OkActionMeta.name(),      OkActionMeta);
        BuiltInActionMetas.put(CancelActionMeta.name(),  CancelActionMeta);
        BuiltInActionMetas.put(RefreshActionMeta.name(), RefreshActionMeta);
    }

    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/

        // cached resources
    private AWResource   _baseResource;
    private AWResource[] _extResources;

        // our resource manager
    private AWResourceManager _resMgr;

        // class name of our delegate
    private String _delegate;

        // source for the summary
    private String _summary;

    // source for the pre TOC
    private String _preToc;

    // source for the post TOC
    private String _postToc;

        // list of step definitions
    private List _steps;

        // info for the optional selections link
    private String _selLabel;
    private String _selIcon;
    private WizardFrameMeta _selFrame;

        // the exit confirmation frame
    private WizardFrameMeta _exitFrame;

        // list of dialog frames
    private Map _dialogs =MapUtil.map();

        // list of custom actions
    private Map _actions = MapUtil.map();


    /*-----------------------------------------------------------------------
        Static Methods
      -----------------------------------------------------------------------*/
    
        // cache of wizard metas, keyed by file path & locale
    private static final MultiKeyHashtable _wizards = new MultiKeyHashtable(2);
    private boolean _allowsClickableSteps;
    private boolean _showSteps;

    public static WizardMeta loadWizardMeta (
        String            path,
        AWResourceManager resMgr,
        boolean           debug)
    {
            // see if the meta is already in the cache
        WizardMeta wizardMeta = (WizardMeta)_wizards.get(path, resMgr);


        if (needToLoadWizardMeta(wizardMeta, debug)) {
            synchronized (_wizards) {
                wizardMeta = (WizardMeta)_wizards.get(path, resMgr);
                if (needToLoadWizardMeta(wizardMeta, debug)) {
                        // load the wizard meta XML and cache the results
                    wizardMeta = loadWizardMetaImpl(path, resMgr);
                    _wizards.put(path, resMgr, wizardMeta);
                }
            }
        }

        return wizardMeta;
    }

    private static WizardMeta loadWizardMetaImpl (String path, AWResourceManager resMgr)
    {
            // load the base wizard XML first (required)
        String wizardName = StringUtil.strcat(path, WizardBaseExtension);
        AWResource base = resMgr.resourceNamed(wizardName);
        Assert.that(base != null, FileErrorMsg, wizardName);

            // look for (optional) wizard extension XML resources
        String wizardExtName = StringUtil.strcat(path, WizardExtExtension);
        AWResource[] extensions = resMgr.resourcesNamed(wizardExtName);

            // create the runtime meta from these resources
        return new WizardMeta(base, extensions, resMgr);
    }

    private static boolean needToLoadWizardMeta (WizardMeta meta, boolean debug)
    {
            // always need to load if the wizard meta is null
        if (meta == null) {
            return true;
        }

            // reload the wizard meta if debugging is turned on at the toolkit
            // level and any of the resources for this wizard have changed
        return (debug && meta.resourcesHaveChanged());
    }

    public static String stringsGroup ()
    {
        return StringsGroup;
    }

    /*-----------------------------------------------------------------------
        Constructor
      -----------------------------------------------------------------------*/

    protected WizardMeta (
        AWResource        base,
        AWResource[]      extensions,
        AWResourceManager resMgr)
    {
            // `cache our resource manager for loading frame files
        _resMgr = resMgr;

            // cache our base and extension resources
        _baseResource = base;
        _extResources = extensions;

            // parse the base wizard and frame XML files
        parseWizardBaseMeta();

            // overlay any extension meta information
        if (!ArrayUtil.nullOrEmptyArray(_extResources)) {
            for (int index = 0; index < _extResources.length; index++) {
                applyWizardExtensionMeta(_extResources[index]);
            }
        }

            // apply any extensions for dialog, exit frames
        Iterator dialogs = _dialogs.values().iterator();
        while (dialogs.hasNext()) {
            WizardFrameMeta frameMeta = (WizardFrameMeta)dialogs.next();
            frameMeta.readWizardFrameExtensions();
        }
        _exitFrame.readWizardFrameExtensions();

            // apply any extensions for step frames
        readStepFrameExtensions(_steps);

            // validate the final meta-data
        validate();
    }

    private void parseWizardBaseMeta ()
    {
            // open the base XML wizard file and parse it
        Document document = createDocumentFromResource(_baseResource, _resMgr);

            // get the main wizard XML element
        Element wizard = document.getDocumentElement();
        XMLUtil.expectTag(wizard, WizardElement);

            // read the wizard attributes
        readWizardAttrs(wizard);

            // read the list of custom actions (optional)
        readWizardActions(wizard);

            // read the list of steps (required)
        _steps = readWizardSteps(wizard, null, true);

            // read the list of dialog frames (optional)
        readWizardDialogFrames(wizard);

            // read the selections frame (optional)
        readWizardSelectionsFrame(wizard);

            // read the exit confirmation frame (required)
        readWizardExitFrame(wizard);
    }

    private void applyWizardExtensionMeta (AWResource extension)
    {
            // open the extension XML wizard file and parse it
        Document document = createDocumentFromResource(extension, _resMgr);

            // get the main 'inWizard' XML element
        Element inWizard = document.getDocumentElement();
        XMLUtil.expectTag(inWizard, InWizardElement);

            // make sure the wizard name in the extension matches the base
        String name = stringAttrFromElement(inWizard, NameAttr);
        assertion(_name.equals(name), ExtNameMismatchMsg, name, _name);

            // apply the wizard extension attributes
        readWizardAttrs(inWizard);

            // apply the action extensions (optional)
        applyWizardActionExtensions(inWizard);

            // apply the steps extensions (optional)
        applyWizardStepsExtensions(inWizard, null, _steps);

            // apply the dialog extensions (optional)
        applyWizardDialogExtensions(inWizard);

            // apply the selections frame extensions (optional)
        applyWizardSelectionsExtensions(inWizard);

            // apply the exit confirmation extensions (optional)
        applyWizardExitExtensions(inWizard);
    }

    private void applyWizardActionExtensions (Element inWizard)
    {
        int count;

            // first read the 'actions' element and add any new actions
        Element actions = readWizardActions(inWizard);

            // next deal with 'inAction' elements
        Element[] inActionElements = XMLUtil.getAllChildren(actions, InActionElement);
        count = (inActionElements == null) ? 0 : inActionElements.length;
        for (int index = 0; index < count; index++) {
            Element inActionElement = inActionElements[index];
            String inActionName = stringAttrFromElement(inActionElement, NameAttr);
            WizardActionMeta actionMeta = (WizardActionMeta)_actions.get(inActionName);
            assertion(actionMeta != null, UnknownActionMsg, inActionName);
            actionMeta.readWizardActionAttrs(inActionElement);
        }

            // finally, handle 'deleteAction' elements
        Element[] delActionElements =
            XMLUtil.getAllChildren(actions, DeleteActionElement);
        count = (delActionElements == null) ? 0 : delActionElements.length;
        for (int index = 0; index < count; index++) {
            Element delActionElement = delActionElements[index];
            String delActionName = stringAttrFromElement(delActionElement, NameAttr);
            WizardActionMeta actionMeta = (WizardActionMeta)_actions.get(delActionName);
            assertion(actionMeta != null, UnknownActionMsg, delActionName);
            _actions.remove(delActionName);
        }
    }

    private void applyWizardDialogExtensions (Element inWizard)
    {
        int count;

            // first read the 'dialogs' element and add any new dialog frames
        Element dialogs = readWizardDialogFrames(inWizard);

            // next deal with 'inFrame' elements
        Element[] inFrameElements = XMLUtil.getAllChildren(dialogs, InFrameElement);
        count = (inFrameElements == null) ? 0 : inFrameElements.length;
        for (int index = 0; index < count; index++) {
            Element inFrameElement = inFrameElements[index];
            String inFrameName = stringAttrFromElement(inFrameElement, NameAttr);
            WizardFrameMeta frameMeta = (WizardFrameMeta)_dialogs.get(inFrameName);
            assertion(frameMeta != null, UnknownFrameMsg, inFrameName, DialogsElement);
            frameMeta.readWizardFrame(inFrameElement);
        }

            // finally, handle 'deleteFrame' elements
        Element[] delFrameElements = XMLUtil.getAllChildren(dialogs, DeleteFrameElement);
        count = (delFrameElements == null) ? 0 : delFrameElements.length;
        for (int index = 0; index < count; index++) {
            Element delFrameElement = delFrameElements[index];
            String frameName = stringAttrFromElement(delFrameElement, NameAttr);
            WizardFrameMeta frameMeta = (WizardFrameMeta)_dialogs.get(frameName);
            assertion(frameMeta != null, UnknownFrameMsg, frameName, DialogsElement);
            _dialogs.remove(frameName);
        }
    }

    private void applyWizardSelectionsExtensions (Element inWizard)
    {
            // the base code works for extensions as well
        readWizardSelectionsFrame(inWizard);
    }

    private void applyWizardExitExtensions (Element inWizard)
    {
            // the base code works for extensions as well
        readWizardExitFrame(inWizard);
    }

    private void readWizardAttrs (Element element)
    {
            // required in both base & extensions
        _name = stringAttrFromElement(element, NameAttr, _name);

            // required in base, optional in extensions
        _label = stringAttrFromElement(element, LabelAttr, _label);

            // optional in both base & extensions
        _delegate = stringAttrFromElement(element, DelegateAttr, _delegate);
        _summary  = stringAttrFromElement(element, SummaryAttr, _summary);
        _preToc   = stringAttrFromElement(element, PreTocAttr, _preToc);
        _postToc  = stringAttrFromElement(element, PostTocAttr, _postToc);
        _allowsClickableSteps = booleanAttrFromElement(element,
                                                       AllowsClickableStepsAttr,
                                                       true);
        _showSteps = booleanAttrFromElement(element, ShowStepsAttr, true);
    }

    private Element readWizardActions (Element parentElement)
    {
            // read the 'actions' sub-element
        Element actionsElement = XMLUtil.getOneChildMaybe(parentElement, ActionsElement);
        if (actionsElement == null) {
            return null;
        }

            // read all of the 'action' elements
        Element[] actionElems = XMLUtil.getAllChildren(actionsElement, ActionElement);
        for (int index = 0; index < actionElems.length; index++) {
            WizardActionMeta actionMeta = new WizardActionMeta(this, actionElems[index]);
            String name = actionMeta.name();
            assertion(actionMetaWithName(name) == null, DuplicateActionMsg, name);
            _actions.put(actionMeta.name(), actionMeta);
        }

        return actionsElement;
    }

    private void readWizardSelectionsFrame (Element element)
    {
            // look for the optional 'selections' sub-element
        Element selections = XMLUtil.getOneChildMaybe(element, SelectionsElement);
        if (selections == null) {
            return;
        }

            // read the selections attributes (required in base, optional in extensions)
        _selLabel = stringAttrFromElement(selections, LabelAttr, _selLabel);
        _selIcon  = stringAttrFromElement(selections, IconAttr, _selIcon);

            // read the selections frame name
        String selectionsFrame = stringAttrFromElement(selections, FrameAttr);
        if (!StringUtil.nullOrEmptyOrBlankString(selectionsFrame)) {
                // look up the frame in our list of dialog frames
            _selFrame = (WizardFrameMeta)_dialogs.get(selectionsFrame);
            assertion(_selFrame != null, UnknownFrameMsg, _selFrame, SelectionsElement);
        }
    }

    private Element readWizardDialogFrames (Element element)
    {
            // look for the 'dialogs' sub-element
        Element dialogsElement = XMLUtil.getOneChildMaybe(element, DialogsElement);
        if (dialogsElement == null) {
            return null;
        }

            // read the 'frame' elements, create new wizard frame metas
        readWizardFrames(dialogsElement, null, _dialogs);

        return dialogsElement;
    }

    private void readWizardExitFrame (Element element)
    {
            // look for the optional 'exit' sub-element
        Element exitElement = XMLUtil.getOneChildMaybe(element, ExitElement);
        if (exitElement == null) {
            return;
        }

            // read the meta for the exit frame
        _exitFrame = readWizardFrames(exitElement, null, null);
    }

    private void readStepFrameExtensions (List steps)
    {
        if (!ListUtil.nullOrEmptyList(steps)) {
            for (int index = steps.size() - 1; index >= 0; index--) {
                    // read frame extensions for this step frame
                WizardStepMeta stepMeta = (WizardStepMeta)steps.get(index);
                WizardFrameMeta frameMeta = stepMeta.frame();
                if (frameMeta != null) {
                    frameMeta.readWizardFrameExtensions();
                }
                    // recurse
                readStepFrameExtensions(stepMeta.substeps());
            }
        }
    }

    /**
        Checks that the final form of our meta-data is valid.  The various
        wizard DTDs help us get close, but extensions can still do things that
        are valid but which leave the meta-data as a whole in an invalid
        state.  For example, multiple extensions might be applied which end up
        leaving the wizard without any steps, which is not allowed.
    */
    private void validate ()
    {
        boolean hasStepFrame = WizardStepMeta.validate(_steps, false);
        Assert.that(hasStepFrame, NoStepFrameMsg, _name);
    }


    /*-----------------------------------------------------------------------
        Field Accessors
      -----------------------------------------------------------------------*/

    public String delegate ()
    {
        return _delegate;
    }

    public String summary ()
    {
        return _summary;
    }

    public String preToc ()
    {
        return _preToc;
    }

    public String postToc ()
    {
        return _postToc;
    }

    public boolean allowsClickableSteps ()
    {
        return _allowsClickableSteps;
    }

    public boolean showSteps ()
    {
        return _showSteps;
    }

    public Iterator actions ()
    {
        return (_actions == null) ? null : _actions.values().iterator();
    }

    public List steps ()
    {
        return _steps;
    }

    public Iterator dialogs ()
    {
        return (_dialogs == null) ? null : _dialogs.values().iterator();
    }

    public String selectionsLabel ()
    {
        return _selLabel;
    }

    public String selectionsIcon ()
    {
        return _selIcon;
    }

    public WizardFrameMeta selectionsFrame ()
    {
        return _selFrame;
    }

    public WizardFrameMeta exitFrame ()
    {
        return _exitFrame;
    }


    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/

    public WizardFrameMeta frameMetaWithName (String name)
    {
            // look for a step frame by that name
        for (int index = 0, count = _steps.size(); index < count; index++) {
            WizardStepMeta stepMeta = (WizardStepMeta)_steps.get(index);
            if (stepMeta.frame().name().equals(name)) {
                stepMeta.frame();
            }
        }

            // otherwise return the named dialog (or null)
        WizardFrameMeta dialog = (WizardFrameMeta)_dialogs.get(name);
        return dialog;
    }

    public WizardActionMeta actionMetaWithName (String name)
    {
        WizardActionMeta meta = (WizardActionMeta)_actions.get(name);
        if (meta != null) {
            return meta;
        }
        else {
            return (WizardActionMeta)BuiltInActionMetas.get(name);
        }
    }

    /*-----------------------------------------------------------------------
        Protected Methods
      -----------------------------------------------------------------------*/

    protected List readWizardSteps (
        Element        parentElement,
        WizardStepMeta parentStep,
        boolean        required)
    {
            // read the 'steps' element
        Element stepsElement = XMLUtil.getOneChildMaybe(parentElement, StepsElement);

            // read all of the 'step' elements
        Element[] elements = XMLUtil.getAllChildren(stepsElement, StepElement);
        int count = elements.length;
        List steps = ListUtil.list(count);
        for (int index = 0; index < count; index++) {
            steps.add(new WizardStepMeta(this, elements[index]));
        }

        return steps;
    }

    protected void applyWizardStepsExtensions (
        Element        parentElement,
        WizardStepMeta parentStep,
        List           steps)
    {
        int count;

            // read the 'steps' element
        Element stepsElement = XMLUtil.getOneChildMaybe(parentElement, StepsElement);
        if (stepsElement == null) {
            return;
        }

            // handle any 'beforeStep' elements
        Element[] beforeStepElems =
            XMLUtil.getAllChildren(stepsElement, BeforeStepElement);
        count = (beforeStepElems == null) ? 0 : beforeStepElems.length;
        for (int index = 0; index < count; index++) {
            addStepBeforeOrAfter(beforeStepElems[index], steps, true);
        }

            // handle any 'afterStep' elements
        Element[] afterStepElems = XMLUtil.getAllChildren(stepsElement, AfterStepElement);
        count = (afterStepElems == null) ? 0 : afterStepElems.length;
        for (int index = 0; index < count; index++) {
            addStepBeforeOrAfter(afterStepElems[index], steps, false);
        }

            // handle 'deleteStep' elements
        Element[] deleteStepElems =
            XMLUtil.getAllChildren(stepsElement, DeleteStepElement);
        count = (deleteStepElems == null) ? 0 : deleteStepElems.length;
        for (int index = 0; index < count; index++) {
            Element deleteStepElement = deleteStepElems[index];
            String stepName = stringAttrFromElement(deleteStepElement, NameAttr);
            int stepIndex = indexOfMetaNamed(stepName, steps);
            assertion(stepIndex >= 0, UnknownStepMsg, stepName, DeleteStepElement);
            steps.remove(stepIndex);
        }

            // handle 'inStep' elements
        Element[] inStepElements = XMLUtil.getAllChildren(stepsElement, InStepElement);
        count = (inStepElements == null) ? 0 : inStepElements.length;
        for (int index = 0; index < count; index++) {
            Element inStepElement = inStepElements[index];
            String stepName = stringAttrFromElement(inStepElement, NameAttr);
            int stepIndex = indexOfMetaNamed(stepName, steps);
            assertion(stepIndex >= 0, UnknownStepMsg, stepName, DeleteStepElement);
            WizardStepMeta stepMeta = (WizardStepMeta)steps.get(stepIndex);
            stepMeta.readInStepSubElements(inStepElement);
        }
    }

    private void addStepBeforeOrAfter (
        Element        element,
        List           steps,
        boolean        before)
    {
            // get the 'name' attribute from the element
        String stepName = stringAttrFromElement(element, NameAttr);

            // look up the named step in the list
        int stepIndex = indexOfMetaNamed(stepName, steps);
        assertion(stepIndex >= 0, UnknownStepMsg, stepName, elementName(element));

            // get the 'step' sub-element, create the WizardStepMeta
        Element stepElement = XMLUtil.getOneChildMaybe(element, StepElement);
        WizardStepMeta stepMeta = new WizardStepMeta(this, stepElement);

            // insert the new step meta before/after the given step
        int insertAt = (before) ? stepIndex : stepIndex + 1;
        steps.add(insertAt, stepMeta);
    }

    protected WizardFrameMeta readWizardFrames (
        Element        parentElement,
        WizardStepMeta parentStep,
        Map            frames)
    {
        Element[] frameElements = XMLUtil.getAllChildren(parentElement, FrameElement);

            // read the list of frames, create metas
        WizardFrameMeta frameMeta = null;
        for (int index = 0; index < frameElements.length; index++) {
            frameMeta = new WizardFrameMeta(this, parentStep, frameElements[index]);
            if (frames != null) {
                Object old = frames.put(frameMeta.name(), frameMeta);
                assertion(old == null, frameMeta.name(), elementName(parentElement));
            }
        }

            // a little weird, but we return the first one as a convenience
            // for WizardStepMeta, since there will only be one in that case
        return frameMeta;
    }

    protected AWResource loadFrameMetaResource (String path, boolean extension)
    {
	// put the appropriate directory path in front of the frame name
        File file = new File(_baseResource.name());
        String wizardDir = file.getParent();
        if (!StringUtil.nullOrEmptyOrBlankString(wizardDir)) {
            wizardDir = StringUtil.strcat(wizardDir, File.separator);
        }

	// get the right file extension
        String ext = (extension) ? WizardFrameExtExtension : WizardFrameBaseExtension;
	
	// look up the frame resource
        String name = StringUtil.strcat(wizardDir, path, ext);
	//boolean cacheEnabled = _resMgr.cacheEnabled();
	//_resMgr.setCacheEnabled(false);
        //ariba.ui.aribaweb.util.AWMultiLocaleResourceManager.enableFailedResourceLookupLogging();        
	AWResource resource = _resMgr.resourceNamed(name);
	//ariba.ui.aribaweb.util.AWMultiLocaleResourceManager.disableFailedResourceLookupLogging();        
	//_resMgr.setCacheEnabled(cacheEnabled);
	Assert.that(resource != null, "resource lookup failed for %s", name);
	return resource;
    }

    protected AWResourceManager resourceManager ()
    {
        return _resMgr;
    }


    /*-----------------------------------------------------------------------
        Private Methods
      -----------------------------------------------------------------------*/

    private boolean resourcesHaveChanged ()
    {
            // see if the base meta resource has changed
        if (_baseResource.hasChanged()) {
            return true;
        }

            // check all wizard extension metas
        if (!ArrayUtil.nullOrEmptyArray(_extResources)) {
            for (int index = 0; index < _extResources.length; index++) {
                if (_extResources[index].hasChanged()) {
                    return true;
                }
            }
        }

            // check all dialog frames
        Iterator dialogs = dialogs();
        while (dialogs.hasNext()) {
            WizardFrameMeta dialog = (WizardFrameMeta)dialogs.next();
            if (dialog.resourcesHaveChanged()) {
                return true;
            }
        }

            // check all steps frames (recursively)
        for (int index = 0, count = _steps.size(); index < count; index++) {
            if (((WizardStepMeta)_steps.get(index)).resourcesHaveChanged()) {
                return true;
            }
        }

        return false;
    }
}
