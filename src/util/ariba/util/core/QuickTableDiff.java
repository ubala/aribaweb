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

    $Id: //ariba/platform/util/core/ariba/util/core/QuickTableDiff.java#4 $
*/

package ariba.util.core;

import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
    Simple command line tool to diff to table files.
    The algorithm is probably not the most efficient but it works.
    Exit code is
    - 0 if the tables are identical ;
    - 1 if they differ ;
    - 2 if one of them can't be loaded.
    @aribaapi private
*/
public class QuickTableDiff implements CommandLine
{

    private static final String OptionFile1 = "diff";
    private static final String OptionFile2 = "against";
    
    private PrintWriter out = SystemUtil.out();
    private PrintWriter err = SystemUtil.err();
        
    private File file1;
    private File file2;
    
    public void setupArguments (ArgumentParser arguments)
    {
        arguments.addRequiredFile(OptionFile1, "file to diff");
        arguments.addRequiredFile(OptionFile2, "file to diff against");
    }

    public void processArguments (ArgumentParser arguments)
    {
        file1 = arguments.getFile(OptionFile1);
        file2 = arguments.getFile(OptionFile2);
    }
    

    public void startup ()
    {
        Map table1 = TableUtil.loadMap(file1);
        if (table1 == null) {
            err.println(Fmt.S("Unable to load %s.",file1));
            SystemUtil.exit(2);
        }
        Map table2 = TableUtil.loadMap(file2);
        if (table2 == null) {
            err.println(Fmt.S("Unable to load %s.",file2));
            SystemUtil.exit(2);
        }

        if (!MapUtil.mapEquals(table1, table2)) {
            out.println("Tables differ:");
            diffMap(table1, table2, "");
            SystemUtil.exit(1);
        }
        else {
            out.println("Tables are identical.");
            SystemUtil.exit(0);
        }
        
    }

    private void diffMap (Map table1, Map table2, String context)
    {
        List keys = MapUtil.keysList(table1);
        ListUtil.sortStrings(keys,true);
        List expectedKeys = MapUtil.keysList(table2);
        ListUtil.sortStrings(expectedKeys,true);
        if (!ListUtil.listEquals(keys, expectedKeys)) {
            out.println(Fmt.S("At '%s', the keys are different " +
                              "(the diff will continue with the common set of keys):",
                              context));
            out.println(Fmt.S(">> The following keys are extra: %s",
                              ListUtil.minus(keys, expectedKeys)));
            out.println(Fmt.S(">> The following keys are missing: %s",
                              ListUtil.minus(expectedKeys, keys)));
            keys.retainAll(expectedKeys);
            return;
        }

        for (Iterator i = keys.iterator(); i.hasNext();) {
            Object key = i.next();
            Object value1 = table1.get(key);
            Object value2 = table2.get(key);
            if (! SystemUtil.equal(value1, value2)) {
                String newContext = StringUtil.nullOrEmptyOrBlankString(context) ?
                    key.toString() :
                    StringUtil.strcat(context, ".", key.toString());

                if (value1 instanceof Map &&
                    value2 instanceof Map)
                {
                    diffMap((Map)value1, (Map)value2, newContext);
                }
                else {
                    out.println(Fmt.S("At '%s', Got %s but expected %s",
                                      newContext,
                                      value1,
                                      value2));
                }
            }
        }
        return;
    }
    
    public static void main (String[] args)
    {
        ArgumentParser.create("ariba.util.core.QuickTableDiff", args);
    }
}
