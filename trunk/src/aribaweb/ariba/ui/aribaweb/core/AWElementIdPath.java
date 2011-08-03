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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWElementIdPath.java#20 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWArrayManager;
import ariba.ui.aribaweb.util.Log;
import ariba.ui.aribaweb.util.AWBase64;
import ariba.util.core.StringUtil;
import ariba.util.core.Assert;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.PerformanceStateCounter;
import ariba.util.core.PerformanceStateCore;

/**
 * Maintains a cache of AWElementIdPath's.  The ElementIdPath holds all useful representations of a path to
 * an AW element: the path itself (an array of chars), a checksum value computed from this array, and the
 * elemenId which is an Base64 encoded string version of the checksum.
 */
public class AWElementIdPath extends AWBaseObject
{
    private static GrowOnlyHashtable ElementIdPaths = new GrowOnlyHashtable();
    private static GrowOnlyHashtable ElementIdPathsByString = new GrowOnlyHashtable();
    private static int ElementIdCacheMaxSize = Integer.MAX_VALUE;
    private static final AWElementIdPath EmptyPath;
    private static final AWElementIdPath NoOpPath;
    public static final int LevelMaxSize = (int)Character.MAX_VALUE;
    public static PerformanceStateCounter ElemendIdInstantiationsCounter =
            new PerformanceStateCounter("Element Id Instantiations", 220,
                    PerformanceStateCore.LOG_COUNT);

    // Using chars here reduces memory usage for the path objects, but assumes we'll never exceed 65,535
    // at any level in a path.  For aw applications, this should be safe.
    // Note that _path is trimmed to proper length
    private final char[] _path;
    private final int _hashcode;
    private final AWEncodedString _elementId;

    static {
        AWElementIdGenerator elementIdGenerator = new AWElementIdGenerator();
        EmptyPath = AWElementIdPath.sharedInstance(elementIdGenerator);
        NoOpPath = new AWElementIdPath(new NoOpEncodedString());
    }

    public static int size ()
    {
        return ElementIdPaths.size();
    }

    protected static int computeHashcode (char[] path, int length)
    {
        // crc32Hash is a bit more crude than the CRC32, so a bit faster.
        return AWChecksum.crc32Hash(path, length);
    }

    /**
     * @param path must be a trimmed array and should not have any empty capacity on its end
     */
    private AWElementIdPath (char[] path)
    {
        _path = path;
        if (path == null) {
            _hashcode = 0;
            _elementId = null;
            Assert.that(false, "null path not allowed");
        }
        else {
            _hashcode = AWElementIdPath.computeHashcode(path, path.length);

            // Now create a short string to represent this _path.
            long checksum = AWChecksum.checksumWithLength(_path, _path.length);
            String checksumString = AWBase64.base64EncodeLong(checksum);

            // IE is case insensitive to element ids, so we need to lower case all ids
            String newElementIdString = StringUtil.strcat("_", checksumString.toLowerCase());
            int index = 0;
            Object collidingElementIdPath;
            // Iterate until no collision exists with the elementIdString. -- this should be extremely rare (1:4billion odds)
            // However, we increased the chance of collision, since we need to lower case all ids now
            while ((collidingElementIdPath = ElementIdPathsByString.get(newElementIdString)) != null) {
                char[] collidingPath = ((AWElementIdPath)collidingElementIdPath).privatePath();
                long collidingChecksum = AWChecksum.checksumWithLength(collidingPath, collidingPath.length);
                if (checksum == collidingChecksum) {
                    // report if this is a real collision (with the ids in their original form)
                    int count = 15;
                    while (count-- > 0) {
                        Log.aribaweb.debug("surprise! real collision in element id: %s", checksumString);
                    }
                }
                newElementIdString = checksumString + Integer.toString(index, 16);
                index++;
            }
            _elementId = new AWEncodedString(newElementIdString);
            ElementIdPathsByString.put(_elementId.string(), this);
        }
    }

    private AWElementIdPath (AWEncodedString encodedString)
    {
        _path = null;
        _hashcode = 0;
        _elementId = encodedString;
    }

    public static AWElementIdPath emptyPath ()
    {
        return EmptyPath;
    }

    public static AWElementIdPath noOpPath ()
    {
        return NoOpPath;
    }

    public static void setElementIdCacheMaxSize (int maxSize) 
    {
        ElementIdCacheMaxSize = maxSize;
    }

    // This is the only way to get a new AWElementIdPath (maintains a cache of them)
    protected static AWElementIdPath sharedInstance (AWElementIdGenerator elementIdGenerator)
    {
        elementIdGenerator.prepareForHashlookup();
        AWElementIdPath elementIdPath = (AWElementIdPath)ElementIdPaths.get(elementIdGenerator);
        if (elementIdPath == null) {
            char[] path = (char[])elementIdGenerator.charArrayManager().trimmedArrayCopy();
            elementIdPath = new AWElementIdPath(path);
            ElemendIdInstantiationsCounter.addCount(1);
            if (ElementIdPaths.size() > ElementIdCacheMaxSize) {
                ElementIdPaths = new GrowOnlyHashtable();
                ElementIdPathsByString = new GrowOnlyHashtable();
                Log.aribawebexec_elementId.info(10547);
            }
            ElementIdPaths.put(elementIdPath, elementIdPath);
        }
        return elementIdPath;
    }

    protected static AWElementIdPath lookup (String elementIdString)
    {
        AWElementIdPath elementIdPath =
            (AWElementIdPath)ElementIdPathsByString.get(elementIdString);
        if (elementIdPath == null && Log.aribawebexec_elementId.isDebugEnabled()) {
            Log.logStack(Log.aribawebexec_elementId,
                Fmt.S(" <<< Lookup cache miss: %s should not occur unless the server has " +
                      "restarted since page was generated.", elementIdString));
        }
        return elementIdPath;
    }

    protected char get (int index)
    {
        return _path[index];
    }

    protected AWEncodedString elementId ()
    {
        return _elementId;
    }

    public int hashCode ()
    {
        // this is the hascode of the char[]
        return _hashcode;
    }

    public boolean equals (Object otherObject)
    {
        boolean isEqual = this == otherObject;
        if (!isEqual) {
            char[] thisPath = _path;
            char[] otherPath = null;
            int otherPathLength = 0;
            if (otherObject instanceof AWElementIdGenerator) {
                AWElementIdGenerator charArrayElementId = (AWElementIdGenerator)otherObject;
                // note: charArrayElementId._path may have empty capacity on the end,
                // so must use charArrayElementId._pathLength.
                AWArrayManager charArrayManager = charArrayElementId.charArrayManager();
                otherPath = (char[])charArrayManager.array();
                otherPathLength = charArrayManager.size();
            }
            else if (otherObject instanceof AWElementIdPath) {
                AWElementIdPath elementIdPath = (AWElementIdPath)otherObject;
                // here elementIdPath._path has no empty capacity on its end, so can use otherPath.thisPathLength.
                otherPath = elementIdPath._path;
                otherPathLength = otherPath.length;
            }
            else {
                return false;
            }
            int thisPathLength = thisPath.length;
            if (thisPathLength == otherPathLength) {
                isEqual = true;
                // scan backwards as its more likely that diffs will be at end
                for (int index = thisPathLength - 1; index > -1; index--) {
                    if (thisPath[index] != otherPath[index]) {
                        return false;
                    }
                }
            }
        }
        return isEqual;
    }

    protected int hashCodeSkipping (int prefixLength)
    {
        return AWChecksum.crc32Hash(_path, prefixLength, _path.length);
    }

    public boolean equalsSkipping (AWElementIdPath elementIdPath, int skipLength)
    {
        char[] thisPath = _path;
        char[] otherPath = elementIdPath._path;
        int otherPathLength = otherPath.length;
        int thisPathLength = thisPath.length;
        if (thisPathLength == otherPathLength) {
            // scan backwards as its more likely that diffs will be at end
            skipLength--;
            for (int index = thisPathLength - 1; index > skipLength; index--) {
                if (thisPath[index] != otherPath[index]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /*
        Determines whether arg is our parent of the.  Since pushLevel() does an increment()
         followed by pushing a zero, we'll pop the trailing zero from the parent.
     */
    public boolean hasPrefix (AWElementIdPath parent)
    {
        char[] thisPath = _path;
        char[] parentPath = parent._path;

        int parentLength = parentPath.length;
        if (parentLength > thisPath.length) return false;
        // don't require match on the trailing 0 on the parent
        if (parentPath[parentLength-1] == 0) parentLength--;
        for (int i=0; i < parentLength; i++) {
            if (thisPath[i] != parentPath[i]) {
                return false;
            }
        }
        return true;
    }

    /*
        Determines whether arg is our parent or anticedent sibling, or "uncle"
     */
    public boolean isParentOrSiblingPredecessor (AWElementIdPath parent)
    {
        char[] thisPath = _path;
        char[] parentPath = parent._path;

        int parentLength = parentPath.length;
        if (parentLength > thisPath.length) return false;
        // don't require match on the trailing 0 on the parent
        for (int i=0; i < parentLength-1; i++) {
            if (thisPath[i] != parentPath[i]) {
                return false;
            }
        }
        return thisPath[parentLength-1] >= parentPath[parentLength-1];
    }

    /**
     * This should only be called by AWElementIdGenerator.nextPrefixMatches().
     * Nobody else should have access to the _path directly.
     */
    protected char[] privatePath ()
    {
        return _path;
    }

    static void debug_dumpElementIdPath (AWRequestContext requestContext,
                                         String msg,
                                         String phase)
    {
        String s = debugElementIdPath(requestContext.currentElementIdPath());
        Log.aribaweb.debug("ElementId path debug: %s -- %s -- %s --",
                           s.toString(), msg, phase);
    }

    static String debugElementIdPath (AWElementIdPath elementIdPath)
    {
        return debugElementIdPathWithDelimiter(elementIdPath, ".");
    }

    static String debugElementIdPathWithDelimiter (AWElementIdPath elementIdPath,
                                                   String delimiter)
    {
        if (elementIdPath == null) {
            return null;
        }
        char[] path = elementIdPath.privatePath();
        FastStringBuffer sb = new FastStringBuffer();
        for (int i=0; i < path.length; i++) {
            int element = (int)path[i];
            sb.append(String.valueOf(element));
            sb.append(delimiter);
        }
        return sb.toString();
    }

    public String toString ()
    {
        return _elementId.string();
    }

    static class NoOpEncodedString extends AWEncodedString
    {
        public NoOpEncodedString ()
        {
            super("");
        }

        public String toString ()
        {
            Assert.fail("No-op string cannot be rendered.");
            return super.toString();
        }
    }
}
