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

    $Id: //ariba/platform/ui/ideplugin/ariba/ideplugin/core/AWScriptRunner.java#2 $
*/
package ariba.ideplugin.core;

import java.util.StringTokenizer;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AWScriptRunner
{
    public static final String TemplateDirKey = "ariba.ideplugin.templateDir";
    public static final String[] ReqJars = { "ariba.aribaweb.jar", "ariba.util.jar" };

    URLClassLoader _loader;
    String _awhome;

    public AWScriptRunner (String awhome)
    {
        _awhome = awhome;
        init();
    }

    public void init () throws WrapperRuntimeException
    {
        if (_awhome == null || _awhome.length() == 0)
            throw new WrapperRuntimeException("Invalid awhome");
        try {
            ArrayList<URL> cpath = new ArrayList<URL>();
            for (String j : ReqJars) {
                File f = new File(_awhome, "/lib/" + j);
                if (!f.exists()) {
                    throw new FileNotFoundException("Required file: "
                        + f.toString());
                }
                cpath.add(f.toURI().toURL());
            }
            getJars(new File(_awhome, "lib/ext"), cpath);
            URL[] urls = new URL[cpath.size()];
            cpath.toArray(urls);
            _loader = new java.net.URLClassLoader(urls, getClass().getClassLoader());
            _loader.loadClass("ariba.util.core.TableUtil");
        }
        catch (Exception e) {
            throw new WrapperRuntimeException(e);
        }
    }

    protected void getJars (File dir, ArrayList<URL> jarFiles) throws MalformedURLException
    {
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.getName().toLowerCase().endsWith(".jar")) {
                jarFiles.add(f.toURI().toURL());
            }
        }
    }

    public List<Map> loadTemplates () throws WrapperRuntimeException
    {
        File templateDir = new File(_awhome, "/tools/templates/");
        File[] templates = templateDir.listFiles();
        List<Map> ret = new ArrayList<Map>();
        for (File template : templates) {
            if (template.isDirectory()) {
                File tinfo = new File(template, "templateInfo.table");
                if (tinfo.exists()) {
                    HashMap map = new HashMap();
                    try {
                        FileReader in = new FileReader(tinfo);
                        char[] buff = new char[512];
                        int len = 0;
                        StringWriter out = new StringWriter();
                        while ((len = in.read(buff)) > 0) {
                            out.write(buff, 0, len);
                        }
                        in.close();
                        out.close();
                        fromSerializedString(map, out.toString());
                        map.put(TemplateDirKey, template);
                        ret.add(map);
                    }
                    catch (IOException ie) {
                        ie.printStackTrace();
                    }
                }
            }
        }
        Collections.sort(ret, new Comparator<Map>() {
            public int compare (Map m1, Map m2)
            {
                int r1 = m1.containsKey("rank") ? 
                    Integer.parseInt((String)m1.get("rank")) : 0;
                int r2 = m2.containsKey("rank") ? 
                    Integer.parseInt((String)m2.get("rank")) : 0;
                return r1 - r2;
            }
        });
        return ret;
    }

    public void fromSerializedString (Map<String, String> map, String val) throws WrapperRuntimeException
    {
        Class<?>[] argTypes = new Class<?>[] { Map.class, String.class };
        Object[] args = new Object[] { map, val };
        invokeStaticMethod("ariba.util.core.MapUtil", "fromSerializedString",
                           argTypes, args);
    }


    public Object invokeStaticMethod (String cname,
                                      String m,
                                      Class<?>[] argTypes,
                                      Object[] args) throws WrapperRuntimeException
    {
        try {
            Class<?> c = _loader.loadClass(cname);
            Method md = c.getMethod(m, argTypes);
            return md.invoke(null, args);
        }
        catch (Exception e) {
            throw new WrapperRuntimeException(e);
        }
    }

    public String getAWHome ()
    {
        return _awhome;
    }

    public Object createBindings (Map<String, Object> bindings) throws InstantiationException,
        IllegalAccessException,
        ClassNotFoundException,
        IllegalArgumentException,
        InvocationTargetException,
        SecurityException,
        NoSuchMethodException
    {
        Class<?> cBindings = _loader.loadClass("groovy.lang.Binding");
        Object bind = cBindings.newInstance();
        Method mtd = cBindings.getMethod("setVariable",
                                         new Class<?>[] { String.class, Object.class });
        for (String key : bindings.keySet()) {
            Object val = bindings.get(key);
            Object[] args = new Object[] { key, val };
            mtd.invoke(bind, args);
        }

        return bind;
    }

    public void invokeScript (String path,
                              String script,
                              Map<String, Object> bindings) throws WrapperRuntimeException
    {
        try {
            Object[] args = new Object[] { path, _loader };
            Class<?> cGroovyEngine = _loader
                .loadClass("groovy.util.GroovyScriptEngine");
            Object groovyEngine = cGroovyEngine
                .getConstructor(String.class, ClassLoader.class).newInstance(args);

            Object bbindings = bindings == null ? null : createBindings(bindings);

            Method runMtd = cGroovyEngine.getMethod("run", new Class<?>[] {String.class,
                                           _loader.loadClass("groovy.lang.Binding") });
            runMtd.invoke(groovyEngine, new Object[] { script, bbindings });

        }
        catch (Exception e) {
            throw new WrapperRuntimeException(e);
        }
    }

    public void createProject (File templateDir,
                               File projectDir,
                               Map<String, String> map) throws WrapperRuntimeException
    {
        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put("ParamTemplateDir", templateDir);
        bindings.put("ParamProjectDir", projectDir);
        bindings.put("ParamConfigMap", map);
        invokeScript(_awhome + "/bin/", "createProject.groovy", bindings);
    }

    public boolean isWindows ()
    {
        return System.getProperty("os.name").toLowerCase().indexOf("windows") > -1;
    }

    String[] antCmd = { "",  //ant command 
                        "-emacs", 
                        "-logger",
                       "org.apache.tools.ant.NoBannerLogger", 
                       "",  // target
                       "-buildfile",
                       "",  //build file 
                       "" };  // aw.home

    public String invokeAnt (File buildFile, String target) throws WrapperRuntimeException
    {
        if (!buildFile.exists()) {
            throw new WrapperRuntimeException("File not found: " + buildFile.toString());
        }
        if (isWindows()) {
            antCmd[0] = _awhome + "/tools/ant/bin/ant.bat";
        }
        else {
            antCmd[0] = _awhome + "/tools/ant/bin/ant";
        }

        antCmd[4] = target;
        antCmd[6] = buildFile.getPath();
        antCmd[7] = "-Daw.home=" + _awhome;

        StringWriter out = new StringWriter();
        try {
            Process process = Runtime.getRuntime().exec(antCmd);

            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                // error stream??
            char[] buff = new char[512];
            int len = 0;
            while ((len = in.read(buff)) > 0) {
                out.write(buff, 0, len);
            }
            in.close();
        }
        catch (IOException e) {
            throw new WrapperRuntimeException(e);
        }
        return out.toString();
    }

    public List<String> getRequiredLibs (File buildFile) throws WrapperRuntimeException
    {
        String res = invokeAnt(buildFile, "echo-build-classpath");

        String classpath = null;
        int sloc = res.indexOf("CLASSPATH: ");
        if (sloc > -1) {
            int eloc = res.indexOf("\n", sloc + 1);
            if (eloc > sloc) {
                classpath = res.substring(sloc + 11, eloc).trim();
            }
        }

        ArrayList<String> ret = new ArrayList<String>();
        if (classpath != null && classpath.length() > 10) {
            StringTokenizer st = new StringTokenizer(classpath, ";");
            while (st.hasMoreTokens()) {
                ret.add(st.nextToken().trim());
            }
        }
        return ret;
    }
            
}
