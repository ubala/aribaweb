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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWPagedVector.java#11 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.ListUtil;
import java.util.List;
import java.util.Iterator;

/**
*/
public final class AWPagedVector extends AWBaseObject
{
    private static final ThreadLocal IteratorPool = new ThreadLocal();
    private final List _pages;
    private final int _pageSize;
    private int _size;
    private Object[] _currentPage;
    // The index within the _currentPage where the next addElement() will be go.
    private int _currentOffset;

    public AWPagedVector (int initialSize)
    {
        _pageSize = initialSize;
        _pages = ListUtil.list();
        clear();
    }

    public void clear ()
    {
        _size = 0;
        _currentOffset = -1;
        _currentPage = new Object[_pageSize];
        _pages.clear();
        _pages.add(_currentPage);
    }

    public int size ()
    {
        return _size;
    }

    public void add (Object element)
    {
        _size++;
        _currentOffset++;
        if (_currentOffset >= _pageSize) {
            _currentPage = new Object[_pageSize];
            _pages.add(_currentPage);
            _currentOffset = 0;
        }
        _currentPage[_currentOffset] = element;
    }

    public Object elementAt (int index)
    {
        int pageLength = _currentPage.length;
        int pageIndex = index / pageLength;
        int pageOffset = index % pageLength;
        Object[] page = (Object[])_pages.get(pageIndex);
        return page[pageOffset];
    }

    public AWPagedVectorIterator elements (int start, int end)
    {
        AWRecyclePool recyclePool = (AWRecyclePool)IteratorPool.get();
        if (recyclePool == null) {
            recyclePool = AWRecyclePool.newPool(16, false, false);
            IteratorPool.set(recyclePool);
        }
        AWPagedVectorIterator iterator = (AWPagedVectorIterator)recyclePool.checkout();
        if (iterator == null) {
            iterator = new AWPagedVectorIterator();
        }
        iterator.reset(this, _pages, start, end);
        return iterator;
    }

    public AWPagedVectorIterator elements ()
    {
        return elements(0, size());
    }

    public void release (AWPagedVectorIterator iterator)
    {
        AWRecyclePool recyclePool = (AWRecyclePool)IteratorPool.get();
        recyclePool.checkin(iterator);
    }

    public static class AWPagedVectorIterator extends AWBaseObject implements Iterator
    {
        private AWPagedVector _pagedVector;
        private List _pages;
        private int _pageLength;
        private int _endIndex;
        private int _nextIndex;
        private int _nextOffset;
        private int _currentPageIndex;
        private Object[] _currentPage;

        public void reset (AWPagedVector pagedVector, List pages, int startIndex, int endIndex)
        {
            _pagedVector = pagedVector;
            _pages = pages;
            _pageLength = ((Object[])_pages.get(0)).length;
            _endIndex = endIndex;
            skipTo(startIndex);
        }

        public boolean hasNext ()
        {
            return _nextIndex < _endIndex;
        }

        public Object next ()
        {
            if (_nextOffset >= _pageLength) {
                _nextOffset = 0;
                _currentPageIndex++;
                _currentPage = (Object[])_pages.get(_currentPageIndex);
            }
            Object nextElement = _currentPage[_nextOffset];
            _nextIndex++;
            _nextOffset++;
            return nextElement;
        }

        public void skipTo (int index)
        {
            _nextIndex = index;
            _nextOffset = _nextIndex % _pageLength;
            _currentPageIndex = _nextIndex / _pageLength;
            _currentPage = (_nextIndex < _endIndex) ? (Object[])_pages.get(_currentPageIndex) : null;
        }

        public void remove ()
        {
            AWUtil.notImplemented("PagedVectorIterator.remove()");
        }

        public void release ()
        {
            AWPagedVector pagedVector = _pagedVector;
            _pagedVector = null;
            _pages = null;
            _pageLength = 0;
            _endIndex = 0;
            _nextIndex = 0;
            _nextOffset = 0;
            _currentPageIndex = 0;
            _currentPage = null;
            pagedVector.release(this);
        }
    }
}
