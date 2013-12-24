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

    $Id: //ariba/platform/util/core/ariba/util/http/multitab/NoMultiTabSupport.java#1 $

    Responsible: fkolar
 */
package ariba.util.http.multitab;

import ariba.util.core.StringUtil;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

/**
 This is dummy implementation for the multitab. We can see this as a empty shell that only
 bypasses the request to normal processing
 */
public class NoMultiTabSupport implements MultiTabSupport
{

    public NoMultiTabSupport ()
    {
    }

    @Override
    public String insertTabInUri (String untabbedUrl, int tabIndex, boolean isNotZero)
    {
        return untabbedUrl;
    }

    @Override
    public String stripTabFromUri (String uri)
    {
        return uri;
    }

    @Override
    public int maximumNumberOfTabs ()
    {
        return 1;
    }

    @Override
    public int uriToTabNumber (String uri, int defaultTab)
    {
        return 0;
    }

    // In this dummy implementation we do not have to do any magic. just return what we
    // get.
    @Override
    public String tabNumberToUri (String servletName, String applicationNameSuffix,
                                  int tabIndex, String uri)
    {
        return StringUtil.strcat(servletName, applicationNameSuffix);
    }

    @Override
    public boolean isMultiTabEnabled ()
    {
        return false;
    }

    @Override
    public void processRequest (MultiTabHandler.RequestInfo data,
                                MultiTabHandler multiTabHandlerCallback)
            throws IOException
    {
        multiTabHandlerCallback.handleDefault(data);
    }

    @Override
    public String defaultTabPrefix ()
    {
        return "";
    }

    @Override
    public MultiTabHandler initHandler (RequestProcessor requestProcessor,
                                        HttpServletRequest request)
            throws IOException
    {
        ByPassHandler byPassHandler = new ByPassHandler(requestProcessor, request);
        Instance.saveHandler(byPassHandler);
        return byPassHandler;
    }

    @Override
    public void registerHandlerClassForName (String name,
                                             Class<? extends MultiTabHandler> handler)
    {

    }

    @Override
    public MultiTabHandler handlerClassForName (String name)
    {
        return null;
    }

    public static class ByPassHandler extends AbstractMultiTabHandler
    {

        public ByPassHandler (RequestProcessor processor, HttpServletRequest request)
        {
            setRequestProcessor(processor);
            setServletRequest(request);
        }
    }
}
