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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWHttpResourceDirectory.java#11 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.HTTP;
import ariba.util.core.StringUtil;
import ariba.util.core.URLUtil;
//import ariba.util.net.https.AribaHttpsURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;

/**
 * If this class is going to be used (ie, undeprecated) then
 * we should be using AribaHttpsURLConnection here to take advantage
 * of the connection pooling capabilities of AribaHttpsURLConnection
 *
 * If used, also need to change AWHttpResource.
 *
 * Taken out for AN since they need AW to be compatible
 * with older branch of util.
 *
 * @deprecated
 */
final class AWHttpResourceDirectory extends AWResourceDirectory
{
    private URL _dirUrl;

    protected AWHttpResourceDirectory (String directoryUrl)
      throws MalformedURLException
    {
        _dirUrl = URLUtil.makeURL(removeTrailingSlashes(directoryUrl));
    }


    public String directoryPath ()
    {
        return _dirUrl.getFile();
    }

    public String urlPrefix ()
    {
        return _dirUrl.toExternalForm();
    }

    protected AWResource locateResourceWithRelativePath (String resourceName,
                                                         String relativePath)
    {
        AWResource resource = null;
        URL url = null;
        if (!StringUtil.nullOrEmptyOrBlankString(resourceName)) {
            String path = _dirUrl.getFile();
            if (relativePath != null) {
                path = StringUtil.strcat(path, "/", relativePath);
            }

// Todo: if this class is going to be used (ie, undeprecated) then
// we should be using AribaHttpsURLConnection here to take advantage
// of the connection pooling capabilities of AribaHttpsURLConnection
// If used, also need to change AWHttpResource.  Taken out for AN
// since they need AW to be compatible with older branch of util.
//            AribaHttpsURLConnection conn = null;
//            try {
//                url = URLUtil.makeURL(_dirUrl.getProtocol(), _dirUrl.getHost(),
//                                      _dirUrl.getPort(), path);
//                conn = new AribaHttpsURLConnection();
//                conn.setRequestMethod("GET");
//                if (conn.getResponseCode() == HTTP.CodeOK) {
//                    resource = new AWHttpResource(resourceName, relativePath, this, conn);
//                }
//            }
//            catch (java.io.IOException ex) {
//                // swallow ?
//            }
//            finally {
//                if (conn != null) {
//                    conn.disconnect();
//                }
//            }
            throw new AWGenericException("AWHttpResourceDirectory not supported.  Please contact AW team if you receive this message.");
        }
        logResourceLookup(url == null ? "" : url.toString(), resource != null);
        return resource;
    }
}
