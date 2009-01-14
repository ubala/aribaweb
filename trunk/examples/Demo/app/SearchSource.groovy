package app;
import ariba.ui.aribaweb.core.*;
import ariba.ui.aribaweb.util.*;
import ariba.ui.widgets.*;
import ariba.ui.table.*;
import ariba.ui.outline.*;
import ariba.util.core.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.search.*
import org.apache.lucene.search.highlight.*
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.index.IndexReader

class SearchSource extends AWComponent {
    static List searchers
    def _searcher, iSearcher
    def queryString, doc, matchDoc, message, showingDetails=false
    Node root, node
    def outlineDisplayGroup = new AWTDisplayGroup(), resultDisplayGroup = new AWTDisplayGroup()
    def _filteredMatches, _lastSelection;
    def fileTypes = [:], fileType, fileTypeList, _enabledTypes = new HashSet()
    def _selectedDoc, selectedPackage, selectedSubDoc
    List selectedDocPeers
    def _displayedDoc, displayedContent, newContentUrl
    int selectedContentTab = 1

    String extract (String s, String pat) { def m = (s =~ pat); m.find() ? m.group(1) : null; }

    void init () {
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE)  // Avoid exception on clause expansion of queries like AW*

        // scan index dirs looking for latest indexes
        def newSearchers = [];

        ["${AWUtil.getenv("ARIBAWEB_HOME")}/docs/"].each {
            def dir = new File(it, "index.lucene")
            if (dir.exists()) {
                newSearchers.add(searchers?.find { it._indexDir == dir } ?: new SourceSearcher("Open Source AW", dir))
            }
        }
        def indexesDir = AWUtil.getenv("ARIBAWEB_INDEX_DIR")
        if (indexesDir) {
            new File(indexesDir).eachDir { indexDir ->
                def  arr = indexDir.listFiles({ it.name.endsWith(".index") } as java.io.FileFilter)
                def indexFiles = Arrays.asList(arr).sort { a, b ->
                    extract(a.path, /-(\d+)/).toInteger() <=> extract(b.path, /-(\d+)/).toInteger()
                }
                println "Indexes: ${indexFiles}"
                if (indexFiles.size()) {
                    def dir = indexFiles[-1]
                    newSearchers.add(searchers?.find { it.indexDir == dir } ?: new SourceSearcher(indexDir.name, dir))
                }
            }
        }
        searchers = newSearchers
        setSearcher(searchers[0])

        // auto-search if we're accessed via http://machine:port/Demo/Ariba/SearchSource.htm?q=someQueryString
        queryString = requestContext().request().formValueForKey("q")
        if (queryString) searchAction()
    }

    def getSearcher () { _searcher }

    void setSearcher (s) {
        _searcher = s
        fileTypes = _searcher.fullFileTypes
        fileTypeList = new ArrayList(fileTypes.keySet()).sort()
        selectedPackage = _selectedDoc = null
        setOutlineRoot(_searcher.fullTree)
    }

    def searchAction () {
        fileTypes = [:]
        String q = !(queryString =~ /([\s\:]+)|(^$)/) ? "${queryString}* className:(${queryString}*)^2 className:(${queryString})^2" : queryString
        doQuery(q, _searcher.typeCollector(fileTypes))
        if (q == "") fileTypes = _searcher.fullFileTypes
        fileTypeList = new ArrayList(fileTypes.keySet()).sort()
        _enabledTypes = new HashSet()
        message = "Results for '$queryString'"
        return null
    }
    def search (String q, collector) {
        _searcher.search(q, collector)
    }

    def doQuery (q, collector) {
        // Query with collector, building an outline of all matched paths
        int matchCount = 0
        Node r = null
        if (q == "") {
            r = _searcher.fullTree;
        } else {
            r = new Node(_name:"All Matches")
            try {
                search(q) { doc ->
                    // println "Doc: ${doc.path()} ${doc.score}"
                    r.add(doc, doc.path().split(/[\/|\\]/), 0)
                    if (collector) collector(doc)
                    matchCount++
                }
            } catch (org.apache.lucene.queryParser.ParseException e) {
                recordValidationError("query", "Unable to parse query", q);
                errorManager().checkErrorsAndEnableDisplay();
            }
            selectedContentTab = 0
        }
        setOutlineRoot(r)
        return null;
    }

    def setOutlineRoot (Node r) {
        // collapse single-child parents, then "smart open" -- until we have 25 open
        root = r;
        root.collapse()
        outlineDisplayGroup.setObjectArray([root])
        outlineDisplayGroup.setSelectedObject(root)
        def outline = outlineDisplayGroup.outlineState()
        int level =0, lastCount = -1, openCount = 0
        while (lastCount != openCount && openCount < 30) {
            lastCount = openCount
            root.eachAtDepth(level++) { node ->
                if (!node.isLeaf() && ((openCount < 10) || (openCount + node.nonLeafChildren().size() < 30))) {
                    outline.setExpansionState(node, true)
                    openCount += node.nonLeafChildren().size()
                }
            }
        }
    }

    def fileTypeEnabled () { return _enabledTypes.contains(fileType); }
    def setFileTypeEnabled (boolean yn) {  if (yn) _enabledTypes.add(fileType); else _enabledTypes.remove(fileType) }
    def fileTypeCount () { fileTypes[fileType] }

    def fileTypesChanged () {
        String q = queryString ? "(${queryString}) " : ""
        if (_enabledTypes.size() > 0 && _enabledTypes.size() < fileTypes.size()) {
            def fieldString =""
            _enabledTypes.each { fieldString += it + " " }
            if (q != "") q += "AND "
            q += "type:(${fieldString})"
        }
        return doQuery(q, null)
    }

    def checkSelection () {
        def selection = outlineDisplayGroup.selectedObjects();
        if (selection != _lastSelection) {
            _lastSelection = selection;
            def byDoc = [:];
            selection.each { node ->
                node.eachNode {
                    def doc = it._doc
                    if (doc) {
                        def key = doc.fullClassName(), other = byDoc[key]
                        if (other) {
                            // println "Matches on ${key}: ${doc.path()} <-> ${other.path()}"
                            other.addOther(doc)
                        } else {
                            byDoc[key] = doc
                        }
                    }
                }
            }
            _filteredMatches = byDoc.size() ? new ArrayList(byDoc.values()).sort { a,b -> return a.path().compareTo(b.path()) } : null
            resultDisplayGroup.setObjectArray(_filteredMatches)
            resultDisplayGroup.setSelectedObject(null);
            selectedSubDoc = _selectedDoc = null;

            // select package
            def packageDoc = null, overviewDoc
            _filteredMatches.each { doc ->
                if (doc.fileName() == "package-summary.html") {
                    packageDoc = packageDoc ? 1 : doc;
                } else if (doc.fileName() == "overview-summary.html") {
                    overviewDoc = doc
                }
            }
            if (packageDoc == 1) packageDoc = null;
            if (packageDoc) {
                selectedPackage = packageDoc
            } else if (overviewDoc) {
                selectedPackage = overviewDoc
            }
        }

        def cur = resultDisplayGroup?.selectedObject();
        if (cur != _selectedDoc) {
            setSelectedDoc(cur)
        }
    }

    def matchDocClicked () {
        setSelectedDoc(matchDoc)
        return null
    }

    def subDocClicked () {
        selectedSubDoc = matchDoc
        return null
    }

    def selectedDoc () { _selectedDoc }

    def setSelectedDoc (doc)
    {
        if (!doc) { selectedSubDoc = _selectedDoc = null; selectedContentTab = 1; return }

        String name = doc.fileName()
        if (name == "package-summary.html" || name == "overview-summary.html") {
            selectedPackage = doc
            selectedContentTab = 1
        } else {
            selectedSubDoc = _selectedDoc = doc;
            resultDisplayGroup.setSelectedObject(doc)
            selectedContentTab = 2
            selectedDocPeers = []
            def q = "(path:\"${doc.directory()}/*\" AND className:${doc.className()}) OR (sampleFor:${doc.className()})";
            search(q) { selectedDocPeers += it }
            println "Results for peer query <${q}> : ${selectedDocPeers}"
        }
    }

    def setDisplayedDoc (doc) {
        if (_displayedDoc != doc) {
            _displayedDoc = doc
            displayedContent = null
        }
    }

    def displayedDoc () { _displayedDoc }

    def displayedFile () { return _displayedDoc?.sourceFile() }

    def isSample (doc) { doc.className() != _selectedDoc.className() }
    def matchDocIsSample () { isSample(matchDoc) }
    def displayedDocIsSample () { isSample(_displayedDoc) }

    def contentLinkClicked () {
        if (newContentUrl =~ /^(http(s)?|file)\:/) return AWRedirect.getRedirect(requestContext(), newContentUrl)
        File root = new File(_displayedDoc.sourceRoot()).getCanonicalFile()
        File f = new File(displayedFile().getParentFile(), newContentUrl).getCanonicalFile()
        if (f.getPath().startsWith(root.getPath())) {
            String relativePath = f.getPath().substring(root.getPath().length()).replace('\\','/')
            println "Relative path: $relativePath"
            def newDoc = null;
            search("path:\"${relativePath}\"") { doc -> newDoc = doc }
            println "Doc for ${relativePath}: ${newDoc}"
            if (newDoc) setSelectedDoc(newDoc)
        } else {
            println "New path outside root of reference: $f / $root"
        }
        return pageComponent()
    }

    def selectedPackageLabel () { selectedPackage?.fileName() == "overview-summary.html" ?
            "AW Overview" : selectedPackage.directory().replace("/", ".") }

    def contentsToDisplay () {
        if (displayedContent == null) {
            File displayedFile = displayedFile()
            // println "contentsToDisplay -- file: ${displayedFile}"
            String contents = displayedFile.text
            /* Should highlight?  -- if (!selectedFileOverride) {
                TokenStream tokenStream = _selectedDoc.index().analyzer.tokenStream("contents", displayedFile.newReader())
                // Highlight entire text
                contents = _selectedDoc.searchState.fullHighlighter.getBestFragments(tokenStream, contents, 1, "...")
            }
            */

            displayedContent = (_displayedDoc.isJavadoc()) ? stripJavaDocNav(extractPlainBody(contents)) : contents;
        }
        return displayedContent;
    }

    def extractPlainBody (String contents) {
        // extract body tag contents and remove SCRIPT
        return contents.replaceAll(/(?s).+<(?:BODY|body) .+?>(.+)<\/(?:BODY|body)>.*/, /$1/)
                       .replaceAll(/(?s)<(?:SCRIPT|script) .+?>.+?<\/(?:SCRIPT|script)>/, "")
    }

    def stripJavaDocNav (String body) {
        return body.replaceAll(/(?s)\<\!-- =+ START OF \w+ NAVBAR .+? END OF \w+ NAVBAR =+ -->(\s*<HR>)?/, "")
    }
}

class SourceSearcher {
    String _name
    File _indexDir, sourceDir;
    def reader, searcher, analyzer;
    List pathsByDocId, typesByDocId;
    Node fullTree = new Node(_name:"All Files")
    def fullFileTypes = [:]

    public SourceSearcher (name, indexDir) {
        _name = name
        _indexDir = indexDir
        println "Created new SourceSearcher for ${_indexDir}"
    }

    def prepare () {
        if (reader) return;
        ProgressMonitor.instance().prepare("Loading Search Indexes...", 0)
        println "Warming docId cache from  ${_indexDir}"
        reader = IndexReader.open(_indexDir.getAbsolutePath())
        def searchState = new SearchState (source:this)
        pathsByDocId = []; typesByDocId = []
        def typeCollector = typeCollector(fullFileTypes)
        (0..reader.maxDoc()-1).each {
            pathsByDocId.add(reader.isDeleted(it) ? null : reader.document(it)["path"])
            typesByDocId.add(reader.isDeleted(it) ? null : reader.document(it)["type"])
            if (!reader.isDeleted(it)) {
                def doc = new DocRef(searchState, it, (float)1.0);
                fullTree.add(doc, doc.path().split(/[\/|\\]/), 0)
                typeCollector(doc)
            }
        }
        println "Read full paths: ${pathsByDocId.size()}"

        searcher = new IndexSearcher(FSDirectory.getDirectory(_indexDir, false))
        analyzer = new StandardAnalyzer()
    }

    def search (String q, Closure collector) { prepare(); new SearchState (source:this).search(q, collector) }
    def getFullTree () { prepare(); return fullTree }
    def getFullFileTypes () { prepare(); return fullFileTypes }
    def getName () { _name }
    def getIndexDir () { _indexDir }
    def typeCollector (map) {
        return { doc ->
            def type = doc.type()
            def val = map[type]
            map[type] = val ? val+1 : 1
        }
    }
}

class SearchState {
    static def highlightFormatter = new SimpleHTMLFormatter('%#%', '#%#')
    SourceSearcher source
    Highlighter excerptHighlighter, fullHighlighter
    def search (String q, Closure collector) {
        def query = new QueryParser("contents", source.analyzer).parse(q)
        source.searcher.search(query, new CollectorRelay( closure: { docId, score -> collector(new DocRef(this, docId, score)) }))
        excerptHighlighter = new Highlighter(highlightFormatter, new QueryScorer(query))
        fullHighlighter = new Highlighter(highlightFormatter, new QueryScorer(query))
        fullHighlighter.setTextFragmenter(new NullFragmenter())
    }
}

class DocRef {
    int docId
    float score
    SearchState searchState
    List _allHits

    public DocRef (ss, int did, float sc) { docId=did; score=sc; searchState = ss; }
    def index () { searchState.source }
    String path () { index().pathsByDocId[docId] }
    def type () { index().typesByDocId[docId] }
    def doc () { index().reader.document(docId) }
    def sourceRoot () { doc()["dir"].replaceAll('\\' + '$(\\w+)', { a, v -> System.getenv(v).replace('\\','/') }) }
    def sourceFile () { new File(sourceRoot(), path()) }
    def className () { AWUtil.stripLastComponent(fileName(), (char)'.') }
    def fullClassName () { AWUtil.stripLastComponent(path(), (char)'.') }
    def fileName () { AWUtil.lastComponent(path(),"/") }
    def extension () { AWUtil.lastComponent(path(),".") }
    def directory () { AWUtil.stripLastComponent(path(), (char)'/') }
    def addOther (doc) { def l = allHits(); if (!l.contains(doc)) { l.add(doc); if (doc.score > score) score = doc.score } }
    def allHits () { (_allHits) ?: (_allHits = [this]) }
    def hitExcerpt () {
        File sourceFile = sourceFile()
        TokenStream tokenStream = index().analyzer.tokenStream("contents", sourceFile.newReader())
        // Get 3 best fragments and separate with a "..."
        def ex = searchState.excerptHighlighter?.getBestFragments(tokenStream, sourceFile.text, 3, "...")?.replaceAll(/\n(\s*\n+)+/, "\n")
        return escapeString(ex)?.replaceAll(/%#%/, '<span class="brandAccent">')?.replaceAll(/#%#/, '</span>')
    }

    def escapeString (s) {
        if (!s) return s
        s = AWUtil.escapeHtml(s).string()
        return s.replaceAll(/(?m)^(\s+)/, '<span style="white-space:pre">\$1</span>').replaceAll('(?m)\$', '<br/>')
    }

    def isJavadoc () { extension() == "html" &&
                    (type() == "doc" || type() == "tutorial") } // path().contains("/docs/")
    def isSample () { extension() == ".awl" && !StringUtil.nullOrEmptyString(doc()["sampleFor"]) }
    def typeLabel () { isJavadoc() ? "doc" : extension() }

    def highlightedContents () {
        File sourceFile = sourceFile()
        TokenStream tokenStream = index().analyzer.tokenStream("contents", sourceFile.newReader())
        // Highlight entire text
        return searchState.fullHighlighter.getBestFragments(tokenStream, sourceFile.text, 1, "...")
    }

    public boolean equals (Object other) {(other instanceof DocRef) && (docId == other.docId) }
}

class CollectorRelay extends HitCollector {
    def closure;
    public void collect(int doc, float score) { closure(doc, score) }
}

class Node {
    public List _children
    int _count = -1
    public String _name
    public def _doc

    def isLeaf () { _children == null }
    def nonLeafChildren () { return _children?.findAll { !it.isLeaf() } }
    def eachNode (closure) { closure(this); _children?.each { it.eachNode(closure) } }
    def eachAtDepth (int level, closure) { if (level == 0) closure(this); else _children?.each {it.eachAtDepth(level-1, closure) }}

    def leafCount () {
        if (_count == -1)  _count = isLeaf() ? 1 : (_children ?_children.inject (0) { c, i -> c + i.leafCount() } : 0)
        return _count
    }

    def add (doc, pathArray, idx) {
        def name = pathArray[idx]
        def child = _children?.find { it._name == name }
        if (!child) {
            if (!_children) _children = []
            child = new Node(_name:name)
            _children.add(child)
        }

        if (idx == pathArray.length - 1) {
            child._doc = doc
        } else {
            child.add(doc, pathArray, idx+1)
        }
    }

    def collapse () {
        if (_children) {
            _children.each { it.collapse() }
            if (_children.size() == 1 && !_children[0].isLeaf()) {
                def child = _children[0]
                _name += "/" + child._name
                _children = child._children
            }
        }
    }
}
