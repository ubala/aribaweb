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

    $Id: //ariba/platform/util/core/ariba/util/core/LinkedList.java#4 $
*/

package ariba.util.core;

/**
    This class implements a basic doublely linked list. However, it is
    recommended that one use the LinkableList class as it removes the
    requirement that the object being linked into the list be a
    subclass of Link; Linkable list only requires that one implement
    the Linkable interface.

    LinkableList uses LinkedList as a base implementation

    @aribaapi private
*/
public class LinkedList
{
    Link head = null;
    int size = 0;

    public int size ()
    {
        return size;
    }

    public boolean empty ()
    {
        return (this.head == null);
    }

    public Link first ()
    {
        return this.head;
    }

    /**
        Note: This can be optimized by using a tail pointer
    */
    public Link last ()
    {
        if (this.empty()) {
            return null;
        }
        Link cursor;
        for (cursor = this.head; cursor.next != null; cursor = cursor.next) {
            ;
        }
        return cursor;
    }

    public void remove (Link link)
    {
        this.size--;
        if (this.head == link) {
            this.head = link.next;
        }
        link.remove();
    }

    public void addAfter (Link referenceLink, Link insertLink)
    {
        size++;
        referenceLink.append(insertLink);
    }

    public void addBefore (Link referenceLink, Link insertLink)
    {
        if (this.head == referenceLink) {
            // we are inserting to the head of the list
            this.insert(insertLink);
        }
        else {
            referenceLink.insert(insertLink);
            this.size++;
        }
    }

    public void insert (Link link)
    {
        this.size++;
        if (this.head != null) {
            this.head.insert(link);
        }
        this.head = link;
    }

    public void add (Link link)
    {
        this.insert(link);
    }

    public void append (Link link)
    {
        this.size++;
        if (this.head != null) {
            this.last().append(link);
        }
        else {
            this.head = link;
        }
    }

    public String toString ()
    {
        FormatBuffer toString = new FormatBuffer();
        Fmt.B(toString, "LinkedList (%s) elements: ", Constants.getInteger(this.size));
        for (Link cursor = head; cursor != null; cursor = cursor.next) {
            Fmt.B(toString, "%s,", cursor);
        }
        return toString.toString();
    }
}
