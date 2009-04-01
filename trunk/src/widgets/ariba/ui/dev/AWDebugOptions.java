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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWDebugOptions.java#18 $
*/

package ariba.ui.dev;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.core.AWConstants;
import ariba.ui.aribaweb.util.AWSingleLocaleResourceManager;
import ariba.ui.widgets.ChooserSelectionSource;
import ariba.ui.widgets.ChooserState;
import ariba.util.core.ListUtil;
import java.util.List;

public final class AWDebugOptions extends AWComponent
{
    //----------------------------------------------------------------------
    //  Constants
    //----------------------------------------------------------------------
    public static final String Name = "AWDebugOptions";

    public static int RepeatCount = 50;
    public static boolean RenderAsRefresh = false;

    //----------------------------------------------------------------------
    //  Configure Template Validation Packages
    //----------------------------------------------------------------------
    List _packageNameList;
    String _currentPackageName;

    public List packageNameList ()
    {
        if (_packageNameList == null) {
            _packageNameList = application().resourceManager().registeredPackageNames();
            ListUtil.sortStrings(_packageNameList, true);
        }
        return _packageNameList;
    }

    public String currentPackageName ()
    {
        return _currentPackageName;
    }

    public void setCurrentPackageName (String packageName)
    {
        _currentPackageName = packageName;
    }

    //////////
    // Errors
    /////////
    public boolean currentPackageErrorsEnabled ()
    {
        return isPackageLevelFlagEnabled (_currentPackageName, AWConcreteApplication.TemplateValidationFlag);
    }

    public void setCurrentPackageErrorsEnabled (boolean enable)
    {
        setCurrentPackageFlag(enable, AWConcreteApplication.TemplateValidationFlag);
    }

    //////////
    // Warnings
    /////////
/*
    public boolean currentPackageWarningsEnabled ()
    {
        // todo: enable warnings
        return false;
    }

    public void setCurrentPackageWarningsEnabled (boolean flag)
    {
        // todo: enable warnings
    }
*/
    //////////
    // Strict Tag Naming
    /////////
    public boolean currentPackageStrictTagNamingEnabled ()
    {
        return isPackageLevelFlagEnabled (_currentPackageName, AWConcreteApplication.StrictTagNamingFlag);
    }

    public void setCurrentPackageStrictTagNamingEnabled (boolean enable)
    {
        setCurrentPackageFlag(enable, AWConcreteApplication.StrictTagNamingFlag);
    }

    private void setCurrentPackageFlag (boolean enable, int flag)
    {
        if (enable) {
           ((AWConcreteApplication)application()).enablePackageLevelFlag(_currentPackageName, flag);
        }
        else {
            ((AWConcreteApplication)application()).disablePackageLevelFlag(_currentPackageName, flag);
        }
    }

    public AWComponent debugResourceManager ()
    {
        return pageWithName(AWResourceManagerInfoPage.Name);
    }

    public boolean getComponentPathDebuggingEnabled ()
    {
        return getFlag(AWConstants.ComponentPathDebugFlagKey);
    }

    public void setComponentPathDebuggingEnabled (boolean flag)
    {
        setFlag(AWConstants.ComponentPathDebugFlagKey, flag);
    }

    public boolean getDropDebuggingEnabled ()
    {
        return getFlag(AWConstants.DropDebugEnabled);
    }

    public void setDropDebuggingEnabled (boolean flag)
    {
        setFlag(AWConstants.DropDebugEnabled, flag);
    }

    private void setFlag (String flagKey, boolean flag)
    {
        AWSession session = requestContext().session(false);
        if (session != null) {
            session.dict().put(flagKey,
                               (flag ? Boolean.TRUE: Boolean.FALSE ));
        }
    }

    private boolean getFlag (String flagKey)
    {
        AWSession session = requestContext().session(false);

        if (session == null) {
            return false;
        }
        else {
            Boolean flag = (Boolean)session.dict().get(flagKey);
            return (flag != null) && flag.booleanValue();
        }
    }

    public boolean accessibilityEnabled ()
    {
        return session().isAccessibilityEnabled();
    }

    public void setAccessibilityEnabled (boolean yn)
    {
        session().setAccessibilityEnabled(yn);
    }

    public boolean pseudoLocalizationEnabled ()
    {
        return AWSingleLocaleResourceManager._pseudoLocalizationMode(session()) == AWSingleLocaleResourceManager.PseudoMode.Files;
    }

    public void setPseudoLocalizationEnabled (boolean yn)
    {
        AWSingleLocaleResourceManager._setPseudoLocalizationMode(session(),
                yn ? AWSingleLocaleResourceManager.PseudoMode.Files
                   : AWSingleLocaleResourceManager.PseudoMode.Off);
    }

    public void setPseudoLocalizationMode (AWSingleLocaleResourceManager.PseudoMode mode)
    {
        AWSingleLocaleResourceManager._setPseudoLocalizationMode(session(), mode);
    }

    public AWSingleLocaleResourceManager.PseudoMode pseudoLocalizationMode ()
    {
        return AWSingleLocaleResourceManager._pseudoLocalizationMode(session());
    }

    public void setPerfWarningsEnabled (boolean yn)
    {
        AWDebugPane.setPerfWarningsEnabled(yn);
    }

    public boolean perfWarningsEnabled ()
    {
        return AWDebugPane.perfWarningsEnabled();
    }

    public boolean perfDetailLoggingEnabled () 
    {
        return ariba.util.log.Log.perf_log_detail.getLevel() == ariba.util.log.Log.DebugLevel;
    }

    public void setPerfDetailLoggingEnabled (boolean yn)
    {
        ariba.util.log.Log.perf_log_detail.setLevel(yn ? ariba.util.log.Log.DebugLevel : ariba.util.log.Log.ErrorLevel);
    }


    public boolean getElementIdTracingEnabled ()
    {
        return getFlag(AWConstants.ElementIdTracingEnabled);
    }

    public void setElementIdTracingEnabled (boolean flag)
    {
        setFlag(AWConstants.ElementIdTracingEnabled, flag);
    }

}