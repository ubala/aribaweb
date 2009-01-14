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

    $Id: //ariba/platform/ui/metaui-jpa/examples/MetaDB/example/busobj/Project.java#3 $
*/
package example.busobj;

import java.util.List;
import java.math.BigDecimal;

import ariba.util.core.Date;
import ariba.util.core.ListUtil;
import ariba.ui.meta.annotations.Action;
import ariba.ui.meta.annotations.Properties;
import ariba.ui.meta.annotations.NavModuleClass;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.OneToMany;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

@Entity
@NavModuleClass
public class Project
{
    @Id @GeneratedValue
    private Long id;

    public String title;
    String description;
    protected Date deadline;
    BigDecimal budget;
    @ManyToOne protected User owner;
    @ManyToMany protected List <User> team = ListUtil.list();
    @OneToMany protected List <Deal> deals = ListUtil.list();
    @OneToMany protected List <Comment> comments = ListUtil.list();

    public enum Status { Draft, Started, Overdue, Finished }
    Status status;

    public String getTitle ()
    {
        return title;
    }

    public void setTitle (String title)
    {
        this.title = title;
    }

    public String getDescription ()
    {
        return description;
    }

    public void setDescription (String description)
    {
        this.description = description;
    }

    public Date getDeadline ()
    {
        return deadline;
    }

    public void setDeadline (Date deadline)
    {
        this.deadline = deadline;
    }

    public User getOwner()
    {
        return owner;
    }

    public void setOwner(User owner)
    {
        this.owner = owner;
    }

    public List<Comment> getComments()
    {
        return comments;
    }

    public void setComments(List<Comment> comments)
    {
        this.comments = comments;
    }

    /**
     * Deal list manipulation
     */
    public List<Deal> getDeals ()
    {
        return deals;
    }

    public void setDeals(List<Deal> deals)
    {
        this.deals = deals;
    }

    public List getTeam ()
    {
        return team;
    }

    public List setTeam (List team)
    {
        return this.team = team;
    }

    public Deal addNewDeal ()
    {
        Deal deal = new Deal();
        deal.init();
        deals.add(deal);
        return deal;
    }

    public void removeDeal (Deal deal)
    {
        deals.remove(deal);
    }

    public Status getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    @Action(message="Archived Projects: %s")
    @Properties("label:'Archive Projects'")
    public static int archiveProjects ()
    {
        return 37;
    }
}
