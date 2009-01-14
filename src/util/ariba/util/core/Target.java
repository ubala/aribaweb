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

    $Id: //ariba/platform/util/core/ariba/util/core/Target.java#4 $
*/

package ariba.util.core;

/**
    Interface enabling a generalized object request framework. Objects
    need to ask other objects to perform certain actions. It may not
    be feasible, however, for the sender to know the exact message to
    send to the target object at compile time. With the Target
    interface, the sender does not have to know anything about the
    target except that it implements the Target interface. The string
    <b>command</b> describes the action that the target should
    perform, with the arbitrary datum <b>data</b>. For example, when
    pressed, a Button needs to ask some object to perform a specific
    action. Rather than subclass Button to connect it to a specific
    method in a specific class, Button sends its messages to a Target
    instance, passing a string command (set as appropriate) and itself
    as the object.

    @aribaapi private
*/
public interface Target
{
    /**
        Tells the target to perform the command <b>command</b>, using
        datum <b>data</b>.
    */
    public void performCommand (String command, Object data);
}

