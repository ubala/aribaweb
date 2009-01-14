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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/AWXDownloadFileAction.java#6 $
*/
package ariba.ui.demoshell;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;

import java.io.File;

import ariba.ui.aribaweb.core.AWRedirect;
import ariba.util.core.URLUtil;
import ariba.ui.aribaweb.util.AWUtil;

public class AWXDownloadFileAction extends AWComponent
{
    protected static final String RCKey = "AWXDownloadFileActionKey";
    protected boolean _openInNewWindow;

    public void awake ()
    {
        _openInNewWindow = booleanValueForBinding("openInNewWindow");
    }

    public static String urlForFile (File f)
    {
        String url = null;
        try {
            if ((f != null) && f.getName().endsWith(".urlJumper")) {
                // read file to extract Url
                url = AWUtil.stringWithContentsOfFile(f, false);
            } else {
                url = AWXHTMLComponentFactory.sharedInstance().docrootRelativeUrlForFile(f);
            }
        } catch (Exception e) {
            url = URLUtil.urlAbsolute(f).toString();
        }
        return url;
    }

    public AWResponseGenerating downloadFile ()
    {
        File file = (File)valueForBinding("file");
        String url = urlForFile(file);
        if (_openInNewWindow) {
            // cycle page, with javascript activated
            requestContext().put(RCKey, url);
            return null;
        }
        AWRedirect redirect = (AWRedirect)pageWithName(AWRedirect.class.getName());
        redirect.setUrl(url);
        return redirect;
    }

    public boolean openNewWindow ()
    {
        return _openInNewWindow && (requestContext().get(RCKey) != null);
    }

    public String openWindowUrl ()
    {
        String url = (String)requestContext().get(RCKey);
        requestContext().put(RCKey, null);
        return url;
    }
}
