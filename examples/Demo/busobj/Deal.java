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

    $Id: //ariba/platform/ui/opensourceui/examples/Demo/busobj/Deal.java#1 $
*/
package busobj;

import ariba.util.core.ListUtil;

import java.math.BigDecimal;
import java.util.List;

public class Deal
{
    protected String _description;
    protected BigDecimal _value;
    protected boolean _active;
    protected User _lead;
    protected List <User>_contacts;

    public String getDescription () {
        return _description;
    }

    public void setDescription (String description) {
        _description = description;
    }

    public BigDecimal getValue ()
    {
        return _value;
    }

    public void setValue (BigDecimal value)
    {
        _value = value;
    }

    public boolean getActive ()
    {
        return _active;
    }

    public void setActive (boolean active)
    {
        _active = active;
    }

    public User getLead ()
    {
        return _lead;
    }

    public void setLead (User lead)
    {
        _lead = lead;
    }

    public List<User> getContacts ()
    {
        return _contacts;
    }

    public void setContacts (List<User> contacts)
    {
        _contacts = contacts;
    }

    /**
     * Init new deal (as opposed to one being reconstituted from a database
     */
    public void init ()
    {
        _description = "Untitled";
        _value = new BigDecimal(0.0);
        _contacts = ListUtil.list();
    }
}
