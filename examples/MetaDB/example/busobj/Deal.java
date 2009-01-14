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

    $Id: //ariba/platform/ui/metaui-jpa/examples/MetaDB/example/busobj/Deal.java#1 $
*/
package example.busobj;

import ariba.util.core.ListUtil;
import ariba.ui.meta.annotations.NavModuleClass;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import java.math.BigDecimal;
import java.util.List;

@Entity
@NavModuleClass
public class Deal
{
    @Id @GeneratedValue
    private Long id;
        
    protected String description;
    protected BigDecimal value;
    protected boolean active;
    @ManyToOne
    protected User lead;
    @ManyToMany
    protected List <User> contacts;

    public String getDescription () {
        return description;
    }

    public void setDescription (String description) {
        this.description = description;
    }

    public BigDecimal getValue ()
    {
        return value;
    }

    public void setValue (BigDecimal value)
    {
        this.value = value;
    }

    public boolean getActive ()
    {
        return active;
    }

    public void setActive (boolean active)
    {
        this.active = active;
    }

    public User getLead ()
    {
        return lead;
    }

    public void setLead (User lead)
    {
        this.lead = lead;
    }

    public List<User> getContacts ()
    {
        return contacts;
    }

    public void setContacts (List<User> contacts)
    {
        this.contacts = contacts;
    }

    /**
     * Init new deal (as opposed to one being reconstituted from a database
     */
    public void init ()
    {
        description = "Untitled";
        value = new BigDecimal(0.0);
        contacts = ListUtil.list();
    }
}
