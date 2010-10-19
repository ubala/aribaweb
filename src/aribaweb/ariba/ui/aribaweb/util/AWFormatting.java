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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWFormatting.java#6 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;
import java.util.Locale;

/**
 * This should really be named the AWFormatterAdapter.
 * This implements the adapter pattern via ClassExtensions.  This leads to a slight
 *  variation of the standard adapter pattern.  In the standard pattern the Adapter
 *  (ie, this class) does all of the adaptations while here we have subclasses of this
 *  class handle the specific adaptations.  To put it another way, this class and its
 *  subclasses take the place of the usual monolithic adapter in the Adapter Pattern.
 * Also see the Wikipedia entry at http://en.wikipedia.org/wiki/Adapter_pattern.
 */
abstract public class AWFormatting extends ClassExtension
{
    private static final ClassExtensionRegistry ClassExtensionRegistry = new ClassExtensionRegistry();

    // ** Thread Safety Considerations: ClassExtension cache can be considered read-only, so it needs no external locking.

    static {
        registerClassExtension(java.text.Format.class, new AWFormatting_JavaFormat());
        registerClassExtension(ariba.util.formatter.Formatter.class, new AWFormatting_AribaFormatter());
        registerClassExtension(AWFormatter.class, new AWFormatting_AWFormatter());
    }

    /**
     * This will make the passed in formatter adapter (AWFormatting object) the proper
     *  formatter adapter to use for objects of type formatterAdapteeClass (receiverClass).
     *
     * @param receiverClass the formatterAdapteeClass
     * @param formatClassExtension the formatterAdapterClassExtension
     */
    public static void registerClassExtension (Class receiverClass, AWFormatting formatClassExtension)
    {
        ClassExtensionRegistry.registerClassExtension(receiverClass, formatClassExtension);
    }

    /**
     * Returns a formatter adapter (AWFormatting object) for the target's type.
     *
     * @param target The formatterAdaptee.
     *
     * @return a formatter adapter that will work on the target's type.
     */
    public static AWFormatting get (Object target)
    {
        return (AWFormatting)ClassExtensionRegistry.get(target.getClass());
    }

    ////////////
    // Api
    ////////////

    /**
     * This is really more like toObject() than parseObject().
     *
     * @param receiver the formatterAdaptee
     * @param stringToParse the objectInStringForm
     *
     * @return an object with the type depending on the receiver (formatterAdaptee)
     */
    abstract public Object parseObject (Object receiver, String stringToParse);

    /**
     * This is really more like toString() than format().
     *  It converts an object to a string.  With the allowed objects depending on the
     *  receiver (formatterAdaptee).
     *
     * @param receiver the formatterAdaptee
     * @param objectToFormat the objectToStringify
     *
     * @return the string form of the objectToFormat (objectToStringify)
     */
    abstract public String format (Object receiver, Object objectToFormat);

    /**
     * This is really more like toObject() than parseObject().
     *
     * @param receiver the formatterAdaptee
     * @param stringToParse the objectInStringForm
     * @param locale the locale
     *
     * @return an object with the type depending on the receiver (formatterAdaptee)
     */
    public Object parseObject (Object receiver, String stringToParse, Locale locale)
    {
        return parseObject(receiver, stringToParse);
    }

    /**
     * This is really more like toString() than format().
     *  It converts an object to a string.  With the allowed objects depending on the
     *  receiver (formatterAdaptee).
     *
     * @param receiver the formatterAdaptee
     * @param objectToFormat the objectToStringify
     * @param locale the locale
     *
     * @return the string form of the objectToFormat (objectToStringify)
     */
    public String format (Object receiver, Object objectToFormat, Locale locale)
    {
        return format(receiver, objectToFormat);
    }
}
