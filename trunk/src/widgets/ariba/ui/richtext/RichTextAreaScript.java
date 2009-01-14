/*
    Copyright (c) 1996-2008 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/ui/widgets/ariba/ui/richtext/RichTextAreaScript.java#9 $

    Responsible: kngan

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
