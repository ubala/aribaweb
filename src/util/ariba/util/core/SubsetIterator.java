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

    $Id: //ariba/platform/util/core/ariba/util/core/SubsetIterator.java#3 $
*/

package ariba.util.core;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
    A SubsetIterator produces a series of subsets of the requested size
    from a source array of objects.
    @aribaapi ariba
*/
public final class SubsetIterator implements Iterator
{
    /*-----------------------------------------------------------------------
        Private Fields
      -----------------------------------------------------------------------*/

        // source array
    private Object[] _source;
    private int      _sourceSize;

        // results array
    private Object[] _result;
    private int      _resultSize;

        // source index for each result element
    private int[] _index;

        // is our next result ready?
    private boolean _nextReady = true;

    
    /*-----------------------------------------------------------------------
        Public Constructor
      -----------------------------------------------------------------------*/

    /**
        Creates a new SubsetIterator for the given <b>source</b> array,
        where each subset will be of the given <b>subsetSize</b>.
    */
    public SubsetIterator (Object[] source, int subsetSize)
    {
        this(source, subsetSize, new Object[subsetSize]);
    }
    
    /**
        Creates a new SubsetIterator for the given <b>source</b> array,
        where each subset will be of the given <b>subsetSize</b>.  The
        caller provides the <b>result</b> array which will be returned in
        each successful call to next().
    */
    public SubsetIterator (Object[] source, int subsetSize, Object[] result)
    {
            // bail out if the subsetSize is invalid
        if ((subsetSize <= 0) || (subsetSize > source.length)) {
            return;
        }
        
            // make sure the result array is big enough
        Assert.that(subsetSize <= result.length, "result array too small");
        
            // cache our source array and its size
        _source = source;
        _sourceSize = source.length;
 
            // cache our result array and its size
        _result = result;
        _resultSize = subsetSize;

            // initialize our indexes and the first result subset
        _index  = new int[_resultSize];
        for (int i = 0; i < _resultSize; i++) {
            _index[i] = i;
            _result[i] = _source[i];
        }
    }


    /*-----------------------------------------------------------------------
        Iterator Interface
      -----------------------------------------------------------------------*/

    /**
        Returns true if the SubsetIterator has more elements.
    */
    public boolean hasNext ()
    {
        if (_nextReady) {
            return true;
        }

        int n;
        int j;
        int t;
        for (int i = _resultSize - 1; i >= 0; --i) {
            n = _index[i];
            if ((n - i + _resultSize) < _sourceSize) {
                for (j = i; j < _resultSize; ++j) {
                    _index[j] = t = ++n;
                    _result[j] = _source[t];
                }
                _nextReady = true;
                return true;
            }
        }
        return false;
    }

    /**
        Returns the next element of this SubsetIterator as an array of
        Objects.  Throws a NoSuchElementException if there is no next
        elemement, i.e. if hasNext() returned false.
    */
    public Object next ()
    {
        if (_nextReady || hasNext()) {
            _nextReady = false;
            return _result;
        }
        else {
            throw new NoSuchElementException();
        }
    }


    public void remove ()
    {
        throw new UnsupportedOperationException();
    }
    
    /*-----------------------------------------------------------------------
        Test Main
      -----------------------------------------------------------------------*/

    /**
        The first argument is assumed to be an integer which is the requested
        subset size.  The remaining arguments are used as the elements of the
        full set.
    */
    public static void main (String[] args)
    {
            // first arg is the subset size
        int size = new Integer(args[0]).intValue();  // OK
        
            // set up the source array as everything but the first arg
        Object[] source = new Object[args.length-1];
        System.arraycopy(args, 1, source, 0, args.length-1);
        long start = System.currentTimeMillis();
            // run the subsets and print each one
        SubsetIterator s = new SubsetIterator(source, size);
        int count = 0;
        Object result;
        while (s.hasNext()) {
            result = s.next();
            printArray((Object[])result);
            count++;
        }
        long end = System.currentTimeMillis();
        Fmt.F(SystemUtil.out(),
              "elapsed time is %s\n", Constants.getLong((end - start)));
        Fmt.F(SystemUtil.out(), "There were %s subsets.", count);
        Fmt.F(SystemUtil.out(), "there were %s enumerations per second",
              Constants.getLong((count / (end-start) * 1000)));
        SystemUtil.exit(0);
    }

    private static void printArray (Object[] objArray)
    {
        SystemUtil.out().print("( ");
        for (int i = 0; i < objArray.length; i++) {
            if (i > 0) {
                SystemUtil.out().print(", ");
            }
            SystemUtil.out().print(objArray[i]);
        }
        SystemUtil.out().println(" )");
    }
}
