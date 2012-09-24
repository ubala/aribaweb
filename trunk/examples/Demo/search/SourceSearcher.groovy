package search;

import ariba.ui.aribaweb.util.*;
import ariba.util.core.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.search.*
import org.apache.lucene.search.highlight.*
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.index.IndexReader
import org.apache.lucene.util.Version

class SourceSearcher {
    String _name
    File _indexDir, sourceDir;
    def reader, searcher, analyzer;
    List pathsByDocId, typesByDocId;
    Node fullTree = new Node(_name:"All Files")
    def fullFileTypes = [:]

    static {
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE)  // Avoid exception on clause expansion of queries like AW*
    }

    static String extract (String s, String pat) { def m = (s =~ pat); m.find() ? m.group(1) : null; }
  
    static List searchers
    static List allSearchers ()
    {
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
        return searchers
    }
    
    public SourceSearcher (name, indexDir) {
        _name = name
        _indexDir = indexDir
        println "Created new SourceSearcher for ${_indexDir}"
    }

    def prepare () {
        if (reader) return;
        ProgressMonitor.instance().prepare("Loading Search Indexes...", 0)
        println "Warming docId cache from  ${_indexDir}"
        reader = IndexReader.open(FSDirectory.open(_indexDir))
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

        searcher = new IndexSearcher(FSDirectory.open(_indexDir))
        analyzer = SourceCodeAnalyzer.analyzerForField("contents", null); // StandardAnalyzer()
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
        def query = new QueryParser(Version.LUCENE_36, "contents", source.analyzer).parse(q)
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
        try {
            File sourceFile = sourceFile()
            TokenStream tokenStream = index().analyzer.tokenStream("contents", sourceFile.newReader())
            // Get 3 best fragments and separate with a "..."
            def ex = searchState.excerptHighlighter?.getBestFragments(tokenStream, sourceFile.text, 3, "...")?.replaceAll(/\n(\s*\n+)+/, "\n")
            return escapeString(ex)?.replaceAll(/%#%/, '<span class="brandAccent">')?.replaceAll(/#%#/, '</span>')
        } catch (FileNotFoundException ex) {
            return "File Not Found" 
        }
    }

    def escapeString (s) {
        if (!s) return s
        s = AWUtil.escapeHtml(s).string()
        return s.replaceAll(/(?m)^(\s+)/, '<span style="white-space:pre">\$1</span>').replaceAll('(?m)\$', '<br/>')
    }

    def isJavadoc () { (extension() == "html" ||  extension() == "htm") &&
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

class CollectorRelay extends Collector {
    def closure;
    public void collect(int doc, float score) { closure(doc, score) }

    public void collect(int doc) { closure(doc, 1) }

    public void setScorer(Scorer scorer)  { }

    public boolean acceptsDocsOutOfOrder() { return true;}

    public void setNextReader(IndexReader r, int i) { }
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
