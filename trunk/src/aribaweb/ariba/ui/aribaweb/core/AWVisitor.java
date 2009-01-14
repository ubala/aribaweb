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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWVisitor.java#5 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;

/**
    Abstract base class of visitor class used to perform some action or validation on
    all nodes within a tree of AWVisitable's.

    Sub-classes need to provide their own version of performAction which performs the
    action or validation, and call super when it is ready to process the child nodes
    of the current visitable node.

    @aribaapi ariba
*/
abstract public class AWVisitor extends AWBaseObject
{
    public void performAction (AWVisitable visitable)
    {
        visitable.continueVisit(this);
    }
}