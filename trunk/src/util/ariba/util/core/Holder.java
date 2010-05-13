/*
    Copyright (c) 1996-2007 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/Holder.java#1 $

    Responsible: dfinlay
*/
package ariba.util.core;

/**
    Convenience class that makes it a little simpler to write a lazily evalated
    value. <p/>

    Use as follows. Instead of writing:<p>
    <pre>
    List&lt;String> _list;
    public List&lt;String> list() {
      if (_list == null) {
        _list = initialzeList();
      }
      return _list;
    }
    </pre>
    You write:
    <pre>
    Holder&lt;List&lt;String>> _list = new Holder&lt;List&lt;String>>() {
        protected List&lt;String> initialValue() { 
          return initializeList();
        }
    }
    </pre>
    You then access the value by <code>_list.get().indexOf("foo")</code> etc.
    @aribaapi ariba
*/
public abstract class Holder<V>
{
    //--------------------------------------------------------------------------
    // data members

    private V _value;

    //--------------------------------------------------------------------------
    // protected methods

    /**
        Subclasses must override to provide the value.
        @aribaapi ariba
    */
    protected abstract V initialValue ();

    //--------------------------------------------------------------------------
    // public methods

    /**
        Returns the value held by <code>this</code>.
        @aribaapi ariba
    */
    public V get ()
    {
        if (_value == null) {
            _value = initialValue();
        }
        return _value;
    }
}
