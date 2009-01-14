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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWFileResource.java#9 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.Constants;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.StringUtil;
import ariba.util.core.URLUtil;
import ariba.util.core.GrowOnlyHashtable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

public final class AWFileResource extends AWResource
{
    private AWFileResourceDirectory _directory;
    private File _file;
    private String _url;
    private String _fullUrl;

    /**
        this is only used in AWMultiLocaleResourceManager to
        create not found marker
    */
    protected AWFileResource ()
    {
        super("scratch", "scratch");
    }
    
    protected AWFileResource (String resourceName,
                              String relativePath,
                              AWFileResourceDirectory directory)
    {
        super(resourceName, relativePath);
        _url = StringUtil.strcat(directory.urlPrefix(), "/", relativePath);
        _url = _url.replace('\\', '/');
        _directory = directory;
        String fullPath = _fullPath();
        _file = new File(fullPath);
        Assert.that(_file.exists(), "Specified file does not exist: \"" + fullPath + "\" resource: " + toString());
        try {
            String canonicalPath = _file.getCanonicalPath();
            Assert.that(canonicalPath.endsWith(_file.getName()), "Case mismatch in file name: \"" + relativePath + "\" Note: was able to locate file with similar name: " + canonicalPath);
        }
        catch (IOException ioexception) {
            throw new AWGenericException(ioexception);
        }
    }
    
    public String _fullPath ()
    {
        // this will probably not be called too often, so do not cache the results.
        return StringUtil.strcat(_directory.directoryPath(), "/", _relativePath);
    }

    public String url ()
    {
        return _url;
    }

    public String fullUrl ()
    {
        if (_fullUrl == null) {
            URL urlObject = URLUtil.urlAbsolute(_file);
            _fullUrl = urlObject.toExternalForm();
        }
        return _fullUrl;
    }
    
    public long lastModified ()
    {
        return fileLastModified(_file);
    }
    
    public InputStream inputStream ()
    {
        try {
            InputStream inputStream = new FileInputStream(_file);
            return inputStream;
        }
        catch (FileNotFoundException fileNotFoundException) {
            throw new AWGenericException(fileNotFoundException);
        }
    }

    public String toString ()
    {
        String string = super.toString();
        if (_file != null) {
            string += " absolutePath: " + _file.getAbsolutePath();
        }
        return string;
    }

    private static GrowOnlyHashtable _FileStatCache = null;
    private static long _StatsLastInvalidatedTime = 0;

    /**
     * Called when new request comes in.  If we haven't invalidated our cache lately, then this is the trigger.
     */
    public static void notifyNewRequest ()
    {
        // invalidate if we haven't done so in 10 seconds
        long currentTime = System.currentTimeMillis();
        if (currentTime -_StatsLastInvalidatedTime > 10000) {
            _FileStatCache = null;
            _StatsLastInvalidatedTime = currentTime;
        }
    }

    /**
     * Lookup cached stat times
     *
     * We expect that this would only be called in RapidTurnaround mode (not in production).
     *
     * @param f the file
     * @return timestamp (possibly slightly out of date)
     */
    public static long fileLastModified (File f)
    {
        GrowOnlyHashtable table = _FileStatCache;
        if (table == null) {
            table = new EqGrowOnlyHashtable();
            _FileStatCache = table;
        }

        Long ts = (Long)table.get(f);
        if (ts == null) {
            ts = Constants.getLong(f.lastModified());
            table.put(f, ts);
        }
        return ts.longValue();
    }
}

class EqGrowOnlyHashtable extends GrowOnlyHashtable
{
    protected boolean objectsAreEqualEnough (Object obj1, Object obj2)
    {
        return  obj1 == obj2;
    }

    protected int getHashValueForObject (Object o)
    {
        return System.identityHashCode(o);
    }
}
