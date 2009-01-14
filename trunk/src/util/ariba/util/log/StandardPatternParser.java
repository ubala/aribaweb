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

    $Id: //ariba/platform/util/core/ariba/util/log/StandardPatternParser.java#6 $
*/

package ariba.util.log;

import ariba.util.core.Fmt;
import org.apache.log4j.helpers.FormattingInfo;
import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.helpers.PatternParser;
import org.apache.log4j.helpers.LogLog;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
    This class parses our standard conversion patterns
    @aribaapi ariba
*/
public class StandardPatternParser extends PatternParser 

{

    static final char CUSTOMTAG_CHAR  = 'T';
    static final char MSGID_CHAR      = 'i';
    static final char DEBUGSTATE_CHAR = 's';
        // Override PatternParser's handling of the date conversion option
    static final char DATE_CHAR = 'd';

    /**
        Instantiates an instance of this class with the specific pattern
        @param pattern the specified pattern
    */
    public StandardPatternParser (String pattern)
    {
        super(pattern);
    }
    
    public void finalizeConverter (char formatChar)
    {
        PatternConverter pc = null;
        
        switch(formatChar)
        {
          case CUSTOMTAG_CHAR:
            pc = new CustomTagPatternConverter(formattingInfo);
            currentLiteral.setLength(0);
            addConverter(pc);
            return;
          case MSGID_CHAR:
            pc = new MessageIdPatternConverter(formattingInfo);
            currentLiteral.setLength(0);
            addConverter(pc);
            return;
          case DEBUGSTATE_CHAR:
            pc = new DebugStatePatternConverter(formattingInfo);
            currentLiteral.setLength(0);
            addConverter(pc);
            return;
                // Override PatternParser's handling of 'd' so we can force
                // the date to be formatted in US English.
          case DATE_CHAR:
            DateFormat df;
            String dOpt = extractOption();
            if (dOpt != null) {
                String dateFormatStr = dOpt;

                try {
                    df = new SimpleDateFormat(dateFormatStr, Locale.US);
                }
                catch (IllegalArgumentException e) {
                    break;
                }
                pc = new DatePatternConverter(formattingInfo, df);
                currentLiteral.setLength(0);
                addConverter(pc);
                return;
            }
        }
            // Use PatternParser method if we had an error processing the case
            // or if it is one of the cases we don't handle.
        super.finalizeConverter(formatChar);
    }

    abstract private static class StandardPatternConverter
        extends PatternConverter 
    {
        StandardPatternConverter (FormattingInfo formattingInfo)
        {
            super(formattingInfo);     
        }
        
        public String convert (org.apache.log4j.spi.LoggingEvent event)
        {
            String result = null;
            
            LoggingEvent appEvent = null;
            
            if (event instanceof LoggingEvent) {
                appEvent = (LoggingEvent)event;
                result = convert(appEvent);
            }
            else {
                result = "unknown";
            }

            return result;
        }
        
        public abstract String convert (LoggingEvent event);
    }


    private static class DatePatternConverter extends StandardPatternConverter {
        private DateFormat df;
        private Date date;

        DatePatternConverter (FormattingInfo formattingInfo, DateFormat df) {
            super(formattingInfo);
            date = new Date();
            this.df = df;
        }

        public String convert (LoggingEvent event)
        {
            date.setTime(event.timeStamp);
            String converted = null;
            try {
                converted = df.format(date);
            }
            catch (NullPointerException ex) {
                LogLog.error("Error occurred while converting date--Date null.",
                    ex);
            }
            return converted;
        }
    }

    
    
    private static class CustomTagPatternConverter
        extends StandardPatternConverter
    {
        
        CustomTagPatternConverter (FormattingInfo formatInfo)
        {
            super(formatInfo);
        }
        
        public String convert (LoggingEvent event)
        {
            return event.getCustomTag();
        }
        
    }

    private static class MessageIdPatternConverter
        extends StandardPatternConverter
    {
        
        MessageIdPatternConverter (FormattingInfo formatInfo)
        {
            super(formatInfo);
        }
        
        public String convert (LoggingEvent event)
        {
            String id = event.getMessageId();
            return (id != null) ?  Fmt.S("[ID%s]", id) : null;
        }
        
    }

    private static class DebugStatePatternConverter
        extends StandardPatternConverter
    {
        
        DebugStatePatternConverter (FormattingInfo formatInfo)
        {
            super(formatInfo);
        }
        
        public String convert (LoggingEvent event)
        {
            String state = event.getCallerThreadDebugState();
            return (state != null) ?  Fmt.S("%s\n", state) : null;
        }
        
    }
}
