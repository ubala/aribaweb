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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/groovy/AWXGroovyTag.java#7 $
*/
package ariba.ui.groovy;

import ariba.ui.aribaweb.core.AWContainerElement;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWTemplate;
import ariba.ui.aribaweb.core.AWElement;
import ariba.ui.aribaweb.core.AWBareString;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindable;
import ariba.ui.aribaweb.core.AWHtmlTemplateParser;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.demoshell.Log;
import ariba.ui.demoshell.AWXHTMLComponentFactory;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.Fmt;
import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import groovy.lang.GroovyClassLoader;

/**
 * AWXGroovyTag tag delimits Groovy script meant to extend AW components on the
 * server.
 */

public class AWXGroovyTag extends AWContainerElement implements AWXHTMLComponentFactory.ScriptClassProvider, AWHtmlTemplateParser.LiteralBody {
    private static final String SelfKey = "__AWXGroovyTag";
    protected Map _accessorNames = MapUtil.map();            
    static Map _classesByName = MapUtil.map();

    public AWXGroovyTag ()
    {
        Log.demoshell.debug("--- creating a new AWXGroovyTag --- ");
    }

    // If a template contains this, use JavaScript
    public Class componentSubclass(File file, AWTemplate template)
    {
        final FastStringBuffer buf = new FastStringBuffer();

        // Get <groovy> tag content
        AWUtil.iterate(template, new AWUtil.ElementIterator() {
            public Object process(AWElement e) {
                if (e instanceof AWXGroovyTag) {
                    buf.append(((AWXGroovyTag)e).scriptString());
                }
                return null; // keep looking
            }
        });

        // Generate code for expression bindings
/* Disable groovy expression in favor of util.expr expressions...
        buf.append("\n// Expression Binding generated accessors ---- \n ");
        AWUtil.iterate(template, new AWUtil.ElementIterator() {
            public Object process(AWElement e) {
               if (e instanceof AWBindable) {
                    AWBindable reference = ((AWBindable)e);
                    Log.demoshell.debug("--- Tag: %s: %s --- ", reference.tagName(), reference.toString());
                    String prefix = reference.tagName(); // componentDefinition().componentName().replace('.','_');
                    processBindings(prefix, reference.allBindings(), buf);
                }
                return null; // keep looking
            }
        });
*/

        String string = buf.toString();
        return groovyClassForBody(file.getName().replace('.', '_').replace('-', '_'), string);
    }

    protected void processBindings (String prefix, AWBinding[] bindings, FastStringBuffer buf)
    {
        int i = bindings.length;
        while (i-- > 0) {
            AWBinding binding = bindings[i];
            Log.demoshell.debug("------- binding: %s", binding.bindingName());

            if (binding instanceof AWBinding.ExpressionBinding) {
                processExpressionBinding(prefix, (AWBinding.ExpressionBinding)binding, buf);
            }
        }
    }

    /*
        Patterns:
          $:expr
              if just \w[]. then can be lvalue (setter supported)
          $:=varName
              declare var name
          $:=expr
              anonymous varname expression
          $:foo=expr
              declare var name with initializer
     */
        // =var_name1
    private static Pattern VarDeclPattern = Pattern.compile("\\=(\\w+)\\s*$");
        // var_name1=expr
    private static Pattern VarDeclInitPattern = Pattern.compile("\\s*(\\w+)\\s*\\=(.+)$");
        // foo.bar[1]
    private static Pattern LValuePattern = Pattern.compile("([\\w|\\.|\\[|\\]]+)\\s*$");

    void processExpressionBinding (String prefix, AWBinding.ExpressionBinding binding, FastStringBuffer buf)
    {
        String exprString = binding.expressionString();

        // compute acceesor name (and de-dup)
        String key = null;

        String accessorString;
        Matcher m = VarDeclPattern.matcher(exprString);
        if (m.matches()) {
            key = m.group(1);
            accessorString = Fmt.S("def %s; // var decl\n", key);
        } else if ((m = VarDeclInitPattern.matcher(exprString)).matches()) {
            key = m.group(1);
            accessorString = Fmt.S("def %s; // var init\n", exprString);
        } else {
            // generate function name
            key = prefix + "_" + ((AWBinding)binding).name().string();
            key = uniqueName(key, _accessorNames);
            _accessorNames.put(key, key);

            // read accessor
            accessorString = Fmt.S("def %s() { %s }\n", key, exprString);

            // write accessor?
            if (LValuePattern.matcher(exprString).matches()) {
                accessorString += Fmt.S("def set%s(arg_a) { %s = arg_a; }\n",
                        StringUtil.capitalizeFirstChar(key), exprString);
            }
        }

        buf.append(accessorString);
        binding.setSubstituteBinding(AWBinding.fieldBinding(key, key, null));
    }

    private static Pattern ClassDeclPattern = Pattern.compile("\\s*class\\s+(\\w+)\\s+extends\\s+([\\w\\.]+)\\s*");
    private static Pattern ImportDeclPattern = Pattern.compile("(\\s*import\\s+[\\w\\.]+\\*?[\\s\\;]*)+");

    protected Class groovyClassForBody (String name, String bodyString)
    {
        final FastStringBuffer buf = new FastStringBuffer();
        String className = uniqueName(name, _classesByName);
        String classString;

        // support for scripts where we don't wrap it -- i.e. where script sets own parent class, imports, etc
        // and can include multiple classes in single file.
        Matcher m = ClassDeclPattern.matcher(bodyString);
        if (m.find()) {
            String theirName = m.group(1);
            String theirSuper = m.group(2);
            classString = m.replaceFirst(Fmt.S("\n\nclass %s extends %s ", className, theirSuper));
        } else {
            String extraImports = "";
            m = ImportDeclPattern.matcher(bodyString);
            if (m.find() && m.start() < 2) {
                extraImports = m.group(0);
                bodyString = m.replaceFirst("");
            }

            classString = Fmt.S("%s\n%s%s\nclass %s extends ariba.ui.demoshell.AWXHTMLComponent {\n %s \n}",
                    "package ariba.ui.demoshell.demo;",
                    "import ariba.ui.aribaweb.core.*;import ariba.ui.widgets.*;import ariba.ui.table.*;import ariba.ui.outline.*;import ariba.util.core.*;",
                    extraImports, className, bodyString);
        }

        // use groovy class loader to load class as necessary...
        GroovyClassLoader gcl = new GroovyClassLoader();

        Log.demoshell.debug("--- Generating class: %s", className);
        // classString += "\n\n class C2 { def test () { return \"Yeah!\" }; } \n";

        Class cls;
        try {
            cls = gcl.parseClass(classString, className+".groovy");
        } catch (Exception e) {
            FastStringBuffer lineBuf = new FastStringBuffer();
            String[] lines = classString.split("\n");
            for (int i=0; i < lines.length; i++) {
                lineBuf.append(Integer.toString(i+1));
                lineBuf.append(": ");
                lineBuf.append(lines[i]);
                lineBuf.append("\n");
            }
            throw new AWGenericException(Fmt.S("Exception parsing groovy for class %s: %s\n%s", className,
                    e.getMessage(), lineBuf.toString()));
        }
        _classesByName.put(className, cls);
        return cls;
    }

    protected String uniqueName (String name, Map map)
    {
        int i = 0;
        String candidate = name;
        while (map.get(candidate) != null) {
            candidate = StringUtil.strcat(name, "_" + Integer.toString(i++));
        }
        return candidate;
    }

    public static AWXGroovyTag instanceInComponent (AWComponent component)
    {
        return (AWXGroovyTag) AWUtil.elementOfClass(component.template(), AWXGroovyTag.class);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        // swallow our content
    }

    public String scriptString ()
    {
        AWElement content = contentElement();
        if (content instanceof AWBareString) {
            return ((AWBareString)content).string();
        }
        throw new AWGenericException("AWXGroovyTag with content that is not a BareString!");
    }
}

