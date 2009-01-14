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

    $Id: //ariba/platform/util/core/ariba/util/core/ClassFactory.java#4 $
*/

package ariba.util.core;

/**
    Interface to provide settable class factory. This is only
    supported for development mode, not a production system.
    @aribaapi ariba
*/
public interface ClassFactory
{
    /**
        Look up a class for the given name.
        @return the Class for the given <code>className</code>, or null if
        the Class doesn't exist.
    */
    public Class forName (String className);
}
