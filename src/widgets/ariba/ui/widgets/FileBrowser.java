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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/FileBrowser.java#8 $
*/

// todo: I think this package is wrong -- should be in some package that is higher-level than
// ariba.ui.table.  ariba.ui.widgets is lower-level than ariba.ui.table hence the BindingNames refs in here
// require full package prefixes (an indication something is wrong).
package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.table.AWTDisplayGroup;
import ariba.util.core.ListUtil;
import ariba.util.core.Date;
import ariba.util.formatter.DateFormatter;
import java.util.List;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;

public class FileBrowser extends AWComponent
{
    public static final String baseDirectory    = "baseDirectory";
    public static final String fileFilter       = "fileFilter";
    public static final String filenameFilter   = "filenameFilter";
    public static final String allowCdUp        = "allowCdUp";
    public static final SuperRootDir SuperRootDirectory = SuperRootDir.Instance;
    public static final String separator = File.separator;

    private static final String[] SupportedBindingNames = {
        baseDirectory, fileFilter, filenameFilter, BindingNames.selection,
        BindingNames.action, ariba.ui.table.BindingNames.enableScrolling,
        ariba.ui.table.BindingNames.showOptionsMenu, ariba.ui.table.BindingNames.batchSize,
        allowCdUp
    };

    public AWTDisplayGroup _displayGroup;
    public File _currentDirectory;
    public File _currentFile;
    public File _baseDirectory;
    public File _currentDirectoryLink;
    private FileFilter _fileFilter;
    private FilenameFilter _filenameFilter;
    private List _currentFileListing;
    public boolean _allowCdUp;

    public boolean isStateless ()
    {
        return false;
    }

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    public void init ()
    {
        AWBinding binding = bindingForName(fileFilter);
        if (binding != null) {
            _fileFilter = (FileFilter)valueForBinding(binding);
        }
        else {
            binding = bindingForName(filenameFilter);
            if (binding != null) {
                _filenameFilter = (FilenameFilter)valueForBinding(filenameFilter);
            }
        }
        _baseDirectory = (File)valueForBinding(baseDirectory);
        if (_baseDirectory == null) {
            _baseDirectory = SuperRootDirectory;
        }
        try {
            _baseDirectory = _baseDirectory.getCanonicalFile();
        }
        catch (IOException e) {
        }
        binding = bindingForName(allowCdUp);
        _allowCdUp = binding != null ? booleanValueForBinding(allowCdUp) : false;
        cdInto(_baseDirectory);
    }

    public List directoryLinks ()
        throws IOException
    {
        List links = ListUtil.list();
        File currentDirectory = _currentDirectory.getCanonicalFile();
        File parentDir = currentDirectory.getParentFile();
        while (parentDir != null) {
            links.add(0, parentDir);
            parentDir = parentDir.getParentFile();
        }
        return links;
    }

    public String currentDirectoryLinkName ()
    {
        return getDisplayName(_currentDirectoryLink);
    }

    public String currentFileName ()
    {
        return getDisplayName(_currentFile);
    }

    public String currentDirectoryName ()
    {
        return getDisplayName(_currentDirectory);
    }

    public String currentParentDirectoryName ()
    {
        return getDisplayName(getParentFile(_currentDirectory));
    }

    public long currentFileSize ()
    {
        return _currentFile.length() / 1024 + 1;
    }

    public String currentFileLastModified ()
    {
        Date lastModifiedDate = new Date(_currentFile.lastModified());
        return DateFormatter.toConciseDateTimeString (lastModifiedDate);
    }

    public boolean canCdUp ()
    {
        return _allowCdUp || !_currentDirectory.equals(_baseDirectory);
    }

    public boolean canNotCdInto ()
    {
        return _currentDirectoryLink.getPath().length() <=
                _baseDirectory.getPath().length();
    }

    public boolean isSizeVisible ()
    {
        boolean isSizeVisible = false;
        int size = _currentFileListing.size();
        for (int i=0; i < size; i++) {
            File file = (File)_currentFileListing.get(i);
            if (!file.isDirectory()) {
                isSizeVisible = true;
                break;
            }
        }
        return isSizeVisible;
    }

    public AWComponent cdDownAction ()
    {
        int size = _currentFileListing.size();
        for (int i=0; i < size; i++) {
            String filename = getName((File)_currentFileListing.get(i));
            if (filename.equals(getName(_currentFile))) {
                File directory = createFile(_currentDirectory, filename);
                cdInto(directory);
                break;
            }
        }
        return null;
    }

    public AWComponent cdUpAction ()
    {
        cdInto(getParentFile(_currentDirectory));
        return null;
    }

    public AWComponent cdIntoAction ()
    {
        cdInto(_currentDirectoryLink);
        return null;
    }

    public AWComponent selectClicked ()
    {
        setValueForBinding(_currentFile, BindingNames.selection);
        AWBinding action = bindingForName(BindingNames.action);
        return (AWComponent)action.value(parent());
    }

    public AWComponent openClicked ()
    {
        cdInto(_currentFile);
        return null;
    }

    private void cdInto (File directory)
    {
        if (directory.exists() && directory.isDirectory()) {
            _currentDirectory = directory;
            _currentFileListing = currentFileListing();

        };
        if (_currentFileListing != null) {
            _displayGroup = new AWTDisplayGroup();
            _displayGroup.setObjectArray(_currentFileListing);
            _displayGroup.setSortOrderings(EmptyVector);
        }
    }

    private List currentFileListing ()
    {
        File[] currentFiles;
        if (_fileFilter != null) {
            currentFiles = _currentDirectory.listFiles(_fileFilter);
        }
        else {
            currentFiles = _currentDirectory.listFiles(_filenameFilter);
        }
        if (currentFiles == null) {
            return EmptyVector;
        }
        return ListUtil.arrayToList(currentFiles);
    }

    private static String getName (File file)
    {
        if (SuperRootDirectory.isRoot(file)) {
            return file.getPath();
        }
        return file.getName();
    }

    private static String getDisplayName (File file)
    {
        String name = getName(file);
        if (SuperRootDirectory.isRoot(file)) {
            name = name.replace(File.separatorChar, ' ');
            name = name.replaceAll(" ", "");
        }
        return name;
    }

    private static File createFile (File parentDir, String filename)
    {
        if (SuperRootDirectory == parentDir) {
            return new File(filename);
        }
        return new File(parentDir, filename);
    }

    private static File getParentFile(File file)
    {
        if (SuperRootDirectory.isRoot(file)) {
            return SuperRootDirectory;
        }
        return file.getParentFile();
    }

    static class SuperRootDir extends File
    {
        public static final SuperRootDir Instance = new SuperRootDir();

        private SuperRootDir ()
        {
            super("");
        }

        public boolean exists()
        {
            return true;
        }

        public boolean isDirectory()
        {
            return true;
        }

        public File[] listFiles (FileFilter fileFilter)
        {
            File[] roots = File.listRoots();
            return roots;
        }

        public File[] listFiles (FilenameFilter filenameFilter)
        {
            File[] roots = File.listRoots();
            return roots;
        }

        public boolean isRoot (File file)
        {
            File[] roots = File.listRoots();
            for (int i = 0; i < roots.length; i++) {
                if (roots[i].equals(file)) {
                    return true;
                }
            }
            return false;
        }

        public String getCanonicalPath ()
        {
            return "";
        }

        public File getCanonicalFile() throws IOException {
            return this;
        }

        public String getAbsolutePath ()
        {
            return "";
        }

        public boolean equals (Object obj)
        {
            return this == obj;
        }

        public String getName ()
        {
            return "";
        }

    }
}