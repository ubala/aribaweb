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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWEvaluateTemplateFile.java#5 $
*/
package ariba.ui.aribaweb.util;

import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import ariba.util.core.URLUtil;
import ariba.util.fieldvalue.FieldValue_Object;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.FieldValueException;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWTemplate;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWValidationContext;
import ariba.ui.aribaweb.core.AWRedirect;
import ariba.ui.aribaweb.core.AWResponseGenerating;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.MalformedURLException;
import java.net.URL;

/**
    Command line task for evaluating an template file (.awl) in terms of a
    set of properties definted in a supplied .properties files.

    Usage:
        java ariba.ui.aribaweb.util.AWEvaluateTemplateFile <template file> <properties file> <output file>
 */
public class AWEvaluateTemplateFile extends AWComponent
{
    static File _templateFile;
    static File _baseFile;
    public Map <String, Object>_properties;
    public String propName;

    public static void main (String[] args)
    {
        Assert.that(args.length == 3, "Usage: AWEvaluateTemplateFile <template file> <properties file> <output file>");
        File templateFile = new File(args[0]);
        File propertiesFile = new File(args[1]);
        File outputFile = new File(args[2]);

        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(propertiesFile));
        } catch (IOException e) {
            Assert.that(false, "Exception loading properties from: %s: %s", propertiesFile, e);
        }

        final AWConcreteApplication application = (AWConcreteApplication)AWConcreteApplication.defaultApplication();

        AWEvaluateTemplateFile page = create(templateFile, properties);

        // force initialization of the default application
        System.out.printf("AWEvaluateTemplateFile evaluating %s to %s ...\n", templateFile, outputFile);
        page.process(outputFile);
    }

    public static AWEvaluateTemplateFile create (File templateFile, Map properties)
    {
        _templateFile = templateFile;
        Assert.that(_templateFile.exists(), "Can't find: %s", _templateFile);
        AWEvaluateTemplateFile page = (AWEvaluateTemplateFile)AWComponent.createPageWithName(AWEvaluateTemplateFile.class.getName());
        page._properties = properties;

        return page;
    }
    
    public static AWEvaluateTemplateFile create (File templateFile, Map properties, File baseFile)
    {
        AWEvaluateTemplateFile page = create(templateFile,  properties);
        _baseFile = baseFile;
        return page;
    }

    public void process (File outputFile)
    {
        String output = generateStringContents();
        AWUtil.writeToFile(output, outputFile);
    }

    public AWTemplate loadTemplate()
    {
        InputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(_templateFile);
        } catch (Throwable t) {
            Assert.that(false, "Exception loading template from: %s: %s", _templateFile, t);
        }
        return parseTemplate(fileInputStream);
    }

    protected String fullTemplateResourceUrl ()
    {
        try {
            return _templateFile.toURL().toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return _templateFile.getAbsolutePath();
        }
    }

    // to prevent whitespace compression
    public boolean useXmlEscaping()
    {
        return true;
    }

    protected void validate (AWValidationContext validationContext)
    {
        // no op, so we don't get complaints about missing bindings for property keys
    }

    public List propertiesMatchingPrefix (String prefix)
    {
        List matches = ListUtil.list();
        for (String name : _properties.keySet()) {
            if (name.startsWith(prefix)) matches.add(name);
        }

        return matches;
    }

    public String decamelize (String string)
    {
        return AWUtil.decamelize(string, '-', false);
    }

    /*
    hrefUrl" action="$resolvedHrefUrl
     */
    public String _hrefUrl;

    static final Pattern LocalResourcePattern = Pattern.compile("^/?([\\w\\-\\./_]+)");

    public AWResponseGenerating resolvedHrefUrlRedirect ()
    {
        if (_baseFile == null || _hrefUrl == null) return null;

        String destUrl = _hrefUrl;
        Matcher m = LocalResourcePattern.matcher(_hrefUrl);
        if (m.matches()) {
            File resourceFile = new File (_baseFile.getParentFile(), m.group(1));
            Assert.that(resourceFile.exists(), "Can't find referenced file: %s", _hrefUrl);

            destUrl = AWUtil.relativeUrlString(URLUtil.url(_baseFile), URLUtil.url(resourceFile));
            if (destUrl.endsWith(".txt")) destUrl = destUrl.replaceAll("\\.txt$", ".htm");
        }
        return AWRedirect.getRedirect(requestContext(), destUrl);
    }

    /*
        FieldValue implementation to support getting dotted path keys out of our _properties map
     */
    static {
        ariba.util.fieldvalue.FieldValue.registerClassExtension (AWEvaluateTemplateFile.class, new FieldValue());
    }

    public static class FieldValue extends FieldValue_Object
    {
        public Object getFieldValue (Object receiver, FieldPath fieldPath)
        {
            String path = fieldPath.fieldPathString();
            Object value = ((AWEvaluateTemplateFile)receiver)._properties.get(path);
            if (value == null) {
                try {
                    value = super.getFieldValue(receiver, fieldPath);
                } catch (FieldValueException e) {
                    // ignore (return null for missing key)
                }
            }
            return value;
        }
    }
}
