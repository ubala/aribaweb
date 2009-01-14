package ariba.ideplugin.eclipse.preferences;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.IAntClasspathEntry;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import ariba.ideplugin.eclipse.Activator;

public class AWPreferencesPage extends FieldEditorPreferencePage implements
    IWorkbenchPreferencePage
{

	private String _prevHome;

	public AWPreferencesPage ()
    {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        _prevHome = Activator.getDefault().getAWHome();
    }

	public void createFieldEditors ()
    {
        addField(new DirectoryFieldEditor(Activator.PrefAWPath,
            "AribaWeb Dir:", getFieldEditorParent()));
        addField(new BooleanFieldEditor(Activator.PrefAutoCheck,
            "Auto check on startup", getFieldEditorParent()));
    }

	public void init (IWorkbench workbench)
    {
    }

	public boolean performOk ()
    {
        boolean ret = super.performOk();
        try {
            JavaCore.setClasspathVariable("AW_HOME", new Path(Activator
                .getDefault().getAWHome()), new NullProgressMonitor());
        }
        catch (JavaModelException e) {
            setErrorMessage(e.getMessage());
            return false;
        }
        if (!Activator.getDefault().getAWHome().equals(_prevHome)) {
            updateAntHome();
        }
        return ret;
    }

	public void updateAntHome ()
    {
        Preferences pref = Activator.getDefault().getPluginPreferences();

        String awhome = pref.getString(Activator.PrefAWPath);
        File anthome = new File(awhome, "tools/ant/");
        if (awhome.length() > 0 && anthome.exists()) {
            File antLib = new File(anthome, "lib");
            String[] antJars = antLib.list(new FilenameFilter() {
                public boolean accept (File dir, String name)
                {
                    return name.endsWith(".jar");
                }
            });
            ClassPathEntry[] entries = new ClassPathEntry[antJars.length];
            for (int i = 0; i < antJars.length; i++) {
                File tjar = new File(antLib, antJars[i]);
                entries[i] = new ClassPathEntry(tjar);
            }
            AntCorePreferences antPref = AntCorePlugin.getPlugin().getPreferences();
            antPref.setAntHome(anthome.toString());
            antPref.setAntHomeClasspathEntries(entries);
            antPref.updatePluginPreferences();
        }
        else {
            AntCorePreferences antPref = AntCorePlugin.getPlugin().getPreferences();
            antPref.setAntHome(antPref.getDefaultAntHome());
            antPref.setAntHomeClasspathEntries(antPref.getDefaultAntHomeEntries());
            antPref.updatePluginPreferences();
        }
    }

	public class ClassPathEntry implements IAntClasspathEntry
    {
        File _entry;

        public ClassPathEntry (File entry)
        {
            _entry = entry;
        }

        public URL getEntryURL ()
        {
            try {
                return _entry.toURI().toURL();
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }
            return null;
        }

        public String getLabel ()
        {
            return _entry.toString();
        }

        public boolean isEclipseRuntimeRequired ()
        {
            return false;
        }

        public boolean equals (Object o)
        {
            if (o instanceof ClassPathEntry) {
                return ((ClassPathEntry)o)._entry.equals(_entry);
            }
            else if (o instanceof IAntClasspathEntry) {
                return ((IAntClasspathEntry)o).getEntryURL().equals(getEntryURL());
            }
            return false;
        }
    }
}