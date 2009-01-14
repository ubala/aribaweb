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

    $Id: //ariba/platform/ui/ideplugin/ariba/ideplugin/eclipse/Control.java#3 $
*/
package ariba.ideplugin.eclipse;

import java.io.File;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class Control implements IStartup {

    public void earlyStartup ()
    {
        Preferences pref = Activator.getDefault().getPluginPreferences();
        if (pref.getBoolean(Activator.PrefAutoCheck)) {
            String awhome = pref.getString(Activator.PrefAWPath);
            if (awhome == null || awhome.length() == 0 || !new File(awhome).exists()) {
                Display.getDefault().asyncExec(new Runnable() {
                    public void run ()
                    {
                        PreferenceDialog pd = PreferencesUtil.createPreferenceDialogOn(
                            PlatformUI.getWorkbench().getWorkbenchWindows()[0].getShell(),
                            "ariba.ideplugin.eclipse.preferences.AWPreferencesPage",
                            new String[0], null);
                        pd.open();
                    }
                });
            }
        }
    }
}
