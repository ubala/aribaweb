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

    $Id: //ariba/platform/util/core/ariba/util/i18n/MergedStringLocalizer.java#4 $
*/
package ariba.util.i18n;

import ariba.util.core.ClassUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringCSVConsumer;
import ariba.util.core.StringTableProcessor;
import ariba.util.core.FastStringBuffer;
import ariba.util.log.Log;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * String localizer that fetches strings from the Resource Service.
 * @aribaapi private
 */
public class MergedStringLocalizer implements StringTableProcessor, LocalizedJavaString.Localizer
{
    /**
        implement LocalizedJavaString.Localizer interface
    */
    public String getLocalizedString (String className,
                                      String key,
                                      String defaultString,
                                      Locale locale)
    {
        String stringTableFile = ClassUtil.stripClassFromClassName(className);
        String shortClassName = ClassUtil.stripPackageFromClassName(className);
        Map map = getLocalizedStrings(stringTableFile, shortClassName, locale);
        String value = null;
        if (map != null) {
            value = (String)map.get(key);
        }
        return value == null ? defaultString : value;
    }
    public Map getLocalizedStrings (String stringTableFile,
                                    String componentName,
                                    Locale locale)
    {
        return getLocalizedString(stringTableFile, componentName, locale);
    }

    public Map cloneStringTable (Map stringTable)
    {
        Map stringTableCopy = MapUtil.cloneMap(stringTable);
        Object[] componentKeys = MapUtil.keysArray(stringTable);
        for (int index = 0, size = componentKeys.length; index < size; index ++) {
            Object componentName = componentKeys[index];
            Map componentString = (Map)stringTableCopy.get(componentName);
            Map componentStringCopy = MapUtil.copyMap(componentString);
            stringTableCopy.put(componentName, componentStringCopy);
        }
        return stringTableCopy;
    }

    protected Map getLocalizedString (String stringTableName,
                                    String componentName,
                                    Locale locale)
    {
        ResourceService resourceService = ResourceService.getService();
        // restrict the locale to what we can use
        locale = resourceService.getRestrictedLocale(locale);

        Map componentTable = null;
        Map stringTable = resourceService.stringTable(stringTableName, locale, this);
        if (stringTable == null) {
                // "Null string table returned in Resource Service"
            Log.util.error(2962, stringTable);
                // prevent the same error from happening
            resourceService.cacheStringTable(stringTableName,
                                             locale,
                                             MapUtil.map(),
                                             this);
        }
        else {
            componentTable = (Map)stringTable.get(componentName);
        }
        return componentTable;
    }


    public StringCSVConsumer createStringCSVConsumer (URL url,
                                                      boolean displayWarning)
    {
        return new AWCSVStringConsumer(url, displayWarning);
    }

    public void mergeStringTables (Map dest, Map source)
    {
        Object[] componentNames = MapUtil.keysArray(source);
        for (int index = 0, size = componentNames.length; index < size; index ++) {
            String componentName = (String)componentNames[index];
            Map sourceComponentStrings = (Map)source.get(componentName);
            Map destComponentStrings = (Map)dest.get(componentName);
            if (destComponentStrings == null) {
                Map componentStringCopy = MapUtil.copyMap(sourceComponentStrings);
                dest.put(componentName, componentStringCopy);
            }
            else {
                mergeMapIntoMap(destComponentStrings, sourceComponentStrings);
            }
        }
    }

    public static void mergeMapIntoMap (
            Map dest, Map source)
    {
        Iterator i = source.keySet().iterator();

        while (i.hasNext()) {
            Object key = i.next();
            Object sourceValue = source.get(key);
            if (sourceValue != null) {
                dest.put(key, sourceValue);
            }
        }
    }

    /**
        Returns a string table for the List of input lines from the
        resource file.

        If resourcePath is non-null, the embedded contextualization information
        is included as part of the localized strings in the string table.
    */

    public static Map convertToLocalizedStringsTable (List lines)
    {
        Map allLocalizedStringsTable = MapUtil.map();
        int linesCount = lines.size();
        for (int index = 0; index < linesCount; index++) {
            List currentLineVector = (List)lines.get(index);
            if (currentLineVector.size() >= 4) {
                String componentName = (String)ListUtil.firstElement(currentLineVector);
                String localizedStringKey = (String)currentLineVector.get(1);
                String translatedString = (String)currentLineVector.get(3);
                Map componentStringsHashtable = (Map)allLocalizedStringsTable.get(componentName);
                if (componentStringsHashtable == null) {
                    componentStringsHashtable = MapUtil.map();
                    allLocalizedStringsTable.put(componentName, componentStringsHashtable);
                }
                componentStringsHashtable.put(localizedStringKey, translatedString);
            }
        }
        return allLocalizedStringsTable;
    }

    public static String unescapeCsvString (String originalString)
    {
        FastStringBuffer fastStringBuffer = new FastStringBuffer();
        int stringLength = originalString.length();
        for (int index = 0; index < stringLength; index++) {
            char currentChar = originalString.charAt(index);
            if (currentChar == '\\') {
                index++;
                if (index >= stringLength) {
                    break;
                }
                currentChar = originalString.charAt(index);
                switch (currentChar) {
                    case 'n':
                        currentChar = '\n';
                        break;
                    case 'r':
                        currentChar = '\r';
                        break;
                    case 't':
                        currentChar = '\t';
                        break;
                    default:
                        break;
                }
            }
            fastStringBuffer.append(currentChar);
        }
        return fastStringBuffer.toString();
    }

    public Object defaultValueIfNullStringTable (Object key)
    {
        return null;
    }
}


class AWCSVStringConsumer implements StringCSVConsumer
{
    private List _lines = ListUtil.list();
    private URL _url;
    private boolean _displayWarning;

    public AWCSVStringConsumer (URL url,
                                boolean displayWarning)
    {
        _url = url;
        _displayWarning = displayWarning;
    }

    // Returns a hashtable that contains the localized strings for a component.
    //
    // This method has to generate the string table using
    // AWUtil.convertToLocalizedStringsTable.  the resulting hash table actually
    // contains hash tables keyed by the component name. So, this method must
    // call get using the component name on the returned hashtable to get the
    // actually localized string look table.
    public Map getStrings ()
    {
        return MergedStringLocalizer.convertToLocalizedStringsTable(_lines);
    }

    public void consumeLineOfTokens (String filepath, int lineNumber, List line)
    {
        for (int index = line.size() - 1; index >= 0; index--) {
            String currentString = (String)line.get(index);
            String unescapedString = MergedStringLocalizer.unescapeCsvString(currentString);
            line.set(index, unescapedString);
        }
        _lines.add(line);
    }
}
