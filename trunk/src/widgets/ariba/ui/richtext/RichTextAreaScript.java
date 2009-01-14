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

    $Id: //ariba/platform/ui/widgets/ariba/ui/richtext/RichTextAreaScript.java#10 $
*/

package ariba.ui.richtext;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWXDebugResourceActions;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.util.core.StringUtil;

public class RichTextAreaScript extends AWComponent
{
    private static String EditorURL = null;
    private static boolean EditorIdsInit = false;
    private static String[] EditorIds = {
        "modules/InternetExplorer/InternetExplorer.js",     
        "modules/Gecko/Gecko.js", "modules/Gecko/paraHandlerBest.js",
        "modules/GetHtml/TransformInnerHTML.js",
        "modules/Dialogs/dialog.js", "modules/Dialogs/inline-dialog.js",
        "modules/FullScreen/full-screen.js", "modules/ColorPicker/ColorPicker.js"
    };
    private static final String XinhaCoreJS = "XinhaCore.js";
    private static final String XinhaCoreJSURL = "xinha/XinhaCore.js";
    private static final String XinhaStringJSURL = "rtstrings.js";

    public String _currentEditorId;

    protected void sleep()
    {
        _currentEditorId = null;
    }

    public String editorURL ()
    {
        if (EditorURL == null) {
            String XinhaCoreUrl = (((AWConcreteApplication)application()).allowsJavascriptUrls())
                ? urlForResourceNamed(XinhaCoreJSURL)
                : AWXDebugResourceActions.urlForResourceNamed(requestContext(), XinhaCoreJSURL);
            EditorURL = XinhaCoreUrl.substring(0, XinhaCoreUrl.lastIndexOf(XinhaCoreJS));
        }
        return EditorURL;
    }

    public String editorLangURL ()
    {
        String langUrl = (((AWConcreteApplication)application()).allowsJavascriptUrls())
            ? urlForResourceNamed(XinhaStringJSURL)
            : AWXDebugResourceActions.urlForResourceNamed(requestContext(), XinhaStringJSURL);
        return langUrl;
    }

    public String[] editorIds ()
    {
        if (!EditorIdsInit) {
            String editorURL = editorURL();
            for (int i = 0; i < EditorIds.length; i++) {
                EditorIds[i] = StringUtil.strcat(editorURL, EditorIds[i]);
            }
            EditorIdsInit = true;
        }
        return EditorIds;
    }

}
