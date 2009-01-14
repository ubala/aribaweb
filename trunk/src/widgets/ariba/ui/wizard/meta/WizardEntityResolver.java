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

    $Id: //ariba/platform/ui/widgets/ariba/ui/wizard/meta/WizardEntityResolver.java#3 $
*/
package ariba.ui.wizard.meta;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.File;

import ariba.util.core.StringUtil;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.widgets.XMLUtil;

public class WizardEntityResolver extends XMLUtil.DefaultResolver
{
    AWResourceManager _resourceManager;

    public WizardEntityResolver (AWResourceManager resourceManager)
    {
        _resourceManager = resourceManager;
    }

    public InputSource resolveEntity(String publicId,
                                     String systemId)
            throws SAXException, IOException
    {
        if (systemId == null) {
            return null;
        }

        String shortSystemId = null;
        int protocolSpliceIndex = systemId.indexOf(":");
        String protocol = "";
        if (protocolSpliceIndex != -1 ) {
            protocol = systemId.substring(0, protocolSpliceIndex);
            if (protocol.equalsIgnoreCase("file") || protocol.equalsIgnoreCase("jar")) {
                int index = systemId.lastIndexOf("/");
                shortSystemId = systemId.substring(index + 1);
            }
        }
        else {
            File file = new File(systemId).getCanonicalFile();
            shortSystemId = file.getName();
        }

        if (shortSystemId != null &&
                (shortSystemId.equals("Frame.dtd") ||
                shortSystemId.equals("Wizard.dtd") ||
                shortSystemId.equals("FrameExt.dtd") ||
                shortSystemId.equals("WizardExt.dtd"))) {
            AWResource resource = _resourceManager.resourceNamed(
                    StringUtil.strcat("ariba/resource/global/dtd/", shortSystemId));
            if (resource != null) {
                String fullUrl = resource.fullUrl();
                return new InputSource(fullUrl);
            }
        }
        return super.resolveEntity(publicId, systemId);
    }
}
