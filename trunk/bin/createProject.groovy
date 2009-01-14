/**
    createProject.groovy
 */
import ariba.util.core.*
import ariba.ui.aribaweb.util.*

def usage = """
    createProject.groovy <templateDirectory>
        - Prompts for ProjectName, choice of template, and any template-specific parameters
        - Creates new project directory based on template
"""

/**
    Design

        tools/templates
            Foo.projectTemplate/
                templateInfo.table
                <other files>
                    Main.awl            <-- copied verbatim
                    Main.java.awtmpl    <-- .awtmpl stripped from name, and run through substitution

            Bar.projectTemplate/
                ...

        Builds list of templates from *.projectTemplate

        templateInfo.table contains config
            {
                title = "MetaUI DB App Template";
                description = "Blah blah blah....";
                parameters = (
                 {
                     key="DomainClass";
                     description="Class name for first persistent object class";
                     default="Entry";
                 },
                 {
                     key="DomainPackage";
                     default="app.model";
                 }
            }
 */
String ParamsFileName = "templateInfo.table"
String SubstFileExtension = ".awtmpl"

assert args.length == 1, usage
File templatesDir = new File(args[0])
assert templatesDir.exists(), "Supplied template directory not found: ${templatesDir}"

templateConfigs = []
templatesDir.eachDir { File dir ->
    if (dir.name.endsWith(".projectTemplate")) {
        File configFile = new File(dir, ParamsFileName);
        if (!configFile.exists()) {
            println "warning: Project dir missing templateInfo.table file: ${configFile}"
        } else {
            Map config = [:];
            MapUtil.fromSerializedString (config, configFile.text)
            config.dir = configFile.parentFile
            templateConfigs += config
        }
    }
}

assert templateConfigs.size() > 0, "No templates found in supplied templates dir: ${templatesDir}"

// sort templates by rank
templateConfigs = templateConfigs.sort { a, b ->
        (a.rank ? Integer.parseInt(a.rank) : 100) <=> (b.rank ? Integer.parseInt(b.rank) : 100)
}

def input = new Scanner(System.in)
// println "${properties.greeting} -- In 10 years you will be ${ + 10}"

Map chosenTemplate = templateConfigs[0]
if (templateConfigs.size() > 0) {
    println "Please choose a template from among the following:"
    int i = 1;
    templateConfigs.each { config ->
        println "    [${i++}] ${config.title}"
        config.description.split("\n").each { println "           ${it}" }
        println ""
    }
    int choice = (int)getChoice(input, "Selection", 1) {
                    int c = Integer.valueOf(it)
                    return (c >0 && c <= templateConfigs.size()) ? c : null
                 }
    chosenTemplate = templateConfigs[choice - 1]
}

String projectName = (String)getChoice(input, "Enter name/path for your project (directory)", "MyApp", null)

Map params = [ProjectName:projectName];
chosenTemplate.parameters?.each { param ->
    params[param.key] = getChoice(input, param["description"], param["default"], null)
}

File projectDir = new File(projectName)
assert !projectDir.exists(), "Directory matching project name already exists: ${projectDir}"

println "Applying template ${chosenTemplate.title} (${chosenTemplate.dir}) with params: ${params}"

projectDir.mkdirs()

File templateDir = chosenTemplate.dir
templateDir.eachFileRecurse { File f ->
    String relativePath = f.getCanonicalPath().substring(templateDir.getCanonicalPath().length())
    relativePath = macroPath(relativePath, params).replaceAll(SubstFileExtension + '$', "")
    File destFile = new File(projectDir, relativePath)
    if (f.isDirectory()) {
        println "Creating dir: ${destFile}"
        destFile.mkdirs()
    } else if (f.name != ParamsFileName && f.name != ".DS_Store") {
        if (f.name.endsWith(SubstFileExtension)) {
            println "Eval and copying file ${f} to ${destFile}"
            evalCopyFile(f, destFile, params)
        } else {
            println "Copying file ${f} to ${destFile}"
            copyFile(f, destFile)
        }
    }
}


println """

DONE CREATING YOUR NEW APPLICATION!

To run:
  % cd ${projectName}
  % aw ant launch

(The app should build, run and open in your browser)

"""

def getChoice (Scanner input, String prompt, Object defaultVal, conversionClosure) {
    def choice = null
    while (!choice) {
        print "${prompt} [${defaultVal}]: "
        String response = input.nextLine()
        if (!response || response == "") {
            choice = defaultVal
        } else {
            choice = (conversionClosure) ? conversionClosure(response) : response
            if (choice == null) println "Invalid entry."
        }
    }
    return choice
}

String macroPath (String path, Map params) {
    // path entries of form _Key_ should be swapped with params
    return path.replaceAll(/_(\w+)_/) { a, v ->
        def p = params[v];
        assert p, "Reference to unbound parameter ${v}"
        p.replace('.','/')
    }
}

def evalCopyFile (File src, File dest, Map params) {
    String contents = src.text.replaceAll(/@(\w+)@/) { a, v ->
        def p = params[v];
        assert p, "Reference to unbound parameter ${v}"
        p
    }
    // println ("${dest}: ${contents}")
    dest.write(contents)
}

def copyFile (File src, File dest) {
    AWUtil.streamCopy(new FileInputStream(src), new FileOutputStream(dest))
}
