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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/WidgetsDelegate.java#13 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWRequestContext;

public interface WidgetsDelegate
{
    /**
        Returns true if hint messages should be shown for the specified
        request.  The application will most likely consult some session state
        to determine this.  The return value is used by the HintMessage tag.

        @param requestContext The requestContext associated with the request
            we are generating the hint messages for (use this to get the session).

        @aribaapi ariba
    */
    public boolean hintMessagesVisible (AWRequestContext requestContext);

    /**
        Returns the url prefix that should be used for resources
        associated with this brand.

        @param brand The name of the brand
        @aribaapi ariba
    */
    public String getUrlPrefixForBrand (String brand);

    /**
        Returns the base directory for resources
        associated with this brand.

        @param brand The name of the brand
        @aribaapi ariba
    */
    public String getDirectoryForBrand (String brand);


    /**
     * Returns the help action handler for the given help key.
     * @param requestContext
     * @param helpKey
     * @aribaapi ariba
     */
    public ActionHandler getHelpActionHandler (AWRequestContext requestContext,
                                               String helpKey);
}
