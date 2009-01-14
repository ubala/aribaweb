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

    $Id: //ariba/platform/util/core/ariba/util/core/Priority.java#4 $
*/

package ariba.util.core;

/**
    Priority represent the user preceived speed of an operation, not
    necessarily the speed of the operation.  For example, if the user
    is waiting for the client to redraw the screen, any operation used to
    redraw the screen should be delt with as a high priority.  The user
    should not wait for the screen to redraw.  On the other hand, they may
    expect to wait for a requisition to be submitted.

    Fortunately, preceived speed is often the reality.  Submitting it slow.
    Saving the requisition is medium slow, etc.

    There are only three rankings because it is felt that more just because
    confusing.  When you create a new rpc set the ranking at the level you
    think the customer is going to feel like the performance of the product
    is good if that operation takes x amount of time.

    You can set the default of the RPC or choose method by method numbers.

        public static final int Rank_Default = <some number or constant>

    applies to the entire rpc file.

        public static final int Rank_methodName = <some number or constant>

    applies only to the methodName.

    @aribaapi private
*/
public interface Priority
{
    public static final int High   = 1;
    public static final int Medium = 2;
    public static final int Low    = 3;

    public static final int TotalPriorities = 3;

    public static final int BackGroundProcesses = 11;
    public static final int FindWorkerQueue     = 12;
};
