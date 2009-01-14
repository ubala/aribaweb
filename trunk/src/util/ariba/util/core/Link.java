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

    $Id: //ariba/platform/util/core/ariba/util/core/Link.java#4 $
*/

package ariba.util.core;

/**
    Link implements a fairly textbook "link" suitable for use
    in a doubly linked list.  You can append, insert another link or
    remove the link itself....

    @aribaapi private
*/
public class Link
{
    public Link next = null;
    public Link prev = null;

    protected void append (Link link)
    {
        link.prev = this;
        link.next = this.next;
        this.next = link;
        if (link.next != null) {
            link.next.prev = link;
        }
    }

    protected void insert (Link link)
    {
        link.next = this;
        link.prev = this.prev;
        this.prev = link;
        if (link.prev != null) {
            link.prev.next = link;
        }
    }

    protected void remove ()
    {
        if (this.prev != null) {
            this.prev.next = this.next;
        }
        if (this.next != null) {
            this.next.prev = this.prev;
        }
        this.next = null;
        this.prev = null;
    }
}
