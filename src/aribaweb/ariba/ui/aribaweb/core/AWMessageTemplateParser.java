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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWMessageTemplateParser.java#16 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.FastStringBuffer;
import java.util.Map;
import ariba.util.core.StringArray;
import ariba.util.core.StringUtil;
import java.util.List;
import java.io.InputStream;
import java.util.Iterator;

public final class AWMessageTemplateParser extends AWBaseObject implements AWTemplateParser
{
    private static final Map EmtpyHashTable = MapUtil.map();
    private static final AWElementContaining DummyContainableElement = new AWConcreteTemplate();
    private static final String WhiteSpaceSet = " \t\r\n";
    private static final String AttributeDemiliterSet = " \t\r\n/";
    private static final String EndOfTagSet = " \t\n\r/>";
    private static final String EndOfArgumentSet = " \t\n\r/}";
    private static final char   LeftAngleBracket = '<';
    private static final char   RightAngleBracket = '>';
    private static final char   BackSlash = '\\';
    private static final char   Slash = '/';
    private static final char   LeftCurlyBrace = '{';
    private static final char   RightCurlyBrace = '}';


    private String _templateName;
    private AWTemplate _resultingTemplate;
    private AWApplication _application;
    private AWElementContaining _topOfStack;
    private FastStringBuffer _currentStringBuffer;
    private List _containerStack;
    private List _nameStack;
    private String _templateString;
    private int _templateStringLength;
    private int _currentIndex = 0;
    private Map _standardTagsHashtable;
    private Map _elementClassesByName;
    private Map _containerClassesByName;
    private AWMessageTemplateParser _nestedTagParser;
    private boolean _insideArgument;


    // ** Thread Safety Considerations: this will lock at all the external entry points (ie all public methods) even though that's a huge scope.  Since in a deployed app, the parser is only run at warmup time, this large scope won't hurt things too much.

    public void init (AWApplication application)
    {
        this.init();
        _application = application;
        _nameStack = ListUtil.list();
        _containerStack = ListUtil.list();
        _standardTagsHashtable = MapUtil.map();
        _elementClassesByName = MapUtil.map();
        _containerClassesByName = MapUtil.map();
        registerStandardTagNames();
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

    private Class classForName (String className)
    {
        Class targetClass = null;
        if (className.length() > 0) {
            boolean isJavaIdentifier = Character.isJavaIdentifierStart(className.charAt(0));
            targetClass = isJavaIdentifier ? _application.resourceManager().classForName(className) : null;
        }
        return targetClass;
    }

    private Class classForTagNameInHashtable (String tagName, Map classHashtable)
    {
        Class elementClass = (Class)classHashtable.get(tagName);
        if (elementClass == null) {
            elementClass = (Class)_standardTagsHashtable.get(tagName.toUpperCase());
            if (elementClass == null) {
                elementClass = classForName(tagName);
            }
        }
        if (elementClass == ObjectClass) {
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
        _standardTagsHashtable.put(tagName.toUpperCase().intern(), ObjectClass);
    }

    public synchronized void duplicateRegistrationsIntoOther (AWTemplateParser other)
    {
        AWHtmlTemplateParser.duplicateRegistrationsIntoOther(_elementClassesByName, _containerClassesByName, other);
    }
        
    private Map bindingsFromAttributeList (Map attributeList)
    {
        boolean isDebuggingEnabled = false;
        String awdebugValue = (String)attributeList.get(AWBindingNames.awdebug);
        if (awdebugValue != null) {
            isDebuggingEnabled = awdebugValue.equals("$true") || awdebugValue.equals("true");
            attributeList.remove(AWBindingNames.awdebug);
        }
        Map bindingsHashtable = MapUtil.map();
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
            else {
                newBinding = AWBinding.bindingWithNameAndConstant(currentAttributeName, currentAttributeValue);
            }
            newBinding.setIsDebuggingEnabled(isDebuggingEnabled);
            bindingsHashtable.put(newBinding.bindingName(), newBinding);
        }
        return bindingsHashtable;
    }

    private void pushContainer (AWElementContaining container)
    {
        _containerStack.add(container);
    }

    //////////////////////
    // Sax-ish stuff
    //////////////////////

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

    void finalizeCurrentString ()
    {
        if (_currentStringBuffer != null) {
            if (_currentStringBuffer.length() > 0) {
                AWBareString bareString = AWBareString.getInstance(_currentStringBuffer.toString());
                addToTopOfStack(bareString);
            }
            _currentStringBuffer = null;
        }
    }

    void pushContainableElementWithName (AWElementContaining containableElement, String elementName)
    {
        finalizeCurrentString();
        _nameStack.add(elementName);
        _containerStack.add(containableElement);
        setTopOfStack(containableElement);
    }

    private void popContainableElementWithName (String elementName)
    {
        finalizeCurrentString();
        int stackCount = _containerStack.size();
        int index = stackCount - 1;
        while (index >= 0) {
            if (index == 0) {
                String errorString = null;
                String errorHtmlString = null;
                if (_insideArgument) {
                    errorString = "{/" + elementName + "}";
                    errorHtmlString = errorString;
                }
                else {
                    errorString = "</" + elementName + ">";
                    errorHtmlString = "&lt;/" + elementName + "&gt;";
                }
                logWarning("** Warning: Unbalanced tags. Found closing tag " + errorString +
                          "templateName: \"" + _templateName + "\"");
                pushString("&nbsp;<font color=\"#ff0000\"><b>** <blink>Warning</blink>: Unbalanced tags. Found closing tag " + errorHtmlString + " templateName: \"" + _templateName + "\" </b></font>");
                break;
            }
            AWElement lastElement = (AWElement)_containerStack.get(index);
            _containerStack.remove(index);
            String lastElementName = (String)_nameStack.get(index);
            _nameStack.remove(index);

            AWElementContaining newTopOfStack = (AWElementContaining)ListUtil.lastElement(_containerStack);
            setTopOfStack(newTopOfStack);
            if (_insideArgument) {
                String errorString = null;
                if (lastElement instanceof AWMessageArgument) {
                    AWMessageArgument messageArgument = (AWMessageArgument)lastElement;
                    String lastArgumentNumber = Integer.toString(messageArgument.argumentNumber());
                    if (lastArgumentNumber.equals(elementName)) {
                        break;
                    }
                    else {
                        errorString = "Expecting \"{/" + lastElementName + "}\" Found \"{/" +
                            elementName + "}\" templateName: \"" + _templateName + "\"";
                    }
                }
                else {
                    errorString =  "Expecting \"</" + lastElementName + ">\" Found \"{/" +
                            elementName + "}\" templateName: \"" + _templateName + "\"";
                }

                logWarning("** Warning: Unbalanced arguments. " + errorString);
                errorString = "&nbsp;<font color=\"#ff0000\"><b>** <blink>Warning</blink>: " +
                    "possibly missing some content due to unbalanced arguments.  " + errorString;
                pushString(errorString);
            }

            else if (elementName.equals(lastElementName)) {
                break;
            }
            else {
                logWarning("** Warning: Unbalanced tags.  Expecting \"</" + lastElementName + ">\" Found \"</" + elementName + ">\" templateName: \"" + _templateName + "\"");
                String errorString = "&nbsp;<font color=\"#ff0000\"><b>** <blink>Warning</blink>: possibly missing some content due to unbalanced tags.  Expecting \"&lt;/" + lastElementName + "&gt;\" ";
                errorString = errorString + "Found \"&lt;/" + elementName + "&gt;\" templateName: \"" + _templateName + "\"</b></font>";
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
        if (!_resultingTemplate.hasElements() && StringUtil.nullOrEmptyString(_templateString)) {
            // This handles case of a completely empty string in strings file for specific key.
            characters(" ");
        }
        finalizeCurrentString();
        flushStacks();
        _currentStringBuffer = null;
    }

    protected void startElement (String elementName, Class elementClass, Map bindingsHashtable)
    {
        AWElement element = null;
        if (AWUtil.classImplementsInterface(elementClass, AWCycleable.class)) {
            try {
                AWCycleableReference cycleableReference = (AWCycleableReference)elementClass.newInstance();
                // todo: need to keep track of line number in here.
                element = cycleableReference.determineInstance(elementName, bindingsHashtable, _templateName, -1);
            }
            catch (IllegalAccessException exception) {
                throw new AWGenericException(exception);
            }
            catch (InstantiationException exception) {
                throw new AWGenericException("Problem creating new instance of \"" + elementName + "\" " + exception + " templateName: \"" + _templateName + "\"", exception);
            }
        }
        else {
            throw new AWGenericException("Error: attempt to use a \"" + elementClass.getName() + "\" as a dynamic element or subcomponent.\nOnly subclasses of \"AWComponent\" or classes which implement the \"AWBindable\" interface may be used as tags within templates.");
        }
        finalizeCurrentString();
        addToTopOfStack(element);
        if (element instanceof AWMessageArgument ) {
            AWMessageArgument messageArgument = (AWMessageArgument)element;
            String argumentNumberString = Integer.toString(messageArgument.argumentNumber());
            pushContainableElementWithName(messageArgument, argumentNumberString);
        }
        else if (element instanceof AWElementContaining) {
            pushContainableElementWithName((AWElementContaining)element, elementName);
        }
        else {
            pushContainableElementWithName(DummyContainableElement, elementName);
        }
    }

    protected void endElement (String elementName)
    {
        popContainableElementWithName(elementName);
        _insideArgument = false;
    }

    protected void characters (String bareString)
    {
        pushString(bareString);
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

    private Map parseTagAttributes (String tagBodyString)
    {
        String equalsSkipSet = "= \t\n\r/";
        Map tagAttributes = MapUtil.map();
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
                    tagAttributes.put(attributeName, "awstandalone");
                    currentIndex = tagBodyLength;
                }
                else {
                    String attributeName = tagBodyString.substring(currentIndex, equalsIndex);
                    currentIndex = skipCharactersInSet(tagBodyString, equalsSkipSet, equalsIndex);
                    if (currentIndex >= tagBodyLength) {
                        tagAttributes.put(attributeName, "awstandalone");
                    }
                    else {
                        String equalsString = tagBodyString.substring(equalsIndex, currentIndex);
                        if (equalsString.indexOf('=') == -1) {
                            tagAttributes.put(attributeName, "awstandalone");
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
                                    tagAttributes.put(attributeName, attributeValueString);
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
                                tagAttributes.put(attributeName, attributeValueString);
                                currentIndex = nextTerminatorIndex + 1;
                            }
                        }
                    }
                }
            }
        }
        return tagAttributes;
    }

    private String parseCloseTagName ()
    {
        char openTagChar = LeftAngleBracket;
        char closingTagChar = RightAngleBracket;
        String endOfTagSet = EndOfTagSet;

        if (_insideArgument) {
            openTagChar = LeftCurlyBrace;
            closingTagChar = RightCurlyBrace;
            endOfTagSet = EndOfArgumentSet;
        }

        String tagName = null;
        if (_currentIndex < _templateStringLength) {
            if ((_templateString.charAt(_currentIndex) == openTagChar) && (_templateString.charAt(_currentIndex + 1) == Slash)) {
                _currentIndex += 2;
                int endTagNameIndex = indexOfCharInSet(_templateString, endOfTagSet, _currentIndex);
                tagName = _templateString.substring(_currentIndex, endTagNameIndex);
                _currentIndex = endTagNameIndex;
                if (_templateString.charAt(_currentIndex) != closingTagChar) {
                    _currentIndex = _templateString.indexOf(closingTagChar, _currentIndex);
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
                throw new AWGenericException("Error: missing closing comment tag. Expected \"-->\" templateName: \"" + _templateName + "\"");
            }
            else {
                commentBody = _templateString.substring(_currentIndex, closingCommentIndex);
                _currentIndex = closingCommentIndex + "-->".length();
            }
        }
        return commentBody;
    }

    private int indexOfClosingTagChar (char openTagChar, char closingTagChar)
    {

        int indexOfClosingAngleBracket = _currentIndex;
        int leftAngleBracketCount = 0;
        while (indexOfClosingAngleBracket < _templateStringLength) {
            char currentChar = _templateString.charAt(indexOfClosingAngleBracket);
            if (currentChar == openTagChar) {
                leftAngleBracketCount++;
            }
            else if (currentChar == closingTagChar && _templateString.charAt(indexOfClosingAngleBracket - 1) != BackSlash) {
                if (leftAngleBracketCount == 0) {
                    return indexOfClosingAngleBracket;
                }
                else if (leftAngleBracketCount > 0) {
                    leftAngleBracketCount--;
                }
            }
            indexOfClosingAngleBracket++;
        }
        return -1;
    }


    private String parseOpenTagBody ()
    {
        char openTagChar = LeftAngleBracket;
        char closingTagChar = RightAngleBracket;

        if (_insideArgument) {
            openTagChar = LeftCurlyBrace;
            closingTagChar = RightCurlyBrace;
        }

        String tagBody = null;
        if (_currentIndex < _templateStringLength) {
            int closingAngleIndex = indexOfClosingTagChar(openTagChar, closingTagChar);
            if (closingAngleIndex == -1) {
                throw new AWGenericException("Malformed Open Tag Body -- missing '" +
                                             closingTagChar + "': " + _templateString.substring(_currentIndex));
            }
            if (_currentIndex != closingAngleIndex) {
                // note: tagBody will have closing '/' if singleton tag.
                tagBody = _templateString.substring(_currentIndex, closingAngleIndex);
            }
            _currentIndex = closingAngleIndex + 1;
        }
        return tagBody;
    }

    private String parseOpenTagName ()
    {
        char openTagChar = LeftAngleBracket;
        String endOfTagSet = EndOfTagSet;

        if (_insideArgument) {
            openTagChar = LeftCurlyBrace;
            endOfTagSet = EndOfArgumentSet;
        }

        String tagName = null;
        if (_currentIndex < _templateStringLength) {
            if ((_templateString.charAt(_currentIndex) == openTagChar) && (_templateString.charAt(_currentIndex + 1) != Slash)) {
                _currentIndex++;
                int endTagNameIndex = indexOfCharInSet(_templateString, endOfTagSet, _currentIndex);
                tagName = _templateString.substring(_currentIndex, endTagNameIndex);
                _currentIndex = endTagNameIndex;
            }
        }
        return tagName;
    }

    private int indexOfNextOpenTagChar (int startIndex, char openTagChar)
    {
        int angleBracketIndex = _templateString.indexOf(openTagChar, startIndex);
        if ((angleBracketIndex != -1) && (angleBracketIndex > 0) && (_templateString.charAt(angleBracketIndex - 1) == BackSlash)) {
            if ((angleBracketIndex > 1) && (_templateString.charAt(angleBracketIndex - 2) != BackSlash)) {
                angleBracketIndex = indexOfNextOpenTagChar (angleBracketIndex + 1, openTagChar);
            }
        }
        return angleBracketIndex;
    }

    private String parseBareString ()
    {
        String bareString = null;
        if (_currentIndex < _templateStringLength) {
            int angleBracketIndex = indexOfNextOpenTagChar(_currentIndex, LeftAngleBracket);
            int openBraceIndex = indexOfNextOpenTagChar(_currentIndex, LeftCurlyBrace);

            if (angleBracketIndex == -1 && openBraceIndex == -1) {
                _insideArgument = false;
                bareString = _templateString.substring(_currentIndex);
            }
            else if (angleBracketIndex != -1 &&
                     openBraceIndex != -1) {
                if (angleBracketIndex < openBraceIndex) {
                    _insideArgument = false;
                    if (angleBracketIndex != _currentIndex) {
                        bareString = _templateString.substring(
                            _currentIndex, angleBracketIndex);
                    }
                }
                else {
                    _insideArgument = true;
                    if (openBraceIndex != _currentIndex) {
                        bareString = _templateString.substring(
                            _currentIndex, openBraceIndex);
                    }
                }
            }
            else if (angleBracketIndex != -1) {
                _insideArgument = false;
                if (_currentIndex != angleBracketIndex) {
                    bareString = _templateString.substring(
                        _currentIndex, angleBracketIndex);
                }
            }
            else if (openBraceIndex != -1) {
                _insideArgument = true;
                if (_currentIndex != openBraceIndex) {
                    bareString = _templateString.substring(
                        _currentIndex, openBraceIndex);
                }
            }

            if (bareString != null) {
                _currentIndex += bareString.length();
                bareString = AWUtil.replaceAllOccurrences(bareString, "\\<", "<");
                bareString = AWUtil.replaceAllOccurrences(bareString, "\\{", "{");
            }
        }
        return bareString;
    }

    private Class elementClassForNameAndAttributes (String openTagName, Map tagAttributes, boolean isElementTag)
    {
        Class elementClass = isElementTag ? elementClassForTagName(openTagName) : containerClassForTagName(openTagName);
        if (elementClass == null && isElementTag) elementClass = containerClassForTagName(openTagName);
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
            String openTagStr = "<";
            String closingTagStr = ">";

            if (_insideArgument) {
                openTagStr = "{";
                closingTagStr = "}";
            }
            if (openTagName != null) {
                if (openTagName.startsWith("!---")) {
                        // Comments with three hyphens will be stripped out of the response.
                    parseCommentBody();
                }
                else if (openTagName.startsWith("!--")) {
                        // reconstitute comment and add to current bare string
                    String commentBody = parseCommentBody();
                    String commentString = StringUtil.strcat(openTagStr, openTagName, commentBody, "--", closingTagStr);
                    characters(commentString);
                }
                else {
                    String openTagBody = null;
                    openTagBody = parseOpenTagBody();
                    boolean hasOpenTagBody = (openTagBody == null) ? false : true;
                    Map tagAttributes = null;
                    AWGenericException parseTagAttributesException = null;
                    String argumentNumber = openTagName;
                    if (_insideArgument) {
                        tagAttributes = MapUtil.map();
                        tagAttributes.put(AWBindingNames.key, openTagName);
                        openTagName = "AWMessageArgument";
                    }
                    else if (hasOpenTagBody) {
                        try {
                            tagAttributes = parseTagAttributes(openTagBody);
                        }
                        catch (AWGenericException genericException) {
                            parseTagAttributesException = genericException;
                            tagAttributes = EmtpyHashTable;
                        }
                    }
                    else {
                        tagAttributes = EmtpyHashTable;
                    }
                    Map bindingsHashtable = bindingsFromAttributeList(tagAttributes);
                    boolean isElementTag = (hasOpenTagBody && openTagBody.endsWith("/")) ||
                        isRegisteredElementClassForTagName(openTagName);
                    Class elementClass = elementClassForNameAndAttributes(openTagName, bindingsHashtable, isElementTag);
                    if (elementClass == null) {
                        if (hasOpenTagBody &&
                            (openTagBody.indexOf(LeftAngleBracket) != -1 || openTagBody.indexOf(LeftCurlyBrace) != -1)) {
                            if (_nestedTagParser == null) {
                                _nestedTagParser = new AWMessageTemplateParser();
                                duplicateRegistrationsIntoOther(_nestedTagParser);
                                _nestedTagParser.init(_application);
                            }
                            AWTemplate template = _nestedTagParser.templateFromString(openTagBody, "AW_NESTED_TAG");
                            characters(openTagStr);
                            characters(openTagName);
                            finalizeCurrentString();
                            addToTopOfStack(template);
                            characters(closingTagStr);
                        }
                        else {
                                // reconstitute tag and add to current bare string
                            String staticTagString = StringUtil.strcat(openTagStr, openTagName, (hasOpenTagBody ? openTagBody : ""), closingTagStr);
                            characters(staticTagString);
                        }
                    }
                    else {
                        if (parseTagAttributesException != null) {
                            throw parseTagAttributesException;
                        }
                        startElement(openTagName, elementClass, bindingsHashtable);
                            // send end-element message as necessary (if trailing '/')
                        if (isElementTag) {
                            endElement(argumentNumber);
                        }
                    }
                }
            }

            String closeTagName = parseCloseTagName();
            String closeTagString = null;
            String argumentNumber = closeTagName;

            if (closeTagName != null) {
                if (_insideArgument) {
                    argumentNumber = closeTagName;
                    closeTagString = "{/" + closeTagString + "}";
                    closeTagName = "AWMessageArgument";
                }
                else {
                    closeTagString = "</" + closeTagName + ">";
                }

                Class elementClass = containerClassForTagName(closeTagName);
                if (elementClass == null) {
                    String expectedNameString = (String)ListUtil.lastElement(_nameStack);
                    if (closeTagName.equals(expectedNameString)) {
                        endElement(argumentNumber);
                    }
                    else {
                            // reconsitute closing tag and add to bare string
                        characters(closeTagString);
                    }
                }
                else {
                    endElement(argumentNumber);
                }
            }
        }
        endDocument();
    }

    private void resetParser ()
    {
        _resultingTemplate = null;
        _templateString = null;
        _templateStringLength = 0;
        _currentIndex = 0;
        _templateName = null;
    }

    public synchronized AWTemplate templateFromString (String templateString, String templateName)
    {
        _templateName = templateName;
        AWTemplate resultingTemplate = null;
        if (_templateString == null) {
            try {
                _templateString = templateString;
                _templateStringLength = _templateString.length();
                _currentIndex = 0;
                parse();
                resultingTemplate = _resultingTemplate;
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
}
