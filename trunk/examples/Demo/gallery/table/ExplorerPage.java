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

    $Id: //ariba/platform/ui/opensourceui/examples/Demo/gallery/table/ExplorerPage.java#1 $
*/
package gallery.table;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.table.AWTDisplayGroup;
import java.util.List;
import java.util.Map;
import ariba.util.core.Date;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;

import java.io.File;

public class ExplorerPage extends AWComponent
{
    public AWTDisplayGroup _displayGroup;
    public List _rootFiles = ListUtil.arrayToList(new File(".").listFiles());
    public File _currentObject;
    public File _selectedObject;
    public List _selectionPath = ListUtil.list();
    protected Map _childrenForFile = MapUtil.map();
    public final Integer IntVal_2 = new Integer(2);
    public boolean _explorerMode = false;

    public void toggleMode ()
    {
        _explorerMode = !_explorerMode;
    }

    private int _count=1;

    public int count () {
        _count += 10;
        return _count;
    }

    public boolean isStateless ()
    {
        return false;
    }

    public List children ()
    {
        return childrenForObject(_currentObject);
    }

    public List childrenForObject (File object)
    {
        List result = (List)_childrenForFile.get(object);
        if (result == null) {
            File[] files = object.listFiles();
            result = (files == null) ? ListUtil.list() : ListUtil.arrayToList(files);
            _childrenForFile.put(object, result);
        }
        return result;
    }

    public boolean hasChildren ()
    {
        return children().size() != 0;
    }

    public boolean isDirectory ()
    {
        return _currentObject.isDirectory();
    }

    public Date modifiedDate ()
    {
        return new Date(_currentObject.lastModified());
    }

    public Object selectedObjectChildren ()
    {
        return (_selectedObject != null) ? childrenForObject(_selectedObject) : _rootFiles;
    }

    public String iconForCurrentObject ()
    {
        return (isDirectory()) ? "AWXFolderIcon.gif" : "AWXFileIcon.gif";
    }

    public boolean isNotDirectory ()
    {
        return !isDirectory();
    }

}

