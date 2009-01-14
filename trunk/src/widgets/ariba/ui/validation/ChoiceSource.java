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

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/ChoiceSource.java#1 $
*/
package ariba.ui.validation;

import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;
import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import ariba.ui.widgets.ChooserSelectionSource;
import java.util.List;

/**
    ChoiceSource: implements a chooser source abstraction on
    various types (Lists, ChooserSelectionSources, etc)
 */
abstract public class ChoiceSource extends ClassExtension
{
    private static ClassExtensionRegistry _registry = null;

    /**
        Register a provider implementation for a given class (and, if not overridden,
        its subclasses).
    */
    public static void registerClassExtension (
        Class targetObjectClass,
        ChoiceSource classExtension)
    {
        // Be thread-safe in case registration occurs in threads due to dynamic loading
        synchronized (ChoiceSource.class) {
            if (_registry == null) {
                _registry = new ClassExtensionRegistry();
            }

            _registry.registerClassExtension(targetObjectClass, classExtension);
        }
    }

    /**
        Retrieve a ClassExtension registered by registerClassExtension(...).  Note that
        this will clone the ClassExtension objects which are registered so that each
        subclass will have its own classExtension implementation.  See
        ClassExtensionRegistry for details on this.
    */
    public static ChoiceSource get (Object target)
    {
        Assert.that(target != null, "The input target class cannot be null");
        return (ChoiceSource)_registry.get(target.getClass());
    }

    /* Method to be overridden in implementations on domain classes */
    public int expectedCount (Object target)
    {
        return -1; // unknown
    }

    abstract public List list (Object target);

    abstract public ChooserSelectionSource chooserSelectionSource (Object target,
                                                                   String searchKey);

    /*
        Some basic implementations
     */
    public static class ChoiceSource_List extends ChoiceSource
    {
        public int expectedCount (Object target)
        {
            return ((List)target).size();
        }

        public List list (Object target)
        {
            return (List)target;
        }

        public ChooserSelectionSource chooserSelectionSource (Object target,
                                                              String searchKey)
        {
            return new ChooserSelectionSource.ListSource(((List)target), searchKey);
        }
    }

    /*
        Some basic implementations
     */
    public static class ChoiceSource_ChooserSelectionSource extends ChoiceSource
    {
        public List list (Object target)
        {
            return ((ChooserSelectionSource)target).match(null, Integer.MAX_VALUE);
        }

        public ChooserSelectionSource chooserSelectionSource (Object target,
                                                              String searchKey)
        {
            return (ChooserSelectionSource)target;
        }
    }
    
    // register default implementations
    static {
        ChoiceSource.registerClassExtension(List.class, new ChoiceSource_List());
        ChoiceSource.registerClassExtension(ChooserSelectionSource.class,
                new ChoiceSource_ChooserSelectionSource());
    }
}
