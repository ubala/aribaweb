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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/meta/GenericMeta.java#2 $
*/

package ariba.ui.wizard.meta;

import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.StringUtil;
import ariba.util.core.URLUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.WrapperRuntimeException;
import ariba.ui.widgets.XMLUtil;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
    GenericMeta is the common superclass for all wizard meta classes within
    this package.  It provides the basic fields (name, label) and a handful
    of useful utility methods for opening and parsing XML documents.

    @aribaapi private
*/
abstract public class GenericMeta
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

        // generic wizard XML attributes
    protected static final String NameAttr  = "name";
    protected static final String LabelAttr = "label";

        // attribute error messages
    protected static final String NoAttrMsg = "missing '%s' attribute of '%s' in '%s'";


    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/

        // internal and display names
    protected String _name;
    protected String _label;

        // cache the current resource we're parsing
    protected AWResource _resource;


    /*-----------------------------------------------------------------------
        Constructors
      -----------------------------------------------------------------------*/

    public GenericMeta ()
    {
    }

    public GenericMeta (String name, String label)
    {
        _name  = name;
        _label = label;
    }


    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/

    public String name ()
    {
        return _name;
    }

    public String label ()
    {
        return _label;
    }


    /*-----------------------------------------------------------------------
        Utility Methods
      -----------------------------------------------------------------------*/

    protected Document createDocumentFromResource (AWResource resource,
                                                   AWResourceManager resourceManager)
    {
            // cache the current resource
        _resource = resource;
        _resource.setObject(this);
        WizardEntityResolver resolver = new WizardEntityResolver(resourceManager);

            // try to open and parse the XML document
        return XMLUtil.document(urlForResource(resource), true, true, resolver);
    }

    protected static String stringAttrFromElement (Element element, String name)
    {
        return stringAttrFromElement(element, name, null);
    }

    protected static String stringAttrFromElement (
        Element element, String name, String defaultValue)
    {
        String str = XMLUtil.stringAttribute(element, name, null);
        return StringUtil.nullOrEmptyOrBlankString(str) ? defaultValue : str;
    }

    protected static Boolean booleanAttrFromElement (
        Element element, String name, Boolean defaultValue)
    {
        String str = XMLUtil.stringAttribute(element, name, null);
        return StringUtil.nullOrEmptyOrBlankString(str) ? defaultValue : Constants.getBoolean(str);
    }

    protected static boolean booleanAttrFromElement (
        Element element, String name, boolean defaultValue)
    {
        String str = XMLUtil.stringAttribute(element, name, null);
        if (StringUtil.nullOrEmptyOrBlankString(str)) {
            return defaultValue;
        }
        else {
            return Constants.getBoolean(str).booleanValue();
        }
    }

    protected static String elementName (Element element)
    {
        return element.getTagName().toLowerCase();
    }

    protected static int indexOfMetaNamed (String name, List metas)
    {
        if (ListUtil.nullOrEmptyList(metas) || StringUtil.nullOrEmptyOrBlankString(name)) {
            return -1;
        }

        for (int index = 0, count = metas.size(); index < count; index++) {
            GenericMeta meta = (GenericMeta)metas.get(index);
            if (name.equals(meta.name())) {
                return index;
            }
        }

        return -1;
    }

    private String assertName ()
    {
        return _resource == null ? _name : _resource.fullUrl();
    }

    protected void assertion (boolean b, String msg)
    {
        Assert.that(b, msg, assertName());
    }

    protected void assertion (boolean b, String msg, Object arg1)
    {
        Assert.that(b, msg, arg1, assertName());
    }

    protected void assertion (boolean b, String msg, Object arg1, Object arg2)
    {
        Assert.that(b, msg, arg1, arg2, assertName());
    }

    private URL urlForResource (AWResource resource)
    {
        String fullUrl = resource.fullUrl();
        try {
            return URLUtil.makeURL(fullUrl);
        }
        catch (MalformedURLException e) {
            throw new WrapperRuntimeException(e);
        }
    }
}
