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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWLocal.java#25 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.ListUtil;
import ariba.ui.aribaweb.util.AWResourceManagerDictionary;
import ariba.ui.aribaweb.util.AWSingleLocaleResourceManager;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWResourceManager;

import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.lang.reflect.Field;
import ariba.util.core.MultiKeyHashtable;
import ariba.util.core.MapUtil;

import javax.servlet.http.HttpSession;

public final class AWLocal extends AWContainerElement
{
    public static boolean IsDebuggingEnabled = false;
    private static MultiKeyHashtable HasLoggedFlags;
    private String _key;
    private AWResourceManagerDictionary _localizedElements = new AWResourceManagerDictionary();

    public void init (
        String tagName,
        Map bindingsHashtable)
    {
        AWBinding keyBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.key);
        if (keyBinding != null) {
            _key = keyBinding.stringValue(null);
        }

        bindingsHashtable.remove(AWBindingNames.comment);

        super.init(tagName, bindingsHashtable);

        AWEncodedString.setDebuggingEnabled(IsDebuggingEnabled);
    }

    public static AWSingleLocaleResourceManager resourceManager (AWRequestContext requestContext)
    {
        AWResourceManager resourceManager = null;
        HttpSession existingHttpSession = (requestContext != null) ? requestContext.existingHttpSession() : null;
        if (existingHttpSession != null) {
            resourceManager = AWSession.session(existingHttpSession).resourceManager();
        }
        else {
            resourceManager = AWConcreteApplication.SharedInstance.resourceManager(Locale.US);
        }

        return AWSingleLocaleResourceManager.ensureSingleLocale(resourceManager);
    }

    public static String localizedJavaString (int key, String originalString, Class cls, AWRequestContext requestContext)
    {
        return ariba.util.i18n.LocalizedJavaString.getLocalizedString(
            cls.getName(),
            key, originalString, resourceManager(requestContext).locale());
    }


    protected static Map loadLocalizedAWLStrings (AWComponent component)
    {
        if (component instanceof AWIncludeBlock) {
            component = component.parent();
        }

        AWStringLocalizer localizer = AWConcreteApplication.SharedInstance.getStringLocalizer();
        return localizer.getLocalizedAWLStrings(component);
    }

    protected static Map loadLocalizedJavaStrings (AWComponent component)
    {
        if (component instanceof AWIncludeBlock) {
            component = component.parent();
        }
        AWStringLocalizer localizer = AWConcreteApplication.SharedInstance.getStringLocalizer();
        return localizer.getLocalizedJavaStrings(component);
    }

    private AWElement localizedElement (AWComponent component)
    {
        boolean UseStaticCache = !AWConcreteApplication.IsRapidTurnaroundEnabled;
        AWSingleLocaleResourceManager resourceManager = (AWSingleLocaleResourceManager)component.resourceManager();
        AWElement localizedElement = (AWElement)_localizedElements.get(resourceManager);
        if (localizedElement == null) {
            synchronized (this) {
                localizedElement = (AWElement)_localizedElements.get(resourceManager);
                if (localizedElement == null) {

                    Map elementCache = null;
                    String stringTableEntry = null;
                    Map localizedStringsHashtable = loadLocalizedAWLStrings(component);
                    if (localizedStringsHashtable != null && _key != null) {
                        if (!UseStaticCache) {
                            elementCache = (Map)localizedStringsHashtable.get("___cache___");
                            if (elementCache != null) {
                                // check if we've inherited some other locales map
                                if (elementCache.get("__RM__") != resourceManager) {
                                    elementCache = null;
                                } else {
                                    localizedElement = (AWElement)elementCache.get(_key);
                                }
                            }

                            if (elementCache == null) {
                                // we put a cache map in the ResourceService strings Map (hack!)
                                // Unfortunately, when en_US is copied to another locale we can inherit that
                                // cache, so we tag the cache and check for it (above)
                                elementCache = MapUtil.map();
                                localizedStringsHashtable.put("___cache___", elementCache);
                                elementCache.put("__RM__", resourceManager);
                            }
                        }
                        if (localizedElement == null) {
                            stringTableEntry = (String)localizedStringsHashtable.get(_key);
                        }
                    } else {
                        // Pseudo localizing?
                        if (resourceManager.pseudoLocalizingAll() && contentElement() != null) {
                            stringTableEntry = localizedStringForElement(contentElement());
                            stringTableEntry = resourceManager.pseudoLocalizeUnKeyed(stringTableEntry);
                        } else {
                            // No key, just use the template content as is
                            localizedElement = contentElement();
                        }
                    }
                    if (localizedElement == null) {
                        if (stringTableEntry != null) {
                            String translatedString = stringTableEntry;
                            // CR 1-3EG7T - This is to make the escaping rule consistent with java.text.MessageFormat
                            if (translatedString.indexOf("{") != -1) {
                                translatedString = translatedString.replaceAll("''", "'");
                            }
                            // Performance: do we really need a new instance each time?
                            AWTemplateParser templateParser = new AWMessageTemplateParser();
                            templateParser.init((AWApplication)AWConcreteApplication.sharedInstance());
                            AWComponent.defaultTemplateParser().duplicateRegistrationsIntoOther(templateParser);
                            AWTemplate localizedTemplate = templateParser.templateFromString(translatedString, component.name());
                            AWElement[] orderedArguments = findOrderedArguments(contentElement());
                            substituteArguments(localizedTemplate, orderedArguments);
                            AWElement[] elementArray = localizedTemplate.elementArray();
                            // This unwraps templates that have a single element in them
                            localizedElement = (elementArray.length == 1) ? elementArray[0] : (AWElement)localizedTemplate;
                            // cache localized string with parsed element for use later during rapid turnaround.
                            if (elementCache != null) elementCache.put(_key, localizedElement);
                        }
                        else {
                            if (HasLoggedFlags == null) {
                                HasLoggedFlags = new MultiKeyHashtable(2);
                            }
                            synchronized (HasLoggedFlags) {
                                if (HasLoggedFlags.get(resourceManager, _key) == null) {
                                    HasLoggedFlags.put(resourceManager, _key, _key);
                                    String errorMessage = getClass().getName() +
                                    ": unable to locate localized string for key \"" + _key + "\" in template: " +
                                            templateName() + ":" + lineNumber();
                                    logString("***** " + errorMessage);
                                }
                            }
                            // rather than throw an exception here, we'll simply pretend like the .strings file was missing.
                            localizedElement = contentElement();
                        }
                    }
                }
                if (UseStaticCache) {
                    // If we do not put the localizedElement in this cache, we will always
                    // get it from the resourceManager's cache (resource.object()).  That way,
                    // if the file is changed and we reload the csv file, we automatically
                    // throw out al the elements derived from that file.  This avoids chache
                    // synchronization/coherency problems during rapidturnaround mode.
                    _localizedElements.put(resourceManager, localizedElement);
                }
            }
        }
        return localizedElement;
    }

    String localizedStringForElement (AWElement element)
    {
        StringBuffer buf = new StringBuffer();
        appendLocalizedString(element, buf, 0);
        return buf.toString();
    }

    int appendLocalizedString(AWElement element, StringBuffer buf, int nextId)
    {
        if (element instanceof AWTemplate) {
            AWElement[] els = ((AWTemplate)element).elementArray();
            for (AWElement e : els) {
                nextId = appendLocalizedString(e, buf, nextId);
            }
        }
        else if (hasDynamicBinding(element)) {
            int id = nextId++;
            if (element instanceof AWContainerElement) {
                buf.append("{" + id + "}");
                nextId = appendLocalizedString(((AWContainerElement)element).contentElement(), buf, nextId);
                buf.append("{/" + id + "}");
            } else {
                buf.append("{" + id + "/}");
            }
        } else {
            element.appendTo(buf);
        }
        return nextId;
    }

    private void substituteArguments (
        AWTemplate localizedTemplate,
        AWElement[] orderedArguments)
    {
        AWElement[] localizedElementArray = localizedTemplate.elementArray();

        for (int i = 0; i < localizedElementArray.length; i++) {

            if (localizedElementArray[i] instanceof AWMessageArgument){
                AWMessageArgument pattern = (AWMessageArgument)localizedElementArray[i];
                int argumentNumber = pattern.argumentNumber();
                if (argumentNumber < orderedArguments.length) {
                    AWElement argument = orderedArguments[argumentNumber];
                    if (argument instanceof AWContainerElement){
                        localizedElementArray[i] = (AWElement)argument.clone();
                        AWContainerElement containerArgument = (AWContainerElement)localizedElementArray[i];
                        AWElement patternContent = pattern.contentElement();
                        containerArgument.setContentElement(patternContent);

                        if (patternContent == null) {
                                // nothing
                        }
                        if (patternContent instanceof AWTemplate) {
                            containerArgument.setContentElement(patternContent);
                            substituteArguments((AWTemplate)patternContent, orderedArguments);
                        }

                        else if (patternContent instanceof AWMessageArgument) {
                            AWMessageArgument contentArgument =  (AWMessageArgument)patternContent;
                            int contentArgumentNumber = contentArgument.argumentNumber();
                            if (contentArgumentNumber < orderedArguments.length) {
                                containerArgument.setContentElement(orderedArguments[contentArgumentNumber]);
                            }
                        }
                    }
                    else {
                        localizedElementArray[i] = argument;
                    }
                }
            }
        }
    }


    /**
        This does a depth first search to find the right argument
    */
    private AWElement[] findOrderedArguments (AWElement root)
    {
        List argumentVector = ListUtil.list();
        addOrderedArguments(root, argumentVector);
        AWElement[] arguments = new AWElement[argumentVector.size()];
        argumentVector.toArray(arguments);
        return arguments;
    }

    private void addOrderedArguments (AWElement parent, List arguments)
    {
        if (parent != null) {
            if (parent instanceof AWTemplate) {
                AWElement[] elementArray = ((AWTemplate)parent).elementArray();
                for (int i = 0; i < elementArray.length; i++) {
                    AWElement element = elementArray[i];
                    if (hasDynamicBinding(element)) {
                        arguments.add(element);
                    }
                    if (element instanceof AWContainerElement) {
                        addOrderedArguments(((AWContainerElement)element).contentElement(), arguments);
                    }
                }
            }
            else if (hasDynamicBinding(parent)) {
                arguments.add(parent);
            }
        }
    }

    private boolean hasDynamicBinding (AWElement element)
    {
        if (!(element instanceof AWBindableElement)) {
            return false;
        }

        AWBindableElement bindableElement = (AWBindableElement)element;
        AWBinding[] bindings = bindableElement.allBindings();
        for (int i = 0; i < bindings.length; i++) {
            AWBinding binding = bindings[i];
            if (binding != null && binding.isDynamicBinding()) {
                return true;
            }
        }
        return false;
    }


    public void applyValues(
        AWRequestContext requestContext,
        AWComponent component)
    {
        AWElement localizedElement = localizedElement(component);
        localizedElement.applyValues(requestContext, component);
    }

    public AWResponseGenerating invokeAction(
        AWRequestContext requestContext,
        AWComponent component)
    {
        AWElement localizedElement = localizedElement(component);
        return localizedElement.invokeAction(requestContext, component);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        AWElement localizedElement = localizedElement(component);

        if (IsDebuggingEnabled) {
            if (contentElement() == localizedElement) {

                // if the localizedElement is the content element, we know that the
                // resource was not localized, and we need to add embedded contextualization
                // since the resource manager will not have had a chance to do it.

                AWResponse response = requestContext.response();
                response.appendContent(AWUtil.getEmbeddedContextBegin(_key, component.namePath()));
                localizedElement.renderResponse(requestContext, component);
                response.appendContent(AWUtil.getEmbeddedContextEnd());
            }
            else {
                localizedElement.renderResponse(requestContext, component);
            }
        }
        else {
            localizedElement.renderResponse(requestContext, component);
        }
    }

    protected Object getFieldValue (Field field)
 	     throws IllegalArgumentException, IllegalAccessException
    {
        try {
            return field.get(this);
        }
        catch (IllegalAccessException ex) {
            return super.getFieldValue(field);
        }
    }
}
