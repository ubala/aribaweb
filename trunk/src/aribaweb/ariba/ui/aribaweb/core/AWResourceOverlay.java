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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWResourceOverlay.java#5 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.List;
import java.util.Iterator;

/*
    Used to temporarily push one or more ResourceService resource file overlays onto the current thread
    state to affect the content resource lookups in the content of the tag.  E.g.
        <AWResourceOverlay aml.sourcing.Core="aml.sourcing.Core.RFP">\
            .. content here (including subcomponents, etc)....
        </AWResourceOverlay>\

    It is also possible to call this method and pass a map of overlays via the overlayMap binding
    where the Map's key is the original file and the Map's value is the overlay file
        <AWResourceOverlay overlayMap="$mapOfOverlays">\
            .. content here (including subcomponents, etc)....
        </AWResourceOverlay>\

 */
public final class AWResourceOverlay extends AWContainerElement
{
    AWBindingDictionary _bindings;
    // ** Thread Safety Considerations: Subcomponents have no threading issues, especially this one since there are no ivars.
    private static final String NoneMarker = "##NONE##";

    private static final String MapBinding = "overlayMap";

    public void init (String tagName, Map bindingsHashtable)
    {
        _bindings = AWBinding.bindingsDictionary(bindingsHashtable);
        super.init(tagName, null);
    }

    public AWBinding[] allBindings ()
    {
        AWBinding[] superBindings = super.allBindings();
        java.util.List bindingVector = _bindings.elementsVector();
        AWBinding[] myBindings = new AWBinding[bindingVector.size()];
        bindingVector.toArray(myBindings);
        return (AWBinding[])(AWUtil.concatenateArrays(superBindings, myBindings));
    }

    protected List pushBindingValues (AWComponent component)
    {
        AWBindingDictionary bindings = _bindings;
        List orig = ListUtil.list(bindings.size());
        for (int index = bindings.size() - 1; index >= 0; index--) {
            AWBinding currentBinding = bindings.elementAt(index);
            String currentBindingName = bindings.keyAt(index);
            if (MapBinding.equals(currentBindingName)) {
                Map bindingMap = (Map)currentBinding.value(component);
                if (!MapUtil.nullOrEmptyMap(bindingMap)) {
                    List mapOrig = ListUtil.list(bindingMap.keySet().size());
                    Iterator iter = bindingMap.keySet().iterator();
                    while (iter.hasNext()) {
                        String key = (String)iter.next();
                        String overlay = (String)bindingMap.get(key);
                        if (overlay != null) {
                            mapOrig.add(ResourceService.pushOverlayForTable(key,
                                overlay));
                        }
                        else {
                            mapOrig.add(NoneMarker);
                        }
                    }
                    orig.add(mapOrig);
                }
                else {
                    orig.add(NoneMarker);
                }
            }
            else {
                String currentValue = currentBinding.stringValue(component);
                if (currentValue != null) {
                    orig.add(ResourceService.pushOverlayForTable(currentBindingName,
                        currentValue));
                }
                else {
                    orig.add(NoneMarker);
                }
            }
        }
        return orig;
    }

    protected void popBindingValues (List orig, AWComponent component)
    {
        AWBindingDictionary bindings = _bindings;
        for (int index = bindings.size() - 1; index >= 0; index--) {
            String currentBindingName = bindings.keyAt(index);
            if (MapBinding.equals(currentBindingName)) {
                AWBinding currentBinding = bindings.elementAt(index);
                Map bindingMap = (Map)currentBinding.value(component);
                if (!MapUtil.nullOrEmptyMap(bindingMap)) {
                    List mapOrig = (List)orig.get(index);
                    Iterator iter = bindingMap.keySet().iterator();
                    int counter = 0;
                    while (iter.hasNext()) {
                        String key = (String)iter.next();
                        String original = (String)mapOrig.get(counter++);
                        if (NoneMarker != original) {
                            ResourceService.popOverlayForTable(key, original);
                        }
                    }
                }
            }
            else if (NoneMarker != orig.get(index)) {
                ResourceService.popOverlayForTable(currentBindingName,
                    (String)orig.get(index));
            }
        }
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        List orig = pushBindingValues(component);
        try {
            super.renderResponse(requestContext, component);
        }
        finally {
            popBindingValues(orig, component);
        }
    }

    public void applyValues(AWRequestContext requestContext,
                                       AWComponent component)
    {
        List orig = pushBindingValues(component);
        try {
            super.applyValues(requestContext, component);
        }
        finally {
            popBindingValues(orig, component);
        }
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext,
                                                        AWComponent component)
    {
        List orig = pushBindingValues(component);
        AWResponseGenerating actionResults = null;
        try {
            actionResults = super.invokeAction(requestContext, component);
        }
        finally {
            popBindingValues(orig, component);
        }
        return actionResults;
    }

    // Copied from AWEnvironment -- why do we need this?
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
