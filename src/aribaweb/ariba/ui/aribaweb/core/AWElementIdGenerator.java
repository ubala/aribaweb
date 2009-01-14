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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWElementIdGenerator.java#5 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWArrayManager;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.util.core.Assert;

/**
 * A new AWElementIdGenerator is created for each request and is retained by the AWRequestContext for the
 * duration of a request.  For each of the three phases, it is reset to start things over again.
 * Most of the interesting work of this system is done by AWElementIdPath, which maintains a cache
 * of the element paths (a char[]), their checksums, and a string representation of the checksum.
 */
public class AWElementIdGenerator extends AWBaseObject
{
    private static final char InitialElementValue = 0;

    private AWArrayManager _charArrayManager;

    public AWElementIdGenerator ()
    {
        // use char to keep array size smaller (values up to 65,535, unsigned) -- see Assertion in increment() below.
        _charArrayManager = new AWArrayManager(Character.TYPE, 32);
        reset();
    }

    protected AWArrayManager charArrayManager ()
    {
        return _charArrayManager;
    }

    public void reset ()
    {
        _charArrayManager.reset();
        _charArrayManager.addElement(InitialElementValue);
    }

    protected void increment (int amount)
    {
        char[] charArray = (char[])_charArrayManager.array();
        int index = _charArrayManager.size() - 1;
        Assert.that(((int)charArray[index] + amount) <= AWElementIdPath.LevelMaxSize,
                "Incremented elementId too many times -- need to use int[] instead of char[]");
        charArray[index] += (char)amount;
    }

//    public static int DEBUG_LEVEL = 0;
//    public static int DEBUG_VAL = 8;

    public void pushLevel ()
    {
//        // break point at specific iteration in the element hierarchy
//        if (_charArrayManager.size() >= DEBUG_LEVEL + 1 &&
//            ((int)((char[])_charArrayManager.array())[DEBUG_LEVEL]) == DEBUG_VAL) {
//            System.out.print("");
//        }
        increment(1);
        _charArrayManager.addElement(InitialElementValue);
    }

    public void popLevel ()
    {
        _charArrayManager.removeLast();
    }

    public void pushLevel (int elementIdComponent)
    {
        pushLevel();
        increment(elementIdComponent);
        pushLevel();
    }

    public void popLevel (int elementIdComponent)
    {
        popLevel();
        popLevel();
    }

    public AWElementIdPath currentElementIdPath ()
    {
        return AWElementIdPath.sharedInstance(this);
    }

    public int currentLevel ()
    {
        return _charArrayManager.size();
    }

    public boolean nextPrefixMatches (AWElementIdPath elementIdPath)
    {
        char[] targetPath = elementIdPath.privatePath();
        int pathLength = _charArrayManager.size();
        int targetPathLength = targetPath == null ? 0 : targetPath.length;
        boolean prefixMatches = pathLength <= targetPathLength;
        if (prefixMatches) {
            char[] charArray = (char[])_charArrayManager.array();
            // Search backwards since most likely to have diff at end
            int index = pathLength - 1;
            // Bump the value of the first one as we're comparing the "next" prefix
            if (charArray[index] + 1 == targetPath[index]) {
                for (index--; index > -1; index--) {
                    if (charArray[index] != targetPath[index]) {
                        prefixMatches = false;
                        break;
                    }
                }
            }
            else {
                prefixMatches = false;
            }
        }
        return prefixMatches;
    }

    ////////////////////////////////////////////
    // Hashtable Suport
    // Used when looking up the AWElementIdPath
    ////////////////////////////////////////////
    private char[] _path;
    private int _pathLength;
    private int _hashcode;

    protected void prepareForHashlookup ()
    {
        _path = (char[])_charArrayManager.array();
        _pathLength = _charArrayManager.size();
        _hashcode = AWElementIdPath.computeHashcode(_path, _pathLength);
    }

    public int hashCode ()
    {
        return _hashcode;
    }

    public boolean equals (Object otherObject)
    {
        boolean isEqual = false;
        if (otherObject instanceof AWElementIdPath) {
            AWElementIdPath otherElementIdPath = (AWElementIdPath)otherObject;
            char[] otherPath = otherElementIdPath.privatePath();
            int otherPathLength = otherPath.length;
            if (_pathLength == otherPathLength) {
                isEqual = true;
                char[] path = _path;
                // scan backwards as its more likely that diffs will be toward end of path
                for (int index = _pathLength - 1; index > -1; index--) {
                    if (path[index] != otherPath[index]) {
                        isEqual = false;
                        break;
                    }
                }
            }
        }
        else if (otherObject instanceof AWElementIdGenerator) {
            throw new AWGenericException("should not have gotten AWElementIdGenerator here");
        }
        return isEqual;
    }
}
