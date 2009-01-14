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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWUserDefaults.java#6 $
*/

package ariba.ui.aribaweb.util;

import java.util.Map;
import ariba.util.core.MapUtil;

public final class AWUserDefaults extends AWBaseObject
{
    private static AWUserDefaults SharedInstance = null;
    private Map _userDefaultsHashtable = null;

    private AWUserDefaults (Map userDefaultsHashtable)
    {
        super();
        _userDefaultsHashtable = userDefaultsHashtable;
    }

    public int intForKey (String userDefaultKey, int defaultValue)
    {
        int intForKey = 0;
        String intString = (String)_userDefaultsHashtable.get(userDefaultKey);
        intForKey = (intString == null) ? defaultValue : Integer.parseInt(intString);
        return intForKey;
    }

    public String stringForKey (String userDefaultKey, String defaultValue)
    {
        String stringForKey = (String)_userDefaultsHashtable.get(userDefaultKey);
        if (stringForKey == null) {
            stringForKey = defaultValue;
        }
        return stringForKey;
    }

    public boolean booleanForKey (String userDefaultKey, boolean defaultValue)
    {
        boolean booleanForKey = defaultValue;
        String valueString = (String)_userDefaultsHashtable.get(userDefaultKey);
        if (valueString == null) {
            booleanForKey = defaultValue;
        }
        else {
            if (valueString.equalsIgnoreCase("YES") || valueString.equalsIgnoreCase("true") || valueString.equals("1")) {
                booleanForKey = true;
            }
            else if (valueString.equalsIgnoreCase("NO") || valueString.equalsIgnoreCase("false") || valueString.equals("0")) {
                booleanForKey = false;
            }
            else {
                String exceptionString = "Command line argument \"" + userDefaultKey + "\" has invalid value \"" + valueString + ".\"  Must be YES, NO, true, false, 1, or 0 (not case sensitive).";
                throw new AWGenericException(exceptionString);
            }
        }
        return booleanForKey;
    }

    private static final Map parseUserDefaultsFromArgsArray (String[] args)
    {
        int argsCount = args.length;
        Map userDefaultsHashtable = MapUtil.map();
        for (int index = 0; index < argsCount; index++) {
            String currentKey = args[index++];
            String currentValue = args[index];
            char hyphenChar = currentKey.charAt(0);
            if (hyphenChar == '-') {
                currentKey = currentKey.substring(1);
            }
            else {
                throw new AWGenericException("Command line argument \"" + currentKey + "\" missing hyphen prefix.");
            }
            userDefaultsHashtable.put(currentKey, currentValue);
        }
        return userDefaultsHashtable;
    }

    /* This was removed when the Util api's changed from 6.1 to 7.0
    public static AWUserDefaults initialize (String registryDomainPath, String userDomainPath, String[] argsArray)
    {
        Map registryDefaults = (Map)Util.loadObject(registryDomainPath);
        Map userDefaults = (Map)Util.loadObject(userDomainPath);
        Map commandLineDefaults = parseUserDefaultsFromArgsArray(argsArray);
        Map userDefaultsHashtable = MapUtil.map();
        AWUtil.addElements(userDefaultsHashtable, registryDefaults);
        AWUtil.addElements(userDefaultsHashtable, userDefaults);
        AWUtil.addElements(userDefaultsHashtable, commandLineDefaults);
        if (SharedInstance == null) {
            SharedInstance = new AWUserDefaults(userDefaultsHashtable);
        }
        return SharedInstance;
    }
    */

    public static AWUserDefaults sharedInstance ()
    {
        return SharedInstance;
    }
    
}
