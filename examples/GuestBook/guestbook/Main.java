package guestbook;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.Date;
import ariba.util.core.StringUtil;

import java.util.List;
import java.util.Vector;

public class Main extends AWComponent
{
    public static class Entry {
        public Date date = new Date();
        public String name = "Anonymous";
        public String comment;
    }

    public static List<Entry> _entries = new Vector();

    public Entry _current = new Entry(), _item;

    public void add ()
    {
        // abort if we have validation errors
        if (errorManager().checkErrorsAndEnableDisplay()) return;

        _current.date = new Date();
        if (StringUtil.nullOrEmptyOrBlankString(_current.name)) _current.name = "Anonymous";
        
        _entries.add(_current);
        _current = new Entry();
    }

    public void delete ()
    {
        _entries.remove(_item);
    }
}
