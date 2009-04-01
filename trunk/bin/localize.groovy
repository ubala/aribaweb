// ----- Global variables -----------------------------------------------------------------

update_option = 1;
bak_option = 0;
warningCount = 0;
summaryUpdateNeeded = 0;

stringTableFileContents = [:];
stringTableFileDirVisited = [:];
stringTableFileUpdateNeeded = [:];

// test();  System.exit(0)

def cl = new ArgParser('groovy localize.groovy [-u] [-l base_locale] [-b] [-n] [-w] [-l] -d "componentDir" [files/locales]*',
   h:[longOpt:'help', desc:'Show usage information and quit'],
   d:[longOpt:'directory', args:1, required:true, desc:'Directory to Search, REQUIRED'],
   n:[longOpt:'noupdate', desc:'no update - just scan'],
   w:[longOpt:'writable', desc:'writable files only'],
   b:[longOpt:'backup-files', desc:'create .bak files for modified files'],
   u:[longOpt:'update-locales', desc:'create/update localized files (given list, or all)'],
   l:[longOpt:'base-locale', desc:'the base locale to write to (default is en_US)'],
   m:[longOpt:'meta-externalize', desc:'externalize Meta classes']
);
def opt = cl.parse(args);

if (!opt || opt.h) {
    cl.usage()
    System.exit(0)
}
update_option = !opt.n;
bak_option = opt.b
base_locale = opt.l ?: "en_US"

File projDir = new File(opt.d)
assert projDir.isDirectory(), "Specified project directory does not exist: ${proDir}"

if (opt.m) {
    List packages = packagesInDir(projDir)
    processMetaStrings(projDir, packages)
} else if (opt.u) {
    List locales = opt.arguments.size() ? opt.arguments : existingLocalesInDir(projDir)
    List stringsFiles = stringsFilesInDir(projDir, base_locale)
    if (!locales.size()) dieError("No exiting locales, and none specified (must provide list)")
    if (!stringsFiles.size()) dieError("No strings files found for base locale (${base_locale}) -- try externalizing first")
    updateLocaleFiles (projDir, locales, stringsFiles)
} else {
    List files = opt.arguments.size() ? opt.arguments.collect { def f = new File(it); assert f.exists(), "Invalid file $f"; f } :
                                        localizableFilesInDir(projDir);
    if (opt.w) files = files.findAll() { it.canWrite() }
    if (files.size() == 0) dieError("No files to scan")

    processFiles(projDir, files)
}
System.exit(0)

def test ()
{
    println "OUT: ${awl_parse_AWLocal_body("Hello <a:Hyperlink>foo</a:Hyperlink> <b style='\$style'>there</b>")}"

    def rootPath = "/Users/craigf/dev/roots-mainline/ariba/platform/ui/widgets"
    def paths = [ "${rootPath}/ariba/ui/widgets/ChooserPanel.awl",
                 "${rootPath}/ariba/ui/widgets/OutlineBox.java"];
    processFiles(new File(rootPath), paths.collect { new File(it) })
}

def packagesInDir (File dir)
{
    def packageDirs = new HashSet()
    dir.eachFileRecurse { file ->
        if (file.isFile() && file.parentFile != dir) packageDirs += file.parentFile;
    }
    return packageDirs.collect { f -> relativePath(dir, f).replace("/", ".") }
}

def localizableFilesInDir (File dir)
{
    def result = []
    dir.eachFileRecurse { file ->
        if(file.isFile() && ["awl", "oss", "java", "groovy"].contains(fileExt(file.getName()))
                && !relativePath(dir, file).startsWith("build/")) {
            result += file;
        }
    }
    return result
}

def stringsFilesInDir (File dir, String defaultLocale)
{
    def result = []
    String localDirSuf = "/$defaultLocale/strings".toString()
    dir.eachFileRecurse { file ->
        if (file.isFile() && fileExt(file.getName()) == "csv"
              && file.parent.replace("\\", "/").endsWith(localDirSuf)
              && !relativePath(dir, file).startsWith("build/")) {
            result += file;
        }
    }
    return result
}

def existingLocalesInDir (File dir)
{
    def locales = new HashSet()
    String localDirSuf =
    dir.eachFileRecurse { file ->
        def m
        if (file.isFile() && (m = file.path =~ /.*\/([\w\_]+)\/strings\/[^\/]+\.csv/)) locales += m.group(1);
    }
    locales.remove(base_locale)
    return new ArrayList(locales)
}

def fileExt (String path)
{
    def m = path =~ /\.(\w+)$/
    return m ? m.group(1) : ""
}

def fileRemoveExtension (String path)
{
    return path.replaceFirst(/\.(\w+)$/, "")
}

def defaultCodeSetForLocale (locale)
{
    def LocalCodeSets = [
        "ja" : "Shift_JIS",
        "ko" : "KS_C_5601-1987",
        "zh_CN" : "GB2312",
        "zh_TW" : "Big5",
        "ru" : "Cp1251"
    ]
    return LocalCodeSets[locale] ?: "Cp1252"
}

def updateLocaleFiles (File projDir, List locales, List stringsFiles)
{
    println "Updating locales: ${locales}.  Files: ${stringsFiles.collect {it.name}}..."
    stringsFiles.each { baseFile ->
        def basePath = baseFile.getPath().replace("\\", "/");
        def baseStrings, baseCodeSet; (baseStrings, baseCodeSet) = parseCSVFileIntoStringTable(basePath);
        locales.each { locale ->
            def localePath = basePath.replace("/${base_locale}/strings/", "/${locale}/strings/");
            // println "basePath = ${baseFile.path}, localePath = ${localePath}"
            File localeFile = new File(localePath)
            def localeStrings, localeCodeSet;
            if (localeFile.exists()) {
                (localeStrings, localeCodeSet) = parseCSVFileIntoStringTable(localePath);
            } else {
                localeStrings = [:]
                localeFile.parentFile.mkdirs()
            }
            if (!localeCodeSet) localeCodeSet = defaultCodeSetForLocale(locale) + "\n"
            
            def mergedStrings = mergeIntoLocaleStrings(localeStrings, baseStrings)
            /*
            println "Locale ${locale}:\nOrig Locale Strings:${localeStrings}\nBase Locale Strings:${baseStrings}"
            println "Merged Strings:${mergedStrings}"
            println "Diff? ${localeStrings == mergedStrings ? "no" : "CHANGED"}"
            println ""
            */
            processStringTableCSVFile (localePath, mergedStrings, localeCodeSet)
        }
    }
}

def mergeIntoLocaleStrings(localeStrings, baseStrings)
{
    def merged = [:]
    // localeStrings.each { k, v -> merged[k] = v.clone() }
    baseStrings.each { fileName, strings ->
        Map lStrings = localeStrings[fileName]
        Map mStrings = [:]
        merged[fileName] = mStrings
        strings.each { key, csvString ->
            if (lStrings && lStrings[key]) {
                def base, trans, comment; (base, trans, comment) = parseCSVLine(csvString)
                def lbase, ltrans, lcomment; (lbase, ltrans, lcomment) = parseCSVLine(lStrings[key])
                csvString = (base == lbase) ? lStrings[key] : "${csvQuoted(base)},${csvQuoted(ltrans)},${csvQuoted(comment + "@")}"
            }
            mStrings[key] = csvString;
        }
    }
    return merged
}

def writeUpdatedFiles ()
{
    processStringTableCSVFiles();

    printLog("info", "file scan completed");

    def updateReallyNeeded = !update_option && summaryUpdateNeeded;

    if (warningCount > 0 && updateReallyNeeded) {
        printLog("warn", "localize script needs to be run in update mode, and there were warningCount warnings about localize problems that need to be fixed.");
        System.exit(3);
    }
    else if (warningCount > 0) {
        printLog("warn", "There were warningCount warnings about localize problems that need to be fixed.");
        System.exit(3);
    }
    else if (updateReallyNeeded) {
        printLog("warn", "localize script needs to be run in update mode!");
        System.exit(2);
    }
    else {
        printLog("info", "localization is up-to-date!");
        System.exit(0);
    }
}

def processMetaStrings(File projDir, List packages)
{
    // println "Waiting for debugger attach..."; Thread.currentThread().sleep(5000)
    // Need to override groovy class loader with one for AW so jar resources are found...
    ClassLoader loader = ariba.ui.aribaweb.core.AWConcreteApplication.class.getClassLoader()
    Thread.currentThread().setContextClassLoader(loader)
    def Externalize = Class.forName("ariba.ui.meta.core.Externalize", true, loader)
    ariba.ui.meta.persistence.PersistenceMeta.setDoNotConnect(true)
    def meta = Externalize.initialize()
    Map byPackage = Externalize.newInstance(meta).stringsForPackages(packages);
    // println "Results : ${byPackage}"
    byPackage.each { pkg, byFile ->
        def stringTableName, packageName;
        (stringTableName, packageName) = stringTablePathForPackage(projDir, pkg)
        def stringsByFile = [:]
        byFile.each { fileName, byKey ->
            def stringsByKey = [:];
            byKey.each { key, vals ->
                stringsByKey[key] = "${csvQuoted(vals[0])},${csvQuoted(vals[1])},${csvQuoted(vals[2])}"
            }
            stringsByFile[fileName+".oss"] = stringsByKey
        }
        registerStrings(stringTableName, stringsByFile, true)
    }
    // println "Results: $stringTableFileContents"
    writeUpdatedFiles()
}

def processFiles (File projDir, List files)
{
    files.each { processFile(projDir, it) }

    writeUpdatedFiles();
}


def processFile (rootDir, file)
{
    def updateNeeded = 0;
    def updatedContents = "";
    Map stringTableRef = [:];
    List warnings = [];
    def absPath = file.getAbsolutePath().replace("\\", "/")

    def stringTableFile, packageName; (stringTableFile, packageName) = composeStringTableFile(rootDir, file);

    println "Checking ${relativePath(rootDir, file)}..."
    
    if (stringTableFile && packageName && (new File(absPath)).isDirectory()) {
        //D// print "DEBUG: absPath - Scanning packageName\n";
        stringTableFileDirVisited[stringTableFile] = 1;
    }
    else if (absPath.endsWith(".awl") || absPath.endsWith(".oss")) {
        //D// print "DEBUG: absPath\n";
        //D// print "DEBUG: before awl_proces_file\n";

        (updateNeeded, updatedContents) =
            awl_process_file(absPath, update_option, warnings);
        stringTableRef = awl_generate_strings(absPath, updatedContents, warnings);
    }
    else if (absPath.endsWith(".java") || absPath.endsWith(".groovy")) {
        (updateNeeded, updatedContents) =
            java_process_file(absPath, update_option, warnings);
        stringTableRef = java_generate_strings(absPath, updatedContents, warnings);
        //D// print "DEBUG: 'absPath' stringTableFile='stringTableFile' packageName='packageName' updateNeeded=updateNeeded\n";
    }

    if (updateNeeded) {
        summaryUpdateNeeded = 1;
    }

    if (stringTableRef) {
        //D// print "DEBUG: absPath got stringTableRef...\n";
        if (!stringTableFile) {
            dieError("File absPath needs localizing, but is not under a component ariba/... directory.");
        }
        registerStrings(stringTableFile, stringTableRef, updateNeeded)
    }

    if (warnings) {
        // If there are warnings, we sort them into line number order, and print them, in a
        // batch for each file.  Due the the multiple passes we make in the processing, the
        // warnings would not come out in order otherwise.
        printWarnings(warnings);
        warningCount += warnings.size();
    }
}

def registerStrings (stringTableFile, stringTableRef, updateNeeded)
{
    if (updateNeeded) {
        stringTableFileUpdateNeeded[stringTableFile] = 1;
    }

    def tableContentsRef = stringTableFileContents[stringTableFile];

    if (tableContentsRef) {
        mergeStringTableContents(tableContentsRef, stringTableRef);
    }
    else {
        stringTableFileContents[stringTableFile] = stringTableRef;
    }
}

def processStringTableCSVFiles ()
{
    stringTableFileContents.each { stringTableFilePath, newStringTableRef ->
        def dirVisited = stringTableFileDirVisited[stringTableFilePath];
        if (!dirVisited) {
            dirVisited = 0;
        }
        def updateNeeded = stringTableFileUpdateNeeded[stringTableFilePath];
        def codesetLine = "Cp1252\n";
        if ((new File(stringTableFilePath)).exists()) {
            def oldStringTableRef, oldCodesetLine;
            (oldStringTableRef, oldCodesetLine) = parseCSVFileIntoStringTable(stringTableFilePath);
            if (oldCodesetLine) {
                codesetLine = oldCodesetLine;
            }
            if (!dirVisited) {
                newStringTableRef = mergeStringTableContents(oldStringTableRef, newStringTableRef);
            }
        }
        processStringTableCSVFile(stringTableFilePath, newStringTableRef, codesetLine);
    }
}


// Return (stringTableRef,codesetLine) for existing csv file.
// The stringTableRef is in the format we build when we parse java or awl file.
// the codesetLine may be empty if there is not a good line in the existing file.
def parseCSVFileIntoStringTable (path)
{
    def results = [:];

    def codesetLine = "";
    def afterFirstLine = false;
    new File(path).eachLine { line ->
        def skip = false;
        if (!afterFirstLine) {
            afterFirstLine = true;
            // Make sure it starts with letter or digit, and ends with newline,
            // before we use it for our initial codeset line.
            if (line =~ /^[A-Za-z0-9]/) {
                if (!(line =~ /\n$/)) {
                    line = line.replaceAll(/\s*$/, "\n");
                    codesetLine = line;
                    skip = true;
                }
            }
        }

        if (!skip) {
            def baseFileName, key, string1, string2, comment;
            (baseFileName, key, string1, string2, comment) = parseCSVLine(line);
            if (!comment) comment = "";
            if (!string2) string2 = "";

            // Ignore line after first line, if it doesn't have well defined
            // baseFileName, key, and string1.  string1 can be empty.
            if (baseFileName && key && string1)
            {
              def csvComment = csvQuoted(comment);
              def csvString1 = csvQuoted(string1);
              def csvString2 = csvQuoted(string2);
              def jointValue = "$csvString1,$csvString2,$csvComment";
              def fileExt = (key =~ /^\d+$/) ? ".java" : ((key =~ /[a-zA-Z]\d+$/) ? ".awl" : ".oss");
              def bareFileName = "$baseFileName$fileExt";
              def keyValuesRef = results[bareFileName];
              if (!keyValuesRef) {
                  keyValuesRef = [:];
                  results[bareFileName] = keyValuesRef;
              }
              keyValuesRef[key] = jointValue;
            }
        }
    }
    return [results, codesetLine];
}

def processStringTableCSVFile (stringTableFilePath, newStringTableRef, codesetLine)
{
    def oldContents = "";

    File file = new File(stringTableFilePath)
    if (file.exists()) {
        oldContents = file.text
    }
    else {
        if (!(file.getParentFile().isDirectory())) {
            println "${file.getParent()}: directory needs to be created.\n";
        }
    }

    def newLines = [];
    for (String bareFileName : newStringTableRef.keySet()) {
        def baseFileName = fileRemoveExtension(bareFileName);
        def keyValuesRef = newStringTableRef[bareFileName];
        for (String key : keyValuesRef.keySet()) {
            def jointValues = keyValuesRef[key];
            //D// print "DEBUG: retrieved baseFileName='baseFileName' key='key' jointValues=[jointValues]\n";
            def line = "\"$baseFileName\",$key,$jointValues\n";
            newLines.add(line);
        }
    }
    newLines = newLines.sort();
    if (newLines.size()) {
        newLines.add(0, codesetLine);
    }
    def newContents = newLines.join("");

    if (oldContents != newContents && newContents) {
        if (update_option) {
            updateFile(stringTableFilePath, newContents);
        }
        else {
            //D// print "DEBUG: calling reportLinesToFix on csv stringTableFilePath...\n";
            reportLinesToFix(stringTableFilePath, oldContents, newContents, 0);
        }
    }
}

def relativePath(rootDir, file)
{
    def rootPath = rootDir.getCanonicalPath().replace("\\", "/");
    def filePath = file.getCanonicalPath().replace("\\", "/");
    if (!rootPath.endsWith("/")) rootPath += "/"
    assert filePath.startsWith(rootPath)
    return filePath.substring(rootPath.length())
}

def composeStringTableFile (componentDir, file)
{
    def relativePath = relativePath(componentDir, file)
    println "componentDir=${componentDir}, file=${file}, relativePath=${relativePath}"
    def m = relativePath =~ /^(.*?)\/([^\/]+)$/
    if (m.matches()) {
        def packageName = m.group(1)
        if (file.isDirectory()) {
            packageName = relativePath;
        }
        println "packageName = ${packageName} -- (file.isDirectory=${file.isDirectory()})"
        return stringTablePathForPackage(componentDir, packageName)
    }
    return ["", ""];
}

def stringTablePathForPackage (projectDir, packageName)
{
    def ResourcePaths = ["resource/ariba/resource/${base_locale}/strings",
                 "ariba/resource/${base_locale}/strings",
                 "resource/${base_locale}/strings"];

    // If packageName has dots in it before converting slashes to dots,
    // that is bad, and we just return empty strings...
    if (!packageName.contains(".")) packageName = packageName.replace("/", ".");
    def dir = ResourcePaths.find { new File(projectDir, it).isDirectory() }
    if (!dir) dieError("No resource subdirectory of $projectDir exists (options: $ResourcePaths)")
    return [new File(projectDir, "$dir/${packageName}.csv").getPath().replace("\\","/"), packageName]
}



// Merge the contents of stringTableRef into the contents of tableContentsRef,
// superceding any bareFileKey value tables that are already there...

def mergeStringTableContents (tableContentsRef, stringTableRef)
{
    stringTableRef.each { bareFileKey, bareFileValue ->
        tableContentsRef[bareFileKey] = bareFileValue;
    }
    return tableContentsRef;
}

def updateFile (file, contents)
{
    // p4 does not like embedded /./ or //, but we seem to construct it that way... OS doesn't care.
    file = file.replaceAll("/\\.?/", "/");

    printLog ("info", "Updating $file".toString());

    def f = new File(file)
    def dir = f.getParentFile()
    if (!dir.exists()) dieError ("Destination directory for resource files does not exist: $dir")
    if (bak_option) {
        def bakFile = new File(dir, f.getName() + ".bak");
        if (bakFile.exists()) bakFile.delete();
        if (f.exists()) f.renameTo(bakFile);
    } else {
      if (f.exists()) f.delete();
    }
    f.write(contents);
}

def reportLinesToFix (file, originalContents, contents, reportAllLines)
{
    originalContents = originalContents.replaceFirst(/\s+/, "");
    contents = contents.replaceFirst(/\s+/, "");

    def lines1 = originalContents.split(/\n/);
    def lines2 = contents.split(/\n/);

    def size = lines1.size();
    if (size > lines2.size()) {
        size = lines2.size();
    }
    def sizesEqual = lines1.size() == lines2.size();

    def reportCount = 0;
    def lineNum = 0;
    def lineCount = 0;
    def blankLineCount = 0;

    def idx;
    for (idx = 0; idx < size; idx++) {
        if (lines1[idx] != lines2[idx]) {
            if (lineNum <= 0) {
                lineNum = idx + 1;
                lineCount = 1;
                blankLineCount = 0;
            }
            else {
                lineCount++;
                lineCount += blankLineCount;
                blankLineCount = 0;
            }
        }
        else if (lineNum > 0 && lines1[idx] =~/^\s+$/) {
            blankLineCount++;
        }
        else if (lineNum > 0) {
            if (reportAllLines || reportCount <= 0) {
                reportLine(file, lineNum, lineCount);
                reportCount++;
            }
            lineNum = 0;
        }
    }
    if (lineNum <= 0 && !sizesEqual) {
        lineNum = size + 1;
    }
    if (lineNum > 0 && (reportAllLines || reportCount <= 0)) {
        reportLine(file, lineNum, 0);
    }
}


//
// LocalizeAwl.pm : support package for localizing .awl files
//


class LVal { Object val; LVal(v) {val = v} }

def awl_process_file (file, update_option, warningsRef)
{
    def updateNeeded = 0;

    def originalContents = new File(file).text;
    def contents = originalContents;

    def maxRef = new LVal(awl_get_max_index(file, contents));


    contents = contents.replaceAll(/\$\[([^\]\:]*)((?:\:[^\]]*)?)\]/) { all, a, b -> awl_process_brackets(a, b, maxRef) }
    //                  (1      1)(2           2)
    contents = contents.replaceAll(/(?s)(\<(?:AW|a\:)Local\b[^\>]*\>)/) { all, a -> awl_process_AWLocal(a, maxRef) } // |gse;

    // Only write stuff out if we have actually changed it...
    if (contents != originalContents) {
        updateNeeded = 1;
        if (update_option) {
            updateFile(file, contents);
        }
        else {
            reportLinesToFix(file, originalContents, contents, 1);
        }
    }
    return [updateNeeded, contents];
}


def awl_process_AWLocal (entity, maxRef)
{
    def rv = entity;

    def m = rv =~ /(key\s*=\s*\")([^\"\>\n]*)\"/
    if (m) {
        def keyLabel = m.group(1), key = m.group(2)
        if (!(key =~ /^a\d+$/)) {
            maxRef.val++;
            key = "a" + String.format("%03d", maxRef.val);
        }
        rv = rv.replaceFirst(/(key\s*=\s*\")([^\"\>\n]*)\"/, keyLabel + key + "\"")
    }
    else {
        maxRef.val++;
        def key = "a" + String.format("%03d", maxRef.val);

        rv = rv.replaceFirst(/(^\<(?:AW|a\:)Local\b)/, /$1 key=\"${key}\"/);
    }
    return rv;
}

def awl_process_brackets (key, comment, maxRef)
{
    if (!(key =~ /^a\d+$/)) {
        maxRef.val++;
        key = "a" + String.format("%03d", maxRef.val);
    }
    def rv = "\$[${key}${comment}]".toString();
    return rv;
}

// get the highest awl index

def awl_get_max_index (file, contents)
{
    // pass 0 for warningsRef, so we don't give warnings in this preliminary parse pass.
    def strings, comments; (strings, comments) = awl_parseFile(file, contents, 0);

    def max = "000";
    for (String stringKey : strings.keySet()) {
        def m = stringKey =~ /^a(\d+)$/
        if (m && m.group(1) > max) {
            max = m.group(1);
        }
    }
    return Integer.parseInt(max);
}

// ----- awl_generate_strings -------------------------------------------------------------

def awl_generate_strings (file, updatedContents, warningsRef)
{
    def f = new File(file)
    def baseFileName = f.name;
    def bareFileName = fileRemoveExtension(baseFileName);

    def content;
    if (updatedContents) {
        content = updatedContents;
    }
    else {
        content = f.text
    }

    def strings, comments; (strings, comments) = awl_parseFile(file, content, warningsRef);

    // Create a combined hash table of results.
    def keyValues = [:];
    for (String key : strings.keySet().sort()) {
        def csvComment = csvQuoted(comments[key]);
        def csvString = csvQuoted(strings[key]);
        def jointValue = "${csvString},${csvString},${csvComment}";

        keyValues[key] = jointValue;
    }
    def results = [:];
    if (keyValues) {
        results[bareFileName+".awl"] = keyValues;
    }
    return results;
}

def awl_parseFile (file, content, warningsRef)
{
    def strings = [:];
    def comments = [:];
    def lineNumRef = new LVal(1);
    def beginPosRef = new LVal(0);

    if (fileExt(file) == "oss") {
        // OSS has bindings outside quotes.  e.g. $[a002]"Hello There" or $[a003]Hello
        def m = (content =~ /\$\[(a\d+)(?:\:(.*?))?\]\s*(?:(?:\"(.*?)\")|(\w+))/)
        m.each { all, key, comment, quotedString, unquotedString ->
            storeAssociation(key, (quotedString ?: unquotedString), comment, strings, comments, file,
                             content, lineNumRef, beginPosRef, m.start(), warningsRef);
        }
    } else {
        // first pass, match [key] and [key:comment]
        def m = (content =~ /\$\[(a\d+)(?:\:(.*?))?\](.*?)\"/)
        m.each { all, key, comment, string ->
            storeAssociation(key, string, comment, strings, comments, file,
                             content, lineNumRef, beginPosRef, m.start(), warningsRef);
        }

        // Now AWLocals
        lineNumRef.val = 1
        beginPosRef.val = 0;
        m = (content =~ /(?s)<(?:AW|a\:)Local\s+key="(a\d+)"(?:\s+comment="(.*?)")?>(.*?)<\/(?:AW|a\:)Local>/)
        m.each { all, key, comment, string ->
            string = awl_parse_AWLocal_body(string);
            storeAssociation(key, string, comment, strings, comments, file,
                             content, lineNumRef, beginPosRef, m.start(), warningsRef);
        }
    }

    return [strings, comments];
}

def awl_parse_AWLocal_body (string)
{
    //D// print "DEBUG: AWLocal_body: raw string=[string]\n";

    // Turn each backslash into space, trim the string, collapse multiple whitespace to
    // single space.  Prefix each left curly brace with two backslashes.

    string = string.replace('\\'," ").replaceFirst(/^\s+/,"").replaceFirst(/\s+$/, "").replaceAll(/\s+/, " ").replaceAll(/\{/, "\\\\{");

    // looking for AW tags with binding, replace it with {0}
    def arg = 0;
    def string1 = "";
    def string2 = string;

    while (1) {
        // println "string2=" + string2 + "\n\n";
        def m = (string2 =~ /(.*?)<\s*([^>\s]+)(\s*)([^>]*?)(\/>|>(.*?)<\/\2>)(.*)/);
        if (m.matches()) {
            def head = m.group(1);
            def tagname = m.group(2);
            def attrdelimiter = m.group(3);
            def attributes = m.group(4);
            def content = m.group(6);
            def tail = m.group(7);
            //print "******head\n" + head + "\n";
            //print "******tagname\n" + tagname + "\n";
            //print "******attributes\n" + attributes + "\n";
            //print "******content\n" + content + "\n";
            //print "******tail\n" + tail + "\n";

            // Swap dynamic tags with {num}
            // Consider dynamic if a) has $ bindings, 2) tag starts with capital, 3) is namespace tag
            if (attributes =~ /["'](\$|\^).*["']/ || tagname =~ /(\w\:|[A-Z]).*/) {
                if (!content) {
                    string1 = string1 + head + "{" + arg + "/}";
                    string2 = tail;
                }
                else {
                    string1 = string1 + head + "{" + arg + "}";
                    string2 = content + "{/" + arg + "}" + tail;
                }
                arg++;
            }
            else {
                if (!content) {
                    string1 = string1 + head + "<" + tagname + attrdelimiter + attributes + "/>";
                    string2 = tail;
                }
                else {
                    string1 = string1 + head + "<" + tagname + attrdelimiter + attributes + ">";
                    string2 = content + "</" + tagname + ">" + tail;
                }
            }
        }
        else {
            break;
        }
    }
    string = string1 + string2;

    // println "DEBUG: AWLocal_body: final string=[$string]\n";

    return string;
}


def storeAssociation(key, string, comment, strings, comments, file, contents, lineNumRef, beginPosRef, matchBeginPos, warningsRef) {
    if (!comment) comment = "";

    assert key, "Error: nullined key"
    assert string, "Error: nullined string"
    assert contents, "Error: nullined contents"

// println "storeAssociation($key, $string, $comment, $strings)"

    checkLiteralStringForWarnings(string, comment, file, contents, lineNumRef,
                                  beginPosRef, matchBeginPos, warningsRef);

    def oldString = strings[key];
    if (!oldString) {
        strings[key] = string;
        comments[key] = comment;
    }
    else if (warningsRef) {
        def oldComment = comments[key];
        if (!oldComment) oldComment = ""

        if (oldString != string) {
            updateLineNumber(lineNumRef, beginPosRef, contents, matchBeginPos);
            pushWarning(warningsRef, file, lineNumRef.val,
                        "Key key redefined with different localize string \"string\"");
        }
        else if (oldComment != comment) {
            updateLineNumber(lineNumRef, beginPosRef, contents, matchBeginPos);
            pushWarning(warningsRef, file, lineNumRef.val,
                        "Key key redefined with different localize comment \"comment\"");
        }
    }
}

def checkLiteralStringForWarnings (string, comment, file, contents, lineNumRef, beginPosRef,
        matchBeginPos, warningsRef)
{
    if (warningsRef) {
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Fix 1-1OFFH: Warn about bad usage, when %s appears in localize string literal.
        def m = string =~ /(\%[sd])/
        if (m) {
            def percentOp = m.group(1);
            updateLineNumber(lineNumRef, beginPosRef, contents, matchBeginPos);
            pushWarning(warningsRef, file, lineNumRef.val,
                        "Fmt.S with percentOp is not allowed in a localized string, use Fmt.Si with {0}, {1}, etc. instead.");
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Fix 1-1OFGP: Warn about localize string with curly brace parameters, but no
        // obvious parameter doc in comment.

        def missingParams = "";
        Map missingParamsMap;
        (string =~ /\{(\/?\d+\/?)\}/).each { all, match ->
            def pattern = "(\\{${match}\\}|${match})\\s*(\\=|\\bis\\b)";

            //D// print "DEBUG: pattern =~ m%pattern%\n";

            // Sometimes a parameter appears multiple times in the string, and we don't
            // want to complain about it being missing more than once, so we keep a map
            // marking the parameters we have already added a complaint for.

            m = comment =~ pattern
            if (!m && !missingParamsMap[match]) {
                missingParamsMap[match] = 1;
                if (missingParams) {
                    missingParams += " ";
                }
                missingParams += "${match}=explanation";
            }
        }
        if (missingParams) {
            updateLineNumber(lineNumRef, beginPosRef, contents, matchBeginPos);
            pushWarning(warningsRef, file, lineNumRef.val,
                        "Comment needs missingParams for translator");
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Warn about HTML usage, globalization team says it adds errors and expense to
        // translation process.  Ignore <b> and </b>, because they are so common, too many
        // to fix at this time, complaining would be like boy who cries wolf, more
        // important stuff needs fixing currently. -rwells, 2006.1.24.
        // TODO - can't actually warn about HTML constructively, LocalizeAwl.pm and the
        // AribaWeb framework are not actually extracting all the markup...  need to fix
        // before I complain about it.

        ////// if (string =~ m|\<\/?[A-Za-z][^\>]*\>|gs) {
        //////     def match = &;
        //////     if (match != "<b>" && match != "</b>") {
        //////         (lineNumRef, beginPosRef) = updateLineNumber(lineNumRef, beginPosRef, contents, matchBeginPos);
        //////         pushWarning(warningsRef, file, lineNumRef,
        //////                     "HTML markup like match should be passed in via Fmt.Si {0}, {1}, etc., and described in the comment for the translator.");
        //////     }
        ////// }
    }
}


def updateLineNumber (lastLineNumRef, lastBeginPosRef, contents, matchBeginPos)
{
    if (!lastLineNumRef.val)  lastLineNumRef.val = 1
    if (!lastBeginPosRef.val) lastBeginPosRef.val = 0

    assert contents, "Error: nullined contents"
    assert matchBeginPos, "Error: nullined matchBeginPos"

    // Trim down to just the contents since lastBeginPos, and before matchBeginPos.
    contents = contents.substring(lastBeginPosRef.val, matchBeginPos);

    (contents =~ /\n/).each { lastLineNumRef.val++; }
    lastBeginPosRef.val = matchBeginPos
}

def parseCSVLine (line)
{
    // Discard trailing newline and whitespace.
    line = line.replaceFirst(/\s+$/, "")

    // Start by splitting values on the commas.
    def rawValues = line.split(/\,/);

    def results = [];
    def valueInProgress = "";
    for (String rawValue : rawValues) {
        // If we are in a quoted value, put comma and next piece back onto it.
        if (valueInProgress) {
            valueInProgress = valueInProgress + "," + rawValue
        }
        // If this raw value starts a quoted value, then start putting the pieces back together.
        // We tolerate and ultimately discard spaces between comma and starting double quote.
        else if (rawValue =~ /^\s*\"/) {
            valueInProgress = rawValue;
        }
        // If this is just a simple unquoted value, push it onto the results as is, spaces, embedded dquotes, and all.
        else {
            results.add(rawValue);
            continue;
        }
        // Now if the current piece terminates the valueInProgress, de-quote it and push it onto results.
        // We tolerate and ultimately discard spaces between ending double quote and comma or end of line.
        // Fix 1-1OFAO: Don't be fooled by interpreting the starting dquote as the ending dquote.
        if (valueInProgress =~ /^\s*\".*\"\s*$/) {
            results.add(parseQuotedValue(valueInProgress));
            valueInProgress = "";
        }
    }
    // If the last quoted value was still in progress at the end of the line, terminate it and put it out;
    // we don't parse multi-line CSV, which is not standard.  We don't get upset about missing end dquote.
    if (valueInProgress) {
        results.add(parseQuotedValue(valueInProgress));
    }

    return results;
}

def parseQuotedValue (value)
{
    // Strip any leading spaces and starting double quote.
    value = value.replaceFirst(/^\s*\"/, "")
    // Strip ending double quote, and any trailing spaces.
    value = value.replaceFirst(/\"\s*$/, "")
    // Convert double double-quote to single double-quote.
    value = value.replaceAll(/\"\"/, "\"")
    return value;
}

def reportLine (file, lineNum, lineCount)
{
    def forSomething = (lineCount < 1) ?  "for additions or deletions in this file" : "for $lineCount line updates in this file";

    println "${file}:${lineNum}: localize -update script needs to be run ${forSomething}\n";
}

def csvQuoted (string)
{
    // For convenience of handling gets on hashtables of optional values,
    // tolerate nullined values...

    if (!string) string = "";

    // XXX This encoding of newline doesn't seem ideal... but this is what it has been
    // doing for awl strings...  -rwells, 2004.11.13.

    string = string.replaceAll(/\n/, "\\n")
    string = string.replaceAll(/\"/, '""');

    return '"' + string + '"';
}

def cleanWS (rv, atLeastOne)
{
    // If there is a newline in the whitespace, preserve indentation after it.
    // Remove any spaces before the first newline.
    rv = rv.replaceFirst(/^[^\n]+/, "")
    if (atLeastOne && rv == "") {
        rv = " ";
    }
    return rv;
}

def pushWarning (warningsRef, file, lineNum, msg)
{
    if (warningsRef) {
        // Generate leading spaces for line number so alphanumeric sort will give same
        // results as numeric sort on line numbers.
        warningsRef.add([file, lineNum, msg]);
    }
}

def printWarnings (warningsRef)
{
    if (warningsRef) {
        for (List warning : warningsRef.sort()) {
            println "${warning[0]}:${warning[1]}: Warning: ${warning[2]}"
        }
    }
}

def printLog (tag, msg)
{
    // Trim off trailing space and lineends.
    msg = msg.replaceFirst(/^\s+/, "");
    print "[$tag] " + " $msg\n";
}

def dieError (msg)
{
    // Trim off trailing space and lineends.
    msg = msg.replaceFirst(/^\s+/, "");
    printLog("error", msg);
    System.exit(1);
}


// JAVA ----------------------------------------------------------------------------------------
//

def java_generate_strings (file, updatedContents, warningsRef)
{
    def baseFileName = new File(file).getName()
    def bareFileName = fileRemoveExtension(baseFileName)

    def strings, comments; (strings, comments) = java_parseFile(file, updatedContents, warningsRef);

    // Create a combined hash table of results.
    def keyValues = [:];
    for (String key : strings.keySet().sort()) {
        def csvComment = csvQuoted(comments[key]);
        def csvString = csvQuoted(strings[key]);
        def jointValue = "$csvString,$csvString,$csvComment";
        keyValues[key] = jointValue;
    }
    def results = [:];
    if (keyValues) {
        results[bareFileName+".java"] = keyValues;
    }
    return results;
}


def java_parseFile (file, updatedContents, warningsRef)
{
    def contents = updatedContents ?:  new File(file).text;

    def strings = [:];
    def comments = [:];

    contents = java_process_cleanContents(contents);

    def lineNumRef = new LVal(1);
    def beginPosRef = new LVal(0);

    // After hiding:
    // \200 = " preceded by active backslash, or in a comment.
    // \201 = /* within a String literal, or within a comment.
    // \202 = */ within a String literal or within a comment.
    // \203 = { within a String literal, or within a comment.
    // \204 = } within a String literal, or within a comment.
    // \205 = ' within a String literal, or within a comment.
    contents = java_process_hideInactiveDelimiters(contents);

    // EXAMPLE //1: localizedJavaString(20, "Create Test {0}" /* 0=projectTypeName (Project,Contract,...) */);
    // EXAMPLE //2: new LocalizedJavaString(ClassName, 1, "(" /*  */);
    // EXAMPLE //3: localizedJavaString(20, "Create Test {0}" );
    // EXAMPLE //4: new LocalizedJavaString(ClassName, 1, "(");
    // EXAMPLE //5: localizedJavaString(20,
    //                 "Create Test {0} with much longer continuation" +
    //                 " that didn't fit nicely on previous line..." +
    //                 "" /* 0=projectTypeName (Project,Contract,...) */);
    //

    // XXX This example is nearly obsolete, was used in ACM for a few cases in late 2004
    // and early 2005. -rwells, 2005.03.31.
    // EXAMPLE //6: localizedJavaStringHere(20, "Create Test {0}" );

    // This is the new style usage, generated by this script.
    // EXAMPLE //7: localizedJString(20, "Create Test {0}" );

    // Fix 1-1OFI7 and 1-24KMJ: recognize negative keys and string keys.  Negative keys are
    // markers for new key allocation by localize.pl, and string keys are now supported by
    // LocalizeJavaString, but not by localizedJString.
    def m = (contents =~
           /(?:localizedJString\s*\(|localizedJavaString\s*\(|localizedJavaStringHere\s*\(|LocalizedJavaString\s*\(([^,]+),)\s*(-?\d+|\"[^\"]*\")\s*,\s*\"((?:[^\"]*(?:\"\s*\+\s*\")?)*)\"\s*((?:\/\*\/*\**(?:[^\/\*]+\/*\**)*\*\/)?)\s*(?:[^\)]*)\)/);
    m.each { all, rawClassPath, rawKey, string, comment ->
        //                                                                                                          (1   1)     (2              2)         (3                          3)     (4                                    4)
        //
        // 1: first param for LocalizedJavaString, untrimmed, and nullined if not capitalized LocalizedJavaString.
        // 2: numeric key value, may now be negative, or may be a dquoted string literal (for LocalizeJavaString).
        // 3: string literal, without double quotes on ends, with zero or more embedded '" + "' continuations, and \200 for embedded \".
        // 4: comment literal, optional, with comment begin/end, and \200 for embedded \".

        // stringKey has to be String and not int so it works correctly as Hashkey and
        // so it gets sorted alphanumerically rather than numerically.

        def classPath = getCleanedClassPath(rawClassPath);
        def stringKey = "$rawKey";

        // Get information about the starting position in contents, for possible warning
        // messages later.

        def matchBeginPos = m.start();

        // Strip dquotes around key, if any; restore hidden delimiters, if any, in key.

        // Fix 1-24KMJ: If stringKey is dquoted, do normal string literal processing,
        // stripping dquotes around key, processing escape sequences, and then restoring
        // hidden delimiters.  Partly because of prior usage, we don't want to dquote the
        // key in the final csv file unless we have to - few if any keys should actually
        // require dquotes.  We want to be able to sort the keys from now on, without
        // further processing, so we will add dquotes and double embedded dquotes if
        // necessary, right here.

        def isStringKey = 0;
        if (stringKey =~ /^\"([^\"]*)\"$/) {
            isStringKey = 1;
            stringKey = 1;
            stringKey = java_process_parseEscapeSequences(stringKey);
            stringKey = java_process_restoreInactiveDelimiters(stringKey);

            if (stringKey =~ /[\"\,]/) {
                stringKey = csvQuoted(stringKey);
            }
        }

        // comment is matched optionally, it will be nullined if there is no comment.
        if (!comment) comment = ""

        //D// print "DEBUG: raw literal=[string]\n";

        // Remove all '" + "' String literal concatenations in the Java source.
        string = string.replaceAll(/\"\s*\+\s*\"/, "");

        //D// print "DEBUG: literal without continuations=[string]\n";

        string = java_process_parseEscapeSequences(string);

        //D// print "DEBUG: literal after parseEscapeSequence=[string]\n";

        // Restore hidden delimiters in string
        string = java_process_restoreInactiveDelimiters(string);

        //D// print "DEBUG: literal after restoreInactiveDelimiters=[string]\n";

        // Fix: 1-1OFC5: Don't double the double quotes in the string, because we will do
        // that by calling csvQuoted on it in java_generate_strings, our likely caller.

        // Replace actual end of line chars with space, so don't break CSV format later.
        string = string.replaceAll(/(\n|\r\n|\r)/, " ");

        //D// print "DEBUG: literal after whitespace cleanup=[string]\n";

        //D// print "DEBUG: comment before cleanup=[comment]\n";

        // Restore hidden delimiters in comment
        comment = java_process_restoreInactiveDelimiters(comment);

        // Strip leading /* and trailing */.
        comment = comment.replaceFirst(/^\/\*/ ,"").replaceFirst(/\*\/$/, "");

        // Fix: 1-1OFC5: Don't double the double quotes in the comment, because we will do
        // that by calling csvQuoted on it in java_generate_strings, our likely caller.
        // Fix 1-25DME: reduce all runs of whitespace including end of line and tabs into
        // a single space.  This is very good for multi-line comments, keeps csv cleaner.
        // BUT: It causes unnecessary diffs.  So only do it around kernels of badness:
        // newline, carriage return, tab.

        comment = comment.replaceAll(/\s*[\n\r\t]\s*/, " ");

        // Strip leading and trailng spaces.
                comment = comment.replaceFirst(/^\s+/ ,"").replaceFirst(/\s+$/, "");

        //D// print "DEBUG: comment after cleanup=[comment]\n";

        //D// print "DEBUG: literal stored key='stringKey' value=[string]\n";

        // Fix 1-2L620: If it is a quoted string key in Java, then we don't store an
        // association for it and we don't put it in our string csv file, it is a global
        // reference to a string that is manually maintained in its string csv file.  We do
        // validation on the key usge, and check the literal string for warnings needed.
        // But if it is a numeric key with LocalizedJavaString(ClassPath,..., then make
        // sure we go to storeAssociation with it.

        if (isStringKey) {
            validateStringKey(classPath, rawKey, stringKey, file, contents,
                              lineNumRef, beginPosRef, matchBeginPos, warningsRef);
            checkLiteralStringForWarnings(string, comment, file, contents, lineNumRef,
                                          beginPosRef, matchBeginPos, warningsRef);
        }
        else {
            storeAssociation(stringKey, string, comment, strings, comments, file,
                             contents, lineNumRef, beginPosRef, matchBeginPos, warningsRef);
        }
    }
    return [strings, comments];
}

// Process Java Escape sequences for Character and String literals; convert them to their
// ASCII char equivalents, with one exception; \" becomes \200 to make it easier to
// correctly parse delimited String literals by simply matching double quotes, since
// embedded double quotes in the string will temporarily be \200.  Also, \" has already
// become \\\200, so turn that into \200, as if \200 was a double quote.  [see Java
// Language Spec, 2nd edition, Section 3.10.6, Escape Sequences for Character and String
// Literals]

def java_parseEscapeSequence (esc)
{
    def rv = esc;

    // Special case for \" or \ \200 becoming \200
    if (esc == "\\\"" || esc == "\\\200") return "\200";

    // for clarity...
    if (esc == "\\b") return "\b";
    if (esc == "\\t") return "\t";
    if (esc == "\\n") return "\n";
    if (esc == "\\f") return "\f";
    if (esc == "\\r") return "\r"
    if (esc == "\\'") return "\'";
    if (esc == "\\\\") return "\\";

    def m = esc =~ /^\\([0-3][0-7][0-7])$/
    if (m) {
        rv = oct(m.group(1));
    }

    return rv;
}

// Process Java Escape sequences for a string, and return the result, using
// java_parseEscapeSequences to handle each one.

def java_process_parseEscapeSequences (string)
{
    return string.replaceAll(/(\\([btnfr\0200\"\'\\]|[0-3][0-7][0-7]))/) { all, first -> java_parseEscapeSequence(first) }
}


// look for /* [comment] */
def java_process_file (file, update_option, warningsRef)
{
    def updateNeeded = 0;
    def componentName = fileRemoveExtension(new File(file).getName());
    def contents = new File(file).text;
    def maxRef = new LVal(java_get_max_index(file, contents) ?: 0);
    def originalContents = contents;

    contents = java_process_cleanContents(contents);

    // After hiding:
    // \200 = " preceded by active backslash, or in a comment.
    // \201 = /* within a String literal, or within a comment.
    // \202 = */ within a String literal.
    // \203 = { within a String literal, or within a comment.
    // \204 = } within a String literal, or within a comment.
    // \205 = ' within a String literal, or within a comment.

    contents = java_process_hideInactiveDelimiters(contents);

    // Replace all matching occurances of the "stringMagic" pattern...
    // Be careful not to lose or add newlines, so we can later report line numbers if needed.

    // Interpretation:
    // "/*" 1:WS "[" (2 <anything except "]" or "*/"> 2) "]" 4:WS "*/" 5:WS
    // 6( <one or more dquoted literals, separated by WS '+" WS 6)

    def pat = /\/\*(\s*)\[(\/*([^\]\/]*|[^\]\*]\/*)*)\](\s*)\*\/(\s*)(\"([^\"]*(\"\s*\+\s*\")?)*\")/
    contents = contents.replaceAll(pat) { all, g1, g2, g3, g4, g5, g6, g7, g8 ->
        java_process_stringMagic(g1, g2, g4, g5, g6, maxRef)
    }
    //         (1 1)  (2  (3                 3)2)  (4 4)    (5 5)(6 (7     (8          8)7)  6)

    // Fix 1-1OFI7 Allocate new key values for negative keys.
    pat = /(localizedJString\s*\(|localizedJavaString\s*\(|localizedJavaStringHere\s*\(|LocalizedJavaString\s*\([^,]+,)(\s*)(\-\d+)(\s*,\s*\"(([^\"]*(\"\s*\+\s*\")?)*)\"\s*(\/\*\/*\**([^\/\*]+\/*\**)*\*\/)?\s*\))/
    contents = contents.replaceAll(pat) { all, g1, g2, g3, g4 ->
        java_process_negativeKey(g1, g2, g3, g4, maxRef);
    }
    //     (1                                                                                                        1)(2 2)(3   3)(4        (5(6    (7          7)6)5)     (8         (9            9)    8)     4)
    //

    // Replace all calls to localizedJavaString or localizedJavaStringHere with calls to localizedJString.
    /*
    pat = /(localizedJavaString\s*\(|localizedJavaStringHere\s*\()(\s*(\d+)\s*,\s*\"(([^\"]*(\"\s*\+\s*\")?)*)\"\s*(\/\*\/*\**([^\/\*]+\/*\**)*\*\/)?\s*)\)/
    contents = contents.replaceAll(pat, /localizedJString($2)/);
    */
    //     (1                                                   1)(2                                                                                   2)
    // 1: localizedJavaString(  or localizedJavaStringHere(
    // 2: everything upto but not including matching close paren.

    // Update localizedJString or related method declarations appropriately.
    /*
    contents = java_updateMethodDecl(contents);
    */
    // Give warnings for various types of usage that may cause problems.
    java_maybeWarnAbout_various(file, contents, warningsRef);

    // Restore hidden delimiters
    contents = java_process_restoreInactiveDelimiters(contents);

    // Only write stuff out if we have actually changed it...
    if (contents != originalContents) {
        updateNeeded = 1;

        if (update_option) {
            updateFile(file, contents);
        }
        else {
            reportLinesToFix(file, originalContents, contents, 1);
        }
    }
    return [updateNeeded, contents];
}


def java_updateMethodDecl (contents)
{
    def hasCall = (contents =~
        /localizedJString\s*\((\s*(\d+)\s*,\s*\"(([^\"]*(\"\s*\+\s*\")?)*)\"\s*(\/\*\/*\**([^\/\*]+\/*\**)*\*\/)?\s*)\)/)
    def hasDecl = (contents =~ /(?:\n\s*\/\*[^\n]*\*\/\s*)?\n\s*(public|protected|private)?\s*(static)?\s*String\s+(localizedJString|localizedJavaString|localizedJavaStringHere)\s*\(\s*int\s+\w+\s*,\s*String\s+\w+\s*\)\s*\{[^\{\}]+\}\s*(?=\n)/)

    def m = contents =~ /\n\s*(?:abstract|final)?\s*(?:public|protected|private)?\s*(?:abstract|final)?\s*class\s+(\w+)\b/;
    def className = m ? m.group(1) : "NoName";

    def decl = """

    /** [Method code generated by localize script.] */
    private static String localizedJString (int key, String originalString)
    {
        return ariba.util.i18n.LocalizedJavaString.getLocalizedString(
            className.class.getName(),
            key, originalString, ariba.base.core.Base.getSession().getRestrictedLocale());
    }
""";

    // Remove old comment that was standard for these methods....
    contents = contents.replaceAll(/\n\s*\/\*\*\s*Helper method for localization that supports the localize-java.pl script, and its\s*conversion of special comments before string literals. Depends on having ClassName\s*defined earlier in this file or more commonly in its generated WhateverBase.java\s*file.  WARNING: This method must be private, it cannot be inherited and operate\s*correctly.  Each source file has to have its own copy, outside of the AW code.\s*\*\/\s*(?=\n)/,'\n');

    if (hasCall && hasDecl) {
        contents = java_updateExistingDecl(contents, decl);
    }
    else if (hasCall && !hasDecl) {
        contents = java_addDecl(contents, decl);
    }
    else if (!hasCall && hasDecl) {
        contents = java_removeDecl(contents);
    }
    return contents;
}


def java_updateExistingDecl (contents, decl)
{
    def declCountRef = new LVal(0);

    def pat = /(?:\n\s*\/\*[^\n]*\*\/\s*)?\n\s*(public|protected|private)?\s*(static)?\s*String\s+(localizedJString|localizedJavaString|localizedJavaStringHere)\s*\(\s*int\s+\w+\s*,\s*String\s+\w+\s*\)\s*\{[^\{\}]+\}\s*(?=\n)/
    return contents.replaceAll(pat) {
        java_updateDeclEval(decl, declCountRef);
    }
}


def java_addDecl (contents, decl)
{
    def counterRef = new LVal(0);

    return contents.replaceAll(/(\{|\n?\s*\})/) { all, g1 ->
        java_addDeclEval(g1, decl, counterRef);
    }
}


def java_removeDecl (contents)
{
    return contents.replaceAll(/(?:\n\s*\/\*[^\n]*\*\/\s*)?\n\s*(public|protected|private)?\s*(static)?\s*String\s+(localizedJString|localizedJavaString|localizedJavaStringHere)\s*\(\s*int\s+\w+\s*,\s*String\s+\w+\s*\)\s*\{[^\{\}]+\}\s*(?=\n)/, "");
}


def java_updateDeclEval (decl, declCounterRef)
{
    if (declCounterRef.val <= 0) {
        declCounterRef.val = 1;
        return decl;
    }
    else {
        return "";
    }
}


def java_addDeclEval (item, decl, counterRef)
{
    def rv = item;

    // The idea is that we add the declarations just before the close curly that matches
    // the first open curly that we find.  We add lines towards the end of the file so we
    // don't mess up the reporting of lines based on differences any more than we have to.

    if (item =~ /^\{$/) {
        counterRef.val++;
    }
    else if (item =~ /^\n?\s*\}$/) {
        counterRef.val--;
        if (counterRef.val == 0) {
            rv = "$decl$item".toString();

            // Set the counter very high so we never drop to zero again... only add
            // declarations the first time.

            counterRef.val = 1000000;
        }
    }

    return rv;
}


def java_process_stringMagic (commentWS1, commentPayload, commentWS2, betweenWS, stringAsIs, maxRef)
{
    maxRef.val++;
    def stringKey = maxRef.val;

    // Strip leading and trailing whitespace from the comment payload, but don't lose it.
    // .*? means match the minimum possible, so we will get all the trailing whitespace in 3.
    def commentWS3 = "", commentWS4 = "";
    def m = commentPayload =~ /^(\s*)(.*?)(\s*)$/
    if (m) (commentWS3, commentPayload, commentWS4) = m[0];

    // Preserve the newline count, to aid in reporting where changes are needed for noupdate.
    def commentWS = cleanWS("$commentWS1$commentWS2$commentWS3$commentWS4".toString(), 1);
    betweenWS = cleanWS(betweenWS, 1);

    return "localizedJavaString($stringKey,$betweenWS$stringAsIs$commentWS/* $commentPayload */)".toString();
}


def java_process_negativeKey (leader, spacer, key, trailer, maxRef)
{
    maxRef.val++;
    def stringKey = maxRef.val;

    def rv = "$leader$spacer$stringKey$trailer".toString();

    //D// print "DEBUG: process_negativeKey: rv=[rv]\n";
    return rv;
}

// ----- java_process_cleanContents --------------------------------------------------------

def java_process_cleanContents (contents)
{
    // We use \200 to \205 as tokens to represent transformed content to simplify parsing.
    // They should never appear in Java input, but just in case, we remove them.
    contents = contents.replaceAll(/[\0200-\0205]/, "");

    // FIX 1-2ROWF: Don't replace Unicode escape sequences, pass them through unchanged.

    return contents;
}

// After hiding:
// \200 = " preceded by active backslash, or in a comment.
// \201 = /* within a String literal, or within a comment.
// \202 = */ within a String literal, or within a comment, or not preceeded by /*.
// \203 = { within a String literal, or within a comment.
// \204 = } within a String literal, or within a comment.
// \205 = ' within a String literal, or within a comment.
// Comments can be delimited by /* */, or // to end of line...

def java_process_hideInactiveDelimiters (contents)
{
    // Replace all occurances of a doubleQuote or singleQuote char, preceded by an odd number of
    // backslashes with \200, so it is easy to find the Java literal string delimiters.

    contents = contents.replaceAll(/(\\+[\"\'])/) { all, g1 ->
        java_process_backslashedQuoteChar(g1);
    }

    def stateRef = new LVal("normal");
    contents = contents.replaceAll(/(\"|\'|\/\*|\*\/|\/\/|\r|\n|\{|\})/) { all, g1 ->
        java_process_hideStateMachine(g1, stateRef);
    }
    return contents;
}


// The idea here is to handle all the complexity and richness of Java source code, but to
// make it easy to parse string literals and delimited comments.  By the time we are done
// hiding, we can pretend that no one ever puts a /* inside a quoted string, or a double
// quote inside a comment, etc.  The parsing of String literals and delimited comments is
// very clean and relatively straight forward after this.  Cool, eh?

def java_process_hideStateMachine (item, stateRef)
{
    def rv = item;

    if (stateRef.val == "normal") {
        if (item == '"') {
            stateRef = "stringLiteral";
        }
        else if (item == "/*") {
            stateRef.val = "delimitedComment";
        }
        else if (item == "*/") {
            // This Java won't compile, but encode it anyway.
            rv = "\202";
        }
        else if (item == "//") {
            stateRef.val = "endOfLineComment";
        }
        else if (item == "{") {
            // Pass-through.
        }
        else if (item == "}") {
            // Pass-through.
        }
        else if (item == "'") {
            stateRef.val = "charLiteral";
        }
        else if (item == "\n" || item == "\r") {
            // Pass-through.
        }
        else {
            dieError("bad state item=\"$item\"");
        }
    }
    else if (stateRef.val == "stringLiteral") {
        if (item == '"') {
            stateRef.val = "normal";
        }
        else if (item == "/*") {
            rv = "\201";
        }
        else if (item == "*/") {
            rv = "\202";
        }
        else if (item == "//") {
            // Pass-through.
        }
        else if (item == "{") {
            rv = "\203";
        }
        else if (item == "}") {
            rv = "\204";
        }
        else if (item == "'") {
            rv = "\205";
        }
        else if (item == "\n" || item == "\r") {
            // This Java won't compile, but encode it anyway.
            stateRef.val = "normal";
        }
        else {
            dieError("bad state item=\"$item\"");
        }
    }
    else if (stateRef.val == "delimitedComment") {
        if (item == '"') {
            rv = "\200";
        }
        else if (item == "/*") {
            rv = "\201";
        }
        else if (item == "*/") {
            stateRef.val = "normal";
        }
        else if (item == "//") {
            // Pass-through.
        }
        else if (item == "{") {
            rv = "\203";
        }
        else if (item == "}") {
            rv = "\204";
        }
        else if (item == "'") {
            rv = "\205";
        }
        else if (item == "\n" || item == "\r") {
            // Pass-through.
        }
        else {
            dieError("bad state item=\"$item\"");
        }
    }
    else if (stateRef.val == "endOfLineComment") {
        if (item == '"') {
            rv = "\200";
        }
        else if (item == "/*") {
            rv = "\201";
        }
        else if (item == "*/") {
            rv = "\202";
        }
        else if (item == "//") {
            // Pass-through.
        }
        else if (item == "{") {
            rv = "\203";
        }
        else if (item == "}") {
            rv = "\204";
        }
        else if (item == "'") {
            rv = "\205";
        }
        else if (item == "\n" || item == "\r") {
            stateRef.val = "normal";
        }
        else {
            dieError("bad state item=\"$item\"");
        }
    }
    else if (stateRef.val == "charLiteral") {
        if (item == '"') {
            rv = "\200";
        }
        else if (item == "/*") {
            rv = "\201";
        }
        else if (item == "*/") {
            rv = "\202";
        }
        else if (item == "//") {
            // Pass-through.
        }
        else if (item == "{") {
            rv = "\203";
        }
        else if (item == "}") {
            rv = "\204";
        }
        else if (item == "'") {
            stateRef.val = "normal";
        }
        else if (item == "\n" || item == "\r") {
            // This Java won't compile, but encode it anyway.
            stateRef.val = "normal";
        }
        else {
            dieError("bad state item=\"$item\"");
        }
    }
    else {
        dieError("bad state value=\"${stateRef.val}\"");
    }

    return rv;
}


def java_process_restoreInactiveDelimiters (contents)
{
    // Restore stuff that was hidden by java_process_hideInactiveDelimiters.
    return contents.replaceAll(/\0200/, /\"/).replaceAll(/\0201/, /\/\*/).replaceAll(/\0202/, /\*\//).
                    replaceAll(/\0203/, /\{/).replaceAll(/\0204/, /\}/).replaceAll(/\0205/, /\'/);
}


def java_process_backslashedQuoteChar (esc)
{
    def rv = esc;

    def m = esc =~ /^(\\*)\\([\"\'])$/
    if (m) {
        def extraBackslashes = m.group(1), quoteChar = m.group(2);
        def extraCount = extraBackslashes.length();
        if ((extraCount & 1) == 0) {
            def hideChar = (quoteChar == '"') ? "\200" : "\205";
            rv = "$extraBackslashes\\$hideChar";
        }
    }
    return rv;
}


// get the highest java index

def java_get_max_index (file, contents)
{
    // pass 0 for warningsRef, so we don't give warnings in this preliminary parse pass.
    def strings, comments; (strings, comments) = java_parseFile(file, contents, 0);

    // Fix 1-24KMJ: Cleanly ignore negative and non-numeric key strings.

    def max = 0;
    for (String stringKey : strings.keySet()) {
        if (stringKey =~ /^\d+/ && Integer.parseInt(stringKey) > max) {
            max = Integer.parseInt(stringKey);
        }
    }
    return "$max";
}

def getCleanedClassPath (string)
{
    if (!string) return string;

    string = string.trim();

    if (string =~ /(?i)^ClassPath/) {
        return null;
    }
    if (string == "") {
        return null;
    }

    return string;
}

def validateStringKey (classPath, rawKey, key, file,
        contents, lineNumRef, beginPosRef, matchBeginPos, warningsRef)
{
    if (warningsRef) {
        if (!classPath) {
            updateLineNumber(lineNumRef, beginPosRef, contents, matchBeginPos);
            pushWarning(warningsRef, file, lineNumRef.val,
                        "String key $rawKey can only be used with LocalizedJavaString(\"classpath.x.y.className\",...");
        }
        else if (key =~ /^[a\-]?\d+$/ && !(classPath =~ /(?i)^ClassName$/) && !(classPath =~ /(?i)^Name$/) ) {
            def thisClassName = "";
            def m = file =~ /\/([A-Z]\w*)\.(java|awl)$/
            if (m) {
                thisClassName = m.group(1);
            }
            def pattern = "^$thisClassName\\.";

            //D// print "DEBUG: classpath pattern=mpattern%\n";

            if (!(classPath =~ pattern)) {
                if (classPath =~ /^(this\.)?getClass\(\)\./) {
                    updateLineNumber(lineNumRef, beginPosRef, contents, matchBeginPos);
                    pushWarning(warningsRef, file, lineNumRef.val,
                                "this.getClass().getName() should not be used, only static class path should be used with LocalizedJavaString(classPath,...");
                }
                else if (classPath =~ /(?i)\.ClassName$/) {
                    updateLineNumber(lineNumRef, beginPosRef, contents, matchBeginPos);
                    pushWarning(warningsRef, file, lineNumRef.val,
                                "Only the current file's ClassName should be used with LocalizedJavaString(classPath,...");
                }
                else {
                    updateLineNumber(lineNumRef, beginPosRef, contents, matchBeginPos);
                    pushWarning(warningsRef, file, lineNumRef.val,
                                "Numeric or awl key key should not be used with LocalizedJavaString(classPath,...");
                }
            }
        }
    }
}

def java_maybeWarnAbout_various (file, contents, warningsRef)
{
    java_maybeWarnAbout_staticContext(file, contents, warningsRef);
    java_maybeWarnAbout_missingSquareBrackets(file, contents, warningsRef);
    java_maybeWarnAbout_localizeSpelling(file, contents, warningsRef);
    java_maybeWarnAbout_badSyntax(file, contents, warningsRef);
    java_maybeWarnAbout_trailingComment(file, contents, warningsRef);
}

// Fix 1-1OFJO: give usage warning for localizedJString call in obvious static context
def java_maybeWarnAbout_staticContext (file, contents, warningsRef)
{
    def lineNumRef = new LVal(1);
    def beginPosRef = new LVal(0);

    def m = contents =~ /\bstatic\b[^\;\{]*\b(localizedJ(?:ava)?String\w*)\s*\(\s*([\w\-]+)/
      //                                     (1                         1)        (2     2)
    m.each { all, methodName, firstParamWord ->
        //D// print "DEBUG: static match=[&] methodName='methodName' firstParamWord='firstParamWord'\n";

        // If the first param word is int or String, we assume it is a declaration and let it pass.
        // otherwise we assume it is a method call in a static context, and we give a warning.
        if (firstParamWord != "int" && firstParamWord != "String" &&
            firstParamWord != "java.lang.String") {
            def matchBeginPos = m.start();
            updateLineNumber(lineNumRef, beginPosRef, contents, matchBeginPos);
            pushWarning(warningsRef, file, lineNumRef.val,
                        "methodName called incorrectly in a static context.");
        }
    }
}

// Fix 1-1OFD1: localize.pl: give usage warning for /* */ "xxx".
def java_maybeWarnAbout_missingSquareBrackets (file, contents, warningsRef)
{
    def lineNumRef = new LVal(1);
    def beginPosRef = new LVal(0);

    // We look for the end of a comment, whitespace, and a dquoted string.  By our style
    // guide this should never be true after localize.pl has transformed
    // /*[comment]*/"string" into localizedJString(key, "string" /*comment*/).  Therefore we
    // complain that the comment must be missing its square brackets.
    def m = contents =~ /\*\/\s*(\"[^\"]*\")/
      //                        (1        1)
    m.each {
        def string = java_process_restoreInactiveDelimiters(m.group(1));
        def matchBeginPos = m.start();
        updateLineNumber(lineNumRef, beginPosRef, contents,
                                                 matchBeginPos);
        pushWarning(warningsRef, file, lineNumRef.val,
                    "Missing square brackets in comment directly before String literal string");
    }
}

// Fix 1-1OFG3: Fix to warn about localizeJString misspelling...
def java_maybeWarnAbout_localizeSpelling (file, contents, warningsRef)
{
    def lineNumRef = new LVal(1);
    def beginPosRef = new LVal(0);

    // Look for calls to localizeJString instead of localizedJString... common misspelling,
    // and causes the strings to be left out of the string file...
    def m = contents =~ /(localizeJString|localizeJavaString|localizeJavaStringHere|LocalizeJavaString|localis\w*)\s*\(/
    m.each { methodName ->
        def matchBeginPos = m.start();
        updateLineNumber(lineNumRef, beginPosRef, contents,
                                                 matchBeginPos);
        pushWarning(warningsRef, file, lineNumRef.val,
                    "The method name methodName needs to be spelled localized, not localize.");
    }
}

// Fix 1-1OFHB: Give usage warning if localizeJString or localizeJavaString is on line with
// wrong syntax, specifically an identifier instead of a string literal...
def java_maybeWarnAbout_badSyntax (file, contents, warningsRef)
{
    def lineNumRef = new LVal(1);
    def beginPosRef = new LVal(0);

    // Complain about identifier as key, since compiler may not complain about it, and we
    // won't put the string in the string file...
    def m = contents =~ /((?:localizedJString|localizedJavaString|localizedJavaStringHere)\s*\(|LocalizedJavaString\s*\([^\,]+\,)\s*([A-Za-z_]\w+)([^\/;]*)(\/\*\/*\**(?:[^\/\*]+\/*\**)*\*\/)?/
        //                 (1                                                                                                    1)   (2          2)(3    3)(4                                4)
    m.each { all, methodMatch, id, middle, comment ->
        if (!middle) middle ""
        if (!comment) comment = ""
        if (id != "int" && id != "String" && id != "java.lang.String") {
            if (!(methodMatch =~ /^LocalizedJavaString/)) {
                def matchBeginPos = m.start();
                updateLineNumber(lineNumRef, beginPosRef, contents, matchBeginPos);
                pushWarning(warningsRef, file, lineNumRef.val,
                            "The key parameter must be an integer literal; it cannot be an identifier like id");
            }
            else if (!(comment =~ /(?i)localized?-ok/)) {
                def matchBeginPos = m.start();
                updateLineNumber(lineNumRef, beginPosRef, contents, matchBeginPos);
                pushWarning(warningsRef, file, lineNumRef.val,
                            "The key parameter should almost always be an integer literal, rather than an identifier like id; put 'localize-ok' in comment after id if the usage in this case is ok.");

            }
        }
    }

    // Complain about identifier as string, since compiler may not complain about it, and
    // we can't put the string in the string file...
    m = contents =~ /(localizedJString\s*\(|localizedJavaString\s*\(|localizedJavaStringHere\s*\(|LocalizedJavaString\s*\((?:[^,]+),)\s*(?:-?\d+|\"[^\"]*\")\s*,\s*([A-Za-z_]\w+)\s*(\/\*\/*\**(?:[^\/\*]+\/*\**)*\*\/)?/
    m.each { all, methodMatch, id, comment ->
        if (!comment) comment = ""

        // If comment contains localize-ok or localized-ok, don't report it.
        if (!(comment =~ /(?i)localized?-ok/)) {
            def matchBeginPos = m.start();
            updateLineNumber(lineNumRef, beginPosRef, contents,
                                                     matchBeginPos);
            if (methodMatch =~ /^LocalizedJavaString/) {
                pushWarning(warningsRef, file, lineNumRef.val,
                            "The localized String should almost always be a String literal, rather than an identifier like id; put 'localize-ok' in comment after id if the usage in this case is ok.");
            }
            else {
                pushWarning(warningsRef, file, lineNumRef.val,
                            "The localized String must be a String literal; it cannot be an identifier like id");
            }
        }
    }
}

// Fix 1-2ROVY: Give usage warning about comment after closing paren for localizeJString
// and LocalizeJavaString, it can be a misplaced translator comment.
def java_maybeWarnAbout_trailingComment (file, contents, warningsRef)
{
/*
    def lineNumRef = new LVal(1);
    def beginPosRef = new LVal(0);

    // m//sg means do multi-line matching, and iterate through the string, returning true on each match.
    def m = contents =~
           /(?:localizedJString\s*\(|localizedJavaString\s*\(|localizedJavaStringHere\s*\(|LocalizedJavaString\s*\(([^,]+),)\s*(-?\d+|\"[^\"]*\")\s*,\s*\"((?:[^\"]*(?:\"\s*\+\s*\")?)*)\"\s*((?:\/\*\/*\**(?:[^\/\*]+\/*\**)*\*\/)?)\s*\)\s*(\/\*\/*\**(?:[^\/\*]+\/*\**)*\*\/)/
      //                                                                                                          (1   1)     (2              2)         (3                          3)     (4                                    4)        (5                               5)
    m.each {
        def comment1 = m.group(4);
        def comment2 = m.group(5);
        def matchBeginPos = m.start();

        updateLineNumber(lineNumRef, beginPosRef, contents, matchBeginPos);

        if (comment1) {
            pushWarning(warningsRef, file, lineNumRef.val,
                        "Comment after parenthesis should be merged into the translator comment after the string literal.");
        }
        else {
            pushWarning(warningsRef, file, lineNumRef.val,
                        "Comment after parenthesis should be  moved between string literal and closing parenthesis.");
        }
    }
    */
}

// Command line argument processing ------------------------------------------------------
//
class ArgParser {
    def msg, opts
    ArgParser(o, m) {opts  = o; msg = m }

    def usage ()
    {
       println msg;
       opts.each { k, v -> println "    -${k} ${v.longOpt ? "  --${v.longOpt}" : ""} : ${v.desc}" }
    }

    def error (msg) { println "ERROR: ${msg}"; return null }

    def popArgs (opt, args)
    {
       def result = []
       for (int i=0; i<opt.args; i++) {
          if (args[0].startsWith("-")) break;
          result += args.remove(0)
       }
       assert result.size() == opt.args, "Insufficient argument count for flag -${opt.opt} (required: ${opt.args}"
       return result.size() == 1 ? result[0] : result;
    }

    def parse (argArr)
    {
        def args = argArr.collect { it }
        Map longOpts = [:]; opts.each { k, v -> v.opt = k; if (v.longOpt) longOpts[v.longOpt] = v }
        Map result = [:];
        while (args.size()) {
           String arg = args[0]
           def m = arg =~ /\-\-?(.+)/
           if (m) {
               args.remove(0)
               def opt = opts[m.group(1)] ?: longOpts[m.group(1)]
               if (!opt) return error("Unknown option: ${arg}")
               result[opt.opt] = (opt.args) ? popArgs(opt, args) : true;
           } else break;
        }
        result.arguments = args
        for (Map opt : opts.values()) { if (opt.required && !result[opt.opt]) return error("Missing required flag: -${opt.opt}") }
        return result;
    }
}

// ----- java test cases -----------------------------------------------------------------------
// Here is some test case Java source that I used in testing various cases:

//    private void testStuff ()
//    {
//        /* [  test1comment]*/   "test1 string to deal with";
//
//        /* [  test1comment]*/
//        "test1 string to deal with";
//
//            // /* [  test1comment]*/   "test1 string to deal with";
//
//        foobar(alpha, /* [  test1comment with nasty " in it ]*/   "test1 string to deal with" +
//               " continuation", gamma);
//
//        foobar(alpha, /* [  test2comment with nasty " in it and newline
//                              also ]*/   "test2 string to deal with a" +
//               " formfeed \f and a tab \t and a backslashed single quote \' and a bare single quote ' also" +
//               " continuation and nested double quote \"hello\" he he", gamma);
//
//        x = " /* [  test1comment]*/   " + test1 + "string to deal with";
//
//        y = /*[]*/"a doubled backslash doublequote like this: \\\\";
//
//        \u0001 \u0020 \u0040  \\\\u0040 \\\u0040 \u00fe \u00ff \u01ff \uffff
//
//            ljs = new LocalizedJavaString(ClassName, 20, "handcrafted...");
//    }

// It should result in the following in the updated Java source file:

//    private void testStuff ()
//    {
//        localizedJavaString(21, "test1 string to deal with" /* test1comment */);
//
//        localizedJavaString(22, "test1 string to deal with" /* test1comment */);
//
//            // /* [  test1comment]*/   "test1 string to deal with";
//
//        foobar(alpha, localizedJavaString(23, "test1 string to deal with" +
//               " continuation" /* test1comment with nasty " in it */), gamma);
//
//        foobar(alpha, localizedJavaString(24, "test2 string to deal with a" +
//               " formfeed \f and a tab \t and a backslashed single quote \' and a bare single quote ' also" +
//               " continuation and nested double quote \"hello\" he he" /* test2comment with nasty " in it and newline
//                              also */), gamma);
//
//        x = " /* [  test1comment]*/   " + test1 + "string to deal with";
//
//        y = localizedJavaString(25, "a doubled backslash doublequote like this: \\\\" /*  */);
//
//        \u0001   @  \\\\u0040 \\@ \u00fe \u00ff \u01ff \uffff
//
//            ljs = new LocalizedJavaString(ClassName, 20, "handcrafted...");
//    }

// And it should result in the following in the string.csv file:

// Cp1252
// "AbstractDocumentPrePublish",20,"handcrafted...","handcrafted...",""
// "AbstractDocumentPrePublish",21,"test1 string to deal with","test1 string to deal with","test1comment"
// "AbstractDocumentPrePublish",22,"test1 string to deal with","test1 string to deal with","test1comment"
// "AbstractDocumentPrePublish",23,"test1 string to deal with continuation","test1 string to deal with continuation","test1comment with nasty "" in it"
// "AbstractDocumentPrePublish",24,"test2 string to deal with a formfeed  and a tab 	 and a backslashed single quote ' and a bare single quote ' also continuation and nested double quote ""hello"" he he","test2 string to deal with a formfeed  and a tab 	 and a backslashed single quote ' and a bare single quote ' also continuation and nested double quote ""hello"" he he","test2comment with nasty "" in it and newline also"
// "AbstractDocumentPrePublish",25,"a doubled backslash doublequote like this: \\","a doubled backslash doublequote like this: \\",""
