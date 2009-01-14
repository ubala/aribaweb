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

    $Id: //ariba/platform/util/core/ariba/util/io/FirstTokenKeyCSVConsumer.java#7 $
*/

package ariba.util.io;

import ariba.util.core.MapUtil;
import ariba.util.core.ListUtil;
import java.util.List;
import java.util.Map;

/**
    The FirstTokenKeyCSVConsumer stores the first token as the key
    and the vector of tokens as the value in a map
    specified during the construction of the class.        

   @aribaapi private
*/
public class FirstTokenKeyCSVConsumer implements CSVConsumer
{
    private Map table = MapUtil.map();
    
    /**
        Called once per CSV line read.

        @param path the CSV source file
        @param lineNumber the current line being reported, 1-based.
        @param line a List of tokens parsed from a one line in the file
    */
    public void consumeLineOfTokens (String path,
                                     int    lineNumber,
                                     List line)
    {
        table.put(ListUtil.firstElement(line), line);
    }
    
    
    /**
        Get a List representing a line read in from the file, given
        a key (the first token in the line).

        @param key the key to find a line read from the file
        @param defaultValue return this List if the key is not found.
    */
    public List get (String key, List defaultValue)
    {
        Object obj = table.get(key);
        if (obj != null && obj instanceof List) {
            return (List)obj;
        }    
        return defaultValue;
    }    
}


