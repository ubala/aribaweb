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

    $Id: //ariba/platform/util/core/ariba/util/core/URLUtil.java#13 $
*/

package ariba.util.core;

import ariba.util.log.Log;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;

/**
    URL Utilities. These are helper functions for dealing with
    urls.

    @aribaapi documented
*/
public final class URLUtil
{

    /* keep people from creating this class */
    private URLUtil ()
    {
    }
    
    /**
        specifies if there is a particular handler we want to use. Normally, when
        we instantiate java.net.URL objects, we let java pick the URLStreamHandler.
        @see java.net.URL
        
        We need to provide control to the https URLStreamHandler when run under
        Websphere. We should revisit this in future.

        The value of this variable is set by the object instance specified by the
        property HttpsUrlStreamHandlerProperty, and should represent a class which
        is a subclass of java.util.URLStreamHandler. Currently this property is
        defined when we run under websphere with value set to ariba.util.net.https.Handler.
        
        @see HttpsUrlStreamHandlerProperty
    */
    private static URLStreamHandler HTTPS_URL_STREAMHANDLER = null;

    /**
        Ariba specific system property that defines a specific urlStreamHandler to use.

        ToDo: This is a temporary solution to websphere issues.
        DO NOT use this property without talking to Server team first.
    */
    private static final String HttpsUrlStreamHandlerProperty =
        "ariba.httpsUrlStreamHandler";

    /**
     Return the secure URL stream handler.
     This provides lazy initialization since we want to
     avoid calls at static initialization that could result
     in logging. There have been bugs due to logging static
     initialization recursion.
        @aribaapi private
    */
    private static final URLStreamHandler getHttpsURLStreamHandler ()
    {
        if (HTTPS_URL_STREAMHANDLER == null) {
            // no need to synchronize here--the last one wins
            HTTPS_URL_STREAMHANDLER = setupHttpsURLStreamHandler();
        }
        return HTTPS_URL_STREAMHANDLER;
    }

    /**
        set up the URLStreamHandler for https protocol, as specified by
        the property "ariba.httpsUrlStreamHandler".

        @return the URLStreamHandler object, <b>null</b> if:
        (1) the property "ariba.httpsUrlStreamHandler" is not specified, OR
        (2) the value specified by the above property is not a subclass of
        java.util.URLStreamHandler, OR
        (3) security exception is raised when the system property is accessed.
    */
    private static URLStreamHandler setupHttpsURLStreamHandler ()
    {
        String handlerClassString = null;
        try {
            handlerClassString = System.getProperty(HttpsUrlStreamHandlerProperty);
        }
        catch (SecurityException e) {
            Log.util.warning(6725, HttpsUrlStreamHandlerProperty, e);
            return null;
        }

        if (StringUtil.nullOrEmptyOrBlankString(handlerClassString)) {
            Log.util.debug("value of %s is either null or blank or empty. " +
                           "No https URLStreamHandler set.",
                           HttpsUrlStreamHandlerProperty);
            return null;
        }

        Object object = ClassUtil.newInstance(handlerClassString,
                                              "java.net.URLStreamHandler",
                                              true);
        Log.util.debug("set up %s to handle https URL streams",
                       handlerClassString);
        return (URLStreamHandler)object;
    }

    /**
        Generate a File for a URL.

        @param urlContext a URL to open in a file. Assumes URL is of
        type file protocol (e.g. file://) and is not null.
        @param warning if <b>true</b>, on error, a warning will be printed

        @return a File representing the URL, or <b>null</b> if the file
        could not be opened

        @aribaapi documented
    */
    public static File file (URL urlContext, boolean warning)
    {
        String protocol = urlContext.getProtocol();
        if (!protocol.equals("file")) {
            if (warning) {
                Log.util.error(2772, urlContext);
            }
            return null;
        }
        String filePath = urlContext.getFile();

          // Add UNC support
        String host = urlContext.getHost();
        if (!StringUtil.nullOrEmptyOrBlankString(host) &&
            SystemUtil.isWin32()) {
            filePath = Fmt.S("\\\\%s\\%s", StringUtil.removeTrailingSlashIfAny(host),
                                           StringUtil.removeLeadingSlashIfAny(filePath));
        }

        if (filePath.endsWith("/.")) {
            filePath = filePath.substring(0, filePath.length() - 1);
        }
            // interestingly if one constructs a file with invalid
            // separatorChar (e.g. '/' on windows) most File members
            // work (e.g. exists()) but getParent() does not. Thus we
            // have to flip the separator on Windows.
        return new File(filePath.replace('/', File.separatorChar));
    }

    /**
        Returns true if the URL may exist.

        If the URL is a file: url, we convert it to a File and call
        exists(), which is faster than dealing with a
        FileNotFoundException.

        This is primarily when the file is likely not to exist, such
        as in the ResourceService which probes many possible filenames.
        @param url the input URL
        @return boolean indicating if URL may exist
        @aribaapi documented
    */
    public static boolean maybeURLExists (URL url)
    {
        File file = URLUtil.file(url, false);
        if (file == null) {
                // it may exist, we aren't sure.
            return true;
        }
        return file.exists();
    }

    /**
        Evaluates a string of a possibly relative url in the context
        of a different url.
        
        @param context the context in which to parse the
        specification.
        @param spec a string representation of a URL.

        @return a new URL, or <b>null</b> if there was an exception in
        the creation

        @see java.net.URL#URL(URL, String)
        @aribaapi documented
    */
    public static URL asURL (URL context, String spec)
    {
            // Needed for java 1.2 bug - PR 12590
        if ("".equals(spec) &&
            context != null &&
            "file".equals(context.getProtocol()))
        {
            spec = "./.";
        }
        try {
            return makeURL(context, spec);
        }
        catch (MalformedURLException e) {
            Log.util.error(2773, spec, e);
            return null;
        }
    }

    /**
        Creates a URL relative to the application working directory.  This
        method should only be used within server code or by command line (i.e.
        non-GUI) clients.

        If you are in GUI code, use Widgets.url().
        If you are in shared code, use Base.service().url().

        @param file the file object used to create the url, cannot be null.
        @return the URL created, or <b>null</b> if it cannot be created.
        
        @aribaapi private
    */
    public static URL url (File file)
    {
        try {
            return file.toURL();
        }
        catch (MalformedURLException e) {
            Log.util.error(2773, file.getPath(), e);
            return null;
        }
    }

    /**
        Creates a URL relative to the application working directory.  This
        method should only be used within server code or by command line (i.e.
        non-GUI) clients.

        If you are in GUI code, use Widgets.url().
        If you are in shared code, use Base.service().url().

        @param spec the String that specifies the resource (typically a file). This String
        must be a relative path (relative to the application working directory).
        @return the URL representing the given spec.
        @aribaapi private
    */
    public static URL url (String spec)
    {
        return asURL(url(), spec);
    }

    /**
        Creates a URL for the application working directory.  This method
        should only be used within server code or by command line clients.

        If you are in GUI code, use Application.codeBase().

        @aribaapi private
    */
    public static URL url ()
    {
        return urlAbsolute(SystemUtil.getCwdFile());
    }


    /**
        Creates a URL that references the given file.  This method
        should only be used within server code or by command line clients.

        @aribaapi private
    */
    public static URL urlAbsolute (File file)
    {
        String filePath = file.getAbsolutePath();
        filePath = filePath.replace(File.separatorChar, '/');

        if (file.isDirectory()) {
            if (filePath.endsWith("/.")) {
                filePath = filePath.substring(0, filePath.length() - 1);
            }
            if (!filePath.endsWith("/")) {
                filePath = Fmt.S("%s/", filePath);
            }
        }
        try {
            /*
                If the file path starts with "/" then don't prepend
                one. Adding the extra slash will yield 'file://..."
                which causes the first node of the path to be
                considered a hostname not part of the path.
            */
            if (filePath.startsWith("/")) {
                return new URL("file", null, Fmt.S("%s", filePath)); // OK
            }
            else {
                return new URL("file", null, Fmt.S("/%s", filePath)); // OK
            }
        }
        catch (MalformedURLException e) {
            Log.util.error(2774, filePath, e);
            return null;
        }
    }

    /**
        Returns whether the specified specification is a
        fully-qualified URL.  The heuristic that we use is to check
        whether it contains the string ":/" before any other
        slashes.  Note:  Applet file codebases a prefixed by "file:/".
        This is why we check for only one slash.

        @aribaapi private
    */
    public static boolean fullyQualifiedURLSpec (String spec)
    {
        int slash = spec.indexOf("/");
        int colon = spec.indexOf(":");

            // We have a complete URL if we have a protocol (the first slash
            // is preceded by a colon)
        return (slash > 0 && colon == slash - 1);
    }
    
    /**
        Create a URL to a web server. If the url is absolute (includes
        http) use it as is.
        
        Otherwise, assume the URL is relative to the <code>context</code>.

        @param url the URL string we are making a URL object for
        @param context the root URL

        @return a new URL as requested
        @exception MalformedURLException
        @aribaapi documented
    */
    public static URL formURL (String url, String context)
      throws MalformedURLException
    {
        URL completeURL = null;
        if (url.startsWith(HTTP.Protocol)) {
            completeURL = makeURL(null, url);
        }
        else {
            URL contextURL = null;
            if (context.endsWith("/")) {
                contextURL = makeURL(null, context);
            }
            else {
                contextURL = makeURL(null, Fmt.S("%s/", context));
            }
            completeURL = makeURL(contextURL, url);
        }

        return completeURL;
    }

    /**
        Create a URL from a path and file name.  The resulting URL will
        be constructed independently of what VM (1.1 or 1.2) is being
        used.

        @param path the path (either absolute or relative)
        @param file the file name.

        @return a new URL as requested
        @aribaapi documented
    */
    public static URL concatURL (String path, String file)
    {
        String string = Fmt.S("%s/%s", path, file);
        try {
            return makeURL(null, string);
        }
        catch (MalformedURLException e) {
            Log.util.error(2773, string, e);
            return null;
        }
    }

    /**
        Wrapper class to create a new URL object. Did this to
        centralize the logic to pass in the https URLStreamHandler.
        new java.net.URL (String) should not be called directly. This
        method should be called instead.
        
        @param spec the URL spec, cannot be null. 

        @exception MalformedURLException if the spec is illegal.

        @return the URL instance given the spec.
        
        @aribaapi documented
    */
    public static URL makeURL (String spec)
      throws MalformedURLException
    {
        return makeURL(null, spec);
    }

    /**
        Wrapper class to create a new URL object. Did this to
        centralize the logic to pass in the https URLStreamHandler.
        new java.net.URL (...) should not be called directly. This
        method should be called instead.
        
        @param      protocol   the name of the protocol to use.
        @param      host       the name of the host.
        @param      port       the port number on the host.
        @param      file       the file on the host
        @exception MalformedURLException if the spec is illegal.
        @return the URL instance given the spec.
        
        @aribaapi documented
    */
    public static URL makeURL (String protocol, String host, int port, String file)
      throws MalformedURLException
    {
        URLStreamHandler handler = null;
        if (HTTP.SecureProtocol.equalsIgnoreCase(protocol)) {
            handler = getHttpsURLStreamHandler();
        }
        return new URL(protocol, host, port, file, handler); // OK
    }

    /**
        Wrapper class to create a new URL object. Did this to
        centralize the logic to pass in the https URLStreamHandler.

        @param context the context URL, can be null.
        @param spec the URL spec, can be null. But if both spec and
        context are null, java.net.URL will catch it.

        @return the URL instance given the context and spec.
        
    */
    private static URL makeURL (URL context, String spec)
      throws MalformedURLException
    {
            // The ultimate goal is to detemine what handler to pass to
            // instantiate a URL object. Javadoc from java.net.URL says
            // the protocol from spec will override the one in context.
            // So we find out the protocol used by spec. 

            // If it is https, we just passed in the static URLStreamHandler
            // specified by HttpsUrlStreamHandlerProperty (can be null).
            // Otherwise, we will let java take care of the handler unless
            // the spec protocol is not specified (i.e. null).
            // Then we determine the protocol from the context, and pass in
            // our handler if the context protocol is https.

        URLStreamHandler handler = null;
        if (spec == null) {
            if (context != null) {
                if (HTTP.SecureProtocol.equalsIgnoreCase(context.getProtocol())) {
                    handler = getHttpsURLStreamHandler();
                }
            }
        }
        else if (isHttps(spec)) {
            handler = getHttpsURLStreamHandler();
        }
        Log.util.debug("making URL: context = %s, spec = %s, handler = %s",
                       context, spec, handler);
        return new URL(context, spec, handler); // OK
    }

    /**
        Determine whether the given spec indicates the protocol to be https.

        @param spec that specifies the URL, must be non-null
        @return whether the spec indicates the protocol to be https
    */
    private static boolean isHttps (String spec)
    {
        return spec.regionMatches(true, 0, HTTP.SecureProtocol,
                                  0, HTTP.SecureProtocol.length());
    }

    /**
        Create a URL string to a web server. If the url is absolute
        (includes http) use it as is.

        Otherwise, assume the URL is relative to the <code>context</code>, e.g.
        "http://hostname/AribaORMS".

        @param url url string we are making a URL object for
        @param context the root url

        @return a String representing the URL
        @aribaapi documented
    */
    public static String formURLString (String url, String context)
    {
        if (fullyQualifiedURLSpec(url)) {
            return url;
        }

            // Concantenate context + url, making sure we don't have double
            // slashes
        FastStringBuffer buf = new FastStringBuffer(context);
        boolean contextSlash = context.endsWith("/");
        boolean urlSlash = url.startsWith("/");

        if (contextSlash && urlSlash) {
            buf.truncateToLength(buf.length()-1);
        }
        else if (!contextSlash && !urlSlash) {
            buf.append("/");
        }
        buf.append(url);

        return buf.toString();
    }
}
