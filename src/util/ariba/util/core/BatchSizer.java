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

    $Id: //ariba/platform/util/core/ariba/util/core/BatchSizer.java#2 $
*/
package ariba.util.core;

/**
    Is a class that defines how a stream of objects should be batched. <p/>

    @aribaapi ariba
*/
public abstract class BatchSizer
{
    /**
        Returns <code>true</code> if, when <code>object</code> is considered
        as the next object in the stream of objects, that after this
        object a new batch should begin. Returns <code>false</code> if
        after <code>object</code> is recognized a new batch should not begin.
        <p/>
        If the addition of <code>object</code> causes this method to return
        <code>true</code>, then this method should continue to return
        <code>true</code> until {@link #reset} is called. <p/>

        @aribaapi ariba
    */
    public abstract boolean addToBatch (Object object);

    /**
        Resets the internal state of <code>this</code> after a batch.
     
        @aribaapi ariba
    */
    public abstract void reset ();

    //--------------------------------------------------------------------------
    // nested class

    /**
        Is a batch sizer that uses a simple fixed batch size.
        @aribaapi ariba
    */
    public static final class Fixed extends BatchSizer
    {
        private int _batchSize;
        private int _currentSize;

        /**
            @aribaapi ariba
        */
        public Fixed (int batchSize)
        {
            _batchSize = batchSize;
            _currentSize = 0;
        }

        public int currentSize ()
        {
            return _currentSize;
        }

        public int batchSize ()
        {
            return _batchSize;
        }

        public void reset ()
        {
            _currentSize = 0;
        }

        public boolean addToBatch (Object object)
        {
            ++_currentSize;
            return _currentSize >= _batchSize;
        }
    }
}
