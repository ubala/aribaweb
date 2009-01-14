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

    $Id: //ariba/platform/util/core/ariba/util/io/TaggedStringProcessor.java#7 $
*/

package ariba.util.io;

import ariba.util.core.ListUtil;
import ariba.util.core.FastStringBuffer;
import java.util.List;

/**
    A TaggedStringProcessor processes a string that contains "tags", i.e.
    substrings delimited by a special sequence of characters.  For each tag
    it finds in the string, it calls its TagHandler with the tag string and
    replaces the tag string (including the delimeters) with the result.

    @see TagHandler

    @aribaapi private
*/
public class TaggedStringProcessor
{
    /*-----------------------------------------------------------------------
        Public Constants
      -----------------------------------------------------------------------*/

        // the tag prefix/suffix; all tags should be enclosed in this string
    public static final String TagDelimiter = "@@";

        // the prefix/suffix for an arguments list
    public static final String BeginArgsChar = "(";
    public static final String EndArgsChar   = ")";

        // the arguments list separator
    public static final String ArgsSeparator = ",";


    /*-----------------------------------------------------------------------
        Constructors
      -----------------------------------------------------------------------*/

    public TaggedStringProcessor (TagHandler handler)
    {
        this.handler = handler;
    }


    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/

    /**
        Given a line of text string, process all the tags that it find in this
        line.  It returns the processed string.

        There are two flavors of processString(), one which accepts an existing
        FastStringBuffer and arguments List created by the caller and one which
        creates one on the fly as needed.  These aren't currently cached to
        avoid synchronization overhead.  The second interface is provided for
        users making heavy use of this routine for batch processing to avoid
        object creation overhead.
    */
    public String processString (String string)
    {
        return processString(string, null, null);
    }

    public String processString (String string,
                                 FastStringBuffer userBuf,
                                 List args)
    {
            // short-circuit: just return if there are no tags
        if (string.indexOf(TagDelimiter) == -1) {
            return string;
        }

            // Use userBuf if given; else create one
        FastStringBuffer buf;
        if (userBuf == null) {
            buf = new FastStringBuffer();
        }
        else {
            buf = userBuf;
        }

        int pos = 0;
        int idx = -1;
        int end;

        String tag, result;

            // search for tags, i.e. words between our tag delimiter string
        while ((idx = string.indexOf(TagDelimiter, pos)) != -1) {

                // copy the text from our current position up to the tag
            buf.append(string.substring(pos, idx));

                // find the ending tag delimiter
            idx += TagDelimiter.length();
            end = string.indexOf(TagDelimiter, idx);
            if (end == -1) {
                break;
            }

                // get the tag string
            tag = string.substring(idx, end);

                // move our current position to just after the tag
            pos = end + TagDelimiter.length();

                // see if the tag is in the form of a method name
                // and a list of arguments, e.g. "foo(hello, bye)"
            String method = method(tag);
            List arguments = arguments(tag, args);

                // append the string returned by the tag handler
            result = handler.stringForTag(method, arguments);

                // just append the tag itself if the handler returns null
            buf.append((result == null) ? tag : result);
        }

            // copy the rest of the string to the buffer
        buf.append(string.substring(pos, string.length()));

        return buf.toString();
    }

    /**
        Given a tag string that in the form of a method with arguments,
        returns the portion of the tag that's the method name.
    */
    public String method (String tag)
    {
            // Look for the beginning parameter string
        int index;
        if ((index = tag.indexOf(BeginArgsChar)) != -1) {
                // Yes, we found the parameter
                // Return only the method part of it
            return(tag.substring(0, index).trim());
        }
        else {
            return(tag);
        }
    }

    /**
        Given a tag string that in the form of a method with arguments,
        returns an array containing the argument strings.
    */
    public static List arguments (String tag, List args)
    {
            // look for the arguments list character
        int beginIndex = tag.indexOf(BeginArgsChar);

            // short circuit: return null if there isn't an arguments list
        if (beginIndex < 0) {
            return null;
        }

            // move past the arguments list character
        beginIndex++;

            // reset arguments vector if we have one
        if (args != null) {
            args.clear();
        }

            // look for all the arguments
        int endIndex = beginIndex;
        while (endIndex >= 0) {
                // arguments are separated by commas or the end of the list
            if (((endIndex = tag.indexOf(ArgsSeparator, beginIndex)) != -1) ||
                ((endIndex = tag.indexOf(EndArgsChar, beginIndex)) != -1)) {

                    // create the arguments vector if null
                if (args == null) {
                    args = ListUtil.list();
                }

                    // Copy the argument into the list
                args.add(tag.substring(beginIndex, endIndex).trim());

                    // reset the begin index position
                beginIndex = endIndex + 1;
            }
        }

        return args;
    }

    /*-----------------------------------------------------------------------
        Protected Fields
      -----------------------------------------------------------------------*/

        // the tag handler, i.e. the code that will give us the string to
        // insert in place of a given tag from the string to process
    protected TagHandler handler;
}
