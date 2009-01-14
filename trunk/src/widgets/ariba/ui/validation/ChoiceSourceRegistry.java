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

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/ChoiceSourceRegistry.java#1 $
*/
package ariba.ui.validation;

import ariba.util.core.ListUtil;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.ClassUtil;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/*
    Most commonly, a generic Provider provider will be registered by the
    persistence layer.  However, a simple implementation could be registered as follows:

         ChoiceSourceRegistry.registerProvider(Boolean.class.getName(),
                new Provider() {
                    public Object choiceSourceForParams (String className, Map params) {
                         return Arrays.asList(true, false);
                    }
                });
 */
public class ChoiceSourceRegistry
{
    /**
        Provider: provide a choice source for a Class
     */
    abstract public static class Provider
    {

        /* Should return null if not applicable for the given params */
        abstract public Object /*ChoiceSource*/ choiceSourceForParams (String className, Map params);
    }


    static GrowOnlyHashtable _ProvidersForClass = new GrowOnlyHashtable();

    public static void registerProvider (String className, Provider provider)
    {
        List providers = (List)_ProvidersForClass.get(className);
        if (providers == null) {
            providers = new ArrayList();
            _ProvidersForClass.put(className, providers);
        }
        providers.add(provider);
    }

    // Search for best matching provider (latest registered, for this class)
    public static Object /*ChoiceSource*/ choiceSourceForClass (String className, Map params)
    {
        String registeredClassName = className;
        while (registeredClassName != null) {
            List<Provider> providers = (List)_ProvidersForClass.get(registeredClassName);
            if (providers != null) {
                // reverse search so get last registered first
                for (int i = providers.size()-1; i >= 0; i--) {
                    Provider p = providers.get(i);
                    Object cs = p.choiceSourceForParams(className, params);
                    if (cs != null) return cs;
                }
            }
            // see if we can find a match on the superclass
            Class cls = ClassUtil.classForName(registeredClassName);
            registeredClassName = ((cls == null) || (cls.getSuperclass() == null))
                    ? null : cls.getSuperclass().getName();
        }
        return null;
    }

    static {
        // List source for Enums
        registerProvider(Enum.class.getName(),
                new Provider() {
                    public Object choiceSourceForParams (String className, Map params) {
                        Class cls = ClassUtil.classForName(className);
                        return (cls != null && cls.isEnum())
                                ? ListUtil.arrayToList(cls.getEnumConstants())
                                : null;
                    }

                });
    }
}
