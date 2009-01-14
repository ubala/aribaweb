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

    $Id: //ariba/platform/util/core/ariba/util/io/Find.java#8 $
*/

package ariba.util.io;

import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.SystemUtil;
import java.util.List;
import ariba.util.core.ListUtil;
import ariba.util.log.Log;
import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.Iterator;

/**
    Tree walker similar to unix find

    @aribaapi private
*/
public class Find implements Iterator
{

    private Iterator dirs;
    private FilenameFilter filter;

    /*
        State Machine
    */
    private static final int NextDirectory = 0;
    private static final int ListFile      = 1;
    private static final int ListDirectory = 2;
    private int state = NextDirectory;

    /*
        Various state
    */
    private File        dir;
    private int         fileIndex;
    private String      fileList[];
    private Iterator subdir;
    private String      file;

    /**
        If we look ahead with hasMoreElements save next element to
        return later
    */
    private File nextElement;

    public Find (File           dir,
                 FilenameFilter filter)
    {
        this(ListUtil.list(dir), filter);
    }

    private Find (List         dirs,
                  FilenameFilter filter)
    {
        this.dirs   = dirs.iterator();
        this.filter = filter;
    }

    public void remove ()
    {
        throw new UnsupportedOperationException();
    }
    
    public boolean hasNext ()
    {
        if (nextElement != null) {
            return true;
        }
        nextElement = (File)next();
        if (nextElement != null) {
            return true;
        }
        return false;
    }

    public Object next ()
    {
        if (nextElement != null) {
            File result = nextElement;
            nextElement = null;
            return result;
        }

        while (true) {
            switch (state) {

              case NextDirectory: {
                  if (!dirs.hasNext()) {
                      return null;
                  }
                      // add trailing separatorChar if missing
                  dir = (File)dirs.next();
                  Assert.that(dir.isDirectory(),
                              "dir.isDirectory(): %s not directory",
                              dir);
                  if (dir.canRead()) {
                      fileList = dir.list();
                  }
                  else {
                      Log.util.warning(2796, dir);
                      fileList = new String[0];
                  }

                      // PR10878: Sometimes the directory list can
                      // return null instead of new String[0]. This
                      // isn't documented. Thanks.
                  if (fileList == null) {
                      Log.util.warning(2797, dir);
                      fileList = new String[0];
                  }

                  fileIndex = 0;
                  state     = ListFile;
                  if (filter.accept(dir, null)) {
                      return dir;
                  }
                  break;
              }

              case ListFile: {
                  if (fileIndex == fileList.length) {
                      state = NextDirectory;
                      break;
                  }

                  file = fileList[fileIndex++];

                  File path = new File(dir, file);
                  if (path.isDirectory()) {
                      subdir = new Find(path, filter);
                      state  = ListDirectory;
                      break;
                  }

                  if (filter.accept(dir, file)) {
                      return path;
                  }

                  break;
              }

              case ListDirectory: {
                  if (!subdir.hasNext()) {
                      state = ListFile;
                      break;
                  }
                  return subdir.next();
              }

              default: {
                  Assert.that(false,
                              "This code should be unreachable: %s",
                              dir);
              }
            }
        }
    }

    public static void main (String[] args)
    {
        if (args.length != 2) {
            Fmt.F(SystemUtil.err(), "usage: Find directory extension");
            SystemUtil.exit(1);
        }
        String directory = args[0];
        String extension = args[1];

            // no constants so we can compile util standalone
        Iterator e = new Find(new File(directory),
                                 new FilenameExtensionFilter(extension));

        PrintWriter out = SystemUtil.out();
        while (e.hasNext()) {
            Fmt.F(out, "%s\n", e.next());
        }
        SystemUtil.exit(0);
    }
}

