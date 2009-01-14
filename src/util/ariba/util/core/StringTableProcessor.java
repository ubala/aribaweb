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

    $Id: //ariba/platform/util/core/ariba/util/core/StringTableProcessor.java#6 $
*/

package ariba.util.core;

import java.net.URL;
import java.util.Map;

/**
    The StringCSVProcessor is callback for ResourceSerivce to process string tables.

    We need the interface because there are two different styles of string table.
    1. Buyer style: created by developer. ex ariba.html.fieldsui.csv. They have three columns,
       the first column is the key, the second column is the localized strings and last one comment.
    2. AribaWeb style: created by localize script.  the strings are extracted from awl file and java files.
       These files have 5 columns. column 1 is the component name, column 2 the key inside this component,
       column 3 the english string, column is the localized string. column 5 comment.

    @aribaapi private
*/
public interface StringTableProcessor
{
    /**
        create a csv consumer.
        @param url location of csv file
        @param displayWarning flag that indicates whether warning should be displayed for errors
        @aribaapi ariba
    */
    public StringCSVConsumer createStringCSVConsumer (URL url,
                                                      boolean displayWarning);
    
    /**
        Merge string map read from different directories.
        @param dest destination map
        @param source source map
        @aribaapi ariba
    */
    public void mergeStringTables (Map dest, Map source);

    public Object defaultValueIfNullStringTable (Object key);
}
