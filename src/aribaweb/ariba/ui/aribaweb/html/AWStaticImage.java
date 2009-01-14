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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWStaticImage.java#10 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBareString;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingDictionary;
import ariba.ui.aribaweb.core.AWBindableElement;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWDirectAction;
import ariba.ui.aribaweb.core.AWDirectActionUrl;
import ariba.ui.aribaweb.core.AWElement;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.util.AWFastStringBuffer;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWImageInfo;
import ariba.ui.aribaweb.util.AWMultiLocaleResourceManager;
import ariba.ui.aribaweb.util.AWUtil;
import java.util.Map;
import ariba.util.core.StringUtil;
import java.lang.reflect.Field;

public final class AWStaticImage extends AWBindableElement
{
    private String imageUrl (AWImageInfo imageInfo, String filename)
    {
        String imageUrl = null;
        if (imageInfo == null) {
            imageUrl = AWUtil.formatErrorUrl(filename);
        }
        else if (AWConcreteApplication.IsDirectConnectEnabled) {
            imageUrl = AWDirectActionUrl.urlForDirectAction(AWDirectAction.AWImgActionName, null, "name", filename);
        }
        else {
            // use computeString here to avoid caching the resulting string within the encodedString
            imageUrl = imageInfo.url().string();
        }
        return imageUrl;
    }

    public AWElement determineInstance (String elementName, Map bindingsHashtable, String templateName, int lineNumber)
    {
        if (!AWBindingNames.UseNamePrefixBinding) {
            bindingsHashtable.remove(AWBindingNames.namePrefix);
        }
        String imgString = computeImgString(bindingsHashtable);
        AWBareString bareString = AWBareString.getInstance(imgString);
        bareString.setTemplateName(templateName);
        bareString.setLineNumber(lineNumber);
        return bareString;
    }

    private AWEncodedString widthString (Map bindingsHashtable, AWImageInfo imageInfo)
    {
        AWEncodedString widthString = null;
        AWBinding widthBinding = (AWBinding)bindingsHashtable.remove(BindingNames.width);
        if (widthBinding != null) {
            if (!widthBinding.isConstantValue()) {
                throw new AWGenericException(getClass().getName() + ": \"width\" binding must be constant.");
            }
            widthString = widthBinding.encodedStringValue(null);
        }
        else if (imageInfo != null) {
            widthString = imageInfo.widthString;
        }
        return widthString;
    }

    private AWEncodedString heightString (Map bindingsHashtable, AWImageInfo imageInfo)
    {
        AWEncodedString heightString = null;
        AWBinding heightBinding = (AWBinding)bindingsHashtable.remove(BindingNames.height);
        if (heightBinding != null) {
            if (!heightBinding.isConstantValue()) {
                throw new AWGenericException(getClass().getName() + ": \"height\" binding must be constant.");
            }
            heightString = heightBinding.encodedStringValue(null);
        }
        else if (imageInfo != null) {
            heightString = imageInfo.heightString;
        }
        return heightString;
    }

    private String computeImgString (Map bindingsHashtable)
    {
        AWBinding filenameBinding = (AWBinding)bindingsHashtable.remove(BindingNames.filename);
        if (!filenameBinding.isConstantValue()) {
            throw new AWGenericException(getClass().getName() + ": \"filename\" binding must be constant.");
        }
        String filename = filenameBinding.stringValue(null);
        AWMultiLocaleResourceManager resourceManager = AWConcreteApplication.SharedInstance.resourceManager();
        AWImageInfo imageInfo = resourceManager.imageInfoForName(filename);
        String imageUrl = imageUrl(imageInfo, filename);
        AWEncodedString width = widthString(bindingsHashtable, imageInfo);
        AWEncodedString height = heightString(bindingsHashtable, imageInfo);
        String widthString = width.string();
        String heightString = height.string();
        AWFastStringBuffer fastStringBuffer = new AWFastStringBuffer();
        fastStringBuffer.append("<img src=\"");
        fastStringBuffer.append(imageUrl);
        fastStringBuffer.append("\"");
        if (widthString != null) {
            fastStringBuffer.append(" width=\"");
            fastStringBuffer.append(widthString);
            fastStringBuffer.append("\"");
        }
        if (heightString != null) {
            fastStringBuffer.append(" height=\"");
            fastStringBuffer.append(heightString);
            fastStringBuffer.append("\"");
        }
        AWBinding borderBinding = (AWBinding)bindingsHashtable.remove(BindingNames.border);
        String borderString = (borderBinding != null) ? borderBinding.stringValue(null) : "0";
        if (borderString != null) {
            fastStringBuffer.append(" border=\"");
            fastStringBuffer.append(borderString);
            fastStringBuffer.append("\"");
        }
        // no alt binding, so specifying empty string for now
        fastStringBuffer.append(" alt=\"\"");
        AWBindingDictionary bindingsDictionary = AWBinding.bindingsDictionary(bindingsHashtable);
        appendHtmlAttributes(fastStringBuffer, bindingsDictionary);
        fastStringBuffer.append(">");
        return fastStringBuffer.toString();
    }

    private void appendHtmlAttributes (AWFastStringBuffer fastStringBuffer, AWBindingDictionary bindingsDictionary)
    {
        for (int index = bindingsDictionary.size() - 1; index >= 0; index--) {
            String bindingName = bindingsDictionary.keyAt(index);
            // bindingNames are uniqued, so its okay to use '!=' to compare these.
            if (bindingName != AWBindingNames.semanticKeyBindingName()) {
                AWBinding binding = bindingsDictionary.elementAt(index);
                String stringValue = binding.stringValue(null);
                if (stringValue != null) {
                    fastStringBuffer.append(" ");
                    fastStringBuffer.append(bindingName);
                    if ((stringValue != BindingNames.awstandalone) && !stringValue.equals(BindingNames.awstandalone)) {
                        if (AWUtil.contains(stringValue, '"')) {
                            stringValue = StringUtil.replaceCharByString(stringValue, '"', AWUtil.QuoteString);
                        }
                        fastStringBuffer.append("=\"");
                        fastStringBuffer.append(stringValue);
                        fastStringBuffer.append("\"");
                    }
                }
            }
        }
    }

    protected Object getFieldValue (Field field)
      throws IllegalArgumentException, IllegalAccessException
    {
        try {
            return field.get(this);
        }
        catch (IllegalAccessException ex) {
            return super.getFieldValue(field);
        }
    }
}
