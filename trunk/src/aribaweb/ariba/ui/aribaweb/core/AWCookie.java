/*
    Copyright 1996-2012 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWCookie.java#11 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.StringUtil;

import javax.servlet.http.Cookie;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

public final class AWCookie extends AWBaseObject
{
    private static FastStringBuffer SharedStringBuffer = new FastStringBuffer();
    private static final Object SharedStringBufferLock = new Object();
    private static final SimpleDateFormat ExpirationDateFormat;

    private final String _headerPrefix;
    private boolean _shouldDelete = false;

    private String _name;
    private String _value;
    private String _domain;
    private String _path;
    private boolean _isSecure;
    private int _expiresInSeconds;

    static {
        // This from dejanews searching for "format http cookie expiration java"
        ExpirationDateFormat = new SimpleDateFormat("E, dd-MM-yyyy HH:mm:ss z");
        ExpirationDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Default HttpOnly to true
     * @param name
     * @param value
     * @param domain
     * @param path
     * @param isSecure
     * @param expiresInSeconds
     */
    public AWCookie (String name, String value, String domain, String path,
                     boolean isSecure, int expiresInSeconds)
    {
        _headerPrefix = computeHeaderPrefix(name, value, domain, path, isSecure, true);
        _name = name;
        _value = value;
        _domain = domain;
        _path = path;
        _isSecure = isSecure;
        _expiresInSeconds = expiresInSeconds;
    }

    public AWCookie (String name, String value, String domain, String path,
                     boolean isSecure, boolean httpOnly, int expiresInSeconds)
    {
        _headerPrefix = computeHeaderPrefix(name, value, domain, path, isSecure, httpOnly);
        _name = name;
        _value = value;
        _domain = domain;
        _path = path;
        _isSecure = isSecure;
        _expiresInSeconds = expiresInSeconds;
    }

    private FastStringBuffer checkoutStringBuffer ()
    {
        FastStringBuffer stringBuffer = null;
        synchronized (SharedStringBufferLock) {
            stringBuffer = SharedStringBuffer;
            SharedStringBuffer = null;
        }
        if (stringBuffer == null) {
            stringBuffer = new FastStringBuffer();
        }
        return stringBuffer;
    }

    private String finalizeStringBuffer (FastStringBuffer stringBuffer)
    {
        String string = stringBuffer.toString();
        string = AWUtil.filterUnsafeHeader(string);
        stringBuffer.truncateToLength(0);
        SharedStringBuffer = stringBuffer;
        return string;
    }

    private String computeHeaderPrefix (String name, String value, String domain,
                                        String path, boolean isSecure, boolean isHTTPOnly)
    {
        FastStringBuffer stringBuffer = checkoutStringBuffer();
        stringBuffer.append(name);
        stringBuffer.append("=");
        stringBuffer.append(value);
        if (path != null) {
            stringBuffer.append("; path=");
            stringBuffer.append(path);
        }
        if (domain != null) {
            stringBuffer.append("; domain=");
            stringBuffer.append(domain);
        }
        if (isSecure) {
            stringBuffer.append("; secure");
        }
        if (isHTTPOnly) {
            stringBuffer.append("; HttpOnly");
        }
        return finalizeStringBuffer(stringBuffer);
    }

    public String headerString (Date expirationDate)
    {
        String headerString = _headerPrefix;
        if (_shouldDelete) {
            headerString = StringUtil.strcat(_headerPrefix, "; expires=Tue, 10-Oct-2000 10:10:10 GMT");
        }
        else if (expirationDate != null) {
            String expirationDateString = ExpirationDateFormat.format(expirationDate);
            headerString = StringUtil.strcat(_headerPrefix, "; expires=", expirationDateString);
        }
        return headerString;
    }

    public String headerString ()
    {
        String headerString = null;
        if (_expiresInSeconds >= 0) {
            Date expirationDate = new Date();
            expirationDate.setTime(expirationDate.getTime() + (_expiresInSeconds * 1000L));
            headerString = headerString(expirationDate);
        }
        else {
            headerString = headerString(null);
        }
        return headerString;
    }

    public void configureForDeletion ()
    {
        _shouldDelete = true;
    }

    public Cookie getCookie ()
    {
        Cookie cookie = new Cookie(_name,_value);
        if (_path != null) {
            cookie.setPath(_path);
        }
        if (_domain != null) {
            cookie.setDomain(_domain);
        }

        cookie.setSecure(_isSecure);

        if (_expiresInSeconds >= 0) {
            cookie.setMaxAge(_expiresInSeconds);
        }

        return cookie;
    }
}
