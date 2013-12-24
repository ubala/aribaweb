/*
    Copyright (c) 2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/http/multitab/RequestProcessor.java#1 $

    Responsible: fkolar
 */
package ariba.util.http.multitab;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 Helper callback interface that let us bridge different logic with our Ariba Servlet.
 Mainly used for pre-processing where we need to do some extra work before sending
 request to
 application
 */
public interface RequestProcessor
{

    /**
     Once the processing is ready we will go back and finish the execution in the on
     the servlet adaptor
     side.
     */
    public void processRequest (HttpServletRequest request,
                                HttpServletResponse response, boolean isGet)
            throws ServletException, IOException;

    /**
     This method let us skip the processRequest method call and call directly rending
     phase. Currently used when
     we want to throws an exception to the user.
     */
    public void delegateTooManyTabsError (HttpServletRequest servletRequest,
                                          HttpServletResponse servletResponse)
            throws IOException, ServletException;

    /**
     Identifies multiTab handler by name
     */
    public String multiTabHandlerName ();
}
