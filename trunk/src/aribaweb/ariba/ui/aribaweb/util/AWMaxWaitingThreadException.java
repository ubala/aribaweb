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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWMaxWaitingThreadException.java#5 $
*/

package ariba.ui.aribaweb.util;

/**
    This exception is thrown when the count of threads waiting
    for a resource has exceeded the maximum.
*/

public final class AWMaxWaitingThreadException extends RuntimeException
{
    public AWMaxWaitingThreadException ()
    {
        super();
    }

    public AWMaxWaitingThreadException (String exceptionMessage)
    {
        super(exceptionMessage);
    }
}
