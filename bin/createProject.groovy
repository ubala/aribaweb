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
    Design:
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
ParamsFileName = "templateInfo.table"
SubstFileExtension = ".awtmpl"

// We may be invoked by the IDE plugins with Param* variables bound
try {
    //If params are bound this call will proceed, otherwise we'll get an exception
    applyTemplate(ParamTemplateDir,ParamProjectDir,ParamConfigMap);
    return;
} catch(MissingPropertyException e) {
    //No params: we are running from command line and should proceed normally
}

assert args.length == 1, usage
File templatesDir = new File(args[0])
assert templatesDir.exists(), "Supplied template directory not found: ${templatesDir}"

input = new Scanner(System.in)

File baseDir = new File(".")
if (new File(baseDir, "createProject.groovy").exists()) baseDir = new File(System.properties["user.home"])
println "(Default directory: ${baseDir.getCanonicalPath()})"

String path = (String)getChoice("Enter name/path for your project (directory)", "MyApp", null)
File projectDir = new File(path)
if (!projectDir.isAbsolute()) projectDir = new File(baseDir, path)
assert !projectDir.exists(), "Directory matching project name already exists: ${projectDir}"

def chosenTemplate = selectTemplate(templatesDir)

String projectName = projectDir.name
Map params = [ProjectName:projectName]
queryParams(chosenTemplate, params)

println "Applying template ${chosenTemplate.title} (${chosenTemplate.dir}) with params: ${params}"
projectDir.mkdirs()
applyTemplate(chosenTemplate.dir, projectDir, params)

File ideTemplatesDir = new File(templatesDir, "IDE")
if (ideTemplatesDir.exists()) {
    println "."
    println "IDE Integration:"
    def ideTemplate = selectTemplate(ideTemplatesDir)

    queryParams(ideTemplate, params)

    println "Applying IDE template ${ideTemplate.title} (${ideTemplate.dir}) with params: ${params}"
    applyTemplate(ideTemplate.dir, projectDir, params)
    String os = ((String)System.getProperties()["os.name"]).replace(" ", "").toLowerCase()
    boolean isMac = os.startsWith("macos"), isWin = os.startsWith("windows")

    if (isMac || isWin) {
        String openCmd = (isMac) ? "open" : "start"
        String choice = getChoice("What next?  o) open project, r) run app, b) both, x) exit", "o", null)

        if (choice.startsWith("o") || choice.startsWith("b")) {
            File openFile = projectDir
            if (ideTemplate.autoOpenFile) {
                openFile = new File(projectDir, substParams(ideTemplate.autoOpenFile, params))
            }
            println "Opening ${openFile}..."
            try {
               "${openCmd} ${openFile}".execute()
            } catch (Exception e) {
                println "Error running ${openCmd}: ${e.message}"
            }
        }
        if (choice.startsWith("r") || choice.startsWith("b")) {
            String antExe = new File(new File(System.getenv()["ANT_HOME"], "bin"), (isWin ? "ant.bat" : "ant")).getAbsolutePath()
            Process p = "${antExe} -f ${projectDir}/build.xml launch".execute()
            p.consumeProcessOutput(System.out, System.err)
            p.waitFor()
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

def selectTemplate (File templatesDir) {
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

    Map template = templateConfigs[0]
    if (templateConfigs.size() > 0) {
        println "Please choose a template from among the following:"
        int i = 1;
        templateConfigs.each { config ->
            println "    [${i++}] ${config.title}"
            config.description.split("\n").each { println "           ${it}" }
            println ""
        }
        int choice = (int)getChoice("Selection", 1) {
                        int c = Integer.valueOf(it)
                        return (c >0 && c <= templateConfigs.size()) ? c : null
                     }
        template = templateConfigs[choice - 1]
    }
    return template
}

def queryParams (template, params) {
    template.parameters?.each { param ->
        params[param.key] = getChoice(param["description"], param["default"], null)
    }
}

def getChoice (String prompt, Object defaultVal, conversionClosure) {
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

def applyTemplate (File templateDir, File projectDir, Map params) {
    templateDir.eachFileRecurse { File f ->
        String relativePath = f.getCanonicalPath().substring(templateDir.getCanonicalPath().length())
        relativePath = macroPath(relativePath, params).replaceAll(SubstFileExtension + '$', "")
        File destFile = new File(projectDir, relativePath)
        if (f.isDirectory()) {
            println "    Creating dir: ${destFile}"
            destFile.mkdirs()
        } else if (f.name != ParamsFileName && f.name != ".DS_Store") {
            if (f.name.endsWith(SubstFileExtension)) {
                println "        ... creating file ${f} to ${destFile}"
                evalCopyFile(f, destFile, params)
            } else {
                println "        ... copying file ${f} to ${destFile}"
                copyFile(f, destFile)
            }
        }
    }
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
    String contents = substParams(src.text, params)
    // println ("${dest}: ${contents}")
    dest.write(contents)
}

String substParams (String str, Map params) {
    str.replaceAll(/@(\w+)@/) { a, v ->
        def p = params[v];
        assert p, "Reference to unbound parameter ${v}"
        p
    }
}

def copyFile (File src, File dest) {
    AWUtil.streamCopy(new FileInputStream(src), new FileOutputStream(dest))
}
