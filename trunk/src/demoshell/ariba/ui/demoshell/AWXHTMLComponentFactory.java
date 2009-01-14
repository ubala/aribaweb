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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/AWXHTMLComponentFactory.java#29 $
*/

package ariba.ui.demoshell;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWTemplateParser;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWComponentDefinition;
import ariba.ui.aribaweb.core.AWTemplate;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWApplication;
import ariba.ui.aribaweb.core.AWHtmlTemplateParser;
import ariba.ui.aribaweb.core.AWGenericElement;
import ariba.ui.aribaweb.core.AWGenericContainer;
import ariba.ui.aribaweb.core.AWElement;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWFileResource;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWNamespaceManager;
import ariba.ui.aribaweb.html.AWBody;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import ariba.ui.wizard.core.*;
import ariba.ui.wizard.component.WizardUtil;
import ariba.ui.table.ResourceLocator;
import ariba.ui.widgets.DocumentHead;
import ariba.ui.groovy.AWXGroovyTag;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AWXHTMLComponentFactory implements AWConcreteApplication.ComponentDefinitionResolver,
        ResourceLocator.Provider
{
    static final String HtmSuffix = ".htm";
    static final String AwzSuffix = ".awz";

    private static AWXHTMLComponentFactory _SharedInstance;
    private GrowOnlyHashtable _pathToDefinition;  // keyed by absolute path
    private GrowOnlyHashtable _definitionToResource;  // component to path where we loaded it
    private AWTemplateParser _templateParser;
    private String _siteRoot;  // root directory for site-relative paths
    private String _docrootDirectory;
    private String _docrootURL;
    private String _docrootRelativeSitePath = null;
    private Map _componentNameAliases = MapUtil.map();

    public static AWXHTMLComponentFactory sharedInstance ()
    {
        if (_SharedInstance == null) {
            _SharedInstance = new AWXHTMLComponentFactory();
            ((AWConcreteApplication)AWConcreteApplication.SharedInstance)._setComponentDefinitionResolver(_SharedInstance);
            ResourceLocator.setProvider(_SharedInstance);
        }
        return _SharedInstance;
    }

    protected AWXHTMLComponentFactory ()
    {
        super();
        _pathToDefinition = new GrowOnlyHashtable();
        _definitionToResource = new GrowOnlyHashtable();
    }

    /**
     * tell us how to alias file paths to paths under the docroot
     * e.g.: setDocRoot("c:/roots/docroot", "/docroot/");
     */
    public void setDocRoot (String filePath, String url)
    {
        _docrootDirectory = filePath;
        _docrootURL = url;
    }

    public String docRootPath ()
    {
        return _docrootDirectory;
    }

    public void setSiteRoot (String siteRoot)
    {
        _siteRoot = siteRoot;
    }

    public String siteRoot ()
    {
        return _siteRoot;
    }

    /**
     * Used to remap component lookups.  For instance:
     * registerComponentAlias.("Main", "/SomePage.htm");
     * will cause SomePage.htm to be used when "Main" is looked up.
     */
    public void registerComponentAlias (String key, String alias)
    {
        _componentNameAliases.put(key, alias);
    }

    public String siteRootUrl ()
    {
        if (_docrootRelativeSitePath == null) {
            // compute part of path under the doc root.
            String relativePath = DemoShellUtil.relativePath(_docrootDirectory, _siteRoot);

            if (relativePath != null) {
                _docrootRelativeSitePath = _docrootURL + relativePath + "/";
                System.out.println("*** Found site root under doc root.  Using resource path: " + _docrootRelativeSitePath);
            } else {
                System.out.println("*** SITE ROOT IS NOT UNDER DOCROOT!  Resources will be served as file:// -- no remote browsing possible");
                System.out.println("*** DocRoot = " +  _docrootDirectory + ",  SiteRoot = " + _siteRoot);

                _docrootRelativeSitePath = "file:" + _siteRoot + "/";
            }
        }
        return _docrootRelativeSitePath;
    }

    public String siteRelativeUrlForFile (File file)
    {
        // compute part of path under the doc root.
        String path = file.getAbsolutePath();
        String relativePath = DemoShellUtil.relativePath(_siteRoot, path);

        if (relativePath == null) {
            throw new AWGenericException("File outside of site root! " + path);
        }
        return siteRootUrl() + relativePath;
    }

    public String docrootRelativeUrlForFile (File file)
    {
        // compute part of path under the doc root.
        String path = file.getAbsolutePath();
        String relativePath = DemoShellUtil.relativePath(_docrootDirectory, path);

        if (relativePath == null) {
            throw new AWGenericException("File outside of docroot! " + path);
        }
        return _docrootURL + relativePath;
    }

    public interface ContentAcceptor
    {
        boolean handleComponentName (String name, String target, AWComponent parent);
    }

    // Should be called for pseudo-actions and anchor tags.
    // This will determine how to activate the given page (either by
    // returning it as a root page or by going up the parent chain and
    // assigning it to an intermediate container
    public AWComponent componentToReturnForRelativePath (String path, AWComponent parentComponent)
    {
        int hashIndex = path.lastIndexOf('#');
        String target = null;
        if (hashIndex > 0) {
            target = path.substring(hashIndex+1, path.length());
            path = path.substring(0, hashIndex);
        }
        return componentToReturnForRelativePath(path, parentComponent, target);
    }
    protected AWComponent componentToReturnForRelativePath (String path, AWComponent parentComponent, String target)
    {
        if (path.endsWith(AwzSuffix) || (path.indexOf(AwzSuffix+"/") > 0)) {
            return createWizardFromDirectory(path, parentComponent);
        }

        // run up the parent chain seeing if we have a component that wants
        // to handle this goto
        AWComponent component = parentComponent;
        while (component != null) {
            if (component instanceof ContentAcceptor) {
                // String absolutePath = fileForRelativePath(path, parentComponent).getAbsolutePath();
                if (((ContentAcceptor)component).handleComponentName(path, target, parentComponent)) {
                    return null;
                }
            }
            component = component.parent();
        }

        component = createComponentForRelativePath(path, parentComponent);

        // if tag ends with #panel then pop it up on the client
        if (target != null && target.equals("panel")) component.setClientPanel(true);

        return component;
    }

    public AWComponent componentToReturnForKeyPath (String keyPath, AWComponent parentComponent)
    {
        boolean isHtm = keyPath.endsWith(HtmSuffix);
        boolean isAwz = keyPath.endsWith(AwzSuffix);
        String suffix = ".htm";  // default, if not .awz
        if (isHtm || isAwz) {
            // strip trailing ".htm"
            int len = keyPath.length();
            suffix = keyPath.substring(len-4, len);
            keyPath = keyPath.substring(0, len-4);
        }
        String path = keyPath.replace('.','/') + suffix;

        Log.demoshell.debug("*** goto: %s --> %s", keyPath, path);

        AWComponent component = componentToReturnForRelativePath(path, parentComponent);
        if (component == null) {
            // try interpreting as an absolute rather than relative reference
            component = componentToReturnForRelativePath("/" + path, parentComponent);
        }
        return component;
    }

    public AWComponent createWizardFromDirectory (String relativeDirPath, AWComponent parentComponent)
    {
        String gotoStepName = null;
        WizardStep gotoStep = null;

        // if we we referenced with path like "Foo.awz/Step3-Blah", then create wizard for Foo.awz,
        // then nav to specified step.
        int awzPos = relativeDirPath.indexOf(AwzSuffix+"/");;
        if (awzPos >= 0) {
            awzPos += AwzSuffix.length()+1;
            gotoStepName = relativeDirPath.substring(awzPos);
            relativeDirPath = relativeDirPath.substring(0, awzPos-1);
        }

        // enumerate files beginning with "step"
        File dir = fileForRelativePath(relativeDirPath, parentComponent);
        if (!dir.isDirectory()) {
            throw new AWGenericException("Cannot create wizard from path which is not a directory: " + relativeDirPath);
        }
        String directory = dir.getAbsolutePath();
        String names[] = dir.list();
        List stepNames = ListUtil.list();
        for (int i=0; i<names.length; i++) {
            String name = names[i];
            if (name.startsWith("Step") && name.endsWith(HtmSuffix)) {
                stepNames.add(name);
            }
        }
        // now sort
        ListUtil.sortStrings(stepNames, true);
        Log.demoshell.debug("### wizard files: %s", stepNames);
        Map context = MapUtil.map();  // we'll give ourselves a Map for context
        Wizard wizard = new Wizard
            ("ariba/ui/demoshell/AWXBlankWizard", context, parentComponent.resourceManager());

        // Make a label by cleaning up the file name
        String wizLabel = DemoShellUtil.removeSuffix(dir.getName(), AwzSuffix).replace('_', ' '); // underbar becomes space
        wizard.setLabel(wizLabel);

// put wizard creation here...
        WizardStep firstStep = (WizardStep)wizard.getSteps().get(0);
        WizardStep lastStep = firstStep;
        WizardStep currentParentStep = null;
        int currentParentNum = -1;

        for (int i=0; i<stepNames.size(); i++) {
            String name = (String)stepNames.get(i);
            String label = labelFromStepName(name);
            File file = new File(directory, name);
            String path = file.getAbsolutePath();

            // this is a parent step if the next step is a child
            boolean isChildStep = substepParentNumber((String)stepNames.get(i)) > 0;
                // defining a new first step
            WizardStep step = new WizardStep(wizard, name, label, currentParentStep);
            WizardFrame frame = null;

            if (!isChildStep) {
                currentParentStep = step;
            }

            // Do parent steps have frames?
            frame = new WizardFrame(wizard, name, label, path);
            step.setFrame(frame);

            if (isChildStep) {
                currentParentStep.insertStepAt(step, currentParentStep.getSteps().size());
                currentParentStep.setFrame(null);
            } else {
                wizard.insertStepAfter(step, lastStep);
                lastStep = step;
            }

            if ((gotoStepName != null) && (name.equals(gotoStepName))) {
                gotoStep = step;
            }

            // we need to give the source a shot at initializing the frame
            AWTemplate template = templateForDefinition(componentDefinitionForFile(file));
            AWXWizardAction.initFrameWithTemplate(frame, template);
        }
        wizard.removeStep(firstStep);

        File exit = new File(directory, "Exit.htm");
        if (exit.exists()) {
            WizardFrame frame = new WizardFrame(wizard, "exit", "Exit", exit.getAbsolutePath());
            wizard.setExitFrame(frame);
        }

        wizard.start();

        if (gotoStep != null) {
            wizard.gotoStep(gotoStep);
        }

        return WizardUtil.createWizardPage(wizard.getCurrentFrame(),
                                           parentComponent.requestContext());
    }

    private static String labelFromStepName (String name)
    {
        // Names are of form "StepXX-Some_Name.htm" --> "Some Name"
        int dashPos = name.indexOf('-');
        if (dashPos < 0) {
            dashPos = 3; // no dash? use the "p" in "Step"
        }
        String label = DemoShellUtil.removeSuffix(name.substring(dashPos+1), HtmSuffix);
        label = label.replace('_', ' '); // underbar becomes space

        return label;
    }

    private static int substepParentNumber (String name)
    {
        // For "Step-X-Some_Name.htm" -> -1
        // For "StepX.Y-Some_Name.htm" --> Y
        int dashPos = name.indexOf('-');
        int dotPos = name.indexOf('.');
        if (dashPos > 0 && (dashPos > dotPos) && name.startsWith("Step")) {
            return Integer.parseInt(name.substring(4, dotPos));
        }
        return -1;
    }

    public AWComponent createComponentForRelativePath (String path, AWComponent parentComponent)
    {
        AWComponent newComponent;
        AWRequestContext requestContext = parentComponent.requestContext();

        AWComponentDefinition componentDefinition = componentDefinitionForRelativePath(path, parentComponent);
        if (componentDefinition == null) {
            // try looking up in component namespace
            newComponent = requestContext.pageWithName(DemoShellUtil.removeSuffix(path, HtmSuffix));
        } else {
            newComponent = createComponentFromDefinition(componentDefinition, requestContext);
        }

        if (newComponent == null) {
            throw new AWGenericException("Unable to locate page with path \"" + path + "\"");
        }
        return newComponent;
    }

    public AWComponent createComponentForAbsolutePath (String path, AWRequestContext requestContext)
    {
        AWComponentDefinition componentDefinition = componentDefinitionForFile(new File(path));
        if (componentDefinition == null) {
            throw new AWGenericException("Unable to locate page with path \"" + path + "\"");
        }
        AWComponent newComponent = createComponentFromDefinition(componentDefinition, requestContext);
        return newComponent;
    }

    public AWComponent createComponentFromDefinition (AWComponentDefinition componentDefinition, AWRequestContext requestContext)
    {
        AWComponent newComponent = componentDefinition.createComponent(componentDefinition.sharedComponentReference(), null, requestContext);
        newComponent.page().ensureAwake(requestContext);
        if (requestContext != null) {
            requestContext.registerNewPageComponent(newComponent);
        }
        return newComponent;
    }

    // hack to lookup defintions for debugging when parent context is no longer known
    // (better would be to always store definitions with the full path *as the name*)
    static Map _DebugDefintionsByRelativePath = MapUtil.map();

    public AWComponentDefinition componentDefinitionForRelativePath(String path, AWComponent parentComponent)
    {
        // if path is relative, figure out the path of the parent and the lookup absolute path
        // FIX ME:  caching is almost certainly in order here -- this full path resolution is expensive.
        File file;
        file = new File(path);
        if (!file.exists()) {
            if (path.startsWith("/") || (parentComponent == null)) {
                // site relative path
                file = new File(_siteRoot, path);
            } else {
                file = fileForRelativePath(path, parentComponent);
            }
        }
        AWComponentDefinition definition = componentDefinitionForFile(file);
        if (definition != null) {
            _DebugDefintionsByRelativePath.put(path, definition);
            _DebugDefintionsByRelativePath.put(file.getName(), definition);
        }
        return definition;
    }

    public File fileForRelativePath (String path, AWComponent parentComponent)
    {
        Assert.that((path != null), "path must be non-null: %s (%s)", path, parentComponent);
        File file = new File(path);
        if (file.isAbsolute() && file.exists()) {
            // we've found it!
        } else if (parentComponent != null) {
            // try to look it up relative to our parent
            String parentPath = directoryOfComponent(parentComponent);

            file = new File(parentPath, path);
        } else {
            file = null;  // can't find it
        }
        return file;
    }

    // called by AWConcreteApplicate.ComponentDefinitionResolver interface
    public AWComponentDefinition definitionWithName(String componentName, AWComponent parent)
    {
        AWComponentDefinition definition = null;

        char firstChar = componentName.charAt(0);
        // match on 1) open references:   "foo"
        //          2) absolute paths "/foo"
        //          3) relative references "substeps/foo"

        if (componentName.lastIndexOf('\\') > 0) {
            componentName = StringUtil.replaceCharByChar(componentName, '\\','/');
        }

        if (!Character.isLetter(firstChar) || Character.isUpperCase(firstChar)
                            || (componentName.lastIndexOf('/') >= 0)) {

            // Aliases -- e.g. we special-case "Main"
            String alias = (String)_componentNameAliases.get(componentName);
            if (alias != null) {
                componentName = alias;
            }

            String name = componentName;
            boolean isExplicitHtm = false;
            if (name.endsWith(HtmSuffix)) {
                isExplicitHtm = true;
            } else {
                name += HtmSuffix;
            }
            if (parent != null) {
                AWComponent htmlParent = htmlTemplateParent(parent);
                definition = componentDefinitionForRelativePath(name, htmlParent);
            } else {
                // absolute path (siteroot relative)
                definition = componentDefinitionForRelativePath(name, null);
                // hack to look for best match if debug pane is calling us back without parent context
                if (definition == null) definition = (AWComponentDefinition)_DebugDefintionsByRelativePath.get(name);
            }
            if ((definition == null) && (isExplicitHtm)) {
                throw new AWGenericException("SwitchComponent -- unable to find file at path: " + name + ", parentComponent="+parent);
            }
            return definition;
        }

        return definition;
    }

    private String directoryOfComponent (AWComponent parentComponent)
    {
        // first see if parent is an HTML component.  If so, we have the path
        AWComponentDefinition parentDefinition = parentComponent.componentDefinition();
        String parentPath = pathForComponentDefinition(parentDefinition);

        if( parentPath == null) {
            // must be a regular AWComponent -- look up the resource by name
            AWFileResource resource =
                (AWFileResource)parentComponent.resourceManager().
                resourceNamed(parentDefinition.templateName());
            parentPath = resource == null ? null : resource._fullPath();
        }

        parentPath = DemoShellUtil.pathByRemovingLastComponent(parentPath); // remove file
        return parentPath;
    }

    public String pathForComponentDefinition (AWComponentDefinition definition)
    {
        _Resource r;
        synchronized (this) {
            r = (_Resource)_definitionToResource.get(definition);
        }
        return (r != null) ?  r.file().getAbsolutePath() : null;
    }

    public AWComponentDefinition componentDefinitionForFile (File file)
    {
        AWComponentDefinition definition;

        synchronized (this) {
            definition = (AWComponentDefinition)_pathToDefinition.get(file.getAbsolutePath());
        }

        if (definition == null) {
            // no cache hit.  Let's try to create this.
            definition = createDefinitionFromFile(file);
        }
        return definition;
    }

    public AWTemplate templateForDefinition (AWComponentDefinition definition) {
        _Resource r = (_Resource)_definitionToResource.get(definition);
        return r.template();
    }

    protected AWComponentDefinition createDefinitionFromFile (File file)
    {
        AWComponentDefinition componentDefinition = null;
        if (!file.exists()) {
            return null;  // throw?
        }

        String path = file.getAbsolutePath();
        String templateName = path;  // ???

        // FIX ME!  not sure if this is right (or if it matters...).
        synchronized (this) {
            componentDefinition = new DynamicComponentDefinition();
            _Resource r = new _Resource();
            _pathToDefinition.put(path, componentDefinition);
            r.setFile(file);
            _definitionToResource.put(componentDefinition, r);
            componentDefinition.init(file.getName(), AWXHTMLComponent.class);
            componentDefinition.setTemplateName(templateName);
        }

        return componentDefinition;
    }

    public interface ScriptClassProvider {
        public Class componentSubclass (File file, AWTemplate template);
    }

    class DynamicComponentDefinition extends AWComponentDefinition
    {
        protected AWTemplate _processedTemplate;

        // deferred scan for our actual subclass -- based on tags in template
        public Class componentClass ()
        {
            _Resource r =(_Resource)_definitionToResource.get(this);
            AWTemplate template = r.template();


            if (template != _processedTemplate) {
                Class cls = componentClassForTemplate(r.file(), r.template());
                setComponentClass(cls);
                _processedTemplate = template;
            }
            return super.componentClass();
        }
    }

    private Class componentClassForTemplate (final File file, final AWTemplate template)
    {
        Class cls = (Class) AWUtil.iterate(template, new AWUtil.ElementIterator() {
            public Object process(AWElement e) {
                if (e instanceof ScriptClassProvider) {
                    return ((ScriptClassProvider)e).componentSubclass(file, template);
                }
                return null;
            }
        });
        return (cls != null) ? cls : AWXHTMLComponent.class;
    }

    private AWTemplate createTemplateFromFile (File file, String templateName)
    {
        AWTemplateParser parser = templateParser();
        FileInputStream stream = null;
        AWTemplate template = null;

        try {
            stream = new FileInputStream(file);
         } catch (FileNotFoundException e) {
            throw new AWGenericException(e);
        }

        template = parser.templateFromInputStream(stream, templateName);

        // force stream closed
        try {
            stream.close();
        } catch (IOException e) {
            throw new AWGenericException(e);
        }

        return template;
    }

    public AWComponent htmlTemplateParent (AWComponent child)
    {
        AWComponent parent = child;

        while ( parent != null) {
            if (parent instanceof AWXHTMLComponent) {
                return parent;
            }
            parent = parent.parent();
        }
        return null;
    }

    public String directoryForParent (AWComponent child)
    {
        AWComponent parent = htmlTemplateParent(child);

        return (parent != null) ? directoryOfComponent(parent) : null;
    }

    AWTemplateParser templateParser()
    {
        if (_templateParser == null) {
            // need to create our own parser that's using aliases
            _templateParser = new AWHtmlTemplateParser();
            _templateParser.init((AWApplication)AWConcreteApplication.sharedInstance());

            AWComponent.defaultTemplateParser().duplicateRegistrationsIntoOther(_templateParser);

            // XXX clloyd: this is really the wrong place to init this stuff, but convenient for now.  I suppose the right place is some sort of config file.
            // craigf:  do we need to do this?
            _templateParser.registerElementClassForTagName("AWTag", AWGenericElement.class);
            _templateParser.registerContainerClassForTagName("AWTag", AWGenericContainer.class);

            // register aliases...
            _templateParser.registerElementClassForTagName("img", AWXImgTag.class);
            _templateParser.registerElementClassForTagName("Img", AWXImgTag.class);
            _templateParser.registerElementClassForTagName("IMG", AWXImgTag.class);

            _templateParser.registerElementClassForTagName("link", AWXLinkTag.class);
            _templateParser.registerElementClassForTagName("Link", AWXLinkTag.class);
            _templateParser.registerElementClassForTagName("LINK", AWXLinkTag.class);

            _templateParser.registerElementClassForTagName("input", AWXInputTag.class);
            _templateParser.registerElementClassForTagName("Input", AWXInputTag.class);
            _templateParser.registerElementClassForTagName("INPUT", AWXInputTag.class);

            _templateParser.registerContainerClassForTagName("form", AWXFormTag.class);
            _templateParser.registerContainerClassForTagName("Form", AWXFormTag.class);
            _templateParser.registerContainerClassForTagName("FORM", AWXFormTag.class);

            _templateParser.registerContainerClassForTagName("script", AWXScriptTag.class);
            _templateParser.registerContainerClassForTagName("Script", AWXScriptTag.class);
            _templateParser.registerContainerClassForTagName("SCRIPT", AWXScriptTag.class);

            _templateParser.registerContainerClassForTagName("A", AWXAnchorTag.class);
            _templateParser.registerContainerClassForTagName("a", AWXAnchorTag.class);

            _templateParser.registerContainerClassForTagName("HEAD", DocumentHead.class);
            _templateParser.registerContainerClassForTagName("head", DocumentHead.class);

            _templateParser.registerContainerClassForTagName("BODY", AWBody.class);
            _templateParser.registerContainerClassForTagName("body", AWBody.class);

            // ToDo:  Loosely bind...
            _templateParser.registerContainerClassForTagName("groovy", AWXGroovyTag.class);
            _templateParser.registerContainerClassForTagName("server", AWXServerScript.class);
            _templateParser.registerContainerClassForTagName("Server", AWXServerScript.class);
            _templateParser.registerContainerClassForTagName("SERVER", AWXServerScript.class);

            // Icky -- swap the component defined by "Widgets"
            _templateParser.registerElementClassForTagName("Include", AWXIncludeTag.class);

            ((AWHtmlTemplateParser)_templateParser).setDefaultResolver(AWNamespaceManager.instance().resolverForPackage("ariba.ui.demoshell"));
        }
        return _templateParser;
    }

    protected class _Resource
    {
        protected AWTemplate _template;
        protected File _file;
        protected long _lastModified;

        public void setFile (File file)
        {
            _file = file;
            _lastModified = file.lastModified();
        }

        public boolean hasChanged ()
        {
            return (AWFileResource.fileLastModified(_file) > _lastModified);
        }

        public File file ()
        {
            return _file;
        }

        public AWTemplate template ()
        {
            if ((_template == null) || (AWConcreteApplication.IsRapidTurnaroundEnabled && hasChanged())) {
                _template = createTemplateFromFile(_file, _file.getAbsolutePath());
                _lastModified = _file.lastModified();
            }
            return _template;
        }
    }
}

