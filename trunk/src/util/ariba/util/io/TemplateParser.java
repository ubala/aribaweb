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

    $Id: //ariba/platform/util/core/ariba/util/io/TemplateParser.java#7 $
*/

package ariba.util.io;

import ariba.util.core.IOUtil;
import ariba.util.core.SystemUtil;
import ariba.util.core.ResourceService;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
    @aribaapi private
*/
public class TemplateParser extends TaggedStringProcessor
{

    private static final String StringTable = "ariba.util.core";

    /*-----------------------------------------------------------------------
        Private Constants
      -----------------------------------------------------------------------*/

        // error messages
    private static final String FileErrorMsgKey    = "FileErrorMsg";
    private static final String IOErrorMsgKey      = "IOErrorMsg";


    /*-----------------------------------------------------------------------
        Constructors
      -----------------------------------------------------------------------*/

    /** Creates a new TemplateParser with the given tag <B>handler</B>. */
    public TemplateParser (TagHandler handler)
    {
        super(handler);
    }


    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/

    /**
        Parses the given <B>template</B> file, replacing any tags found with
        the string returned by our tag handler's processTag() method.  This
        method may print an error message to the output stream if an unexpected
        error occurs.
    */
    public void parseTemplate (String template, PrintWriter out)
    {
            // open the template file, make sure it exists and is readable
        File file = new File(template);
        if (!file.isFile() || !file.canRead()) {
            signalError(out, ResourceService.getString(StringTable, FileErrorMsgKey));
            return;
        }

            // create the tagged string processor
        TaggedStringProcessor tsp = new TaggedStringProcessor(handler);

            // create the appropriate input stream from the file
        try {
            BufferedReader input =
                IOUtil.bufferedReader(file,
                                    SystemUtil.getFileEncoding());

                // process each line of the file
            String line;
            while ((line = input.readLine()) != null) {
                out.println(tsp.processString(line));
            }
            out.flush();
            input.close();
        }
        catch (IOException e) {
            signalError(out, ResourceService.getString(StringTable, IOErrorMsgKey));
            return;
        }
    }


    /*-----------------------------------------------------------------------
        Protected Methods
      -----------------------------------------------------------------------*/

    /** Writes an error report to the given stream for the given error. */
    protected void signalError (PrintWriter out, String error)
    {
            // Write out the error message
        out.println(error);
    }
}
