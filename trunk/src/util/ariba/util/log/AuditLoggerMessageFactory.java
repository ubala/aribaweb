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

    $Id: //ariba/platform/util/core/ariba/util/log/AuditLoggerMessageFactory.java#3 $
*/
package ariba.util.log;

import ariba.util.core.ClassUtil;

/**
 * @aribaapi ariba
 */
public class AuditLoggerMessageFactory
{
    private String className;

    public AuditLoggerMessageFactory (String className)
    {
        this.className = className;
    }

    public AuditLoggerMessage createAuditLoggerMessage ()
    {
        return (AuditLoggerMessage)ClassUtil.newInstance(
            className, AuditLoggerMessage.class, true);
    }
}
