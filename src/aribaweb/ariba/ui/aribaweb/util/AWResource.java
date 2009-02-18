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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWResource.java#8 $
*/

package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.util.core.StringUtil;
import java.io.InputStream;

public abstract class AWResource extends AWBaseObject
{
    protected String _resourceName;
    protected String _relativePath;
    protected Object _object;
    protected long _objectLastModified;
    protected int _hashCode;

    protected AWResource (String resourceName, String relativePath)
    {
        _resourceName = resourceName;
        _relativePath = relativePath;
        _hashCode = resourceName.hashCode();
    }

    public String name ()
    {
        return _resourceName;
    }

    public String relativePath ()
    {
        return _relativePath;
    }

    public int hashCode ()
    {
        return _hashCode;
    }

    /**
        returns the url.  This might not be the absolute and complete url
        that can be used to instantiate an java.net.URL object.  In the case of
        local resource, this returns something like "/w/logo.gif".
    */
    public abstract String url ();

    /**
        Returns the full, absolute URL of this resource as a URL object.
    */
    public abstract String fullUrl ();

    /**
        returns the object this resource represents.
    */
    public Object object ()
    {
        return _object;
    }

    /**
        sets the object this resource represents.
    */
    public void setObject (Object object)
    {
        _object = object;
        _objectLastModified = lastModified();
    }

    public abstract long lastModified ();

    /**
        returns the input stream for this resource.
    */
    public abstract InputStream inputStream ();

    /**
        checks whether the resource is modified since the last
        setObject() call.
    */
    public boolean hasChanged ()
    {
        return (lastModified() > _objectLastModified) || _object == null;
    }

    public boolean equals (Object object)
    {
        boolean equals = (this == object);
        if (!equals) {
            if (object instanceof AWResource) {
                equals = _resourceName.equals(((AWResource)object)._resourceName);
            }
        }
        return equals;
    }

    public String toString ()
    {
        return StringUtil.strcat(getClass().getName(),
                            " resourceName: ", _resourceName,
                            " url: ", url());
    }

    /**
        Try to find another resource at location relative to this one
     */
    public AWResource relativeResource (String relativePath, AWResourceManager resourceManager)
    {
        return null;
    }

    public boolean canCacheUrl ()
    {
        return ((AWConcreteApplication)AWConcreteApplication.sharedInstance()).canCacheResourceUrls();
    }
}
