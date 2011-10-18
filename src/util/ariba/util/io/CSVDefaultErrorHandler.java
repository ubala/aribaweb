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

    $Id: //ariba/platform/util/core/ariba/util/io/CSVDefaultErrorHandler.java#3 $
*/

package ariba.util.io;

import ariba.util.core.Constants;
import ariba.util.log.Log;

/**
    Default implementation used by CSVReader for handling CSV format errors found
    while parsing a CSV record.
    
    @aribaapi private
*/
public class CSVDefaultErrorHandler
    implements CSVErrorHandler
{
    /**
         Handles CSV format errors found while parsing a CSV record.  Called by
         CSVReader when an error is encountered.

         @aribaapi private

         @param  errorCode   Error code
         @param  location    Location (filename, URL, or other source) of CSV data
         being parsed
         @param  lineNumber  Line number where error was found
    */
    public void handleError (int errorCode, String location, int lineNumber)
    {
        switch (errorCode) {
            case CSVReader.ErrorMissingComma:
                    // "end of field not followed by newline or comma in ""{0}"", line {1}"
                Log.util.info(2793, location, lineNumber);
                break;
            case CSVReader.ErrorUnbalancedQuotes:
                    // "Unbalanced quotes in ""{0}"", line {1}"
                Log.util.info(8832, location, lineNumber);
                break;
            case CSVReader.ErrorIllegalCharacterOrByteSequence:
                    // "Exception during reading line {0} of {1}, perhaps the
                    // wrong encoding is used to read the resource"
                Log.util.info(8379, location, lineNumber);
                break;
            default:
                Object[] args = {
                    Constants.getInteger(errorCode),
                    location,
                    Constants.getInteger(lineNumber)
                };
                    // "Unknown CSV format error code {0} in ""{1}"", line {2}"
                Log.util.warning(8831, args);
                break;
        }
    }
}
