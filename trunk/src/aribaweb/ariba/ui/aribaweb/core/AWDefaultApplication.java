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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWDefaultApplication.java#18 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWMultiLocaleResourceManager;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWBrand;
import ariba.ui.aribaweb.util.AWBrandManager;
import ariba.util.core.ResourceService;
import ariba.util.i18n.LocalizedJavaString;

import java.io.StringWriter;

/**
    This is provided as an application-level subclass of AWConcreteAppliction in
    lieu of the application's having its own subclass.  This is used in those
    situations where we do not have a standard awf application, but we're using
    the awf to do string rendering or parsing. (for example, the messaging framework)
*/
public class AWDefaultApplication extends AWConcreteApplication
{
    private String _loginUrl;
    private AWMergedStringLocalizer _localizer = new AWMergedStringLocalizer();

    public AWDefaultApplication ()
    {
        _loginUrl = "./";
    }

    public void init ()
    {
        // Note: must init *after* line above, because super will call our resourceUrl()
        super.init();

        // Todo!  Why were we calling "couldBeNull" -- we'll blow up downstream it this isn't initialized...
        ResourceService resourceService = ResourceService.getService(); // ResourceService.serviceCouldBeNull();
        if (resourceService != null) {
            resourceService.registerStringProcessor(_localizer);
        }
        LocalizedJavaString.registerLocalizer(_localizer);
    }

    public AWDefaultApplication (String loginUrl, String applicationName)
    {
        _loginUrl = loginUrl;
        setName(applicationName);
    }

    public String resourceURL ()
    {
            // not implemented
        return null;
    }

    public String resourceFilePath ()
    {
        // this is our default path inside of the wars and jars (and web server as well)
        return "docroot/";
    }

    protected String initAdaptorUrl ()
    {
        return _loginUrl;
    }

    protected String initAdaptorUrlSecure ()
    {
        return _loginUrl;
    }

    public String webserverDocumentRootPath ()
    {
        return "No webserverDocumentRootPath for AWDefaultApplication";
    }

    public String applicationUrl (AWRequest request)
    {
        return "No applicationUrl for AWDefaultApplication";
    }

    public AWResponseGenerating monitorStatsPage (AWRequestContext requestContext)
    {
        throw new AWGenericException(
            "AWDefaultApplication monitorStatsPage not supported.");
    }

    public AWMultiLocaleResourceManager createResourceManager ()
    {
        AWMultiLocaleResourceManager resourceManager =
            new AWMultiLocaleResourceManager() {};
        resourceManager.init();
        return resourceManager;
    }

    public AWRequest createRequest (Object request)
    {
        throw new AWGenericException("AWDefaultApplication createRequest not supported.");
    }

    public AWResponse createResponse ()
    {
        return new AWWriterResponse(new StringWriter());
    }

    //////////////////////////
    // Localization
    ///////////////////////////
    public AWStringLocalizer getStringLocalizer ()
    {
        return _localizer;
    }

    public boolean initIsRapidTurnaroundEnabled ()
    {
        return false;
    }

    public boolean initIsStatisticsGatheringEnabled ()
    {
        return false;
    }

    public String deploymentRootDirectory ()
    {
        return System.getProperty("user.dir");
    }

    public AWSession checkoutSessionId (String id)
    {
        return null;
    }

    public void checkinSession (AWSession session)
    {
    }

    public AWBrandManager getBrandManager ()
    {
        throw new AWGenericException(
            "AWDefaultApplication getBrandManager not supported");
    }
}
