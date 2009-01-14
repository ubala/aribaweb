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

    $Id: //ariba/platform/util/core/ariba/util/core/HTTP.java#6 $
*/

package ariba.util.core;

/**
    HTTP.  The HTTP/1.0 protocol.

    http://info.internet.isi.edu/in-notes/rfc/files/rfc1945.txt

    Status codes from HTTP/1.1 protocol

    http://info.internet.isi.edu/in-notes/rfc/files/rfc2068.txt

    @aribaapi private
*/
public class HTTP
{
    /*-----------------------------------------------------------------------
        Public Constants
      -----------------------------------------------------------------------*/

    /**
        the version 1.0 of HTTP supported
    */
    public static final String Version10 = "HTTP/1.0";
    public static final double VersionNumber10 = 1.0; 

    /**
        the version 1.1 of HTTP supported
    */
    public static final String Version11 = "HTTP/1.1";
    public static final double VersionNumber11 = 1.1;
    
    /**
        the name of the HTTP protocol
    */
    public static final String Protocol = "http";

    /**
        default port number for HTTP connections
    */
    public static final int Port = 80;

    /**
        the name of the HTTPS protocol
    */
    public static final String SecureProtocol = "https";

    /**
        default port number for HTTPS connections
    */
    public static final int SecurePort = 443;

        // request methods
    public static final String GetMethod  = "GET";
    public static final String PostMethod = "POST";
    public static final String HeadMethod = "HEAD";

        // entity headers
    public static final String HeaderContentEncoding  = "CONTENT-ENCODING";
    public static final String HeaderExpires          = "EXPIRES";
    public static final String HeaderLastModified     = "LAST-MODIFIED";
    public static final String HeaderAllow            = "ALLOW";
    public static final String HeaderUserAgent        = "USER-AGENT";
    public static final String HeaderCookie           = "COOKIE";
    public static final String HeaderSetCookie        = "SET-COOKIE";
    public static final String HeaderFrom             = "FROM";
    public static final String HeaderAccept           = "ACCEPT";
    public static final String HeaderAcceptEncoding   = "ACCEPT-ENCODING";
    public static final String HeaderAcceptLanguage   = "ACCEPT-LANGUAGE";
    public static final String HeaderReferer          = "REFERER";
    public static final String HeaderAuthorization    = "AUTHORIZATION";
    public static final String HeaderChargeTo         = "CHARGETO";
    public static final String HeaderIfModifiedSince  = "IF-MODIFIED-SINCE";
    public static final String HeaderHost             = "HOST";
    
    public static final String CookieVersion = "$VERSION";
    public static final String CookieDomain  = "$DOMAIN";
    public static final String CookiePath    = "$PATH";

        // Headers used by proxies to provide client host and IP address
    public static final String HeaderRemoteAddr = "REMOTE_ADDR";
    public static final String HeaderRemoteHost = "REMOTE_HOST";

        // response headers
    public static final String HeaderLocation = "LOCATION";
    
    public static final String HeaderPragma = "PRAGMA";
    public static final String PragmaNoCache = "no-cache";

    public static final String HeaderWWWAuthenticate = "WWW-Authenticate";
    public static final String AuthSchemeBasic = "Basic ";
    public static final String AuthSchemeBasicWithRealm = (AuthSchemeBasic +
                                                           "realm=");

    public static final String HeaderConnection       = "CONNECTION";
    public static final String ConnectionClose        = "close";
    public static final String HeaderRetryAfter = "Retry-After";

        // response codes
    public static final int CodeContinue                    = 100;
    public static final int CodeSwitchingProtocols          = 101;
    public static final int CodeOK                          = 200;
    public static final int CodeCreated                     = 201;
    public static final int CodeAccepted                    = 202;
    public static final int CodeNonAuthoritativeInformation = 203;
    public static final int CodeNoContent                   = 204;
    public static final int CodeResetContent                = 205;
    public static final int CodePartialContent              = 206;
    public static final int CodeMultipleChoices             = 300;
    public static final int CodeMovedPermanently            = 301;
    public static final int CodeMovedTemporarily            = 302;
    public static final int CodeSeeOther                    = 303;
    public static final int CodeNotModified                 = 304;
    public static final int CodeUseProxy                    = 305;
    public static final int CodeBadRequest                  = 400;
    public static final int CodeUnauthorized                = 401;
    public static final int CodePaymentRequired             = 402;
    public static final int CodeForbidden                   = 403;
    public static final int CodeNotFound                    = 404;
    public static final int CodeMethodNotAllowed            = 405;
    public static final int CodeNotAcceptable               = 406;
    public static final int CodeProxyAuthenticationRequired = 407;
    public static final int CodeRequestTimeOut              = 408;
    public static final int CodeConflict                    = 409;
    public static final int CodeGone                        = 410;
    public static final int CodeLengthRequired              = 411;
    public static final int CodePreconditionFailed          = 412;
    public static final int CodeRequestEntityTooLarge       = 413;
    public static final int CodeRequestURITooLarge          = 414;
    public static final int CodeUnsupportedMediaType        = 415;
    public static final int CodeInternalServerError         = 500;
    public static final int CodeNotImplemented              = 501;
    public static final int CodeBadGateway                  = 502;
    public static final int CodeServiceUnavailable          = 503;
    public static final int CodeGatewayTimeOut              = 504;
    public static final int CodeHTTPVersionNotSupported     = 505;

        // response messages
    public static final String MsgContinue =
        "Continue";
    public static final String MsgSwitchingProtocols =
        "Switching Protocols";
    public static final String MsgOK =
        "OK";
    public static final String MsgCreated =
        "Created";
    public static final String MsgAccepted =
        "Accepted";
    public static final String MsgNonAuthoritativeInformation =
        "Non-Authoritative Information";
    public static final String MsgNoContent =
        "No Content";
    public static final String MsgResetContent =
        "Reset Content";
    public static final String MsgPartialContent =
        "Partial Content";
    public static final String MsgMultipleChoices =
        "Multiple Choices";
    public static final String MsgMovedPermanently =
        "Moved Permanently";
    public static final String MsgMovedTemporarily =
        "Moved Temporarily";
    public static final String MsgSeeOther =
        "See Other";
    public static final String MsgNotModified =
        "Not Modified";
    public static final String MsgUseProxy =
        "Use Proxy";
    public static final String MsgBadRequest =
        "Bad Request";
    public static final String MsgUnauthorized =
        "Unauthorized";
    public static final String MsgPaymentRequired =
        "Payment Required";
    public static final String MsgForbidden =
        "Forbidden";
    public static final String MsgNotFound =
        "Not Found";
    public static final String MsgMethodNotAllowed =
        "Method Not Allowed";
    public static final String MsgNotAcceptable =
        "Not Acceptable";
    public static final String MsgProxyAuthenticationRequired =
        "Proxy Authentication Required";
    public static final String MsgRequestTimeOut =
        "Request Time-out";
    public static final String MsgConflict =
        "Conflict";
    public static final String MsgGone =
        "Gone";
    public static final String MsgLengthRequired =
        "Length Required";
    public static final String MsgPreconditionFailed =
        "Precondition Failed";
    public static final String MsgRequestEntityTooLarge =
        "Request Entity Too Large";
    public static final String MsgRequestURITooLarge =
        "Request-URI Too Large";
    public static final String MsgUnsupportedMediaType =
        "Unsupported Media Type";
    public static final String MsgInternalServerError =
        "Internal Server Error";
    public static final String MsgNotImplemented =
        "Not Implemented";
    public static final String MsgBadGateway =
        "Bad Gateway";
    public static final String MsgServiceUnavailable =
        "Service Unavailable";
    public static final String MsgGatewayTimeOut =
        "Gateway Time-out";
    public static final String MsgHTTPVersionNotSupported =
        "HTTP Version not supported";


    public static final String message (int code)
    {
        switch (code) {
          case CodeContinue:
            return MsgContinue;
          case CodeSwitchingProtocols:
            return MsgSwitchingProtocols;
          case CodeOK:
            return MsgOK;
          case CodeCreated:
            return MsgCreated;
          case CodeAccepted:
            return MsgAccepted;
          case CodeNonAuthoritativeInformation:
            return MsgNonAuthoritativeInformation;
          case CodeNoContent:
            return MsgNoContent;
          case CodeResetContent:
            return MsgResetContent;
          case CodePartialContent:
            return MsgPartialContent;
          case CodeMultipleChoices:
            return MsgMultipleChoices;
          case CodeMovedPermanently:
            return MsgMovedPermanently;
          case CodeMovedTemporarily:
            return MsgMovedTemporarily;
          case CodeSeeOther:
            return MsgSeeOther;
          case CodeNotModified:
            return MsgNotModified;
          case CodeUseProxy:
            return MsgUseProxy;
          case CodeBadRequest:
            return MsgBadRequest;
          case CodeUnauthorized:
            return MsgUnauthorized;
          case CodePaymentRequired:
            return MsgPaymentRequired;
          case CodeForbidden:
            return MsgForbidden;
          case CodeNotFound:
            return MsgNotFound;
          case CodeMethodNotAllowed:
            return MsgMethodNotAllowed;
          case CodeNotAcceptable:
            return MsgNotAcceptable;
          case CodeProxyAuthenticationRequired:
            return MsgProxyAuthenticationRequired;
          case CodeRequestTimeOut:
            return MsgRequestTimeOut;
          case CodeConflict:
            return MsgConflict;
          case CodeGone:
            return MsgGone;
          case CodeLengthRequired:
            return MsgLengthRequired;
          case CodePreconditionFailed:
            return MsgPreconditionFailed;
          case CodeRequestEntityTooLarge:
            return MsgRequestEntityTooLarge;
          case CodeRequestURITooLarge:
            return MsgRequestURITooLarge;
          case CodeUnsupportedMediaType:
            return MsgUnsupportedMediaType;
          case CodeInternalServerError:
            return MsgInternalServerError;
          case CodeNotImplemented:
            return MsgNotImplemented;
          case CodeBadGateway:
            return MsgBadGateway;
          case CodeServiceUnavailable:
            return MsgServiceUnavailable;
          case CodeGatewayTimeOut:
            return MsgGatewayTimeOut;
          case CodeHTTPVersionNotSupported:
            return MsgHTTPVersionNotSupported;
          default:
            return Fmt.S("Code %s", Constants.getInteger(code));
        }
    }
}
