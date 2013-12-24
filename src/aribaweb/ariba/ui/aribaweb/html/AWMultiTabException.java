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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWMultiTabException.java#5 $

    Responsible: ahebert
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWHandleExceptionPage;
import ariba.util.http.multitab.MultiTabSupport;

/*
 * This is simple class associated to a awl file which creates a exception page
 * when user has reached the maximum of tab allowed by the application.
 * It specifies the user that he can not go anywhere with this tab
 * and has to close it.
 */
public class AWMultiTabException extends AWComponent
        implements AWHandleExceptionPage
{
    public static final String Name = "AWMultiTabException";

    // implementation of the interface AWHandleExceptionPage's
    // setException method
    public void setException (Throwable exception)
    {
    }

    public String name ()
    {
        return Name;
    }

    public String getMultiTabExceptionPageTitle ()
    {
        return localizedJavaString(Name, 1,
                "Tab Limit",
                AWConcreteApplication.SharedInstance.
                        resourceManager(this.preferredLocale()));
    }

    public String getMultiTabExceptionPageMessage ()
    {
        return localizedJavaString(Name, 2,
                "Too Many Tabs or Windows Opened",
                AWConcreteApplication.SharedInstance.
                        resourceManager(this.preferredLocale()));
    }

    public String getMultiTabExceptionMessage ()
    {
        int maxTabs = MultiTabSupport.Instance.get().maximumNumberOfTabs();
        return localizedJavaString(Name, 3,
                "You have already opened " + maxTabs +
                  " tabs or windows, which is the maximum allowed by" +
                  " this application.",
                AWConcreteApplication.SharedInstance.
                        resourceManager(this.preferredLocale()));
    }

    public String getMultiTabExceptionReportMessage ()
    {
        return localizedJavaString(Name, 4,
                "Close one of the other tabs or windows and try again.",
                AWConcreteApplication.SharedInstance.
                        resourceManager(this.preferredLocale()));
    }
}