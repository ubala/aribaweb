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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWResponse.java#11 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWCharacterEncoding;
import ariba.ui.aribaweb.util.AWContentType;
import ariba.ui.aribaweb.util.AWEncodedString;
import java.util.Map;

public interface AWResponse extends AWResponseGenerating
{
    public class StatusCodes extends Object {
        public static final int NoContent     = 204;
        public static final int RedirectMoved = 301;
        public static final int RedirectFound = 302;
        public static final int ErrorNotFound = 404;
    }
    public void init (AWCharacterEncoding characterEncoding);
    public void init ();

    public void appendContent (AWEncodedString encodedString);
    public void appendContent (String contentString);
    public void appendContent (char contentChar);
    public void setContent (byte[] bytes);
    public void setContentFromFile (String filePath);
    public String contentString ();
    public byte[] content ();

    public void setHeaderForKey (String headerValue, String headerKey);
    public void setHeadersForKey (String[] headerValues, String headerKey);
    public void setContentType (AWContentType contentType);
    public AWContentType contentType ();
    public void setStatus (int statusCode);
    public void setBrowserCachingEnabled (boolean flag);
    public boolean browserCachingEnabled ();
    public void setCharacterEncoding (AWCharacterEncoding characterEncoding);
    public AWCharacterEncoding characterEncoding ();
    public void disableClientCaching ();

    public AWCookie createCookie (String cookieName, String cookieValue);
    public void addCookie (AWCookie cookie);

        // record & playback
    public Map elementIdToSemanticKeyTable ();
    public Map semanticKeyToElementIdTable ();
    public void _debugSetSemanticKeyMapping (byte[] bytes);
    public byte[] _debugGetSemanticKeyMapping ();
    public Map _debugHeaders ();
    public void _debugSetRecordPlaybackParameters (AWRecordingManager recordingMgr,
                                                   boolean appendSemanticKeys);
}
