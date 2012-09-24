String usage = """
    Creates a single lucene SourceSearch index, or manages a set of indexes for a set of builds.
    Usage:
        indexsource destDir (sourceRootDir symbolicKey)+
    Or
        indexsource --watchbuilds <index_home>
    Or
        indexsource --indexbuilds <index_home>
""";
/**
            <index_home>/ should be preconfigured as follows:
                S4-Hawk/        <-- user presenatable name
                    config.table *
                    Bigbend-8.index
                    Bigbend-9.index-new
                SSP-Hawk/
                    config.table *
                    SSPHawk-6.index
                    SSPHawk-8.index-new
                ...

              * config.table:
                {
                    buildsDir=/home/rc/archive/builds/s4;
                    buildName=Bigbend;
                    roots = (
                        {
                            sourcePattern="image/internal/source/ariba.* /";
                            type="source";
                        },
                        {
                            sourcePattern="image/internal/opensource/docs/api/";
                            type="doc";
                        },
                        {
                            sourcePattern="image/internal/opensource/examples/";
                            type="examples";
                        }
                    );
                }
    Note: trailing / on paths means that last path element is *not* part of
          the relative path for contained files
*/
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.LogByteSizeMergePolicy
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.Version
import ariba.util.core.*
import java.util.regex.*

println "In indexsource - Args=${args}"

if (args[0].startsWith("-")) {
    assert args.size() == 2, usage
    File indexHome = new File(args[1])
    assert indexHome.exists(), "Index home does not exist: $indexHome"
    if (args[0] == "--watchbuilds") {
        watchbuilds(indexHome)
    } else if (args[0] == "--indexbuilds") {
        indexbuilds(indexHome)
    } else assert false, usage
} else {
    createIndex(args)
}

// println "Glob: ${glob("/Users/craigf/*/"+ "*.txt")}"
// println "Glob: ${glob("/home/rc/archive/builds/s4/Bigbend-8/image/internal/source/" + "*")}"
def indexbuilds (File indexHome) {
    indexHome.eachDir { File baseDir ->
        File configFile = new File (baseDir, "config.table")
        Map config = [:]
        MapUtil.fromSerializedString (config, configFile.text)
        File buildsDir = new File((String)config.buildsDir)
        String buildName = config.buildName
        File stableBuilds = new File(buildsDir, "stable.txt")
        Matcher m = stableBuilds.text =~ ("(?m)^${buildName.toLowerCase()}(.+)\$")
        assert m.find(), "Can't find stable build for ${buildName} in ${stableBuilds} : ${stableBuilds.text}"
        String buildNum = m.group(1).split(/\s+/)[-1]
        File buildDir = new File(buildsDir, "${buildName}-${buildNum}")
        println "Checking build: ${buildDir}"
        File indexDir = new File(baseDir, "${buildName}-${buildNum}.index")
        if (indexDir.exists()) {
            println "Index already present for latest stable build: ${buildName}-${buildNum} -- skipping"
        } else {
            File indexTempDir = new File(baseDir, "${buildName}-${buildNum}.temp-index")
            List argList= [indexTempDir.getCanonicalPath()]
            String buildDirPath = buildDir.path.replace("\\", "/")
            if (!buildDirPath.endsWith("/")) buildDirPath += "/"
            config.roots.each { sourceConfig ->
               String dirPrefix = sourceConfig.sourcePattern.startsWith("/") ? "" : buildDirPath;
               argList += dirPrefix + sourceConfig.sourcePattern
               argList += dirPrefix + sourceConfig.sourcePattern
               argList += sourceConfig.type
            }
            createIndex(argList.toArray())

            println "renaming: ${indexTempDir} to ${indexDir}"
            indexTempDir.renameTo(indexDir)
        }
    }
}

def watchbuilds (File indexHome) {
    int sleepMins = 20
    while (true) {
        indexbuilds(indexHome)
        println "Sleeping for $sleepMins minutes"
        Thread.currentThread().sleep(sleepMins*1000*60)
    }
}

def createIndex (args) {
    def _IndexedExtensions = new HashSet(
            ["java", "aml", "awl", "htm", "html", "css", "js", "afr", "awz",
             "dtd", "groovy", "table", "pl", "module", "bdf", "rul", "pml", "acf"]);
             // not:  "xml", "csv"
    File indexDir = new File(args[0])
    def _analyzer = search.SourceCodeAnalyzer.analyzerForField("contents", null);
    def _writerConfig = new IndexWriterConfig(Version.LUCENE_36, _analyzer);
    _writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
    def mp = new LogByteSizeMergePolicy();
    mp.setUseCompoundFile(true);
    mp.setNoCFSRatio(1.0);
    _writerConfig.setMergePolicy(mp);
    println ("Creating writer")
    def _writer = new IndexWriter(FSDirectory.open(indexDir), _writerConfig)
    _writer.useCompoundFile = false
    println "Created index in ${args[0]}"

    int i = 1
    while (i < args.length) {
        String dataDirPat = args[i++].replace('\\','/')
        def includeTailInRelativePath = !dataDirPat.endsWith("/")
        String symbolicName = args[i++].replace('\\','/')
        String type = args[i++]
        glob(dataDirPat).each { File dataDir ->
            int prefixLen = (includeTailInRelativePath ? dataDir.parentFile : dataDir).getAbsolutePath().length()
            String dirName = (symbolicName == dataDirPat) ? dataDir.getAbsolutePath().substring(0,prefixLen) : symbolicName
            println "Indexing dir ${dataDir} as ${type}, root=${dirName}"
            dataDir.eachFileRecurse { File f ->
                String path = f.getAbsolutePath().substring(prefixLen+1).replace('\\','/')
                boolean skip = false
                // Hack for AN (which includes old release builds in path)
                if (dataDir.getName() == "release") {
                    String projName = path.split("/")[0]
                    if (new File(dataDir.getParentFile(), projName).exists()) skip = true;
                }
                def m = f.name =~ /.+\.(\w+)$/
                // println "${f.name} : ${m.matches() ? m.group(1) : 'no match'}"
                if (skip) {
                  // skip
                } else if (f.directory) {
                    println "   ... indexing $path"
                } else if (m.matches() && _IndexedExtensions.contains(m.group(1)) && (f.parentFile.name != "class-use") && !f.hidden && f.exists() && f.canRead() && f.length() < 1000000) {
                    String className = f.getName().replaceAll(/\.(\w+)$/, "")
                    def doc = new Document()
                    doc.add(new Field("contents", new FileReader(f)))
                    doc.add(new Field("className", className, Field.Store.YES, Field.Index.ANALYZED))
                    // doc.add(new Field("type", m.group(1), Field.Store.YES, Field.Index.TOKENIZED))
                    doc.add(new Field("type", type, Field.Store.YES, Field.Index.ANALYZED))
                    doc.add(new Field("dir", dirName, Field.Store.YES, Field.Index.NO))
                    doc.add(new Field("path", path, Field.Store.YES, Field.Index.ANALYZED))

                    // check for extra search fields in special comment at top of .awl files
                    if (f.name.endsWith(".awl")) {
                        def fieldsMatch = f.text =~ /^\s*<!---\s+SearchFields(.+?)-->/
                        if (fieldsMatch) {
                            println " -- ${f.name}: Found extra search fields decl: ${fieldsMatch.group(1)}"
                            (fieldsMatch.group(1) =~ (/\s*(\w+)\s*:\s*(.+?)\s*/ + "(?:;|\$)")).each { all, key, value ->
                                doc.add(new Field(key, value, Field.Store.YES, Field.Index.ANALYZED))
                                // println "--- <${key}> : <${value}>"
                            }
                        }
                    }

                    doc.add(new Field("contents", new FileReader(f)))

                    _writer.addDocument(doc)
                }
            }
        }
    }

    println ("Number of files indexed ${_writer.numDocs()}")
    _writer.optimize()
    _writer.close()  // Close index
}

// Simple support for expanding file paths containing "*"
def glob (String pat) {
    File root = new File(".")
    // absolute path? (need to handle windows "//server/drive", "c:/" and unix "/")
    def m = pat =~ '^((?://\\w+/\\w+/)|(?:(?:\\w\\:)?/{1}+))(.+)'
    if (m.matches()) { root = new File(m.group(1)); pat = m.group(2); }
    // println "root: $root, pat: $pat"
    _glob( root, Arrays.asList(pat.split("/")), [])
}

def _glob (File curDir, List path, List result) {
    def pat = "^".concat(path[0].replace(".", "\\.").replace("*", ".+")).concat("\$")
    if (path.size() > 1) {
        curDir.eachDir { File dir ->
            if (dir.getName() =~ pat) _glob(dir, path[1..-1], result)
        }
    } else {
        curDir.eachFile { File f ->
            if (f.getName() =~ pat) result.add(f)
        }
    }
    return result
}
