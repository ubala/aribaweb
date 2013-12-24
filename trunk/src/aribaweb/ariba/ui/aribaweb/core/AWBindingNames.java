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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWBindingNames.java#33 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;

abstract public class AWBindingNames extends AWBaseObject
{
    public static boolean UseNamePrefixBinding = false;
    /**
     * @deprecated  Use semanticKeyBindingName()
     */
    public static final String namePrefix             = "namePrefix";
    public static final String awname                 = "awname";
    /**
         For internal use only
     */
    public static final String _awname                 = "_awname";

    public static String semanticKeyBindingName ()
    {
        return UseNamePrefixBinding ? namePrefix : awname;
    }

    public static final String count                  = "count";
    public static final String index                  = "index";
    public static final String value                  = "value";
    public static final String string                 = "string";
    public static final String tagName                = "tagName";
    public static final String editable               = "editable";
    public static final String elementName            = "elementName";
    public static final String elementId              = "elementId";
    public static final String senderId               = "senderId";
    public static final String isSender               = "isSender";
    public static final String invokeAction           = "invokeAction";
    public static final String isClosed               = "isClosed";
    public static final String omitTags               = "omitTags";
    public static final String emitTags               = "emitTags";
    public static final String formValuesKey          = "formValuesKey";
    public static final String formValues             = "formValues";
    public static final String formValue              = "formValue";
    public static final String take                   = "take";
    public static final String invoke                 = "invoke";
    public static final String append                 = "append";
    public static final String item                   = "item";
    public static final String list                   = "list";
    public static final String parent                 = "parent";
    public static final String name                   = "name";
    public static final String templateName           = "templateName";
    public static final String parentTemplateName     = "parentTemplateName";
    public static final String template               = "template";
    public static final String pageName               = "pageName";
    public static final String action                 = "action";
    public static final String exception              = "exception";
    public static final String start                  = "start";
    public static final String formatter              = "formatter";
    public static final String escape                 = "escape";
    public static final String escapeHtml             = "escapeHtml";
    public static final String selection              = "selection";
    public static final String selections             = "selections";
    public static final String condition              = "condition";
    public static final String ifTrue                 = "ifTrue";
    public static final String ifFalse                = "ifFalse";
    public static final String notEqualNull           = "notEqualNull";
    public static final String equalNull              = "equalNull";
    public static final String negate                 = "negate";
    public static final String displayString          = "displayString";
    public static final String hasClosingTag          = "hasClosingTag";
    public static final String htmlAttributes         = "htmlAttributes";
    public static final String isXml                  = "isXml";
    public static final String emptyStringValue       = "emptyStringValue";
    public static final String awcomponent            = "awcomponent";
    public static final String awcomponentName        = "awcomponentName";
    public static final String awcomponentReference   = "awcomponentReference";
    public static final String awcontent              = "awcontent";
    public static final String awcontentElement       = "awcontentElement";
    public static final String awbindingsDictionary   = "awbindingsDictionary";
    public static final String disabled               = "disabled";
    public static final String type                   = "type";
    public static final String filename               = "filename";
    public static final String src                    = "src";
    public static final String key                    = "key";
    public static final String comment                = "comment";
    public static final String enumeration            = "enumeration";
    public static final String iterator               = "iterator";
    public static final String scriptFile             = "scriptFile";
    public static final String scriptString           = "scriptString";
    public static final String language               = "language";
    public static final String otherBindings          = "otherBindings";
    public static final String border                 = "border";
    public static final String multiple               = "multiple";
    public static final String noSelectionString      = "noSelectionString";
    public static final String size                   = "size";
    public static final String maxLength              = "maxLength";
    public static final String state                  = "state";
    public static final String initState              = "initState";
    public static final String isExternal             = "isExternal";
    public static final String trueImageName          = "trueImageName";
    public static final String falseImageName         = "falseImageName";
    public static final String hilightColor           = "hilightColor";
    public static final String currentTemplateName    = "currentTemplateName";
    public static final String method                 = "method";
    public static final String width                  = "width";
    public static final String height                 = "height";
    public static final String inputStream            = "inputStream";
    public static final String bytes                  = "bytes";
    public static final String mimeType               = "mimeType";
    public static final String required               = "required";
    public static final String awdebug                = "awdebug";
    public static final String isOrdered              = "isOrdered";
    public static final String onClick                = "onClick";
    public static final String onChange               = "onChange";
    public static final String onSubmit               = "onSubmit";
    public static final String onLoad                 = "onLoad";
    public static final String onKeyPress             = "onKeyPress";
    public static final String checked                = "checked";
    public static final String background             = "background";
    public static final String label                  = "label";
    public static final String tip                    = "tip";
    public static final String showTip                = "showTip";
    public static final String isVisible              = "isVisible";
    public static final String initiallyVisible       = "initiallyVisible";
    public static final String fontFace               = "fontFace";
    public static final String fontSize               = "fontSize";
    public static final String fontColor              = "fontColor";
    public static final String bgcolor                = "bgcolor";
    public static final String isDepressed            = "isDepressed";
    public static final String isDisabled             = "isDisabled";
    public static final String allowsHilight          = "allowsHilight";
    public static final String useColor               = "useColor";
    public static final String fragmentIdentifier     = "fragmentIdentifier";
    public static final String isItemSelected         = "isItemSelected";
    public static final String submitForm             = "submitForm";
    public static final String target                 = "target";
    public static final String alt                    = "alt";
    public static final String scrollToVisible        = "scrollToVisible";
    public static final String classBinding           = "class";
    public static final String style                  = "style";
    public static final String id                     = "id";
    public static final String autoselect             = "autoselect";
    public static final String href                   = "href";
    public static final String url                    = "url";
    public static final String title                  = "title";
    public static final String itemClassName          = "itemClassName";
    public static final String keyClassName           = "keyClassName";
    public static final String valueClassName         = "valueClassName";
    public static final String isEqual                = "isEqual";
    public static final String isNotEqual             = "isNotEqual";
    public static final String isLessThan             = "isLessThan";
    public static final String isGreaterThan          = "isGreaterThan";
    public static final String isLessOrEqual          = "isLessOrEqual";
    public static final String isGreaterOrEqual       = "isGreaterOrEqual";
    public static final String isMeta                 = "isMeta";
    public static final String map                    = "map";
    public static final String sort                   = "sort";
    public static final String disableKeyPress        = "disableKeyPress";
    public static final String disableClick           = "disableClick";
    public static final String tabIndex               = "tabIndex";
    public static final String ignore                 = "ignore";
    public static final String isRefresh              = "isRefresh";
    public static final String isScope                = "isScope";
    public static final String alwaysRender           = "alwaysRender";
    public static final String dropType               = "dropType";
    public static final String isDragDropEnabled      = "isDragDropEnabled";
    public static final String isTrigger              = "isTrigger";
    public static final String interval               = "interval";
    public static final String pollInterval           = "pollInterval";
    public static final String requiresRefresh        = "requiresRefresh";
    public static final String newMode                = "newMode";
    public static final String forceRefreshOnChange   = "forceRefreshOnChange";
    public static final String forceDirectInclude     = "forceDirectInclude";
    public static final String synchronous            = "synchronous";
    public static final String bh                     = "bh";
    public static final String useId                  = "useId";
    public static final String contentType            = "contentType";
    public static final String behavior               = "behavior";
    public static final String visibles               = "visibles";
    public static final String sessionless            = "sessionless";
    public static final String focus                  = "focus";
    public static final String placeholder            = "placeholder";


    // ** These are not a binding names -- just a constants that need homes
    public static final String awstandalone           = "awstandalone";
    public static final String intType                = "int";
    public static final String booleanType            = "boolean";
    public static final String NumberType             = "Number";
    public static final String awtabinfo              = "awtabinfo";
    public static final String context                = "context";
    public static final String useBR                  = "useBR";
    public static final String useNbsp                = "useNbsp";
    public static final String scriptForceDirectInclude = "scriptForceDirectInclude";
}
