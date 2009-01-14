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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWHttpResource.java#9 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.HTTP;
//import ariba.util.net.https.AribaHttpsURLConnection;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * If this class is going to be used (ie, undeprecated) then
 * we should be using AribaHttpsURLConnection here to take advantage
 * of the connection pooling capabilities of AribaHttpsURLConnection
 *
 * If used, also need to change AWHttpResourceDirectory.
 *
 * Taken out for AN since they need AW to be compatible
 * with older branch of util.
 *
 * @deprecated
 */
final class AWHttpResource extends AWResource
{
    private URL                        _url;
    private long                       _lastModified;
    private InputStream                _inputStream;

    /**
        the connection passed in there will be used by the AWHttpResourceDirectory
        later.  So don't count on it being useful at later time.
        get any useful information out in the constructor
    */
    public AWHttpResource (
        String                     resourceName,
        String                     relativePath,
        AWHttpResourceDirectory    directory,
        HttpURLConnection          conn)
    {
        super(resourceName, relativePath);
        _url = conn.getURL();
        try {
            _inputStream = conn.getInputStream();
        }
        catch (IOException ex) {
            _inputStream = null;
        }

        _lastModified = conn.getLastModified();
    }

    public String url ()
    {
        return _url.toExternalForm();
    }

    public String fullUrl ()
    {
        return url();
    }

    /**
        for now, we don't get back to the server to check the modified
        timestamp.  We will change the implementation realtime check is needed
    */
    public long lastModified ()
    {
        return _lastModified;
    }

    /**
        returns the input stream for this resource.  It always returns an
        brand new input stream that has been read (so no loss of data due to
        multiple read).
    */
    public InputStream inputStream ()
    {
        InputStream ret = null;
        if (_inputStream == null) {
            ret = _inputStream;
            _inputStream = null;
        }
        else {
// Todo: if this class is going to be used (ie, undeprecated) then
// we should be using AribaHttpsURLConnection here to take advantage
// of the connection pooling capabilities of AribaHttpsURLConnection
// If used, also need to change AWHttpResourceDirectory.  Taken out for AN
// since they need AW to be compatible with older branch of util.
//            AribaHttpsURLConnection conn =
//                new AribaHttpsURLConnection(_url);
//            try {
//                conn = (HttpURLConnection)_url.openConnection();
//                conn.setRequestMethod("GET");
//                if (conn.getResponseCode() == HTTP.CodeOK) {
//                    ret = conn.getInputStream();
//                }
//            }
//            catch (IOException ex) {
//                return null;
//            }
//            finally {
//                conn.disconnect();
//            }
            throw new AWGenericException("AWHttpResource not supported.  Please contact AW team if you receive this message.");
        }
        return ret;
    }
}
