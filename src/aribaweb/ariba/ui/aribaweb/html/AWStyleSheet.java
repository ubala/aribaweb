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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWStyleSheet.java#6 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWXDebugResourceActions;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWDirectAction;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWBrand;
import ariba.util.core.Assert;

public final class AWStyleSheet extends AWComponent
{
    private AWResource _styleSheetResource;

    protected void sleep ()
    {
        _styleSheetResource = null;
    }

    private String styleSheetName ()
    {
        String filename = (String)valueForBinding(AWBindingNames.filename);
        if (filename == null) {
            filename = "StyleSheet.css";
        }
        return filename;
    }

    public AWResource styleSheetResource ()
    {
        if (_styleSheetResource == null) {
            _styleSheetResource = (AWResource)valueForBinding("resource");
            if (_styleSheetResource == null) {
                _styleSheetResource =
                    resourceManager().resourceNamed(styleSheetName());
            }
        }
        return _styleSheetResource;
    }

    public String styleSheetResourceUrl ()
    {
        return urlForResourceNamed(styleSheetResource().name(), false, true);
    }
    
    public String directConnectResourceUrl ()
    {
        String url = null;
        AWResource resource = styleSheetResource();
        Assert.that(resource != null, "Stylesheet resource %s not found",
                    styleSheetName());

        AWRequestContext requestContext = requestContext();
        AWConcreteApplication application =
            (AWConcreteApplication)AWConcreteApplication.sharedInstance();

        AWBrand brand = application().getBrand(requestContext);
        if (application.allowBrandingImages() && brand != null) {
            url = AWDirectAction.brandUrlForResourceNamed(requestContext,
                                                     resource.name(),
                                                     brand);
        }
        else {
            url =
                AWXDebugResourceActions.urlForResourceNamed(
                    requestContext, resource.name());
        }
        return url;
    }
}
