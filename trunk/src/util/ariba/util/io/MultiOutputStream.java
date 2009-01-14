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

    $Id: //ariba/platform/util/core/ariba/util/io/MultiOutputStream.java#4 $
*/

package ariba.util.io;

import java.io.OutputStream;
import java.io.IOException;

/**
    MultiOutputStream

    Multiplex over multiple OutputStreams.

    @aribaapi private
*/
public class MultiOutputStream extends OutputStream
{
    private int count;
    private AROutputStream[] streams = new AROutputStream[count];

    public MultiOutputStream ()
    {
    }

    public MultiOutputStream (OutputStream stream)
    {
        addStream(stream);
    }

    public void addStream (OutputStream s)
    {
        this.addStream(s, false);
    }

    public void addStream (OutputStream s, boolean buffered)
    {
        if (s == null) {
            return;
        }
        AROutputStream[] newthis = new AROutputStream [this.count + 1];
        for (int i = 0; i < this.count; i++) {
            if (this.streams[ i ].stream == s) {
                return;
            }
            newthis[ i ] = this.streams [ i ];
        }

        newthis[ this.count ] = new AROutputStream(s, buffered);
        this.streams = newthis;
        this.count++;
    }

    public void removeStream (OutputStream s)
    {
        for (int i = 0; i < this.count; i++) {
            if (this.streams[ i ].stream == s) {
                this.streams[ i ] = this.streams [ this.count - 1 ];
                this.streams [ this.count - 1 ] = null;
                this.count--;
                break;
            }
        }
    }

    /**
        Called when we only want to flush cheap streams.

        For example, in logging, we always want to flush to the
        console, but we want to be lazy about forcing it to the file.
    */
    public void flushIfNotBuffered () throws IOException
    {
        for (int i = 0; i < this.count; i++) {
            if (!this.streams[i].buffered) {
                this.streams[i].stream.flush();
            }
        }
    }

    //--- OutputStream --------------------------------------------------------

    public void write (int b) throws IOException
    {
        for (int i = 0; i < this.count; i++) {
            this.streams[i].stream.write(b);
        }
    }

    public void write (byte[] b) throws IOException
    {
        for (int i = 0; i < this.count; i++) {
            this.streams[i].stream.write(b);
        }
    }

    public void write (byte[] b, int offset, int length) throws IOException
    {
        for (int i = 0; i < this.count; i++) {
            this.streams[i].stream.write(b, offset, length);
        }
    }

    public void flush () throws IOException
    {
        for (int i = 0; i < this.count; i++) {
            this.streams[i].stream.flush();
        }
    }

    public int count ()
    {
        return count;
    }
    
    public boolean isEmpty ()
    {
        return count == 0;
    }

    public void close () throws IOException
    {
        for (int i = 0; i < this.count; i++) {
            this.streams[i].stream.close();
        }
    }
}

class AROutputStream
{
    boolean buffered;
    OutputStream stream;

    public AROutputStream (OutputStream s)
    {
        this.buffered = false;
        this.stream = s;
    }

    public AROutputStream (OutputStream s, boolean buffered)
    {
        this.buffered = buffered;
        this.stream = s;
    }
}
