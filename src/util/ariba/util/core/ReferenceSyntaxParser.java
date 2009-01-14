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

    $Id: //ariba/platform/util/core/ariba/util/core/ReferenceSyntaxParser.java#5 $
*/
package ariba.util.core;

import ariba.util.log.Log;

/**
    This class provides methods to parse Strings that may contain
    remote and/or local references.

    The syntax is as follows: $<remoteReference^alias>$, where

    'remoteReference' specifies a String that locates the remote
    reference. This class does not interpret this String. It
    merely parse a given input string for this substring and hands
    it back to its caller for processing. It is illegal for this
    reference string to contain the '^' scope delimiter or the
    substring "$<" or ">$".

    'alias' specifies a key whose value can be obtained from the
    resource specified 'remoteReference'. Again, this class does
    not interpret this string. It is illegal for this reference
    string to contain the '^' scope delimiter or the substring "$<"
    or ">$".

    The carat (^) scope delimiter is optional. If absent, this
    means there is no remote reference, and the substring
    enclosed within the $<...>$ delimiters is a local alias.
    Again, it is up to the caller to interpret the local alias.

    Note that nested references are not supported.
    
    @aribaapi ariba
*/
public class ReferenceSyntaxParser
{
    /**
        Indicates the beginning of the reference token.
        @aribaapi ariba
    */
    public static final String BeginDelimiter = "$<";

    /**
        Indicates the end of the reference token.
        @aribaapi ariba
    */
    public static final String EndDelimiter = ">$";

    /**
        Separates the remote reference from the alias key
        Would like to use colon (':') but that is too popular
        Don't want to confuse those tokenizers that tokenize
        with the colon as delimiters.

        @aribaapi ariba
    */
    public static final char RemoteRefDelimiter = '^';

    /**
        Separates the remote reference from the alias key,
        represented as a String

        Would like to use colon (':') but that is too popular
        Don't want to confuse those tokenizers that tokenize
        with the colon as delimiters.

        @aribaapi ariba
    */
    public static final String RemoteRefDelimiterString = "^";

    /**
        Empty Constructor to instantiate this class

        @aribaapi ariba
    */
    public ReferenceSyntaxParser ()
    {
    }

    /**
        Returns the interval of the specified string that denotes
        a substring that begins and ends with the specified
        delimiters that this class understands.
        
        @param str the input string that may contain 0 or more
        subsequences that begin and end with the specified delimiters
        that this class understands. If it is null or blank or empty,
        just return null.
        
        @return an int array (i, j) of 2 integers. The first element
        points to the position in the given str where the beginning
        delmiter starts, in this case, str[i] == '$', str[i+1] ==
        '<'. The second elements points to the position one past the
        first occurrence of the end delimiter, in this case, str[j-2]
        = '>', str[j-1] = '$'. Clearly, i > j. Returns <b>null</b> if
        no such interval can be found.

        Examples:
        getNextReference("noReference") return null
        getNextReference(">$xx$<alias>$yy") return (4, 13)
        getNextReference("xx$<alias>$yy") return (2, 11)
        getNextReference("x$<missingEndDelimiter") returns null
        getNextReference("xx$<aa$<bbb>$yy") return (2, 13)
        getNextReference("$<>$") return (0,4), note that in this case
          it is up to the caller to handler the empty string contained
          by the delimiters.

        @aribaapi ariba
    */
    public int[] getNextReference (String str)
    {
        Log.paramReference.debug("getNextReference for input String: %s",
                                 str);
        if (StringUtil.nullOrEmptyOrBlankString(str)) {
            return null;
        }
        int beginIndex = str.indexOf(BeginDelimiter);
        if (beginIndex == -1) {
            return null;
        }
        /* skip the length of the delimiter */
        int searchIndex = beginIndex + BeginDelimiter.length();
        int endIndex = str.indexOf(EndDelimiter, searchIndex);
        if (endIndex == -1) {
            return null;
        }

        /* cook up the interval to return. Note that the
            we add back the length of the end delimiter
            so that the substring specified by the interval
            contains the delimiters.
        */
        int[] interval = new int[2];
        interval[0] = beginIndex;
        interval[1] = endIndex + EndDelimiter.length();
        Log.paramReference.debug("getNextReference returning interval (%s, %s)",
                                 interval[0], interval[1]);
        return interval;
    }

    /**
        Return a array of String (of at most 2 elements) that specifies the
        (optional) remote reference, and the alias key.

        @param strWithDelimiters is a string that begins with
        BeginDelimiter and ends with EndDelimiter.

        @return string array of 2 elements (all stripped of any delimiters):
        Element 0: specifies the id to locate the reference resource. Note that
        this can be null, which means that the next element (see below) refers
        to a local alias.
        Element 1: specifies the alias to be resolved by the reference specified
        by element 0.

        Note that the elements can contain empty or blank Strings. It is up to the
        caller to decide if the contents of the String elements make sense.

        @aribaapi ariba
    */
    public String[] getReferenceAndAlias (String strWithDelimiters)
    {
        Log.paramReference.debug("getReferenceAndAlias input String: %s",
                                 strWithDelimiters);
        Assert.that(strWithDelimiters != null &&
                    strWithDelimiters.startsWith(BeginDelimiter) &&
                    strWithDelimiters.endsWith(EndDelimiter),
                    "invalid input string '%s'", strWithDelimiters);
        String[] refAndAlias = new String[2];
        String strippedStr =
            strWithDelimiters.substring(
                BeginDelimiter.length(),
                strWithDelimiters.length() - BeginDelimiter.length());

            // get the reference id
        int index = strippedStr.indexOf(RemoteRefDelimiter);
        if (index == -1) {
            refAndAlias[0] = null;
            refAndAlias[1] = strippedStr;
        }
        else {
            refAndAlias[0] = strippedStr.substring(0, index);
            refAndAlias[1] = strippedStr.substring(index+1);

                // make sure there is only one occurrence of RemoteRefDelimiter ('^')
            index = strippedStr.indexOf(RemoteRefDelimiter, index+1);
            Assert.that(index == -1,
                        "input String '%s' cannot contain " +
                        "multiple reference delimiters ('%s')",
                        strippedStr, RemoteRefDelimiterString);
        }
        Log.paramReference.debug("getReferenceAndAlias returning array (%s, %s)",
                                 refAndAlias[0], refAndAlias[1]);
        return refAndAlias;
    }

    /**
        Creates a string with the correct syntax understood by this
        class to specify a reference to a given reference/alias pair.
        This created String can later be passed back as arguments
        to the getNextReference or getReferenceAndAlias methods for
        processing by possibly another component. 

        @param reference the reference. Cannot contain the
        carat ('^') character, or the ">$" or "$>" substrings. A
        value of null designates local reference.
        @param alias the alias, cannot be null. Cannot contain the
        carat ('^') character, or the ">$" or "$>" substrings.
        @return a reference String of the right syntax understood by
        this class.
        @aribaapi ariba
    */
    public static String makeReferenceString (String reference,
                                              String alias)
    {
        if (reference != null) {
            checkReferenceSyntax(reference);
        }
        checkAliasSyntax(alias);

        if (reference == null) {
            return StringUtil.strcat(BeginDelimiter,
                                     alias,
                                     EndDelimiter);
        }

        return StringUtil.strcat(BeginDelimiter,
                                 reference,
                                 RemoteRefDelimiterString,
                                 alias,
                                 EndDelimiter);
    }
                                               
    /**
        Checks the syntax of the given alias String for correct
        syntax. Assert if the syntax is wrong.
        @param the given alias String, must not be null.
    */
    private static void checkAliasSyntax (String alias)
    {
        checkSyntax(alias);
    }

    /**
        Checks the syntax of the given reference for correct
        syntax. Assert if the syntax is wrong.
        @param the given reference String, must not be null.
    */
    private static void checkReferenceSyntax (String reference)
    {
        checkSyntax(reference);
    }

    /**
        check the syntax of the given String for correct syntax
        understood by this class. See the description for this
        class for the correct syntax.
        Assert if the syntax is incorrect.
        @param str the given input String whose syntax is to be
        checked.
    */
    private static void checkSyntax (String str)
    {
        Assert.that(str != null, "input string cannot be null");
        Assert.that(str.indexOf(RemoteRefDelimiterString) == -1,
                    "input string %s cannot contain '%s'",
                    str, RemoteRefDelimiterString);
        Assert.that(!StringUtil.contains(str, BeginDelimiter),
                    "input string '%s' cannot contain '%s'",
                    str, BeginDelimiter);
        Assert.that(!StringUtil.contains(str, EndDelimiter),
                    "input string '%s' cannot contain '%s'",
                    str, EndDelimiter);
    }
}
