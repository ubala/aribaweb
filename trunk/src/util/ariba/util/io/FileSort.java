/*
    Copyright 1996-2009 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/io/FileSort.java#1 $
*/
package ariba.util.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Comparator;

/**
    A utility to sort the contents of a file. The sorting is done in a resource-limited
    fashion and is suitable to sort large files. The resource limit is counting lines since the size of a string
    in bytes is not straightforward to determine.
*/

public class FileSort
{
    private static final int DefaultMaxLines = 200000;
    private static final int DefaultMaxHandles = 10;


    private static class StringResource
        implements BufferedSort.Resource
    {
        private String s;

        private StringResource (String s)
        {
            this.s = s;
        }

        public static StringResource getResource (String s)
        {
            return s != null ? new StringResource(s) : null;
        }


        public long resourceSize ()
        {
            return 1;
        }

        public int hashCode ()
        {
            return s.hashCode();
        }

        public boolean equals (Object obj)
        {
            return s == obj;
        }

        public String toString ()
        {
            return s;
        }
    }

    private static class SortComparator
       implements Comparator<BufferedSort.Resource>
    {
        Comparator<String> comp;

        public SortComparator (Comparator<String> comp)
        {
            this.comp = comp;
        }

        public int compare (BufferedSort.Resource o1, BufferedSort.Resource o2)
        {
            if (comp == null) {
                return o1.toString().compareTo(o2.toString());
            }
            else {
                return comp.compare(o1.toString(), o2.toString());
            }
        }
    }

    private static class SortBuffer
        implements BufferedSort.SortBuffer
    {
        private static int numOpen = 0;
        private File tempDirectory;
        private String encoding;
        private static final int BufferSize = 8192*2;


        private File file;
        private BufferedReader reader;
        private BufferedWriter writer;

        public SortBuffer (File tempDirectory, String encoding)
        {
            this.tempDirectory = tempDirectory;
            this.encoding = encoding;
            file = TempFile.createTempFile(tempDirectory, "sort","buf");
            file.deleteOnExit();
        }


        public SortBuffer (File original, File tempDirectory, String encoding)
        {
            this.tempDirectory = tempDirectory;
            this.encoding = encoding;
            file = original;
        }

        public BufferedSort.Resource next ()
                throws IOException
        {
            return StringResource.getResource(reader.readLine());
        }

        public void write (BufferedSort.Resource value)
                throws IOException
        {
            writer.write(value.toString());
            writer.newLine();
        }

        public boolean open (boolean force)
                throws IOException
        {
            if (numOpen > 10 && !force) {
                return false;
            }
            numOpen++;
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding), BufferSize);

            return true;
        }

        public void close ()
                throws IOException
        {
            if (reader != null) {
                reader.close();
                numOpen--;
                reader = null;
            }
            if (writer != null) {
                writer.close();
                writer = null;
            }
        }

        public void dispose ()
        {
            file.delete();
        }

        public File getFile ()
        {
            return file;
        }

        protected SortBuffer openForWrite ()
                throws IOException
        {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding), BufferSize);
            return this;
        }

        public BufferedSort.SortBuffer newSortBuffer ()
                throws IOException
        {
            return new SortBuffer(tempDirectory, encoding).openForWrite();
        }
    }

    /**
        Convenience method that sorts a file using the default resource limits (200K strings and 10 open file handles max)
        and the default string comparator

        @param source file object pointing to the source file. The source file will not be changed
        @param tempDirectory directory to store temporary files in. The resulting sorted file will be in this directory
        @param encoding encoding of the source file
        @return file object pointing to the sorted file

        @throws IOException
     */
    public static File sort (File source, File tempDirectory, String encoding)
            throws IOException

    {
        return sort(source, tempDirectory, encoding, DefaultMaxLines, DefaultMaxHandles, null);
    }

    /**

     @param source file object pointing to the source file. The source file will not be changed
     @param tempDirectory directory to store temporary files in. The resulting sorted file will be in this directory
     @param encoding encoding of the source file
     @param maxLines maximum number of lines concurrently in memory
     @param maxHandles maximum number of concurrently open file handles
     @param comparator string comparator to be used
     @return file object pointing to the sorted file

     @throws IOException
     */
    public static File sort (File source,
                             File tempDirectory,
                             String encoding,
                             int maxLines,
                             int maxHandles,
                             Comparator<String> comparator)
            throws IOException
    {
        SortBuffer sb = new SortBuffer(source, tempDirectory, encoding);
        BufferedSort bsort = new BufferedSort(sb, maxLines, maxHandles, new SortComparator(comparator));

        return ((SortBuffer)bsort.sort()).getFile();

    }
}
