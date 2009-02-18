package search;

import ariba.ui.aribaweb.core.*;
import ariba.ui.table.*;

class SearchSource extends AWComponent {
    List searchers
    def _searcher
    public def _iSearcher
    def queryString, doc, matchDoc, message, showingDetails=false
    Node root, node
    AWTDisplayGroup outlineDisplayGroup = new AWTDisplayGroup(), resultDisplayGroup = new AWTDisplayGroup()
    def _filteredMatches, _lastSelection;
    def fileTypes = [:], fileType, fileTypeList, _enabledTypes = new HashSet()
    def _selectedDoc, selectedPackage, selectedSubDoc
    List selectedDocPeers
    def _displayedDoc, displayedContent, newContentUrl
    int selectedContentTab = 1

    void init () {
        searchers = SourceSearcher.allSearchers()
        if (hasSearchers()) {
          // auto-search if we're accessed via http://machine:port/Demo/Ariba/SearchSource.htm?q=someQueryString
          setSearcher(searchers[0])
          queryString = requestContext().request().formValueForKey("q")
          if (queryString) searchAction()
        }
    }

    def show (String path) {
        queryString = "path:\"${path}\"".toString();
        searchAction()
        checkSelection()
        if (resultDisplayGroup.filteredObjects().size() == 1) {
            setSelectedDoc(resultDisplayGroup.filteredObjects()[0]);
        }
    }

    boolean hasSearchers () { searchers && searchers.size() }

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

