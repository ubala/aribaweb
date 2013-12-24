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

    $Id: //ariba/platform/util/core/ariba/util/http/multitab/AbstractMultiTabHandler.java#1 $

    Responsible: fkolar
*/
package ariba.util.http.multitab;

import ariba.util.core.ResourceService;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractMultiTabHandler implements MultiTabHandler
{
    protected RequestProcessor requestProcessor;
    protected HttpServletRequest currentRequest;


    @Override
    public void initialize ()
    {
    }

    @Override
    public void setServletRequest (HttpServletRequest request)
    {
        this.currentRequest = request;
    }


    @Override
    public void setRequestProcessor (RequestProcessor processor)
    {
        this.requestProcessor = processor;
    }

    @Override
    public void handleDefault (RequestInfo data) throws IOException
    {
        HttpServletResponse response = (HttpServletResponse)data.getResponse();
        HttpServletRequest request = (HttpServletRequest)data.getRequest();

        // convert to a RuntimeException, so TabManager functions doesn't
        // need to throw ServletException
        try {
            requestProcessor.processRequest(request, response,
                    "GET".equals(request.getMethod()));
        }
        catch (ServletException e) {
            throw new MultiTabException(e);
        }
    }

    @Override
    public boolean isTabEnabled (RequestInfo data)
    {
        return false;
    }

    @Override
    public void handleNewTab (RequestInfo data) throws IOException
    {
    }

    @Override
    public void handleExistingTab (RequestInfo data) throws IOException
    {
    }

    @Override
    public String mungeRedirectUrl (String redirectUrl)
    {
        return redirectUrl;
    }

    @Override
    public void handleTooManyTabs (RequestInfo data) throws IOException
    {
        HttpServletResponse response = (HttpServletResponse)data.getResponse();
        response.setContentType("text/html");
        String responseStr = "<!DOCTYPE html><html><head>" +
                "<meta charset=\"utf-8\" />" +
                "<title>Inspector</title></head>";
        responseStr += "<body>";
        responseStr += ResourceService.getString(StringTable,
                MultiTabExceptionMessageKey);
        responseStr += "<br>";
        responseStr += ResourceService.getString(StringTable,
                MultiTabExceptionReportMessageKey);
        responseStr += "</body>";
        responseStr += "</html>";
        response.getOutputStream().write(responseStr.getBytes());
    }
}
