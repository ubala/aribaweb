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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWImage.java#8 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWElement;
import ariba.ui.aribaweb.util.AWUtil;
import java.util.Map;

/**
    subclassed by ariba/common/template/customer/AribaImage

    @aribaapi private
*/

public class AWImage extends AWBaseImage
{
    private static String[] StaticImageNames = {};

    // ** Thread Safety Considerations: see AWComponent.

    static {
        registerStaticImageName("cleardot.gif");
        registerStaticImageName("_cleardot.gif");
        registerStaticImageName("blackdot.gif");
    }

    public static void registerStaticImageName (String imageName)
    {
        StaticImageNames = (String[])AWUtil.addElement(StaticImageNames, imageName);
    }

    public AWElement determineInstance (String elementName, Map bindingsHashtable, String templateName, int lineNumber)
    {
        AWElement imageElement = null;
        boolean isStaticImageName = false;
        AWBinding filenameBinding = (AWBinding)bindingsHashtable.get(BindingNames.filename);
        if ((filenameBinding != null) && (filenameBinding.isConstantValue())) {
            String filename = filenameBinding.stringValue(null);
            if (AWUtil.contains(StaticImageNames, filename)) {
                isStaticImageName = true;
            }
        }
        if (!isStaticImageName || AWBinding.hasDynamicBindings(bindingsHashtable)) {
            imageElement = super.determineInstance(elementName, bindingsHashtable, templateName, lineNumber);
        }
        else {
            AWStaticImage staticImage = new AWStaticImage();
            imageElement = staticImage.determineInstance(elementName, bindingsHashtable, templateName, lineNumber);
        }
        return imageElement;
    }
}
