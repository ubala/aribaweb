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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWRequest.java#21 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWCharacterEncoding;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWFileData;
import java.util.Map;
import java.util.List;
import java.io.InputStream;
import java.util.Locale;
import javax.servlet.http.HttpSession;

public interface AWRequest
{
    public static final String HeaderContentDispositionForMacintosh  = "ONTENT-DISPOSITION";

    public String method ();
    public String headerForKey (String requestHeaderKey);
    public byte[] content ();
    public Map formValues ();
    public AWFileData fileDataForKey (String formValueKey);
    public String formValueForKey (String formValueKey, boolean ignoresCase);
    public String formValueForKey (String formValueKey);
    public String formValueForKey (AWEncodedString formValueKey);
    public String[] formValuesForKey (String formValueKey, boolean ignoresCase);
    public String[] formValuesForKey (String formValueKey);
    public String[] formValuesForKey (AWEncodedString formValueKey);
    public boolean hasFormValueForKey (String formValueKey, boolean ignoresCase);
    public boolean hasFormValueForKey (String formValueKey);
    public boolean hasFormValueForKey (AWEncodedString formValueKey);
    public String cookieValueForKey (String cookieName);
    public String[] cookieValuesForKey (String cookieName);
    public Map headers ();

    public String requestHandlerKey ();
    public boolean hasHandler ();
    public String[] requestHandlerPath ();
    public List requestLocales ();
    public Locale preferredLocale ();
    public void setCharacterEncoding (AWCharacterEncoding characterEncoding);
    public AWCharacterEncoding characterEncoding ();
    public String applicationNumber ();
    public String userAgent ();
    public boolean isBrowserFirefox ();
    public boolean isBrowserMicrosoft ();
    public boolean isBrowserIE55 ();
    public boolean isBrowserSafari();
    public boolean isBrowserChrome();
    public boolean isMacintosh ();
    public boolean isIPad ();
    public boolean isSecureScheme ();
    public boolean isTooManyTabRequest ();
    public String uri ();
    public String queryString ();
    public String requestId ();
    public AWEncodedString responseId ();
    /** @deprecated
     */
    public AWEncodedString senderId ();
    public String sessionId ();
    public AWEncodedString frameName ();
    public void resetRequestId ();

    public String serverPort ();
    public String remoteHost ();
    public String remoteHostAddress ();
    public String contentType ();
    public int contentLength ();
    public InputStream inputStream ();

    //////////////////////
    // HttpServletRequest
    //////////////////////
    public HttpSession getSession (boolean shouldCreate);
    public HttpSession getSession ();

    public String requestString ();
}

