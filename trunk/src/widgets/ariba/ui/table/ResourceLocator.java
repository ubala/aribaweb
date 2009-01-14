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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/ResourceLocator.java#6 $
*/
package ariba.ui.table;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWResource;

import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;

import ariba.util.core.Assert;

public final class ResourceLocator
{
    public interface Provider
    {
        public File fileForRelativePath (String path, AWComponent parentComponent);
    }

    static Provider _Provider;
    public static void setProvider (Provider provider)
    {
        _Provider = provider;
    }

    public static URL urlForRelativePath (String path, AWComponent parentComponent)
    {
        AWResource baseResource = parentComponent.templateResource();
        AWResource resource = baseResource.relativeResource(path, AWComponent.templateResourceManager());
        try {
            return (resource != null) ? new URL(resource.fullUrl()) : null;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static File fileForRelativePath (String path, AWComponent parentComponent)
    {
        File f = null;

        if (_Provider != null) {
            f = _Provider.fileForRelativePath(path, parentComponent);
        }

        if (f == null) {
            f = new File(path);
        }
        Assert.that(f.exists(), "Path for resource not found: %s (parent: %s, provider: %s)",
                        path, parentComponent.toString(), _Provider);
        return f;
    }
}
