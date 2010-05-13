/*
    Copyright (c) 1996-2007 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/TreeIterable.java#3 $

    Responsible: dfinlay
*/
package ariba.util.core;

import java.util.Iterator;
import java.util.List;
import java.util.Collections;

/**
    @aribaapi ariba
*/
public class TreeIterable<T> implements Iterable<T>
{
    public enum Traversal { PreOrder, LevelOrder }

    private Iterable<T> _roots;
    private Function<Iterable<T>> _childrenGetter;
    private Traversal _traversal;

    public TreeIterable (
            Iterable<T> roots,
            Traversal traversal,
            Function<Iterable<T>> childrenGetter
    )
    {
        _roots = roots;
        _traversal = traversal != null ? traversal : Traversal.PreOrder;
        _childrenGetter = childrenGetter;
    }

    public TreeIterable (List<T> roots, Traversal traversal)
    {
        this(roots, traversal, null);
    }

    public TreeIterable (T root, Traversal traversal, Function<Iterable<T>> childrenGetter)
    {
        this(Collections.singletonList(root), traversal, childrenGetter);
    }

    public TreeIterable (T root, Traversal traversal)
    {
        this(Collections.singletonList(root), traversal);
    }

    public TreeIterable (T root)
    {
        this(root, Traversal.PreOrder);
    }

    public TreeIterable (List<T> roots)
    {
        this(roots, Traversal.PreOrder);
    }

    public Iterator<T> iterator ()
    {
        return new Iter();
    }

    protected Iterable<T> getChildren (T parent)
    {
        return _childrenGetter != null ? _childrenGetter.evaluate(parent) : null;
    }

    private class Iter implements Iterator<T>
    {
        private List<Iterator<T>> _iterators;
        private int _min;
        private int _idx;

        private Iter ()
        {
            if (_roots == null) {
                _iterators = ListUtil.list();
            }
            else {
                _iterators = ListUtil.list(_roots.iterator());
            }
            _min = 0;
            _idx = _traversal == Traversal.PreOrder ? _iterators.size() - 1 : _min;
        }

        public boolean hasNext ()
        {
            int idx = _idx;
            int size = _iterators.size();
            int delta = _traversal == Traversal.PreOrder ? -1 : +1;
            while (idx >= _min && idx < size) {
                if (_iterators.get(idx).hasNext()) {
                    return true;
                }
                idx += delta;
            }
            return false;
        }

        public T next ()
        {
            int size = _iterators.size();
            int delta = _traversal == Traversal.PreOrder ? -1 : +1;
            while (_idx >= _min && _idx < size) {
                Iterator<T> iterator = _iterators.get(_idx);
                if (iterator.hasNext()) {
                    T result = iterator.next();
                    Iterable<T> children = getChildren(result);
                    if (children != null) {
                        Iterator<T> iter = children.iterator();
                        if (iter.hasNext()) {
                            _iterators.add(iter);
                            _idx += (_traversal == Traversal.PreOrder ? 1 : 0);
                        }
                    }
                    return result;
                }
                if (_traversal == Traversal.PreOrder) {
                    _iterators.remove(_idx);
                }
                else {
                    _iterators.set(_idx, null);
                    _min += delta;
                }
                _idx += delta;
            }
            return null;
        }

        public void remove ()
        {
            throw new UnsupportedOperationException();
        }
    }

}
