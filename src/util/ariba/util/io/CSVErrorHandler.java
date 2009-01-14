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

    $Id: //ariba/platform/util/core/ariba/util/io/CSVErrorHandler.java#2 $
*/

package ariba.util.io;

/**
    Handles CSV format errors found while parsing a CSV record.
    
    @aribaapi ariba
*/
public interface CSVErrorHandler
{
    /**
        Handles CSV format errors found while parsing a CSV record.  Called by
        CSVReader when an error is encountered.  This method should never throw
        a RuntimeException because they will not be handled by CSVReader.

        @aribaapi ariba

        @param  errorCode   Error code
        @param  location    Location (filename, URL, or other source) of CSV data
                            being parsed
        @param  lineNumber  Line number where error was found
    */
    public void handleError (int errorCode, String location, int lineNumber);
}
