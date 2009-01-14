String usage = """
Usage:
    processdoc templatefile txtrootdir... outputdir

        templatefiles:  awl template to use to format txt files
        txtrootdir...:  one or more root directories to scan for .txt files to process
        outputdir:      root directory for output
"""
import ariba.ui.aribaweb.util.AWEvaluateTemplateFile
import ariba.ui.aribaweb.core.AWConcreteApplication
import ariba.ui.aribaweb.util.AWNamespaceManager

assert args.length >= 3, usage

// println "Sleeping for debugger attach..."; Thread.currentThread().sleep(5000)

File templateFile = new File(args[0])
List inputDirs = args[1..-2].collect() { new File(it) }
File outputDir = new File(args[-1])

// inputDirs.each { println "input dir: $it" }
// System.exit(0)

// Need to override groovy class loader with one for AW so jar resources are found...
Thread.currentThread().setContextClassLoader(AWConcreteApplication.class.getClassLoader())

AWConcreteApplication application = (AWConcreteApplication)AWConcreteApplication.defaultApplication()

// Need to set resolver for AWEvaluateTemplate to include widgets
AWNamespaceManager ns = AWNamespaceManager.instance();
ns.registerResolverForPackage("ariba.ui.aribaweb.util", ns.resolverForPackage("ariba.ui.widgets"));

inputDirs.each { File dir ->
    if (!dir.exists()) {
        println "Warning: input dir not found: $dir"
    } else {
        int prefixLen = dir.getCanonicalPath().length()
        println "Scanning $dir"
        dir.eachFile { File file ->
            if (file.name.endsWith(".txt")) {
                String relativePath = file.getCanonicalPath().substring(prefixLen)
                File outputFile = new File(outputDir, relativePath.replaceAll(/\.txt$/, ".html"))
                println "    ... processing $relativePath ..."
                String markdown = file.text
                String title = file.name.replace('_', ' ')
                AWEvaluateTemplateFile page = AWEvaluateTemplateFile.create(templateFile,
                        [contents:markdown, title:title])
                page.process(outputFile)
            }
        }
    }
}
