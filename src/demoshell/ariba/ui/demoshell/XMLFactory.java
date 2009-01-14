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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/XMLFactory.java#14 $
*/
package ariba.ui.demoshell;


import ariba.ui.aribaweb.core.*;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWContentType;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.table.ResourceLocator;
import ariba.ui.widgets.XMLUtil;
import ariba.util.core.*;
import ariba.util.i18n.I18NUtil;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.w3c.dom.*;

/**
 * This covers:
 *   1) a convenience for locating an reading in XML DOMs
 *   2) FieldValue support for navigating DOM trees via field paths.

        To understand how field path notation is applied, take the following example:

        <!-- PO1.xml... -->
        <?xml version="1.0" encoding="UTF-8"?>
        <cXML payloadID="32232995@ariba.acme.com" xml:lang="en-US">
            ...
            <Request deploymentMode="test">
                 <OrderRequest>
                      <OrderRequestHeader orderID="DO1234" orderDate="2000-10-12"
                                          type="new">
                          <Total>
                              <Money currency="USD">187.60</Money>
                          </Total>
                            ...

        // read PO
        Document po = ariba.ui.demoshell.XMLFactory.xmlNamed("POs/PO1.xml", component);

        // key path illustrations
        - Get an attribute value
            $po.payloadID
                --> 32232995@ariba.acme.com

        - Navigate to an inner tag an get its value
            $po.Request.OrderRequest.OrderRequestHeader.orderID
                --> DO1234

        - Navigate and get the text content of a tag (note: "text" is a special key)
            $po.Request.OrderRequest.OrderRequestHeader.Total.Money.text
                --> 187.60

        - Getting a child node
            $po.Request.OrderRequest.OrderRequestHeader
                --> Node for OrderRequestHeader
            NOTE: if OrderRequest had more than one OrderRequestHeader child, this
                  would throw an exception.  To get array of children, see below...

        - Getting an array of child tags...
             ...
             <OrderRequest>
                  <OrderRequestHeader orderID="DO1234" orderDate="2000-10-12" type="new">
                        ...
                  </OrderRequestHeader>
                  <ItemOut quantity="10" requestedDeliveryDate="2000-10-18" lineNumber="1">
                      <ItemDetail>
                         <UnitPrice>
                             <Money currency="USD">1.34</Money>
                         </UnitPrice>
                         <Description xml:lang="en">hello from item 1
                      </ItemDetail>
                  </ItemOut>
                  <ItemOut quantity="20" requestedDeliveryDate="2000-10-18" lineNumber="4">
                      ...
                  </ItemOut>
                  <ItemOut quantity="30" requestedDeliveryDate="2000-10-18" lineNumber="5">
                      ...
                  </ItemOut>
              </OrderRequest>

            $po.Request.OrderRequest.children   ("children" is a special key)
                --> [array of OrderRequesHeader node and three ItemOut nodes]

            $po.Request.OrderRequest.ItemOut[]  ("[]" is a special notation)
                --> [array of three ItemOut nodes -- i.e. filtered list of children]
 */
public class XMLFactory
{
    static {
        XMLUtil.setResolver(new Resolver());
    }

    protected static XMLFactory _registeredInstance = null;
    static org.xml.sax.EntityResolver DefaultResolver = XMLUtil.defaultResolver;

    public static Document xmlNamed (String path)
    {
        AWResource resource = AWConcreteServerApplication.sharedInstance().resourceManager().resourceNamed(path);
        Assert.that((resource != null), "Couldn't find resource for path %s", path);
        URL url;
        try {
            url = URLUtil.makeURL(resource.fullUrl());
        } catch (MalformedURLException e) {
            throw new AWGenericException(e);
        }
        return registeredInstance().documentForUrl(url, null);
    }

    public static Document xmlNamed (String path, AWComponent parent)
    {
        File file = ResourceLocator.fileForRelativePath(path, parent);
        return registeredInstance().documentForUrl(URLUtil.urlAbsolute(file), parent.requestContext());
    }

    /**
     * XMLFactory can be subclassed by an application to provide alternate lookup
     * behavior.  An instance of the subclass should then be set as the "registeredInstance"
     */
    public static XMLFactory registeredInstance ()
    {
        if (_registeredInstance == null) {
            _registeredInstance = new XMLFactory();  // use default implementation
        }
        return _registeredInstance;
    }

    public static void setRegisteredInstance (XMLFactory instance)
    {
        _registeredInstance = instance;
    }


    /** Instance methods for Resource resolution */
    protected Document documentForUrl (URL url, AWRequestContext requestContext)
    {
        DocumentContext context = documentContext(requestContext);
        Document result = null;

        if ((context != null) && ((result = context.documentForUrl(url)) != null)) return result;

        result = readDocumentFromUrl(url);

        if (context != null) {
            context.registerDocument(result, url);
        }

        return result;
    }

    protected Document lookupReferenceDocument (Document referringDocument, String src)
    {
        Document result = null;
        if (src.equals(".")) {
            result = referringDocument;
        } else {
            // create absolute URL from path relative to referrent
            DocumentContext context = DocumentContext.contextForDocument(referringDocument);
            Assert.that(context != null, "Doc reference from document not loaded through context");
            URL referringUrl = context.urlForDocument(referringDocument);
            File referringFile = new File(referringUrl.getFile());
            File f = new File(referringFile.getParentFile(), src);
            URL docUrl = URLUtil.urlAbsolute(f);
            result = documentForUrl(docUrl, null);
        }
        return result;
    }

    protected Element lookupReference(Document referringDocument, String src, String elementPath, String key, String value)
    {
        return XMLUtil.expectElement(lookupReferenceDocument(referringDocument, src));
    }

    protected DocumentContext documentContext (AWRequestContext requestContext)
    {
        DocumentContext result = null;
        AWSession session;
        if ((requestContext != null) && ((session = requestContext.session()) != null)) {
            result = (DocumentContext)session.httpSession().getAttribute("XMLFactoryContext");
            if (result == null) {
                result = new DocumentContext();
                session.httpSession().setAttribute("XMLFactoryContext", result);
            }
        }
        return result;
    }

    public static Document documentFromString (String xmlString)
    {
        Document document = XMLUtil.document (new org.xml.sax.InputSource(xmlString), false, true, DefaultResolver);
        return document;
    }

    public static Document readDocumentFromUrl (URL url)
    {
        return XMLUtil.document(url, false, true, DefaultResolver);
    }

    public static Document readDocumentFromUrlString (String urlString)
    {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new AWGenericException(e);
        }

        return readDocumentFromUrl(url);
    }

    protected static boolean isReference (Element e)
    {
        String src = e.getAttribute("src");
        return (src != null) && (src.length() > 0) && (e.hasChildNodes() == false);
    }

    /** A scope of interrelated documents
     *      -- generally tied to a session.
     * FIXME!  -- demoware because LEAKS contexts
     */
    public static class DocumentContext
    {
        protected static Map _DocumentToContext = MapUtil.map();
        protected Map _urlToDocument = MapUtil.map();
        protected Map _documentToURL = MapUtil.map();

        public static DocumentContext contextForDocument (Document document)
        {
            return (DocumentContext)_DocumentToContext.get(document);
        }

        public void registerDocument (Document document, URL url)
        {
            _urlToDocument.put(url, document);
            _documentToURL.put(document, url);
            _DocumentToContext.put(document, this);
        }

        public Document documentForUrl (URL url)
        {
            return (Document)_urlToDocument.get(url);
        }

        public URL urlForDocument (Document document)
        {
            return (URL)_documentToURL.get(document);
        }
    }

    /**
     * Callback routines for fieldvalue
     */
    private static class Resolver implements XMLUtil.Resolver
    {
        public Element resolveReferences (Element e)
        {
            if ((e != null) && isReference(e)) {
                String src = e.getAttribute("src");
                e = registeredInstance().lookupReference(e.getOwnerDocument(), src, null, null, null);
            }
            return e;
        }

        public Element[] resolveReferences (Element[] elems)
        {
            // make a pass seeing whether we need a new array.  if so, resolve the children
            int count = elems.length;
            boolean hasRefs = false;
            for (int i=0; i<count; i++) {
                if (isReference(elems[i])) {
                    hasRefs = true;
                    break;
                }
            }

            if (hasRefs) {
                Element [] newElems = new Element[count];
                for (int i=0; i<count; i++) {
                    newElems[i] = resolveReferences(elems[i]);
                }
                elems = newElems;
            }
            return elems;
        }
    }

    public static Document documentFromStream (InputStream result)
    {
        String resultString = AWUtil.stringWithContentsOfInputStream(result);
        System.out.println("Response: " + resultString);
        result = new StringBufferInputStream(resultString);

        return XMLUtil.document(new org.xml.sax.InputSource(result), false, true, DefaultResolver);
        // Need tweak to XMLUtil (to use alternate resolver?)...
        // return XMLUtil.document(result, false, true);
    }

    /* hack because xerces parser having problem with file://d:/ */
    private static File DTDCACHE = null;

    /* ToDo: cXML support depends on CXMLNetworkFileCache which is not part of the open source frameworks...

    public static Document post (URL url, String content)
    {
        boolean isSSL = url.getProtocol().equals("https");
        String contentType = AWContentType.TextXml.name;
        String encoding = I18NUtil.EncodingUTF8;

        try {

            if (DTDCACHE == null) {
                File dtdCache = new File("etc/dtds/cXML");
                DTDCACHE = dtdCache;
                List urls = ListUtil.list(CXMLNetworkFileCache.CXMLUrlString, "http://svcqa.ariba.com/schemas/cXML/");
                    // add CXMLSource location from AppInfo, if available
                // ToDo: should not be hard-coded
                String cxmlSource = "Buyer"; // AppInfo.getRunningAppInfo().getCXMLSource();
                if (!StringUtil.nullOrEmptyOrBlankString(cxmlSource)) {
                    int index = cxmlSource.lastIndexOf("cXML");
                    if (index > -1) {
                        urls.add(StringUtil.strcat(cxmlSource.substring(0, index+"cXML".length()), "/"));
                    }
                }
                CXMLNetworkFileCache _cache = new CXMLNetworkFileCache(dtdCache, true, urls);
                NetworkFileCache.setDTDCache(_cache);
            }

            InputStream  result = HTTPClient.post (url, contentType, content, encoding, isSSL,
                    null, null);
            return documentFromStream(result);
        } catch (IOException e) {
            throw new AWGenericException(e);
        }
    }
    
    public static Document post (String urlString, String content)
    {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new AWGenericException(e);
        }

        return post(url, content);
    }

    public static Document post (String urlString, AWComponent component)
    {
        AWRequestContext context = ((AWConcreteApplication)AWConcreteApplication.sharedInstance()).createRequestContext(null);
        AWResponse response = component.generateResponse(null, context);

        String content = response.generateStringContents();
        return post(urlString, content);
    }

    public static Document post (String urlString, String filePath, AWComponent parentComponent)
    {
        String content = null;
        try {
            File file = ResourceLocator.fileForRelativePath(filePath, parentComponent);
            content = AWUtil.getString(new FileReader(file));
        } catch (FileNotFoundException e) {
            throw new AWGenericException(e);
        }
        return post(urlString, content);
    }

     */


    public static String toString (Document document)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLUtil.serializeDocument(document, out);
        return out.toString();
    }
}
