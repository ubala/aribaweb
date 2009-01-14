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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/groovy/AWGroovyLoader.java#6 $
*/
package ariba.ui.groovy;

import groovy.lang.GroovyClassLoader;
import ariba.ui.aribaweb.util.AWClassLoader;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWSingleLocaleResourceManager;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.util.core.ClassUtil;
import ariba.util.core.GrowOnlyHashtable;

import java.io.InputStream;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * AWClassLoader implementation for .groovy source files for AWComponents.
 */
public class AWGroovyLoader extends AWClassLoader.PairedFileLoader
{
    static boolean _DidInit = false;

    public static void initialize ()
    {
        if (_DidInit) return;
        _DidInit = true;
        AWUtil.setClassLoader(new AWGroovyLoader());
        AWGPathClassExtensions.initialize();
        AWXGroovyTag.initialize();
    }

    static GroovyClassLoader _Gcl;

    protected static GroovyClassLoader gcl ()
    {
        if (_Gcl == null) _Gcl = new GroovyClassLoader();
        return _Gcl;
    }

    protected String sourceFileExtension ()
    {
        return ".groovy";
    }

    protected void compileSourceFromResource (String className, AWResource sourceResource)
    {
        InputStream sourceStream = sourceResource.inputStream();
        System.out.printf("Loading %s\n", sourceResource.fullUrl());
        Class cls = gcl().parseClass(sourceStream, sourceResource.relativePath());
        if (cls != null) recordClassForResource(cls.getName(), cls, sourceResource);
    }
}
