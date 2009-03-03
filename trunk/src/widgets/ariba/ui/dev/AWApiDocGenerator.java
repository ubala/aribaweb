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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWApiDocGenerator.java#2 $
*/
package ariba.ui.dev;

import ariba.util.core.Assert;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWApi;
import ariba.ui.aribaweb.core.AWConcreteTemplate;
import ariba.ui.aribaweb.core.AWApplication;
import ariba.ui.aribaweb.core.AWBindingApi;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWNamespaceManager;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

public class AWApiDocGenerator extends AWComponent
{
    public AWApi _api;
    public String _title;
    public Object _currentBinding;
    public Object _currentContent;

    /**
        Merge AWAPI doc html into given JavaDoc root directory for all
        .awls found under given list of source roots.

        java ... -class ariba.ui.aribaweb.util.AWApiDocGenerator <javadocdir> <xsddir> <srcroot1> [<srcroot2> ...]

        The (package) directory structure for .awl files found under each sourceroot
        should be replicated under the destdir (for the already generated JavaDoc)

        @param args array of paths
     */
    public static void main (String[] args)
    {
        System.out.println("*** In AWApiDocGenerator!  Args: " + Arrays.toString(args));
        Assert.that(args.length >= 3, "Incorrect number of args.  Usage: java ... -class ariba.ui.aribaweb.util.AWApiDocGenerator <javadocdir> <xsddir> <srcroot1> [<srcroot2> ...]");
        final File destDir = new File(args[0]);
        Assert.that(destDir.exists(), "Destination dir does not exist: %s", destDir);
        final File xsdDir = new File(args[1]);
        Assert.that(xsdDir.exists(), "XSD destination dir does not exist: %s", xsdDir);

        // force initialization of the default application
        final AWConcreteApplication application = (AWConcreteApplication)AWConcreteApplication.defaultApplication();
        AWComponent.defaultTemplateParser().registerContainerClassForTagName("a:Api", AWApi.class);

        mergeAWApiDoc(args, destDir, application);
        fixPackageOverviews(destDir, application);
        writeXSDFiles(xsdDir);
    }


    private static void mergeAWApiDoc (String[] args, final File destDir, final AWConcreteApplication application)
    {
        for (int i=2; i<args.length; i++) {
            File srcDir = new File(args[i]);
            Assert.that(srcDir.exists(), "Source dir does not exist: %s", srcDir);
            final int srcDirPathLen = srcDir.getAbsolutePath().length();
            AWUtil.eachFile(srcDir,
                new FileFilter(){
                    public boolean accept(File file)
                    {
                        return file.isDirectory() || file.getName().endsWith(".awl") || file.getName().endsWith(".awapi");
                    }
                },
                new AWUtil.FileProcessor () {
                    public void process(File file)
                    {
                        String relativePath = file.getParentFile().getAbsolutePath().substring(srcDirPathLen+1);
                        generateDoc(file, destDir, relativePath, application);
                    }
                });
        }
    }

    static final String _ClassHeaderMarker = "<!-- ======== START OF CLASS DATA ======== -->";

    public static void generateDoc (File templateFile, File destRoot, String relativePath, AWApplication application)
    {
        File destDir = new File (destRoot, relativePath);
        String componentName = AWUtil.stripLastComponent(templateFile.getName(), '.');
        // System.out.println("Generating doc for " + templateFile + " to " + destDir);
        try {
            AWConcreteTemplate template = (AWConcreteTemplate)AWComponent.defaultTemplateParser().templateFromInputStream(new FileInputStream(templateFile), templateFile.getName());
            AWApi api = template.removeApiTag();
            String docString = null;
            String referenceName = referenceNameForComponent(relativePath, componentName);

            if (api != null) {
                // generate response as string
                AWApiDocGenerator page = (AWApiDocGenerator)AWComponent.createPageWithName(AWApiDocGenerator.class.getName());
                page._api = api;
                page._title = referenceName;

                docString = page.generateStringContents();
            }

            String destFileName = componentName.concat(".html");
            File destFile = new File(destDir, destFileName);

            if (!destFile.exists()) {
                System.out.println("WARNING: missing destination javadoc file (" + destFile + ") -- skipping");
                return;
            }

            String javaDoc = AWUtil.stringWithContentsOfFile(destFile);
            String newJavaDoc = javaDoc;

            if (docString != null) {
                int pos = javaDoc.indexOf(_ClassHeaderMarker);
                if (pos == -1) {
                    System.out.println("WARNING: unable to find class header in javadoc (" + destFile + ") -- skipping");
                    return;
                }

                newJavaDoc = javaDoc.substring(0, pos).concat(docString).concat(javaDoc.substring(pos));
            }

            // remove fields block
            int sPos = newJavaDoc.indexOf("<!-- =========== FIELD SUMMARY =========== -->");
            int ePos = newJavaDoc.indexOf("<!-- ======== CONSTRUCTOR SUMMARY ======== -->");
            if (sPos != -1 && ePos != -1) {
                newJavaDoc = newJavaDoc.substring(0, sPos).concat(newJavaDoc.substring(ePos));
            }

            AWUtil.writeToFile(newJavaDoc, destFile);

            if (api != null) {
                generateXSD(api, referenceName, componentName);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    static class Holder { PrintWriter pw; StringWriter sw; }

    static Map<String, Holder> _XsdByNamespace = new HashMap();

    static void generateXSD (AWApi api, String referenceName, String componentName)
    {
        int colonPos = referenceName.indexOf(':');
        if (colonPos == -1) return;

        String namespace = referenceName.substring(0, colonPos);
        String baseName = referenceName.substring(colonPos + 1);
        Holder h = _XsdByNamespace.get(namespace);
        PrintWriter out;
        if (h == null) {
            h = new Holder();
            h.sw = new StringWriter();
            out = h.pw = new PrintWriter(h.sw);
            _XsdByNamespace.put(namespace, h);
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            out.println("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">");
            out.printf("<!-- definition of AribaWeb elements for namespace '%s' -->\n", namespace);
        } else {
            out = h.pw;
        }
        out.printf("<xs:element name=\"%s\">\n", baseName);

        out.printf("    <xs:complexType>\n" +
                   "        <xs:sequence>\n");

        // list of named contents?

        out.printf("            <xs:any minOccurs=\"0\"/>\n" +
                   "        </xs:sequence>\n");

        // list of attributes
        generateXSDAttributes(api, out);

        out.printf("        <xs:anyAttribute/>\n" +
                   "    </xs:complexType>\n");
        out.printf("</xs:element>\n\n");
    }

    static void generateXSDAttributes (AWApi api, PrintWriter out)
    {
        for (AWBindingApi binding : api.bindingApis()) {
            // is there some way to indicate html attributes okay here? (like with ref to xhtml xsd?)
            if (binding.key().equals("*")) continue;

            out.printf("        <xs:attribute name=\"%s\" type=\"xs:string\"%s/>\n",
                    binding.key(),
                    (binding.isRequired() ? " use=\"required\"" : ""));
        }
    }

    private static String referenceNameForComponent(String packagePath, String componentName)
    {
        String pkg = packagePath.replace('/', '.').replace('\\', '.');
        AWNamespaceManager ns = AWNamespaceManager.instance();
        AWNamespaceManager.Resolver resolver = ns.resolverForPackage(pkg);
        String refName = (resolver != null) ? resolver.referenceNameForClassName(pkg, componentName) : null;
        if (refName == null) refName = componentName;
        return refName;
    }

    static void writeXSDFiles (File destDir)
    {
        for (String ns : _XsdByNamespace.keySet()) {
            File outfile = new File(destDir, ns + "-api.xsd");
            Holder h = _XsdByNamespace.get(ns);
            PrintWriter out = h.pw;
            out.println("</xs:schema>");

            System.out.printf("Writing xsd file: %s\n", outfile);
            AWUtil.writeToFile(h.sw.toString(), outfile);
        }
    }


    private static void fixPackageOverviews (final File destDir, final AWConcreteApplication application)
    {
        AWUtil.eachFile(destDir,
            new FileFilter(){
                public boolean accept(File file)
                {
                    return file.isDirectory() || file.getName().equals("package-summary.html");
                }
            },
            new AWUtil.FileProcessor () {
                public void process(File file)
                {
                    if (!file.isDirectory()) fixPackageOverview(file);
                }
            });
    }

    static final String TopStartMarker = "<!-- ========= END OF TOP NAVBAR ========= -->";
    static final String TopEndMarker = "<A HREF=\"#package_description\"><B>Description</B></A>";
    static final String DescriptionStartMarker = "<A NAME=\"package_description\">";
    static final String DescriptionEndMarker = "<!-- ======= START OF BOTTOM NAVBAR ====== -->";

    private static void fixPackageOverview (File file)
    {
        String javaDoc = AWUtil.stringWithContentsOfFile(file);
        int tS = javaDoc.indexOf(TopStartMarker), tE = javaDoc.indexOf(TopEndMarker);
        int dS = javaDoc.indexOf(DescriptionStartMarker), dE = javaDoc.indexOf(DescriptionEndMarker);
        if (tS != -1 && tE != -1 && dS != -1 && dE != -1) {
            System.out.println("Moving package description to top : " + file);                        
            String newJavaDoc = javaDoc.substring(0, tS + TopStartMarker.length())
                    + javaDoc.substring(dS, dE)
                    + javaDoc.substring(tE + TopEndMarker.length(), dS)
                    + javaDoc.substring(dE);
            AWUtil.writeToFile(newJavaDoc, file);
        } else {
            System.out.println("WARNING: unable to find markers in package-summary.html file (" + file + ") -- skipping");
        }
    }
}
