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

    $Id: //ariba/platform/util/core/ariba/util/core/PermutationIterator.java#3 $
*/

package ariba.util.core;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
    A PermutationIterator produces a series of permutations of the
    requested size from a source array of objects.

    @aribaapi ariba
*/
public class PermutationIterator implements Iterator
{
    /*-----------------------------------------------------------------------
        Private Fields
      -----------------------------------------------------------------------*/

        // source array
    private Object[] _source;

        // the desired permutation size
    private int _permSize;

        // enumeration of subsets and full permutations of each
    private SubsetIterator   _subsets;
    private FullPermutationEnum _perms;

    
    /*-----------------------------------------------------------------------
        Public Constructor
      -----------------------------------------------------------------------*/

    /**
        Creates a new PermutationIterator for the given <b>source</b> array
        using all elements of the array.
    */
    public PermutationIterator (Object[] source)
    {
        this(source, source.length);
    }

    /**
        Creates a new PermutationIterator for the given <b>source</b> array
        where each permutation will be of the given <b>permSize</b>.
    */
    public PermutationIterator (Object[] source, int permSize)
    {
            // make sure the input arguements are valid
        Assert.that(permSize > 0 && permSize <= source.length, "invalid arguments");

        _source = source;
        _permSize = permSize;

            // open a stream of subsets of the desired permutation size
        _subsets = new SubsetIterator(source, permSize);

            // start permuting the first subset
        _perms = new FullPermutationEnum((Object[])_subsets.next());
    }
    

    /*-----------------------------------------------------------------------
        Iterator Interface
      -----------------------------------------------------------------------*/

    /**
        Returns true if the PermutationIterator has more elements.
    */
    public boolean hasNext ()
    {
        if (_perms.hasNext()) {
            return true;
        }

        if (_subsets.hasNext()) {
            _perms = new FullPermutationEnum((Object[])_subsets.next());
        }
        else {
            return false;
        }
        
        return hasNext();
    }

    /**
        Returns the next element of this PermutationIterator as an array of
        Objects.  Throws a NoSuchElementException if there is no next
        elemement, i.e. if hasNext() returned false.
    */
    public Object next ()
    {
        if (hasNext()) {
            return _perms.next();
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

    public static void main (String[] args)
    {
            // first arg is the permutation size
        int size = new Integer(args[0]).intValue();  // OK
        
            // set up the source array as everything but the first arg
        Object[] source = new Object[args.length-1];
        System.arraycopy(args, 1, source, 0, args.length-1);

            // run the permutations and print each one
        PermutationIterator p = new PermutationIterator(source, size);
        int count = 0;
        Object result;
        while (p.hasNext()) {
            result = p.next();
            printArray((Object[])result);
            count++;
        }
        Fmt.F(SystemUtil.out(), "There were %s permutations.\n", count);
        SystemUtil.exit(0);
    }

    private static void printArray (Object[] objArray)
    {
        PrintWriter out = SystemUtil.out();
        out.print("( ");
        for (int i = 0; i < objArray.length; i++) {
            if (i > 0) {
                out.print(", ");
            }
            out.print(objArray[i]);
        }
        out.println(" )");
    }
}

class FullPermutationEnum implements Iterator
{
    private Object[] _source;
    private int      _sourceSize;
    private int[]    _list;
    private boolean  _nextReady = true;
    
    FullPermutationEnum (Object[] source)
    {
        _sourceSize = source.length;
        _source = new Object[_sourceSize];
        System.arraycopy(source, 0, _source, 0, _sourceSize);
        _list = new int[_sourceSize+1];
        for (int i = 0; i < _sourceSize + 1; i++) {
            _list[i] = i;
        }
    }

    public boolean hasNext ()
    {
        if (_nextReady) {
            return true;
        }

        int k, j, r, s;

        k = _sourceSize - 1;
        while (_list[k] > _list[k+1]) {
            k--;
        }
        if (k == 0) {
            return(false);
        }
        else {
            j = _sourceSize;
            while (_list[k] > _list[j]) {
                j--;
            }
            swap(j, k);
            r = _sourceSize; s = k + 1;
            while (r > s) {
                swap(r, s);
                r--;
                s++;
            }
        }
        
        _nextReady = true;
        return true;
    }

    public Object next ()
    {
        if (hasNext()) {
            _nextReady = false;
            return _source;
        }
        else {
            throw new NoSuchElementException();
        }
    }

    public void remove ()
    {
        throw new UnsupportedOperationException();
    }
    
    private void swap (int i, int j)
    {
        int ti;
        Object to;
        
        ti = _list[i];
        _list[i] = _list[j];
        _list[j] = ti;

        to = _source[i-1];
        _source[i-1] = _source[j-1];
        _source[j-1] = to;
    }
}

/** Some of this code translated from the following: */

/*===================================================================*/
/* C program for distribution from the Combinatorial Object Server.  */
/* Generate permutations in lexicographic order. This is             */
/* the same version used in the book "Combinatorial Generation" by   */
/* Frank Ruskey.                                                     */
/* The program can be modified, translated to other languages, etc., */
/* so long as proper acknowledgement is given (author and source).   */  
/* Programmer: Frank Ruskey 19??, Joe Sawada, 1997.                  */
/* The latest version of this program may be found at the site       */
/* http://www.theory.uvic.ca/~cos/inf/perm/PermInfo.html             */
/*===================================================================*/
