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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWHtmlTemplateParser.java#49 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWCaseInsensitiveHashtable;
import ariba.ui.aribaweb.util.AWNamespaceManager;
import ariba.util.core.ClassUtil;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import java.util.Map;
import ariba.util.core.StringArray;
import ariba.util.core.StringUtil;
import ariba.util.core.Assert;

import java.util.List;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

public class AWHtmlTemplateParser extends AWBaseObject implements AWTemplateParser
{
    protected static Class AWGenericElementClass = AWGenericElement.class;
    protected static Class AWGenericContainerClass = AWGenericContainer.class;
    private static final Map EmtpyHashTable = MapUtil.map();
    private static final AWElementContaining DummyContainableElement = new AWConcreteTemplate();
    private static final String AttributeDemiliterSet = " \t\r\n/";

    private AWComponent _component;
    private AWNamespaceManager.Resolver _resolver;
    private String _templateName;
    private AWTemplate _resultingTemplate;
    private AWApplication _application;
    private AWElementContaining _topOfStack;
    private FastStringBuffer _currentStringBuffer;
    private List _containerStack;
    private List _nameStack;
    protected String _templateString;
    private int _templateStringLength;
    private int _currentIndex = 0;
    private int _currentLine = 0;
    private int _lastOpenTagLine = 0;
    private Map _standardTagsHashtable;
    private Map _elementClassesByName;
    private Map _containerClassesByName;
    private Map _tagNameAliases;
    private boolean _useXmlEscaping;
    private int _embeddedKeyPathSuppressCount;

    // ** Thread Safety Considerations: this will lock at all the external entry points (ie all public methods) even though that's a huge scope.  Since in a deployed app, the parser is only run at warmup time, this large scope won't hurt things too much.

    public void init (AWApplication application)
    {
        this.init();
        _application = application;
        _nameStack = ListUtil.list();
        _containerStack = ListUtil.list();
        _standardTagsHashtable = new AWCaseInsensitiveHashtable();
        _elementClassesByName = MapUtil.map();
        _containerClassesByName = MapUtil.map();
        _tagNameAliases = MapUtil.map();
        // leave this commented-out code here as it is appropriate place to do this, however due
        // to the way resource dirs are registered after the first template parser is created, we need
        // this in resetParser below.  Perhaps someday we can re-jigger all that and do this here.
        //registerStandardTagNames();
    }

    private void registerStandardTagNames ()
    {
        AWResource resource = _application.resourceManager().resourceNamed("AWStandardTagNames.txt");
        if (resource != null) {
            InputStream inputStream = resource.inputStream();
            String standardTagNamesString = AWUtil.stringWithContentsOfInputStream(inputStream);
            AWUtil.close(inputStream);
            StringArray standardTagNames = AWUtil.componentsSeparatedByString(standardTagNamesString, ",");
            String[] standardTagNamesArray = standardTagNames.array();
            for (int index = (standardTagNames.inUse() - 1); index >= 0; index--) {
                String currentTagName = standardTagNamesArray[index];
                currentTagName = currentTagName.trim();
                registerStandardTagName(currentTagName);
            }
        }
    }

    public static void duplicateRegistrationsIntoOther (Map elementClassesByName,
                                                        Map containerClassesByName,
                                                        AWTemplateParser other)
    {
        Iterator e;

        e = elementClassesByName.keySet().iterator();
        while (e.hasNext()) {
            String key = (String)e.next();
            other.registerElementClassForTagName(key, (Class)elementClassesByName.get(key));
        }

        e = containerClassesByName.keySet().iterator();
        while (e.hasNext()) {
            String key = (String)e.next();
            other.registerContainerClassForTagName(key, (Class)containerClassesByName.get(key));
        }
    }

    /**
        Registers all of the container and element classes registered on "other" onto this parser.
        (Useful to get all of the classes registered on the AWComponent.defaultTemplateParser()).
    */
    public synchronized void duplicateRegistrationsIntoOther (AWTemplateParser other)
    {
        duplicateRegistrationsIntoOther(_elementClassesByName, _containerClassesByName, other);
    }

    private Class classForName (String className)
    {
        Class targetClass = null;
        if (className.length() > 0) {
            if (Character.isJavaIdentifierStart(className.charAt(0))) {
                targetClass = _application.resourceManager().classForName(className);
                if (targetClass == null) {
                    AWComponentDefinition componentDefinition = _application.componentDefinitionForName(className);
                    if (componentDefinition != null) {
                        targetClass = componentDefinition.componentClass();
                    }
                }
            }
        }
        return targetClass;
    }

    private Class classForTagNameInHashtable (String tagName, Map classHashtable)
    {
        Class elementClass = (Class)classHashtable.get(tagName);
        if (elementClass == null) {
            elementClass = (Class)_standardTagsHashtable.get(tagName);
            if (elementClass == null) {
                elementClass = classForName(tagName);
            }
        }
        if (elementClass != null && (elementClass == ObjectClass
                || !AWUtil.classImplementsInterface(elementClass, AWCycleable.class)))
        {
            elementClass = null;
        }
        return elementClass;
    }

    private Class elementClassForTagName (String tagName)
    {
        return classForTagNameInHashtable(tagName, _elementClassesByName);
    }

    private Class containerClassForTagName (String tagName)
    {
        return classForTagNameInHashtable(tagName, _containerClassesByName);
    }

    public synchronized void registerContainerClassForTagName (String tagName, Class containerClass)
    {
        _containerClassesByName.put(tagName.intern(), containerClass);
    }

    public synchronized void unregisterContainerClassForTagName (String tagName)
    {
        _containerClassesByName.remove(tagName);
    }

    public synchronized void registerElementClassForTagName (String tagName, Class elementClass)
    {
        _elementClassesByName.put(tagName.intern(), elementClass);
    }

    public synchronized void unregisterElementClassForTagName (String tagName)
    {
        _elementClassesByName.remove(tagName);
    }

    public synchronized boolean isRegisteredElementClassForTagName (String tagName)
    {
        return (_elementClassesByName.get(tagName) != null);
    }

    public synchronized void registerStandardTagName (String tagName)
    {
        _standardTagsHashtable.put(tagName.intern(), ObjectClass);
    }

    private Map bindingsFromAttributeList (Map attributeList)
    {
        boolean isDebuggingEnabled = false;
        String awdebugValue = (String)attributeList.get(AWBindingNames.awdebug);
        if (awdebugValue != null) {
            isDebuggingEnabled = awdebugValue.equals("$true") || awdebugValue.equals("true");
            attributeList.remove(AWBindingNames.awdebug);
        }
        // LinkedHashMap to keep bindings in declaration order
        Map bindingsHashtable = new LinkedHashMap();
        Iterator keysIterator = attributeList.keySet().iterator();
        while (keysIterator.hasNext()) {
            AWBinding newBinding = null;
            String currentAttributeName = (String)keysIterator.next();
            String currentAttributeValue = (String)attributeList.get(currentAttributeName);
            char firstChar = (currentAttributeValue.length() > 0) ? currentAttributeValue.charAt(0) : '\0';
            if (firstChar == '$') {
                // ** remove the '$'
                String keyPathString = currentAttributeValue.substring(1);
                newBinding = AWBinding.bindingWithNameAndKeyPath(currentAttributeName, keyPathString);
            }
            else if (firstChar == '^') {
                newBinding = AWBinding.bindingWithNameAndKeyPath(currentAttributeName, currentAttributeValue);
            }
            else if (firstChar == '@') {
                newBinding = AWBinding.bindingWithNameAndNLSKey(currentAttributeName, currentAttributeValue);
            } else {
                newBinding = AWBinding.bindingWithNameAndConstant(currentAttributeName, currentAttributeValue);
            }
            newBinding.setIsDebuggingEnabled(isDebuggingEnabled);
            bindingsHashtable.put(newBinding.bindingName(), newBinding);
        }
        return bindingsHashtable;
    }

    //////////////////
    // Stack Handling
    //////////////////
    void setTopOfStack (AWElementContaining elementContainer)
    {
        _topOfStack = elementContainer;
        if (_topOfStack == null) {
            resetParser();
            throw new AWGenericException("Template parsing cannot continue -- topOfStack is null.");
        }
    }

    void addToTopOfStack (AWElement element)
    {
        if (_topOfStack == null) {
            resetParser();
            throw new AWGenericException("Template parsing cannot continue -- topOfStack is null.");
        }
        _topOfStack.add(element);
    }

    private String stripFormattingWhitespace (String stringValue)
    {
        int stringLength = stringValue.length();
        char[] destChars = new char[stringLength];
        int destCharsIndex = 0;
        for (int index = 0; index < stringLength; index++) {
            char currentChar = stringValue.charAt(index);
            int nextCharIndex = index + 1;
            if ((currentChar == '\\') && (nextCharIndex < stringLength)) {
                char nextChar = stringValue.charAt(nextCharIndex);
                if (AWUtil.isNewline(nextChar)) {
                    for (index++; index < stringLength; index++) {
                        currentChar = stringValue.charAt(index);
                        if (!AWUtil.isWhitespace(currentChar)) {
                            break;
                        }
                    }
                    if (currentChar == '\\') {
                        index--;
                        continue;
                    }
                    if (index >= stringLength) {
                        break;
                    }
                }
            }
            destChars[destCharsIndex] = currentChar;
            destCharsIndex++;
        }
        return (destCharsIndex > 0) ? new String(destChars, 0, destCharsIndex) : "";
    }

    void pushString (String stringValue)
    {
        if (_currentStringBuffer == null) {
            _currentStringBuffer = new FastStringBuffer();
        }
        String strippedStringValue = stripFormattingWhitespace(stringValue);
        _currentStringBuffer.append(strippedStringValue);
    }

    void pushString (FastStringBuffer stringBuffer)
    {
        pushString(stringBuffer.toString());
    }

    /**
    This method converts escaped $ characters with plain $ characters
    and then creates an AWBareString element and adds it to the stack.
    */
    private void addBareString (String string)
    {
        if (string.indexOf("\\$") != -1) {
            string = AWUtil.replaceAllOccurrences(string, "\\$", "$");
        }
        if (string.length() > 0) {
            AWBareString bareStringElement = AWBareString.getInstance(string);
            addToTopOfStack(bareStringElement);
        }
    }

    public static final Pattern _NewlineWSPat = Pattern.compile("\\s*[\\n\\r]\\s*", Pattern.MULTILINE);

    /**
    This method takes an ordinary bare string and scans it for embedded
    $key.path sequences.  Such embeded $key.path sequences are extracted and
    converted to AWString elements.  This allows for cleaner refs to AWStrings,
    especially from within Html templates.
    */
    void finalizeCurrentString ()
    {
        if (_currentStringBuffer != null) {
            if (_currentStringBuffer.length() > 0) {
                String bareString = _currentStringBuffer.toString();

                int bareStringLength = bareString.length();
                int currentStartIndex = 0;
                int indexOfDollar = bareString.indexOf("$", currentStartIndex);
                boolean allowEmbeddedKeyPaths = (_embeddedKeyPathSuppressCount == 0)
                        && (_component == null ? _application.useEmbeddedKeyPathes()
                                               : _component.allowEmbeddedKeyPaths());
                while (indexOfDollar != -1 && allowEmbeddedKeyPaths) {
                    if (indexOfDollar == 0 || bareString.charAt(indexOfDollar - 1) != '\\') {
                        String prefix = bareString.substring(currentStartIndex, indexOfDollar);
                        addBareString(prefix);
                        // find end
                        int currentEndIndex = indexOfNonKeyPathChar(bareStringLength, indexOfDollar, bareString);
                        String bindingString = bareString.substring(indexOfDollar, currentEndIndex);
                        if (bindingString.length() > 1) {
                            bindingString = bindingString.substring(1);
                            AWBinding binding = AWBinding.bindingWithNameAndKeyPath(AWBindingNames.value, bindingString);
                            Map bindingsHashtable = MapUtil.map();
                            bindingsHashtable.put(AWBindingNames.value, binding);
                            AWString stringElement = new AWString();
                            stringElement.init(AWString.StandAloneStringTag, bindingsHashtable);
                            stringElement.setTemplateName(_templateName);
                            int lineNumber = _currentLine - countCarriageReturns(bareString.substring(currentEndIndex));
                            stringElement.setLineNumber(lineNumber);
                            addToTopOfStack(stringElement);
                        }
                        else {
                            addBareString(bindingString);
                        }
                        currentStartIndex = currentEndIndex;
                        indexOfDollar = bareString.indexOf("$", currentStartIndex);
                    }
                    else if (indexOfDollar + 1 >= bareStringLength) {
                        String remainder = bareString.substring(currentStartIndex, bareStringLength);
                        addBareString(remainder);
                        break;
                    }
                    else {
                        indexOfDollar = bareString.indexOf("$", indexOfDollar + 1);
                    }
                    if (currentStartIndex >= bareStringLength) {
                        break;
                    }
                }
                if (currentStartIndex < bareStringLength) {
                    String remainingString = bareString.substring(currentStartIndex);
                    addBareString(remainingString);
                }
            }
            _currentStringBuffer = null;
        }
    }

    private int indexOfNonKeyPathChar (int bareStringLength, int indexOfDollar, String bareString)
    {
        int currentEndIndex = bareStringLength;
        for (int i=indexOfDollar+1, length=bareString.length(); i < length; i++) {
            char currChar = bareString.charAt(i);
            if (!Character.isLetterOrDigit(currChar) &&
                currChar != '.' &&
                currChar != '^' &&
                currChar != '_' &&
                currChar != '|' &&
                !(currChar == '$' && (i > indexOfDollar+1) && bareString.charAt(i-1) == '|') &&
                currChar != '[' &&
                currChar != ']' &&
                currChar != '!') {
                currentEndIndex = i;
                break;
            }
        }
        return currentEndIndex;
    }

    void pushContainableElementWithName (AWElementContaining containableElement, String elementName)
    {
        finalizeCurrentString();
        _nameStack.add(elementName);
        _containerStack.add(containableElement);
        setTopOfStack(containableElement);
        if (containableElement instanceof SupressesEmbeddedKeyPaths) _embeddedKeyPathSuppressCount++;
    }

    private void popContainableElementWithName (String elementName)
    {
        finalizeCurrentString();
        int stackCount = _containerStack.size();
        int index = stackCount - 1;
        while (index >= 0) {
            String lastElementName = (String)_nameStack.get(index);
            if (index == 0) {
                logWarning("** Warning: Unbalanced tags. Found closing tag </" + elementName + "> templateName: \"" + _templateName + "\"" + " lineNumber: "+ _currentLine);
                pushString("&nbsp;<font color=\"#ff0000\"><b>** <blink>Warning</blink>: Unbalanced tags. Found closing tag &lt;/" + elementName + "&gt;\"" +
                           " templateName: \"" + _templateName + "\" lineNumber: "+ _currentLine + "</b></font><br/>");
                break;
            }
            Object oldTop = _containerStack.remove(index);
            if (oldTop instanceof SupressesEmbeddedKeyPaths) _embeddedKeyPathSuppressCount--;

            _nameStack.remove(index);
            AWElementContaining newTopOfStack = (AWElementContaining)ListUtil.lastElement(_containerStack);
            setTopOfStack(newTopOfStack);
            if (elementName.equals(lastElementName)) {
                break;
            }
            else {
                logWarning("** Warning: Unbalanced tags.  Expecting \"</" + lastElementName + ">\" Found \"</" + elementName + ">\" templateName: \"" + _templateName + "\""+ " lineNumber: "+ _currentLine);
                String errorString = "&nbsp;<font color=\"#ff0000\"><b>** <blink>Warning</blink>: possibly missing some content due to unbalanced tags." +
                                     " Expecting \"&lt;/" + lastElementName + "&gt;\""+
                                     " Found \"&lt;/" + elementName + "&gt;\""+
                                     " templateName: \"" + _templateName + "\" lineNumber: " + _currentLine + "</b></font><br/>";
                pushString(errorString);
            }
            index--;
        }
    }

    private void flushStacks ()
    {
        _containerStack.clear();
        _nameStack.clear();
        _topOfStack = null;
    }

    ///////////////////////
    // Event handlers
    ///////////////////////
    protected void startDocument ()
    {
        flushStacks();
        _currentStringBuffer = null;
        _resultingTemplate = new AWConcreteTemplate();
        _resultingTemplate.init();
        _currentLine = 1;
        pushContainableElementWithName((AWElementContaining)_resultingTemplate, "DOCUMENT");
    }

    protected void endDocument ()
    {
        // use > 1 because DOCUMENT tag is left in stack at this point.
        if (_nameStack.size() > 1) {
            ListUtil.removeFirstElement(_nameStack);
            String errorMessage = "Error: unbalanced tags detected.  Need to close the following tags: " + _nameStack + " templateName: \"" + _templateName + "\"";
            logWarning(errorMessage);
            pushString("&nbsp;<font color=\"#ff0000\"><blink><b>" + errorMessage + "</b></blink></font>");
        }
        if (!_resultingTemplate.hasElements()) {
            // This handles case of a completely empty .awl file
            characters(" ");
        }
        finalizeCurrentString();
        flushStacks();
        _currentStringBuffer = null;
    }

    protected void startElement (String elementName, String resolvedName, Class elementClass, Map bindingsHashtable)
    {
        AWElement element = null;
        if (AWUtil.classImplementsInterface(elementClass, AWCycleable.class)) {
            try {
                AWCycleableReference cycleableReference = (AWCycleableReference)elementClass.newInstance();
                element = cycleableReference.determineInstance(elementName, translateAliasedTagName(resolvedName), bindingsHashtable, _templateName, _lastOpenTagLine);
                ((AWBaseElement)element).setTemplateName(_templateName);
                ((AWBaseElement)element).setLineNumber(_lastOpenTagLine);
            }
            catch (IllegalAccessException illegalAccessException) {
                throw new AWGenericException(illegalAccessException);
            }
            catch (InstantiationException instantiationException) {
                String errorMessage = Fmt.S("Problem creating new instance of \"%s\" (%s) templateName: \"%s\" lineNumber: %s",
                                            elementName, elementClass.getName(), _templateName, String.valueOf(_currentLine));
                throw new AWGenericException(errorMessage, instantiationException);
            }
        }
        else {
            throw new AWGenericException("Error: attempt to use a \"" + elementClass.getName() + "\" as a dynamic element or subcomponent.\nOnly subclasses of \"AWComponent\" or classes which implement the \"AWBindable\" interface may be used as tags within templates.");
        }
        finalizeCurrentString();
        addToTopOfStack(element);
        if (element instanceof AWElementContaining) {
            pushContainableElementWithName((AWElementContaining)element, elementName);
        }
        else {
            pushContainableElementWithName(DummyContainableElement, elementName);
        }
    }

    // Embedded (inline) key paths) should be left alone
    public interface SupressesEmbeddedKeyPaths {}

    // means body should be literal string (bare string) -- not interpreted
    public interface LiteralBody extends SupressesEmbeddedKeyPaths {}
    public static class LiteralContainer extends AWGenericContainer implements LiteralBody {}

    // Hook to filter tag contents during parse (before parsing body)
    public interface FilterBody
    {
        public String filterBody (String bodyString);
    }

    protected void endElement (String elementName)
    {
        popContainableElementWithName(elementName);
    }

    protected void characters (String bareString)
    {
        // Applying whitespace compression to XML caused (false) cxml unit test failures.
        //  -- replacing the white spaces caused the diff to fail against the base line.
        // For now we'll only do this for non-XML files
        if (!_useXmlEscaping)  bareString = _NewlineWSPat.matcher(bareString).replaceAll("\n");

        pushString(bareString);
    }

    /**
        Looks up and returns the actual tagName for an aliased tag, if one exists; otherwise returns the orignal tagName which is passed in.  This allows AWComponent's determineInstance to be a little bit smarter about the way it looks up the componentDefinitions and avoid scanning through all package names to locate the class for the aliased component tag.
    */
    private String translateAliasedTagName (String tagName)
    {
        String translatedTagName = null;
        if (tagName != null) {
            translatedTagName = (String)_tagNameAliases.get(tagName);
            if (translatedTagName == null) {
                Class classAlias = (Class)_elementClassesByName.get(tagName);
                if (classAlias == null) {
                    classAlias = (Class)_containerClassesByName.get(tagName);
                }
                if (classAlias != null) {
                    translatedTagName = ClassUtil.stripPackageFromClassName(classAlias.getName());
                    _tagNameAliases.put(tagName, translatedTagName);
                }
                else {
                    translatedTagName = tagName;
                }
            }
        }
        return translatedTagName;
    }

    ///////////////
    // Tokenizing
    ///////////////
    private int indexOfCharInSet (String sourceString, String charSet, int startIndex)
    {
        int indexOfCharInSet = -1;
        int sourceStringLength = sourceString.length();
        int currentIndex = startIndex;
        char currentChar = '\0';
        while ((currentIndex < sourceStringLength) && (currentChar = sourceString.charAt(currentIndex)) != '\0') {
            if (charSet.indexOf(currentChar) != -1) {
                indexOfCharInSet = currentIndex;
                break;
            }
            currentIndex++;
        }
        return indexOfCharInSet;
    }

    private int skipCharactersInSet (String sourceString, String charSet, int startIndex)
    {
        int currentIndex = startIndex;
        int sourceStringLength = sourceString.length();
        if (currentIndex < sourceStringLength) {
            while (charSet.indexOf(sourceString.charAt(currentIndex)) != -1) {
                currentIndex++;
                if (currentIndex >= sourceStringLength) {
                    break;
                }
            }
        }
        return currentIndex;
    }

    private String removeBackslashes (String dirtyString)
    {
        String cleanString = dirtyString;
        if (dirtyString.indexOf('\\') != -1) {
            int dirtyStringLength = dirtyString.length();
            FastStringBuffer stringBuffer = new FastStringBuffer(dirtyStringLength);
            for (int index = 0; index < dirtyStringLength; index++) {
                char currentChar = dirtyString.charAt(index);
                if (currentChar == '\\') {
                    index++;
                    if (index < dirtyStringLength) {
                        currentChar = dirtyString.charAt(index);
                    }
                }
                stringBuffer.append(currentChar);
            }
            cleanString = stringBuffer.toString();
        }
        return cleanString;
    }

    private void addToTagAttributesMap (Map tagAttributes, String key, String value)
    {
        if (!key.startsWith("__")) {
            tagAttributes.put(key, value);
        }
    }

    private Map parseTagAttributes (String tagBodyString)
    {
        String equalsSkipSet = "= \t\n\r/";
        // use LinkedHashMap to preserve attribute ordering
        Map tagAttributes = new LinkedHashMap();
        int currentIndex = 0;
        if (tagBodyString.endsWith("/")) {
            tagBodyString = tagBodyString.substring(0, tagBodyString.length() - 1);
        }
        int tagBodyLength = tagBodyString.length();
        while (currentIndex < tagBodyLength) {
            currentIndex = skipCharactersInSet(tagBodyString, AttributeDemiliterSet, currentIndex);
            if (currentIndex < tagBodyLength) {
                int equalsIndex = indexOfCharInSet(tagBodyString, equalsSkipSet, currentIndex);
                if (equalsIndex == -1) {
                    String attributeName = tagBodyString.substring(currentIndex, tagBodyLength);
                    addToTagAttributesMap(tagAttributes, attributeName, "awstandalone");
                    currentIndex = tagBodyLength;
                }
                else {
                    String attributeName = tagBodyString.substring(currentIndex, equalsIndex);
                    currentIndex = skipCharactersInSet(tagBodyString, equalsSkipSet, equalsIndex);
                    if (currentIndex >= tagBodyLength) {
                        addToTagAttributesMap(tagAttributes, attributeName, "awstandalone");
                    }
                    else {
                        String equalsString = tagBodyString.substring(equalsIndex, currentIndex);
                        if (equalsString.indexOf('=') == -1) {
                            addToTagAttributesMap(tagAttributes, attributeName, "awstandalone");
                        }
                        else {
                            char quoteChar = tagBodyString.charAt(currentIndex);
                            if ((quoteChar == '"') || (quoteChar == '\'')) {
                                currentIndex++;
                                int closingQuoteIndex = tagBodyString.indexOf(quoteChar, currentIndex);
                                while ((closingQuoteIndex > - 1) && tagBodyString.charAt(closingQuoteIndex - 1) == '\\' && tagBodyString.charAt(closingQuoteIndex - 2) != '\\') {
                                    closingQuoteIndex = tagBodyString.indexOf(quoteChar, closingQuoteIndex + 1);
                                }
                                if (closingQuoteIndex == -1) {
                                    throw new AWGenericException("Error: missing closing quote in tag with body \"" + tagBodyString + "\" templatename: \"" + _templateName + "\"");
                                }
                                else {
                                    String attributeValueString = tagBodyString.substring(currentIndex, closingQuoteIndex);
                                    attributeValueString = removeBackslashes(attributeValueString);
                                    addToTagAttributesMap(tagAttributes, attributeName, attributeValueString);
                                    currentIndex = closingQuoteIndex + 1;
                                }
                            }
                            else {
                                String valueTerminatorCharacterSet = " \t\n\r";
                                int nextTerminatorIndex = indexOfCharInSet(tagBodyString, valueTerminatorCharacterSet, currentIndex);
                                if (nextTerminatorIndex == -1) {
                                    nextTerminatorIndex = tagBodyLength;
                                }
                                String attributeValueString = tagBodyString.substring(currentIndex, nextTerminatorIndex);
                                attributeValueString = removeBackslashes(attributeValueString);
                                addToTagAttributesMap(tagAttributes, attributeName, attributeValueString);
                                currentIndex = nextTerminatorIndex + 1;
                            }
                        }
                    }
                }
            }
        }
        return tagAttributes;
    }

    private int countCarriageReturns (String val)
    {
        int count = 0;
        if (val == null) {
            return count;
        }
        return StringUtil.occurs(val,'\n');
    }

    private String parseCloseTagName ()
    {
        String tagName = null;
        if (_currentIndex < _templateStringLength) {
            if ((_templateString.charAt(_currentIndex) == '<') && (_templateString.charAt(_currentIndex + 1) == '/')) {
                _currentIndex += 2;
                int endTagNameIndex = indexOfCharInSet(_templateString, " \t\n\r/>", _currentIndex);
                tagName = _templateString.substring(_currentIndex, endTagNameIndex);
                _currentIndex = endTagNameIndex;
                if (_templateString.charAt(_currentIndex) != '>') {
                    int oldIndex = _currentIndex;
                    _currentIndex = _templateString.indexOf('>', _currentIndex);
                    _currentLine += countCarriageReturns(_templateString.substring(oldIndex,_currentIndex));
                }
                _currentIndex++;
            }
        }
        return tagName;
    }

    private String parseCommentBody ()
    {
        String commentBody = null;
        if (_currentIndex < _templateStringLength) {
            int closingCommentIndex = _templateString.indexOf("-->", _currentIndex);
            if (closingCommentIndex == -1) {
                throw new AWGenericException("Error: missing closing comment tag. Expected \"-->\" templateName: \"" +
                                             _templateName + "\" line:" + _currentLine);
            }
            else {
                commentBody = _templateString.substring(_currentIndex, closingCommentIndex);
                _currentLine += countCarriageReturns(commentBody);
                _currentIndex = closingCommentIndex + "-->".length();
            }
        }
        return commentBody;
    }

    private String matchBody (String tagName)
    {
        String bodyString = null;
        String closeTag = Fmt.S("</%s>", tagName);
        if (_currentIndex < _templateStringLength) {
            int closeTagIndex = _templateString.indexOf(closeTag, _currentIndex);
            if (closeTagIndex == -1) {
                throw new AWGenericException("Error: missing closing element tag. Expected \"" + closeTag + "\" templateName: \"" +
                                             _templateName + "\" line:" + _currentLine);
            }
            bodyString = _templateString.substring(_currentIndex, closeTagIndex);
        }
        return bodyString;
    }

    private void parseLiteralBody (String tagName)
    {
        String bodyString = matchBody(tagName);
        if (bodyString != null) {
            _currentLine += countCarriageReturns(bodyString);
            _currentIndex = _currentIndex + bodyString.length();
        }
        pushString(bodyString);
    }

    private String parseOpenTagBody ()
    {
        String tagBody = null;
        if (_currentIndex < _templateStringLength) {
            int closingAngleIndex = indexOutsideQuotes(_templateString, '>', _currentIndex);
            if (_currentIndex != closingAngleIndex) {
                // note: tagBody will have closing '/' if singleton tag.
                tagBody = _templateString.substring(_currentIndex, closingAngleIndex);
                _currentLine += countCarriageReturns(tagBody);
            }
            _currentIndex = closingAngleIndex + 1;
        }
        return tagBody;
    }

    private int indexOutsideQuotes (String str, int matchChar, int startPos)
    {
        boolean inQuote = false;
        int quoteChar=0;
        for (int i=startPos, len = str.length(); i < len; i++) {
            int ch = str.charAt(i);
            if (inQuote) {
                if (ch == quoteChar) {
                    inQuote = false;
                }
                else if (ch == '\n' || ch == '\r') break;
            } else if (ch == '\"' || ch == '\'') {
                inQuote = true;
                quoteChar = ch;
            }
            else if (ch == matchChar && !inQuote) return i;
        }
        logWarning(Fmt.S("** %s (%s) Hit end-of-line in quote while searching for closing '>' -- check for extra quotes!",
                _templateName, Integer.toString(_currentLine)));
        return str.indexOf(matchChar, startPos);
    }

    private String parseOpenTagName ()
    {
        String tagName = null;
        if (_currentIndex < _templateStringLength) {
            if ((_templateString.charAt(_currentIndex) == '<') && (_templateString.charAt(_currentIndex + 1) != '/')) {
                _currentIndex++;
                int endTagNameIndex = indexOfCharInSet(_templateString, " \t\n\r/>", _currentIndex);
                tagName = _templateString.substring(_currentIndex, endTagNameIndex);
                _currentIndex = endTagNameIndex;
                _lastOpenTagLine = _currentLine;
            }
        }
        return tagName;
    }

    protected int indexOfNextLeftAngle (int startIndex)
    {
        int angleBracketIndex = _templateString.indexOf('<', startIndex);
        if ((angleBracketIndex != -1) && (angleBracketIndex > 0) && (_templateString.charAt(angleBracketIndex - 1) == '\\')) {
            if ((angleBracketIndex > 1) && (_templateString.charAt(angleBracketIndex - 2) != '\\')) {
                angleBracketIndex = indexOfNextLeftAngle(angleBracketIndex + 1);
            }
        }
        return angleBracketIndex;
    }

    protected int dynamicTagMarkerWidth ()
    {
        return 0;
    }

    protected String parseBareString ()
    {
        String bareString = null;
        if (_currentIndex < _templateStringLength) {
            int angleBracketIndex = indexOfNextLeftAngle(_currentIndex);
            if (angleBracketIndex == -1) {
                bareString = _templateString.substring(_currentIndex);
            }
            else if (_currentIndex != angleBracketIndex) {
                bareString = _templateString.substring(_currentIndex, angleBracketIndex);
            }

            // Note: punt on dynamicTagMarkers for now ... line numbers will be
            // incorrect for pages with dynamicTagMarkers
            _currentIndex += dynamicTagMarkerWidth();

            if (bareString != null) {
                _currentLine += countCarriageReturns(bareString);
                _currentIndex += bareString.length();
                bareString = AWUtil.replaceAllOccurrences(bareString, "\\<", "<");
                //bareString = AWUtil.replaceAllOccurrences(bareString, "\\\\", "\\");
            }
        }
        return bareString;
    }

    private boolean isStrictTagNaming ()
    {
        return (_component != null) ? _component.isStrictTagNaming() : false;
    }

    protected Class elementClassForNameAndAttributes (String openTagName, Map tagAttributes, boolean isElementTag)
    {
        Class elementClass = isElementTag ? elementClassForTagName(openTagName) : containerClassForTagName(openTagName);
        if (elementClass == null && isElementTag) elementClass = containerClassForTagName(openTagName);

        if (AWConcreteApplication.IsDebuggingEnabled &&
            elementClass == null &&
            (!StringUtil.nullOrEmptyString(openTagName) && Character.isUpperCase(openTagName.charAt(0))) && isStrictTagNaming()) {

            logWarning("** Warning: Unable to find definition for <"+openTagName+"> in "+ _templateName + ":"+ _currentLine);

            _component.componentDefinition().addUnknownTag(_component.requestContext().validationContext(),
                                                           openTagName, _templateName, _currentLine);

            /* Given that we're registering it above, no need to do it here...
            String errorString =
                "&nbsp;<font color=\"#ff0000\"><b>** <blink>Warning</blink>: unable to find definition for &lt;" +
                openTagName + "&gt; ("+_templateName+":"+_currentLine+")" + " Tag placed directly in output." +
                "</b></font><br/>";
            pushString(errorString);*/
        }

        if ((elementClass == null) && AWBinding.hasDynamicBindings(tagAttributes)) {
            elementClass = isElementTag ? AWGenericElement.class : AWGenericContainer.class;
        }

        return elementClass;
    }

    private void parse ()
    {
        startDocument();
        while (_currentIndex < _templateStringLength) {
            String bareString = parseBareString();
            if (bareString != null) {
                characters(bareString);
            }
            String openTagName = parseOpenTagName();
            if (openTagName != null) {
                if (openTagName.startsWith("!---")) {
                    // Comments with three hyphens will be stripped out of the response.
                    parseCommentBody();
                }
                else if (openTagName.startsWith("!--")) {
                    // reconstitute comment and add to current bare string
                    String commentBody = parseCommentBody();
                    // For XML we leave comments in place, for HTML, we strip them out.
                    // If you're rendering HTML and you really want a comment in the
                    // output, use <a:Comment>.
                    if (_useXmlEscaping) {
                        commentBody = AWUtil.replaceAllOccurrences(commentBody, "$", "\\$");
                        String commentString = StringUtil.strcat("<", openTagName, commentBody, "-->");
                        characters(commentString);
                    }
                }
                else {
                    String openTagBody = parseOpenTagBody();
                    boolean hasOpenTagBody = (openTagBody == null) ? false : true;
                    Map tagAttributes = null;
                    RuntimeException parseTagAttributesException = null;
                    if (hasOpenTagBody) {
                        try {
                            tagAttributes = parseTagAttributes(openTagBody);
                        }
                        catch (RuntimeException genericException) {
                            parseTagAttributesException = genericException;
                            tagAttributes = EmtpyHashTable;
                        }
                    }
                    else {
                        tagAttributes = EmtpyHashTable;
                    }
                    Map bindingsHashtable = bindingsFromAttributeList(tagAttributes);
                    boolean isElementTag = (hasOpenTagBody && openTagBody.endsWith("/")) || isRegisteredElementClassForTagName(openTagName);
                    String resolvedName = openTagName;
                    /* Can't assert this -- doesn't work when generating XML with namespaces (e.g. SOAP)
                    Assert.that((openTagName.indexOf(':') == -1 || _resolver != null),
                            "Encountered namespaced tag but no resolver...",
                            openTagName, _component);
                    */
                    if (_resolver != null) {
                        resolvedName = _resolver.lookup(openTagName);
                        if (resolvedName == null) {
                            // FIXME - should only assert if tag uses known namespace -- failed lookup on
                            // non AW namespace could be (valid) XML reference
                            Assert.that((openTagName.indexOf(':') == -1), "Failed to resolve reference to %s in component %s",
                                    openTagName, _component);
                            if(!(!Character.isUpperCase(openTagName.charAt(0)) || _useXmlEscaping)) {
                                Assert.that(false,
                                    "%s: Namespace-less reference to component '%s' in namespace aware package. Template string: %s", 
                                    _component, openTagName, _templateString);
                            }
                            resolvedName = openTagName;
                        }
                    }
                    Class elementClass = elementClassForNameAndAttributes(resolvedName, bindingsHashtable, isElementTag);
                    if (elementClass == null) {
                        // reconstitute tag and add to current bare string
                        String staticTagString = StringUtil.strcat("<", openTagName, (hasOpenTagBody ? openTagBody : ""), ">");
                        characters(staticTagString);
                    }
                    else {
                        if (parseTagAttributesException != null) {
                            throw parseTagAttributesException;
                        }
                        startElement(openTagName, resolvedName, elementClass, bindingsHashtable);
                        // send end-element message as necessary (if trailing '/')
                        if (isElementTag) {
                            endElement(openTagName);
                        } else {
                            if (_topOfStack instanceof LiteralBody) {
                                parseLiteralBody(openTagName);
                            }
                            else if (_topOfStack instanceof FilterBody)
                            {
                                // filter and insert filtered body
                                String origBody = matchBody(openTagName);
                                String newBody = ((FilterBody)_topOfStack).filterBody(origBody);
                                _templateString = _templateString.substring(0, _currentIndex)
                                        .concat(newBody)
                                        .concat(_templateString.substring(_currentIndex + origBody.length()));
                                _templateStringLength = _templateString.length();
                            }
                        }
                    }
                }
            }
            String closeTagName = parseCloseTagName();
            if (closeTagName != null) {
                Class elementClass = containerClassForTagName(closeTagName);
                if (elementClass == null) {
                    String expectedNameString = (String)ListUtil.lastElement(_nameStack);
                    if (closeTagName.equals(expectedNameString)) {
                        endElement(closeTagName);
                    }
                    else {
                        // reconsitute closing tag and add to bare string
                        String closingTag = "</" + closeTagName + ">";
                        characters(closingTag);
                    }
                }
                else {
                    endElement(closeTagName);
                }
            }
        }
        endDocument();
        Assert.that(_embeddedKeyPathSuppressCount==0, "Unbalanced _embeddedKeyPathSuppressCount (== %s)",
                _embeddedKeyPathSuppressCount);
    }

    private void resetParser ()
    {
        _resultingTemplate = null;
        _templateString = null;
        _templateStringLength = 0;
        _currentIndex = 0;
        _templateName = null;
        _component = null;
        // see comment above about why we call registerStandardTagNames() here
        if (_standardTagsHashtable.size() == 0) {
            registerStandardTagNames();
        }
    }

    public synchronized AWTemplate templateFromString (String templateString, String templateName)
    {
        _templateName = templateName;
        AWTemplate resultingTemplate = null;
        if (_templateString == null) {
            try {
                _useXmlEscaping = (_component == null || _component.useXmlEscaping());
                _templateString = templateString;
                _templateStringLength = _templateString.length();
                _currentIndex = 0;
                parse();
                resultingTemplate = _resultingTemplate;
                resultingTemplate.setTemplateName(templateName);
            }
            finally {
                resetParser();
            }
        }
        else {
            throw new AWGenericException("Error: attempt to re-enter template parser.");
        }
        return resultingTemplate;
    }

    public synchronized AWTemplate templateFromInputStream (InputStream inputStream, String templateName)
    {
        AWTemplate template = null;
        String templateString = null;
        try {
            resetParser();
            templateString = AWUtil.stringWithContentsOfInputStream(inputStream);
        }
        catch (RuntimeException exception) {
            throw new AWGenericException("Error: unable to convert input stream to string. templateName: \"" + _templateName + "\"", exception);
        }
        try {
            template = templateFromString(templateString, templateName);
        }
        finally {
            resetParser();
        }
        return template;
    }

    public void setDefaultResolver (AWNamespaceManager.Resolver resolver)
    {
        _resolver = resolver;
    }

    public static String packageNameForTemplate (String templateName, AWComponent component)
    {
        // try to use the path in the template name as the package, if available
        return (templateName.contains("/"))
                ? templateName.substring(0, templateName.lastIndexOf("/")).replace("/", ".")
                : component.getCurrentPackageName();
    }

    public synchronized AWTemplate templateFromString (String templateString, String templateName, AWComponent component)
    {
        // This gets cleared by reset()
        _component = component;
        AWNamespaceManager.Resolver origResolver = _resolver;
        _resolver = AWNamespaceManager.instance().resolverForPackage(packageNameForTemplate(templateName, component));
        AWTemplate template = templateFromString(templateString, templateName);
        _resolver = origResolver;
        return template;
    }
}
