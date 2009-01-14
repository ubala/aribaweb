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

    $Id: //ariba/platform/util/core/ariba/util/core/LinkableList.java#4 $
*/

package ariba.util.core;

/**
    LinkableList implements a fairly straightforward doubly linked
    list.

    The only points are that one should never follow or modify the
    links themselves but make use of the cover fuctions within this
    class. For example,

    DO NOT:
    Shme shme = list.first();
    shme.remove();

    DO:
    Shme shme = list.first();
    list.remove(shme);

    Perhaps I will put this in it's own package sometime soon but for now
    limit yourself to public members of this class.

    A short dicussion in Linkable.java discusses how to implement the
    Linkable interface.

    @aribaapi private
*/
public class LinkableList
{
    LinkedList list = new LinkedList();

    public int size ()
    {
        return this.list.size;
    }

    public boolean empty ()
    {
        return this.list.empty();
    }

    public Linkable first ()
    {
        LinkPayload link = (LinkPayload)this.list.first();
        if (link == null) {
            return null;
        }
        return link.payload;
    }

    public Linkable last ()
    {
        LinkPayload link = (LinkPayload)this.list.last();
        if (link == null) {
            return null;
        }
        return link.payload;
    }

    public void remove (Linkable linkable)
    {
        this.list.remove(linkable.getLink());
    }

    /**
        the insertLinkable is "inserted" into the list after the
        referenceLinkable
    */
    public void addAfter (Linkable referenceLinkable, Linkable insertLinkable)
    {
        this.list.addAfter(referenceLinkable.getLink(),
                           insertLinkable.getLink());
    }

    /**
        the insertLinkable is "inserted" into the list before the
        referenceLinkable
    */
    public void addBefore (Linkable referenceLinkable, Linkable insertLinkable)
    {
        this.list.addBefore(referenceLinkable.getLink(),
                            insertLinkable.getLink());
    }

    /**
        the Linkable is put at the head of the list
    */
    public void insert (Linkable linkable)
    {
      this.list.insert(linkable.getLink());
    }

    /**
        the Linkable is put somewhere in the list
    */
    public void add (Linkable linkable)
    {
        this.insert(linkable);
    }

    /**
        the Linkable is put at the tail of the list
        (Currently this is not optimized - the list must be walked to the end)
    */
    public void append (Linkable linkable)
    {
        this.list.append(linkable.getLink());
    }

    public String toString ()
    {
        return this.list.toString();
    }

    /**
        return the next Linkable following the Linkable passed in
    */
    public Linkable next (Linkable linkable)
    {
        LinkPayload link = (LinkPayload)linkable.getLink().next;
        if (link == null) {
            return null;
        }
        return link.payload;
    }

    /**
        return the prev Linkable following the Linkable passed in
    */
    public Linkable prev (Linkable linkable)
    {
        LinkPayload link = (LinkPayload)linkable.getLink().prev;
        if (link == null) {
            return null;
        }
        return link.payload;
    }
}
