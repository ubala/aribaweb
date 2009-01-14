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

    $Id: //ariba/platform/util/core/ariba/util/core/ReferenceHandler.java#5 $
*/

package ariba.util.core;

import ariba.util.log.Log;
import java.util.Map;

/**
    Handles paramaters via indirect references. 

    @aribaapi ariba
*/
public class ReferenceHandler
{
    /**
        This class knows how to parse the input String.
    */
    private ReferenceSyntaxParser parser;

    /**
        Map of handlers. The value of each key is an
        instance of ReferenceReader class.
    */
    private Map handlers;
    
    /**
        Handles the interpretation of a local aliassss. The
        default implementation is to simply return the string.
        Subclass should override this method.

        @param str the string to interpret.

        @aribaapi ariba
    */
    protected String handleLocalAlias (String str)
    {
        return str;
    }
      
    /**
        Contructs a ReferenceHandler object.

        @param parser the class that know how to parse the input string.
        cannot be null.
        @param handlers a map of handlers. The key specifies the
        an individual handler, the value is an instance of ReferenceReader
        that knows how to read the data. This map must not be null.

        @aribaapi ariba
    */
    public ReferenceHandler (ReferenceSyntaxParser parser,
                             Map handlers)
    {
        Assert.that(parser != null,
                    "cannot instantiate class with a null syntax parser");
        Assert.that(handlers != null,
                    "cannot instantiate class with a null handlers");
        this.handlers = handlers;
        this.parser = parser;
    }

    /**
        Adds a handler for a specified  remote reference 
        @param key the key of the handler to be added. This key specifies the
        remote reference. Must not be null.
        @param handler the handler to be added. must not be null.
        @return true if the addition is successful. false if the key already
        exists, in which case, the handler is not added.
    */
    public boolean addHandler (String key,
                               ReferenceReader handler)
    {
        if (!handlers.containsKey(key)) {
            handlers.put(key, handler);
            Log.paramReference.debug("added handler for key '%s'", key);
            return true;
        }
        Log.paramReference.debug("handler for key '%s' already exists, not adding...",
                                 key);
        return false;
    }

    /**
        @param str input String to interpret
        
        @aribaapi ariba
    */
    public String interpret (String str)
    {
        int[] interval = null;
        String tempStr = str;
        FastStringBuffer interpretedString = new FastStringBuffer();
        
            // print a blank line for easy reading of debug output
        Log.paramReference.debug("");
        Log.paramReference.debug("ReferenceHandler interpreting input string: %s",
                                 str);

        if (StringUtil.nullOrEmptyOrBlankString(str)) {
            return str;
        }
        while ((interval = parser.getNextReference(tempStr)) != null) {
            int start = interval[0];
            int onePastEnd = interval[1];

                // invariant:
                // 1) the substring of tempStr from 0 to start-1 is
                // the string that does not contain any reference delimiters
                // 2) the substring of tempStr from start to onePastEnd-1
                // contains the reference delimiters and need to be interpreted.
                // Therefore it is passed to interpretSubString.
            interpretedString.appendStringRange(tempStr, 0, start);
                //            interpretedString.append(tempStr.substring(0, start));
            interpretedString.append(
                interpretSubString(tempStr.substring(start, onePastEnd)));
            tempStr = tempStr.substring(onePastEnd);
        }

        /* handle the last substring */
        if (!StringUtil.nullOrEmptyString(tempStr)) {
            interpretedString.append(tempStr);
        }
        Log.paramReference.debug("ReferenceHandler returning interpreted string: %s",
                                 interpretedString);
        return interpretedString.toString();
    }

    /**
        Resolves reference in the specified input String. The reference
        is interpreted and resolved by the various handlers.

        IMPLEMENTATION DETAILS: note that at this point, we don't handle
        nested alias. That is, the indirect reference can be at most one
        level deep.
        
        @param str string to be interpreted, must not be null.

        @return the resolved String if it can be resolved. If not (because it
        is a local reference), then the original str is returned.
    */
    private String interpretSubString (String str)
    {
        String[] tokens = parser.getReferenceAndAlias(str);
        Log.paramReference.debug("interpretSubString: remoteRef = '%s', alias='%s'",
                                 tokens[0], tokens[1]);
        if (tokens[0] == null) {
            Log.paramReference.debug("handling local reference for input '%s'", str);
            return handleLocalAlias(tokens[1]);
        }

        ReferenceReader handler = (ReferenceReader)handlers.get(tokens[0]);
        if (handler != null) {
            try {
                return (String)handler.read(tokens[1]);
            }
            catch (ReferenceReaderException e) {
                Assert.that(false, "error interpreting string: %s", e);
            }
        }
        Log.paramReference.warning(7038, tokens[0]);
        return str;
    }
}
