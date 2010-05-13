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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/meta/WizardFrameMeta.java#3 $
*/

package ariba.ui.wizard.meta;

import ariba.ui.aribaweb.util.AWResource;
import ariba.util.core.ArrayUtil;
import ariba.util.core.Assert;
import ariba.util.core.StringUtil;
import ariba.util.core.WrapperRuntimeException;
import ariba.util.core.ListUtil;
import ariba.ui.widgets.XMLUtil;
import java.util.List;
import java.util.Iterator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
    A WizardFrameMeta is the runtime counterpart to an XML definition of a
    wizard frame.  It represents the static information about a frame in a
    wizard flow, i.e. name, label, content source, delegate class, etc.

    @aribaapi private
*/
public final class WizardFrameMeta extends GenericMeta
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

        // values for the 'type' field
    public final static String JspType = "jsp";
    public final static String AWType = "aw";

        // generic wizard frame XML elements
    private static final String FrameElement   = "frame";
    private static final String ActionsElement = "actions";
    private static final String ActionElement  = "action";
    private static final String ContentElement = "content";

        // generic wizard frame XML attributes
    private static final String DelegateAttr = "delegate";
    private static final String DefaultAttr  = "default";
    private static final String TypeAttr     = "type";
    private static final String SourceAttr   = "source";
    private static final String EncodingAttr = "formEncoding";
    private static final String SubmitFormDefaultAttr = "submitFormDefault";

        // extension wizard frame XML elements
    private static final String InFrameElement    = WizardMeta.InFrameElement;
    private static final String DeleteActionElement = WizardMeta.DeleteActionElement;
    private static final String BeforeActionElement = "beforeAction";
    private static final String AfterActionElement  = "afterAction";

        // frame file error messages
    private static final String FileErrorMsg = "error opening wizard frame file %s";

        // element error messages
    private static final String BadActionMsg = "unknown action '%s' found in %s";

        // extension error messages
    private static final String ExtNameMismatchMsg =
        "wizard frame extension name '%s' doesn't match base frame name '%s' in %s";
    private static final String DuplicateActionMsg =
        "action '%s' declared in %s already defined on this frame";

        // validation error messages
    private static final String NoLabelForFrameActionMsg =
        "action '%s' in wizard '%s' used in frame without 'label' attribute defined";

    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/

        // cached resources
    private AWResource   _baseResource;
    private AWResource[] _extResources;

        // our main wizard meta info
    private WizardMeta _wizard;

        // source filename
    private String _filename;

        // our parent step (null for a dialog frame)
    private WizardStepMeta _step;

        // frame delegate class
    private String _delegate;

        // list of custom actions
    private List _actions = ListUtil.list();

        // the default action (highlighted)
    private String _default;

        // frame content source
    private String _source;

        // frame content type
    private String _type;

    private String _formEncoding;

    private boolean _submitFormDefault;

    /*-----------------------------------------------------------------------
        Constructor
      -----------------------------------------------------------------------*/

    /**
        Creates a new WizardFrameMeta from static data for name, label, etc.
        as opposed to reading in that data from XML.
    */
    public WizardFrameMeta (WizardMeta wizard,
                            String name,
                            String label,
                            String source)
    {
        this(wizard, name, label,
             source, null, null,
             null, null);
    }

    /**
        Creates a new WizardFrameMeta from static data for name, label, etc.
        as opposed to reading in that data from XML.
    */
    public WizardFrameMeta (WizardMeta wizard, String name, String label,
                            String source,  String delegate, String type,
                            String formEncoding, String[] actionNames)
    {
            // initialize the generic meta data
        super(name, label);

            // cache our core data
        _wizard = wizard;
        _delegate = delegate;
        _type = type == null ? AWType : type;
        _source = source;
        _formEncoding = formEncoding;
        _submitFormDefault = false;

            // if any actions should be shown for this frame, look each the
            // corresponding WizardActionMeta in the top level list
        if (actionNames != null && actionNames.length > 0) {
            _actions = ListUtil.list();
            for (int i = 0; i < actionNames.length; i++) {
                WizardActionMeta actionMeta = wizard.actionMetaWithName(actionNames[i]);
                assertion(actionMeta != null, BadActionMsg, name);
                _actions.add(actionMeta);
            }
        }

            // no XML in this case, so resources are null
        _baseResource = null;
        _extResources = null;
        _filename = null;
    }

    /**
        Creates a new WizardFrameMeta by reading in data from a wizard frame
        XML document.  This is the normal case, as opposed to instances which
        are created via application code (indirectly via the WizardFrame
        constructor).
    */
    protected WizardFrameMeta (WizardMeta wizard, WizardStepMeta step, Element element)
    {
            // cache our top level meta info
        _wizard = wizard;
        _step = step;

        readWizardFrame(element);
    }

    protected void readWizardFrame (Element frameElement)
    {
            // read the internal frame name attribute
        _name = stringAttrFromElement(frameElement, NameAttr, _name);

            // read the source attribute, load if it is set/changed
        String oldFilename = _filename;
        _filename = stringAttrFromElement(frameElement, SourceAttr, _filename);

        if (_filename != oldFilename) {
                // load the base frame file
            _baseResource = _wizard.loadFrameMetaResource(_filename, false);
            Assert.that(_baseResource != null, FileErrorMsg, _filename);
	    _resource = _baseResource;
	    
		// parse the base frame file
            parseFrameBaseMeta();
        }
    }

    private void parseFrameBaseMeta ()
    {
            // parse the frame XML document
        Document document = createDocumentFromResource(_baseResource, _wizard.resourceManager());

            // get the main frame XML element
        Element frameElement = document.getDocumentElement();
        XMLUtil.expectTag(frameElement, FrameElement);

            // read the wizard frame attributes
        readWizardFrameAttrs(frameElement);

            // read the list of actions (optional)
        readWizardFrameActions(frameElement);

            // read the frame content
        readWizardFrameContent(frameElement);
    }

    protected void readWizardFrameExtensions ()
    {
	/*
	// read any frame extension files that can be found
	_extResources = _wizard.loadFrameMetaResources(_filename, true);
        if (!ArrayUtil.nullOrEmptyArray(_extResources)) {
	try {
	for (int index = 0; index < _extResources.length; index++) {
		applyFrameExtensionMeta(_extResources[index]);
		}
		}
		catch (XMLParseException e) {
		throw new WrapperRuntimeException(e);
		}
		}
	*/
    }

    private void applyFrameExtensionMeta (AWResource frameExtResource)
    {
            // parse the frame extension XML document
        Document document = createDocumentFromResource(frameExtResource, _wizard.resourceManager());

            // get the main 'inFrame' XML element
        Element inFrame = document.getDocumentElement();
        XMLUtil.expectTag(inFrame, InFrameElement);

            // make sure the frame name in the extension matches the base
        String name = stringAttrFromElement(inFrame, NameAttr);
        assertion(_name.equals(name), ExtNameMismatchMsg, name, _name);

            // apply the frame extension attributes
        readWizardFrameAttrs(inFrame);

            // apply the action extensions (optional)
        applyWizardFrameActionExtensions(inFrame);

            // apply the content extension (optional)
        readWizardFrameContent(inFrame);
    }

    private void applyWizardFrameActionExtensions (Element inFrame)
    {
        int count;

            // read the optional 'actions' sub-element
        Element actions = XMLUtil.getOneChildMaybe(inFrame, ActionsElement);
        if (actions == null) {
            return;
        }

            // handle any 'beforeAction' elements
        Element[] beforeActions = XMLUtil.getAllChildren(actions, BeforeActionElement);
        count = (beforeActions == null) ? 0 : beforeActions.length;
        for (int index = 0; index < count; index++) {
            addActionBeforeOrAfter(beforeActions[index], true);
        }

            // handle any 'afterAction' elements
        Element[] afterActions = XMLUtil.getAllChildren(actions, AfterActionElement);
        count = (afterActions == null) ? 0 : afterActions.length;
        for (int index = 0; index < count; index++) {
            addActionBeforeOrAfter(afterActions[index], false);
        }

            // handle any 'deleteAction' elements
        Element[] delActionElements =
            XMLUtil.getAllChildren(actions, DeleteActionElement);
        count = (delActionElements == null) ? 0 : delActionElements.length;
        for (int index = 0; index < count; index++) {
            Element delActionElement = delActionElements[index];
            String delActionName = stringAttrFromElement(delActionElement, NameAttr);
            int actionIndex = indexOfMetaNamed(delActionName, _actions);
            assertion(actionIndex >= 0, WizardMeta.UnknownActionMsg, delActionName);
            _actions.remove(actionIndex);
        }
    }

    private void addActionBeforeOrAfter (Element element, boolean before)
    {
            // get the 'name' attribute from the element
        String relativeTo = stringAttrFromElement(element, NameAttr);

            // look up the named action in our list
        int index = indexOfMetaNamed(relativeTo, _actions);
        assertion(index >= 0, WizardMeta.UnknownActionMsg, relativeTo);

            // read the 'action' sub-element, lookup its meta on the wizard meta
        Element actionElement = XMLUtil.getOneChildMaybe(element, ActionElement);
        String actionName = stringAttrFromElement(actionElement, NameAttr);
        WizardActionMeta actionMeta = _wizard.actionMetaWithName(actionName);
        assertion(actionMeta != null, BadActionMsg, actionName);

            // make sure it's not a duplicate of an action already on this frame
        if (indexOfMetaNamed(actionName, _actions) >= 0) {
            assertion(false, DuplicateActionMsg, actionName);
        }

            // insert the action in the appropriate slot
        int insertAt = (before) ? index : index + 1;
        _actions.add(insertAt, actionMeta);
    }

    private Element readWizardFrameActions (Element frameElement)
    {
            // read the optional 'actions' sub-element
        Element actionsElement = XMLUtil.getOneChildMaybe(frameElement, ActionsElement);
        if (actionsElement == null) {
            return null;
        }

            // read the default action if it's given
        _default = stringAttrFromElement(actionsElement, DefaultAttr, _default);
        if (_default != null) {
            assertion(_wizard.actionMetaWithName(_default) != null, BadActionMsg, _default);
        }

            // read all of the 'action' elements, rendezvous them with
            // the actions that were declared in the main wizard meta
        Element[] actionElements = XMLUtil.getAllChildren(actionsElement, ActionElement);
        for (int index = 0; index < actionElements.length; index++) {
                // get the 'name' attribute from this action
            Element action = actionElements[index];
            String name = stringAttrFromElement(action, NameAttr);
                // lookup this action name on the wizard meta
            WizardActionMeta actionMeta = _wizard.actionMetaWithName(name);
            assertion(actionMeta != null, BadActionMsg, name);
                // add it to our list
            _actions.add(actionMeta);
        }

        return actionsElement;
    }

    private void readWizardFrameContent (Element element)
    {
            // get the 'content' sub-element
        Element contentElement = XMLUtil.getOneChildMaybe(element, ContentElement);
        if (contentElement != null) {
                // read the wizard frame content attributes
            readWizardFrameContentAttrs(contentElement);
        }
    }

    private void readWizardFrameContentAttrs (Element contentElement)
    {
            // required in both base & extensions
        _source = stringAttrFromElement(contentElement, SourceAttr);
        _type   = stringAttrFromElement(contentElement, TypeAttr);
    }

    private void readWizardFrameAttrs (Element element)
    {
            // required in base, optional in extensions
        _label = stringAttrFromElement(element, LabelAttr, _label);

            // optional in both base & extensions
        _delegate = stringAttrFromElement(element, DelegateAttr, _delegate);
        _formEncoding = stringAttrFromElement(element, EncodingAttr, _formEncoding);
        _submitFormDefault = booleanAttrFromElement(element, SubmitFormDefaultAttr, false);

    }

    /**
        @aribaapi private
    */
    protected void validate ()
    {
            // make sure our actions have their button attribute set
        for (int index = _actions.size() - 1; index >= 0; index--) {
            WizardActionMeta actionMeta = (WizardActionMeta)_actions.get(index);
            boolean hasLabel = !StringUtil.nullOrEmptyOrBlankString(actionMeta.label());
            Assert.that(hasLabel, NoLabelForFrameActionMsg, actionMeta.name(), _wizard);
        }
    }


    /*-----------------------------------------------------------------------
        Field Accessors
      -----------------------------------------------------------------------*/

    public WizardMeta wizard ()
    {
        return _wizard;
    }

    public WizardStepMeta step ()
    {
        return _step;
    }

    public void setStep (WizardStepMeta step)
    {
        _step = step;
    }

    public String delegate ()
    {
        return _delegate;
    }

    public String defaultAction ()
    {
        return _default;
    }

    public String formEncoding ()
    {
        return _formEncoding;
    }

    public boolean submitFormDefault ()
    {
        return _submitFormDefault;
    }

    public String source ()
    {
        return _source;
    }

    public String type ()
    {
        return _type;
    }

    public Iterator actions ()
    {
        return _actions.iterator();
    }


    /*-----------------------------------------------------------------------
        Protected Methods
      -----------------------------------------------------------------------*/

    protected boolean resourcesHaveChanged ()
    {
        if (_baseResource.hasChanged()) {
            return true;
        }

            // check all frame extension metas
        if (!ArrayUtil.nullOrEmptyArray(_extResources)) {
            for (int index = 0; index < _extResources.length; index++) {
                if (_extResources[index].hasChanged()) {
                    return true;
                }
            }
        }

        return false;
    }
}
