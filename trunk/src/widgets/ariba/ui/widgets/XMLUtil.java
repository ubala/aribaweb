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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/XMLUtil.java#4 $
*/
package ariba.ui.widgets;

import ariba.ui.aribaweb.util.AWGenericException;
import ariba.util.core.Assert;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.fieldvalue.FieldValue_Object;
import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.Attr;
import org.w3c.dom.DocumentType;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;


public final class XMLUtil
{
    private static boolean _DidReg = false;
    public static void registerXMLNodeFieldValueClassExtension ()
    {
        if (!_DidReg) {
            FieldValue.registerClassExtension(Node.class, new FieldValue_XMLNode());
            _DidReg = true;
        }
    }

    private static final String FeatureNodeExpansion =
        "http://apache.org/xml/features/dom/defer-node-expansion";
    private static final String FeatureContinueAfterFatalError =
        "http://apache.org/xml/features/continue-after-fatal-error";


        // jaxp 1.2 constants
    static final String JAXP_SCHEMA_LANGUAGE =
        "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String W3C_XML_SCHEMA =
        "http://www.w3.org/2001/XMLSchema";
    static final String JAXP_SCHEMA_SOURCE =
        "http://java.sun.com/xml/jaxp/properties/schemaSource";

    private static final String EmptyString = "";
    private static final String WildCard = "*";

    public static final DefaultResolver defaultResolver =
        new DefaultResolver();

    public static Document document (URL url, boolean validate,
                                     boolean removeWhitespace,
                                     EntityResolver resolver)
    {
        /* open input stream */
        InputStream input = null;
        Document    doc = null;

        try {
            input = url.openStream();
        }
        catch (IOException e) {
            throw new AWGenericException(Fmt.S("Cannot Open URL %s, error: %s",
                                              url.toString(), e.getMessage()));
        }
        try {
            /* create input source */
            String   urlString = url.toString();
            InputSource inputSource = new InputSource(input);

            /* turn on for debug  */
            /* Log.startupUtil.debug("Reading URL: " + urlString); */
            inputSource.setSystemId(urlString);

            /* parse document */
            doc = document(inputSource, validate,
                           removeWhitespace, resolver);
        }
        finally {
            try {
                /* close input stream */
                input.close();
            }
            catch (IOException e) {
                throw new AWGenericException(
                    Fmt.S("Cannot Open URL %s, error: %s",
                          url.toString(), e.getMessage()));
            }
        }

        return doc;
    }

    /**
        Creates a Document object based on the given URL

        @aribaapi ariba
    */
    public static Document document (InputSource content,
                                     boolean validate,
                                     boolean removeWhitespace,
                                     EntityResolver resolver)
    {
        Document doc = null;

        try {
            DocumentBuilder builder = getDOMParser(validate);

            // use default if none exists
            if (resolver != null) {
                builder.setEntityResolver(resolver);
            }

            // set error handler
            builder.setErrorHandler(new ErrorReporter());

            doc = builder.parse(content);

                // if ignore whitespace
            if (removeWhitespace) {
                Element root = doc.getDocumentElement();
                if (root != null) {
                    root.normalize();
                }
            }


        }
        catch (java.lang.IllegalArgumentException e) {
            throw new AWGenericException (
                Fmt.S("\n Bad arguments for parser: %s\n", e.getMessage()));

        }
        catch (SAXParseException e) {
            throw new AWGenericException (
                Fmt.S("\nParseException caught loading document %s: %s\n" +
                      "  Column:  %s\n    Line:  %s\n",
                      content.getSystemId(),
                      e.getMessage(),
                      Integer.toString(e.getColumnNumber()),
                      Integer.toString(e.getLineNumber())));
        }
        catch (SAXException e) {
            throw new AWGenericException(
                Fmt.S("\nGeneralException caught loading document %s: %s\n",
                      content.getSystemId(),
                      e.getMessage()));
        }
        catch (IOException e) {
            throw new AWGenericException(
                Fmt.S("\nIOException caught loading document %s: %s\n",
                      content.getSystemId(),
                      e.getMessage()));
        }

        return doc;
    }

    /**
        Creates a Document object based on the given URL

        @aribaapi ariba
    */
    public static Document document (String content,
                                     boolean validate,
                                     boolean removeWhitespace,
                                     EntityResolver resolver)
    {
        StringReader reader = new StringReader(content);
        return document(new InputSource(reader), validate, removeWhitespace, resolver);
    }

    public static DocumentBuilder getDOMParser (boolean validating)
    {
        return getDOMParser(validating,false,false);
    }

    public static DocumentBuilder getDOMParser (boolean validating,
                                                boolean enableDeferNodeExpantion,
                                                boolean enableContinueAfterFatalError)
    {
        try {
            DocumentBuilderFactory dbf =
                DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setValidating(validating);
            if (enableDeferNodeExpantion) {
            dbf.setAttribute(FeatureNodeExpansion,
                             Boolean.TRUE);
            }
            if (enableContinueAfterFatalError) {
                dbf.setAttribute(FeatureContinueAfterFatalError,
                                 Boolean.TRUE);
            }
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setEntityResolver(defaultResolver);
            return db;
        }
        catch (ParserConfigurationException e) {
            throw new AWGenericException(
                Fmt.S("Cannot create DOM Parser error: %s",e.getMessage()));
        }
        catch (IllegalArgumentException e) {
            throw new AWGenericException(
                Fmt.S("Cannot create DOM Parser error: %s",e.getMessage()));
        }

    }

    public static boolean isElement (Node e)
    {
        return e.getNodeType() == Node.ELEMENT_NODE;
    }

    /**
        Return an array of all the children of PARENT that are TAG
        elements. <p>
        @return all children with matching <code>tag</code>;
                is not <code>null</code>
    */
    public static Element[] getAllChildren (Element parent, String tag)
    {
        if (parent == null) {
            return new Element[0];
        }

        // get the children with this tag
        return getElementsByTag(parent, tag);
    }
    
    private static Element[] getElementsByTag (Element element, String tag)
    {
        NodeList children = element.getChildNodes();
        int  size = children.getLength();
        int  elemCount = 0;
        boolean wildCard = tag == null;

        for (int i = 0; i < size; i++) {
            Node child = children.item(i);
            if (isElement(child) &&
                (wildCard || tag.equals(((Element)child).getTagName()))) {
                elemCount++;
            }
        }

        Element  elems[] = new Element[elemCount];
        for (int i = 0, j = 0; j < elemCount; i++) {
            Node child = children.item(i);

            if (isElement(child) &&
                (wildCard || tag.equals(((Element)child).getTagName()))) {
                elems[j++] = (Element)child;
            }
        }

        return elems;
    }
    
    public static Element getOneChildMaybe (Element parent, String tag)
    {
        if (parent == null) {
            return null;
        }

        // get the children with this tag
        Element children[] = getElementsByTag(parent, tag);
        int count = children.length;

        if (count == 0) {
            return null;
        }
        else if (count > 1) {
            Assert.that(false, "Multiple occurrences of tag %s", tag);
            return null;
        }
        else {
            return children[0];
        }
    }

    /**
        Returns first child element of the specified parent element.
        Throws an XMLParseException is a child container element is not found.
    */
    public static Element getFirstChildElement (Element parent)
    {
        if (parent != null) {
            // get the children with this tag
            Element children[] = getElementsByTag(parent, WildCard);
            int count = children.length;
            if (count > 0) {
                return children[0];
            }
        }

        throw new AWGenericException(
            Fmt.S("Did not find required sub-element under parent element '%s'",
                   (parent == null ? "Null Parent" : parent.getTagName())));
    }

    /**
        Serialize an XML Document using a stream. The document pretty
        prints by default - if you didn't remove whitespace then it should
        retain structure - DTD path will be fully resolved
        assumes that this util was used to get document
    */
    public static void serializeDocument (Document doc, OutputStream output)
    {
        Document document = doc;
        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            DocumentType docType = doc.getDoctype();
            if ( docType != null && docType.getSystemId()!=null) {
                transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
                                              docType.getSystemId());
            }
            transformer.transform(new DOMSource(doc),
                                  new StreamResult(output));
        }
        catch (TransformerConfigurationException e)
        {
            throw new AWGenericException(
                Fmt.S("\nUnable to generate *copy* transformer %s: %s\n",
                      doc.getDocumentElement().getTagName(),
                      e.getMessage()));
        }
        catch (TransformerException e)
        {
            throw new AWGenericException(
                Fmt.S("\nUnable to execute *copy* transformer %s: %s\n",
                      doc.getDocumentElement().getTagName(),
                      e.getMessage()));
        }
    }

    private static FastStringBuffer getTextBuffer (Node parent)
    {
        FastStringBuffer buffer = new FastStringBuffer();

        NodeList childNodes = parent.getChildNodes();
        int  listSize = childNodes.getLength();
        boolean found = false;

        for (int i = 0; i < listSize; i++) {
            Node childNode = childNodes.item(i);

            if (childNode.getNodeType() == Node.COMMENT_NODE) {
                continue;
            }

            if (childNode.getNodeType() == Node.TEXT_NODE ||
                childNode.getNodeType() == Node.CDATA_SECTION_NODE) {

                buffer.append(((CharacterData)childNode).getData());
                found = true;
            }
            else if (childNode.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
                FastStringBuffer refResult = getTextBuffer(childNode);
                if (refResult != null) {
                    buffer.append(refResult);
                    found = true;
                }
            }
            else {
                 // found non text content
                throw new AWGenericException(
                    Fmt.S("Expected text for parent %s, got non-text elements", parent));
            }
        }

        if (found) {
            return buffer;
        }

        return null;
    }

    protected static void expectNodeType (Node node, int type)
    {
        if (node == null || node.getNodeType() != type) {
            throw new AWGenericException(Fmt.S("expected node type %s",
                                              type));
        }
    }

    protected static Node getNodeIfDocument (Node node)
    {
        if (node.getNodeType() == Node.DOCUMENT_NODE) {
            return ((Document)node).getDocumentElement();
        }
        else {
            return node;
        }
    }

    public static Element expectElement (Node node)
    {
        node = getNodeIfDocument(node);
        expectNodeType(node, Node.ELEMENT_NODE);
        return (Element)node;
    }

    /**
        Raise an exception if the current element is not the expected
        tag - namespaces are not supported.
    */
    public static void expectTag (Element elem, String tag)
    {
        if (!elem.getTagName().equals(tag)) {
            throw new AWGenericException(Fmt.S(
                "expected element tagged %s, got %s",
                tag, elem.getTagName()));
        }
    }

    /**
        Assume ELEMENT had attribute ATTRNAME, get its value and
        return it as a string.  If there is no attribute return the default.
    */
    public static String stringAttribute (Element element,
                                          String  attrName,
                                          String  theDefault)
    {
        String attr = element.getAttribute(attrName);

        if (attr.equals(EmptyString)) {
            /* empty string maybe a valid option */
            Attr attrNode = element.getAttributeNode(attrName);
            if (attrNode != null) {
                return EmptyString;
            }

            return theDefault;
        }
        else {
            return attr;
        }
    }
    
    public static String getText (Element parent, String theDefault)
    {
        FastStringBuffer result = getTextBuffer(parent);

        // if there was empty content return default
        if (result == null) {
            return theDefault;
        }

        return result.toString();
    }

    static class ErrorReporter implements ErrorHandler
    {
        public void warning (SAXParseException e)
        {
            FastStringBuffer buf = new FastStringBuffer();
            buf.append("** ");
            buf.append("Warning");
            buf.append("\n   URI = ");
            buf.append(e.getSystemId());
            buf.append(" Line = ");
            buf.append(ariba.util.core.Constants.getInteger(e.getLineNumber()).toString());
            buf.append("\n   Message = ");
            buf.append(e.getMessage());
            ariba.util.log.Log.util.debug("%s",buf);
        }

        public void error (SAXParseException e) throws SAXException
        {
            throw e;
        }

        public void fatalError (SAXParseException e) throws SAXException
        {
            throw e;
        }
    }

    public static class DefaultResolver implements EntityResolver
    {

        /**
            Resolver interface

            This default interface has been created to overcome the Xerces BaseURI
            bug for multi-byte character. The URIResolver supplied with Xerces unable
            to construct the uri with proper URI
            For details see: http://nagoya.apache.org/bugzilla/show_bug.cgi?id=14239

            @aribaapi private
        */
        public InputSource resolveEntity (String publicId,
                                          String systemId)
          throws SAXException, IOException
        {
            if ( systemId == null ) {
                return  null;
            }
            int protocolSpliceIndex = systemId.indexOf(":");
            String protocol = "";
            if (protocolSpliceIndex != -1 ) {
                protocol = systemId.substring(0, protocolSpliceIndex);
                }
            if (protocol.equalsIgnoreCase("file") ||
                protocol.equalsIgnoreCase("http"))
            {
                return new InputSource(systemId);
            }
            File file = new File(systemId).getCanonicalFile();
            if (!file.exists()) {
                return null;
            }
            return new InputSource(file.toURL().toString());
        }
    }
        
    /**
     * This interface is used by demoshell to do lookups on elements that specify references to
     * nodes in other files.
     */
    public interface Resolver
    {
        Element resolveReferences (Element e);
        Element[] resolveReferences (Element[] elems);
    }

    private static Resolver _Resolver = null;
    public static void setResolver (Resolver r)
    {
        _Resolver = r;
    }

    public static Element resolveReferences (Element e)
    {
        if (_Resolver != null) {
            e = _Resolver.resolveReferences(e);
        }
        return e;
    }

    public static Element[] resolveReferences (Element[] e)
    {
        if (_Resolver != null) {
            e = _Resolver.resolveReferences(e);
        }
        return e;
    }

    public static final class FieldValue_XMLNode extends FieldValue_Object
    {
        public Object getFieldValuePrimitive (Object receiver, FieldPath keyPath)
        {
            Element element = expectElement((Node)receiver);
            String key = keyPath.car();

            // try a string value
            String attrValue = stringAttribute (element, key, null);
            if (attrValue != null) return attrValue;

            // magic keys...
            if (key.equals("tagName")) {
                return element.getTagName();
            }

            if (key.equals("children")) {
                return resolveReferences(getElementsByTag(element, null));
            }

            if (key.equals("text")) {
                String content = getText(element, null);
                return content;
            }

            if (key.endsWith("[]")) {
                key = key.substring(0, key.length()-2);
                Element[] children = getElementsByTag(element, key);
                return resolveReferences(children);
            }

            // how about a single node?
            Element child = getOneChildMaybe(element, key);
            return resolveReferences(child);
        }

        public void setFieldValue (Object target,
                                   FieldPath fieldPath, Object value)
        {
            // create intermediate child nodes on demand
            try {
                Element element = expectElement((Node)target);
                if ((fieldPath.cdr() != null)
                        && (getOneChildMaybe(element, fieldPath.car()) == null))
                {
                    Element e = element.getOwnerDocument().createElement(fieldPath.car());
                    element.appendChild(e);
                }
                super.setFieldValue(target, fieldPath, value);
            } catch (DOMException e) {
                throw new AWGenericException(e);
            }
        }

        public void setFieldValuePrimitive (Object receiver, FieldPath keyPath, Object value)
        {
            Assert.that((keyPath.cdr()==null), "setFieldValuePrimitive called with elements left in path");
            String key = keyPath.car();

            try {
                Element element = expectElement((Node)receiver);

                if (value instanceof Node) {
                    element.appendChild((Node)value);
                } else if (key.equals("text")) {
                    // remove existing children
                    Node child;
                    while ((child = element.getFirstChild()) != null) element.removeChild(child);

                    // Add new Text child
                    Text e = element.getOwnerDocument().createTextNode(value.toString());
                    element.appendChild(e);
                } else {
                    element.setAttribute(key, value.toString());
                }
            } catch (DOMException e) {
                throw new AWGenericException(e);
            }
        }
    }
}
