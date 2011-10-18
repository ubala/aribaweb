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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWApi.java#14 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.ListUtil;

import java.util.List;

/**
    <Binding key="leftSideOfBinding" direction="setOrGetOrBoth" type="classOfField" required="$false" alternate="otherKey" default="defaultValueIfNotRequiredAndNotProvided">
        Description in here
    </Binding>

 For the Binding meta tag, the following attributes are allowed/expected:

 key:        [required] appears on the left side of the binding (eg action="$linkClicked" -- here 'action' is the key)
 direction:  [required] either set, get, or both, depending on if the binding is for getting, setting, or both.
 type:       [required] the class that is returned or should be passed when being set/get.
 required:   [alternate:default] true/false or, if not provided, assumed to be false.
 default:    [alternate:required] cannot exist if required=$true, but indicates the default value if required is false.
 alternates: [optional] if not required, then a list alternate keys may be specified in its place,
             but only one from the list may be used.
 deprecated: [optional] defaults to false.  If true, users should not use and will get a deprecated warning.

*/
public final class AWApi extends AWContainerElement
{
    private static final AWBindingApi[]     EmptyBindingApis     = new AWBindingApi[0];
    private static final AWContentApi[]     EmptyContentApis     = new AWContentApi[0];
    private static final AWExampleApi[]     EmptyExampleApis     = new AWExampleApi[0];
    private static final AWIncludeExample[] EmptyIncludeExamples = new AWIncludeExample[0];
    private AWBindingApi[]     _bindingApis     = EmptyBindingApis;
    private AWContentApi[]     _contentApiAWs   = EmptyContentApis;
    private AWExampleApi[]     _exampleApis     = EmptyExampleApis;
    private AWIncludeExample[] _includeExamples = EmptyIncludeExamples;
    private AWElement _overview = null;
    private AWEncodedString _responsible = null;
    private boolean _allowsBindingPassThrough = true;

    public AWBindingApi[] bindingApis ()
    {
        if (_bindingApis == EmptyBindingApis) {
            AWElement contentElement = contentElement();
            if (contentElement instanceof AWTemplate) {
                AWTemplate template = (AWTemplate)contentElement();
                _bindingApis = (AWBindingApi[])template.extractElementsOfClass(AWBindingApi.class);
            }
            else if (contentElement instanceof AWBindingApi) {
                _bindingApis = new AWBindingApi[] {
                    (AWBindingApi)contentElement,
                };
            }
        }
        return _bindingApis;
    }

    public AWContentApi[] contentApis()
    {
        if (_contentApiAWs == EmptyContentApis) {
            AWElement contentElement = contentElement();
            if (contentElement instanceof AWTemplate) {
                AWTemplate template = (AWTemplate)contentElement();
                _contentApiAWs = (AWContentApi[])template.extractElementsOfClass(AWContentApi.class);
            }
            else if (contentElement instanceof AWContentApi) {
                _contentApiAWs = new AWContentApi[] {
                    (AWContentApi)contentElement,
                };
            }
        }
        return _contentApiAWs;
    }

    public AWExampleApi[] exampleApis()
    {
        if (_exampleApis == EmptyExampleApis) {
            AWElement contentElement = contentElement();
            if (contentElement instanceof AWTemplate) {
                AWTemplate template = (AWTemplate)contentElement();
                _exampleApis = (AWExampleApi[])template.extractElementsOfClass(AWExampleApi.class);
            }
            else if (contentElement instanceof AWExampleApi) {
                _exampleApis = new AWExampleApi[] {
                    (AWExampleApi)contentElement,
                };
            }
        }
        return _exampleApis;
    }

    public AWIncludeExample[] includeExamples()
    {
        if (_includeExamples == EmptyIncludeExamples) {
            AWElement contentElement = contentElement();
            if (contentElement instanceof AWTemplate) {
                AWTemplate template = (AWTemplate)contentElement();
                _includeExamples = (AWIncludeExample[])template.extractElementsOfClass(AWIncludeExample.class);
            }
            else if (contentElement instanceof AWIncludeExample) {
                _includeExamples = new AWIncludeExample[] {
                        (AWIncludeExample)contentElement,   
                };
            }
        }
        return _includeExamples;
    }

    public AWGenericContainer locateTagNamed (String tagName)
    {
        AWElement contentElement = contentElement();
        if (contentElement instanceof AWTemplate) {
            AWElement[] elements = ((AWTemplate)contentElement).elementArray();
            for (int index = 0; index < elements.length; index++) {
                AWElement element = elements[index];
                if (element instanceof AWGenericContainer) {
                    AWGenericContainer genericContainer = (AWGenericContainer)element;
                    String currentTagName = genericContainer.tagName();
                    if (tagName.equals(currentTagName)) {
                        return genericContainer;
                    }
                }
            }
        }
        return null;
    }

    public AWElement overview ()
    {
        if (_overview == null) {
            _overview = locateTagNamed("Overview");
        }
        return _overview;
    }

    public AWEncodedString responsible ()
    {
        if (_responsible == null) {
            AWGenericContainer genericContainer = locateTagNamed("Responsible");
            if (genericContainer == null) {
                _responsible = AWEncodedString.sharedEncodedString("unassigned");
            }
            else {
                _responsible = ((AWBareString)genericContainer.contentElement()).encodedString();
            }
        }
        return _responsible;
    }

    protected AWBindingApi getBindingApi (String name)
    {
        AWBindingApi[] bindingApis = bindingApis();
        for (int index = bindingApis.length - 1; index > -1; index--) {
            AWBindingApi bindingApi = bindingApis[index];
            if (bindingApi.key().equals(name)) {
                return bindingApi;
            }
        }
        return null;
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        return null;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
    }

    ////////////////////////
    // component validation
    ////////////////////////
    public boolean validateRequiredBindings (AWValidationContext validationContext, AWComponent component,
                                             AWBindingDictionary bindings)
    {
        // check each binding api to see if it is required
        AWBindingApi[] bindingApis = bindingApis();
        AWComponentDefinition componentDefinition = component.componentDefinition();
        for (int i = 0; i < bindingApis.length; i++) {
            AWBindingApi bindingApi = bindingApis[i];
            String bindingName = bindingApi.key();
            if (bindingApi.isRequired() && bindings.get(bindingName) == null) {
                // required binding not found ... check for alternates
                String[] alternates = bindingApi.alternatesArray();
                if (alternates == null) {
                    // if no alternates, then problem
                    componentDefinition.addMissingRequiredBinding(validationContext, component, bindingName);
                }
                else {
                    // walk through alternates and check for default value or available binding
                    boolean foundAlternate = false;
                    for (int j = 0; j < alternates.length && !foundAlternate; j++) {
                        AWBindingApi alternateBindingApi = getBindingApi(alternates[j]);
                        if (alternateBindingApi != null) {
                            if (alternateBindingApi.defaultValue() != null) {
                                // an alternate provides a default value so we're done
                                foundAlternate = true;
                            }
                            else if (bindings.get(alternateBindingApi.key()) != null) {
                                // an alternate binding is defined
                                foundAlternate = true;
                            }
                        }
                    } // alternate bindings list

                    if (!foundAlternate) {
                        // if no alternates defined, then problem
                        componentDefinition.addMissingRequiredBinding(validationContext, component, bindingName);
                    }
                }
            }
        } // binding Api's loop

        return true;
    }

    ////////////////////////
    // metadata validation
    ////////////////////////
    public boolean allowsPassThrough ()
    {
        // note: this method should only be called after the AWApi has been validated.
        return _allowsBindingPassThrough;
    }

    // Validates
    public void validate (AWValidationContext validationContext, AWComponent component)
    {
        AWBindingApi[] bindingApis = bindingApis();

        // check supported binding list
        validateSupportedBindings(validationContext, component);

        // set pass through
        _allowsBindingPassThrough = (component.supportedBindingNames() != null);

        // validate each binding api
        for (int i = 0; i < bindingApis.length; i++) {
            AWBindingApi bindingApi = bindingApis[i];
            bindingApi.validate(validationContext, component);

            // cross binding validations

            // alternates validation
            String[] alternates = bindingApi.alternatesArray();
            if (alternates != null) {
                verifyMatchingAlternates(validationContext, component, bindingApi, alternates);
            }
        }
    }

    private void validateSupportedBindings (AWValidationContext validationContext, AWComponent component)
    {
        // if a supported bindings exists, then make sure that it matches
        // exactly with the bindings defined in AWApi
        String[] supportedBindings = component.supportedBindingNames();
        if (supportedBindings != null) {
            AWBindingApi[] bindingApis = bindingApis();
            List vSupportedBindings = ListUtil.arrayToList(supportedBindings);

            // make sure that all of the binding api's are in the supported list
            AWComponentDefinition componentDefinition = component.componentDefinition();
            for (int i = 0; i < bindingApis.length; i++) {
                AWBindingApi binding = bindingApis[i];
                // todo: need to handle semantic of "*" binding for real (or eliminate its use)
                if (!vSupportedBindings.remove(binding.key()) && !"*".equals(binding.key())) {
                    // binding found in AWApi that does not exist in supported list
                    componentDefinition.addInvalidComponentApiBindingDefinition(validationContext, binding.key(),
                        AWComponentDefinition.UnsupportedBindingDefinition);
                }
            }
            if (vSupportedBindings.size() != 0) {
                // bindings in supported list but not in AWApi
                for (int i = 0, vSupportedBindingssize = vSupportedBindings.size(); i < vSupportedBindingssize; i++) {
                    String supportedBinding = (String)vSupportedBindings.get(i);
                    // Crazy:  for component subclasses that use their parent's template, they have no
                    // way to document their extra supported bindings, so we let them get away with not doc'ing them.
                    if (componentDefinition.usesOwnTemplate(component)) {
                        componentDefinition.addInvalidComponentApiBindingDefinition(validationContext, supportedBinding,
                                AWComponentDefinition.MissingSupportedBindingDefinition);
                    }
                }
            }
        }
    }

    private void verifyMatchingAlternates (AWValidationContext validationContext, AWComponent component,
                                           AWBindingApi bindingApi, String[] alternates)
    {
        // for each of the strings in the alternate list, get the binding api, then get the alternates
        // from the binding api.  Then compare against current list of alternates.

        // A: B&&C, D, E&&F
        // B: A,D,E&&F
        // C: A,D,E&&F
        // D: A,B&&C,E&&F
        // E: A,B&&C,D
        // F: A,B&&C,D
        for (int i=0; i<alternates.length; i++) {
            String alternateVal = alternates[i];
            String alternateBinding;

            int index = alternateVal.indexOf("&&");
            if (index != -1) {
                String[] multiBinding = AWUtil.parseComponentsString(alternateVal,"&&");
                // ### loop through and call
                for (int j=0; j<multiBinding.length; j++) {
                    matchAlternate(validationContext, component, bindingApi, alternateVal,
                                   multiBinding[i], alternates);
                }
            }
            else {
                alternateBinding = alternateVal;
                matchAlternate(validationContext, component, bindingApi, alternateVal,
                               alternateBinding, alternates);
            }
        }
    }

    private void matchAlternate (AWValidationContext validationContext,
                                 AWComponent component, AWBindingApi bindingApi,
                                 String alternateVal, String alternateBinding,
                                 String[] alternates)
    {
        AWComponentDefinition componentDefinition = component.componentDefinition();

        AWBindingApi alternateBindingApi = getBindingApi(alternateBinding);
        if (alternateBindingApi == null) {
            componentDefinition.addInvalidComponentApiAlternate(validationContext,
                                                                bindingApi.key(),
                                                                alternateBinding);
        }
        else {
            String currBinding = bindingApi.key();
            List alternatesAlternates =
                ListUtil.arrayToList(alternateBindingApi.alternatesArray());
            // make sure the current binding is in alternate's alternates.
            boolean found = alternatesAlternates.remove(currBinding);
            boolean isMultiBinding = false;
            if (!found) {
                // setup for multibinding check
                found = true;
                isMultiBinding = true;
            }
            for (int j=0; found && j < alternates.length; j++) {
                String altName = alternates[j];
                // don't look for myself in my own alternates list
                if (!altName.equals(alternateVal)) {
                    found = alternatesAlternates.remove(altName);
                }
            }

            // if all my bindings were found in my alternateAlternates, and
            // we're in multiBinding, and there is only a single binding left in my
            // alternatesAlternates, then if the one left is a multialternate,
            // make sure one of the multi-bindings is the current binding.
            // case1:
            // currBinding: B  alternateVal: A alternateBinding: A
            // alternatesAlternates: B&&C
            // case2:
            // currBinding: B  alternateVal: E&&F alternateBinding E
            // alternatesAlternates: B&&C
            if (found && isMultiBinding && alternatesAlternates.size()==1) {
                String alternate = (String)alternatesAlternates.get(0);
                if (alternate.indexOf("&&") != -1) {
                    String[] multiBinding = AWUtil.parseComponentsString(alternate,"&&");
                    found = false;
                    for (int i=0; !found && i < multiBinding.length; i++) {
                        found = currBinding.equals(multiBinding[i]);
                    }
                    if (found) {
                        return;
                    }
                }
            }

            // if any alternates in current not found in alternateAlternates or
            // if any alternatesAlternates not found in alternates, then mismatch
            if (!found || alternatesAlternates.size() != 0) {
                // mismatch alternates
                componentDefinition.addMismatchedComponentApiAlternates(
                    validationContext, bindingApi.key(), alternateBinding);
            }
        }
    }
}
