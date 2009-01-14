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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWFileResourceDirectory.java#9 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.StringUtil;
import ariba.util.core.Assert;
import java.io.File;

public final class AWFileResourceDirectory extends AWResourceDirectory
{
    private String _path;
    private String _urlPrefix;

    protected AWFileResourceDirectory (
        String directoryPath,
        String urlPrefix)
    {
        _path = removeTrailingSlashes(directoryPath);
        if (StringUtil.nullOrEmptyOrBlankString(_path)) {
            Assert.that(StringUtil.nullOrEmptyOrBlankString(_path), "Registered blank path!");
        }
        _urlPrefix = removeTrailingSlashes(urlPrefix);
    }

    public String urlPrefix ()
    {
        return _urlPrefix;
    }

    public String directoryPath ()
    {
        return _path;
    }

    public AWResource createResource(String resourceName, String relativePath)
    {
        return new AWFileResource(resourceName, relativePath, this);
    }

    protected AWResource locateResourceWithRelativePath (String resourceName, String relativePath)
    {
        AWResource resource = null;
        if (relativePath != null) {
            File resourceFile = new File(_path, relativePath);
            if (resourceFile.exists()) {
                if (!StringUtil.nullOrEmptyOrBlankString(resourceName)) {
                    resource = new AWFileResource(resourceName, relativePath, this);
                }
            }
            logResourceLookup(resourceFile.getPath(), resource != null);
        }
        else {
            logResourceLookup(StringUtil.strcat(_path, "/", resourceName), false);
        }
        return resource;
    }

    public String[] filesWithExtension (String relativePath, String fileExtension)
    {
        String path = (StringUtil.nullOrEmptyString(relativePath)) ? _path : StringUtil.strcat(_path, "/", relativePath);
        return AWUtil.filesWithExtension(path, fileExtension);
    }
}
