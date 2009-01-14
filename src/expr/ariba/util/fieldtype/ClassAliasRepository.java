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

    $Id: //ariba/platform/util/expr/ariba/util/fieldtype/ClassAliasRepository.java#4 $
*/

package ariba.util.fieldtype;

import ariba.util.core.ClassUtil;
import ariba.util.core.Constants;
import ariba.util.core.FileUtil;
import ariba.util.core.Fmt;
import ariba.util.core.MapUtil;
import ariba.util.core.ListUtil;
import ariba.util.i18n.I18NUtil;
import ariba.util.io.CSVConsumer;
import ariba.util.io.CSVReader;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import ariba.util.log.Log;

/**
    A singleton class that acts as a repository for storing information
    about alias name for class.

    @aribaapi ariba
*/
public class ClassAliasRepository
{
    private static final String ClassAliasCSVName = "ClassAlias.csv";
    private static final String ClassAliasCSVPath = 
        FileUtil.fixFileSeparators("etc/classalias");
    private static final ClassAliasRepository INSTANCE = new ClassAliasRepository();

    public static ClassAliasRepository getInstance ()
    {
        return INSTANCE;
    }

    //-----------------------------------------------------------------------
    // nested class

    /**
        A class that knows how to read CSV into Java class name to
        method specification table.

        @aribaapi private
    */
    private static class ClassAliasCSVConsumer implements CSVConsumer
    {
        private Map/*<String, String>*/ _aliasToName;
        private Map/*<String, String>*/ _nameToAlias;

        public ClassAliasCSVConsumer ()
        {
            _aliasToName = MapUtil.map();
            _nameToAlias = MapUtil.map();
        }

        public void consumeLineOfTokens (String path, int lineNumber, List line)
        {
            if (lineNumber == 1) {
                // we skip the header
                return;
            }
            if (line.size() < 2) {
                String msg = Fmt.S(
                    "%s:%s requires at least 2 columns but it " +
                    "has %s columns: %s",
                    path,
                    Constants.getInteger(lineNumber),
                    Constants.getInteger(line.size()),
                    line);
                    Log.util.warn(msg);
                return;
            }

            // check to see if the specified Java class name is valid
            Iterator iter = line.iterator();
            String javaClassName = (String)iter.next();
            Class javaClass = ClassUtil.classForName(javaClassName, false);
            if (javaClass == null) {
                String msg = Fmt.S("%s:%s contains invalid java class: %s",
                                   path, Constants.getInteger(lineNumber), javaClassName);
                Log.util.warn(msg);
                return;
            }

            // get the alias name for the java class name
            String alias = (String)iter.next();
            if (alias != null) {
                for (int i=0; i < alias.length(); i++) {
                    char c = alias.charAt(i);
                    if (!Character.isLetterOrDigit(c) && c != '_') {
                        String msg = Fmt.S(
                            "Alias '%s' for class '%s' contains invalid character '%s'",
                                   alias, javaClassName, String.valueOf(c));
                        Log.util.warn(msg);
                        return;
                    }
                }
            }

            // check to see if alias is a class name
            Class aliasClass = ClassUtil.classForName(alias, false);
            if (aliasClass != null) {
                String msg = Fmt.S("Alias '%s' for class '%s' conflicts with a java class/type name.",
                                   alias, javaClassName);
                Log.util.warn(msg);
                return;
            }

            // check to see if the alias is the common package name.
            List packagePrefixes = ListUtil.list("java", "com", "org", "ariba", "test");
            if (packagePrefixes.contains(alias)) {
                String msg = Fmt.S("Alias '%s' for class '%s' is a common package prefix (%s).",
                                   alias, javaClassName, packagePrefixes);
                Log.util.warn(msg);
                return;
            }

            // check to see if the alias and classname are the same
            if (alias.equals(javaClassName)) {
                String msg = Fmt.S(
                    "Alias '%s' is identical to class name '%s'.  This entry is skipped.",
                     alias, javaClassName);
                Log.util.warn(msg);
                return;
            }

            // check to see if the alias already exist
            String otherClass = (String)_aliasToName.get(alias);
            if (otherClass != null) {
                String msg = Fmt.S(
                    "Alias '%s' for class '%s' is already used for class '%s' ",
                     alias, javaClassName);
                Log.util.warn(msg);
                return;
            }

            _aliasToName.put(alias, javaClassName);
            _nameToAlias.put(javaClassName, alias);
        }


        public Map getAliasMap ()
        {
            return _aliasToName;
        }

        public Map getClassNameMap ()
        {
            return _nameToAlias;
        }
    }

    //-----------------------------------------------------------------------
    // private data members

    private Map _alias2Name;
    private Map _name2Alias;

    //-----------------------------------------------------------------------
    // constructor

    /*
        Suppresses default constructor for noninstantiability
    */
    private ClassAliasRepository ()
    {
        createClassAliasMap();
    }

    private void createClassAliasMap ()
    {
        File csvFile = new File(ClassAliasCSVPath, ClassAliasCSVName);
        if (csvFile.exists()) {
            ClassAliasCSVConsumer consumer = new ClassAliasCSVConsumer();
            CSVReader reader = new CSVReader(consumer);
            try {
                reader.read(csvFile, I18NUtil.EncodingUTF8);
                _alias2Name = consumer.getAliasMap();
                _name2Alias = consumer.getClassNameMap();
                return;
            }
            catch (IOException ex) {
                String msg = Fmt.S("file %s could not be read",
                                   csvFile.getPath());
                Log.util.warn(msg);
            }
        }

        _alias2Name = MapUtil.map();
        _name2Alias = MapUtil.map();
    }

    public String getClassNameForAlias (String alias)
    {
        return (String)_alias2Name.get(alias);
    }

    public String getAliasForClassName (String className)
    {
        return (String)_name2Alias.get(className);
    }
}
