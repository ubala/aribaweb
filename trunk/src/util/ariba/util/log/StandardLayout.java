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

    $Id: //ariba/platform/util/core/ariba/util/log/StandardLayout.java#6 $
*/

package ariba.util.log;

import ariba.util.core.Version;
import ariba.util.core.Fmt;
import ariba.util.core.Date;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.PatternParser;

/**
    The standard layout for our logging component.
    @aribaapi ariba
*/
public class StandardLayout extends PatternLayout
{
    protected static String DefaultLogPattern =
        "%d{EEE MMM dd HH:mm:ss zzz yyyy} (%T) (%c:%p) %i: %m%n%s";
    /** 
        Instantiates an instance of this class with the default log
        pattern.
        @aribaapi ariba
    */
    public StandardLayout ()
    {
        super(DefaultLogPattern);
    }

    /** 
        Instantiates an instance of this class with the specified pattern
        @param pattern the pattern string to use for this layout.
        @aribaapi ariba
    */
    public StandardLayout (String pattern)
    {
        super(pattern);
    }
       
    /**
        Returns the header string when we start logging.
        @return the header string
        @aribaapi ariba
    */
    public String getHeader ()
    {
        String header = 
            Fmt.S("[%s] Start Log.  " +  // OK
                  "Software Version %s.  %s%s",
                  new Date().toString(),
                  Version.versionImage,
                  Version.copyrightImage,
                  System.getProperty("line.separator"));
        return header;
    }

    /* Note: why is this public? The super class's method is protected */
    public PatternParser createPatternParser (String pattern)
    {
        PatternParser result;
        if (pattern == null) {
            result = new StandardPatternParser(DefaultLogPattern);
        }
        else {
            result = new StandardPatternParser(pattern);
        }
      return result;
      
    }
    
    
    
}
