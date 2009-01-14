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

    $Id: //ariba/platform/util/core/ariba/util/formatter/FormatterHandlesNulls.java#2 $
*/

package ariba.util.formatter;

/**
    This interface marks formatting classes that handle null values.
    The default for the UI infrastructure is that null values are represented as empty
    strings.  However, some formatters may wish to handle null values explicitly.  Such
    formatters can implement this interface - and then the low-level UI components will
    not use the default behavior (empty strings) but let the formatter handle them.
    @aribaapi documented
*/
public interface FormatterHandlesNulls
{
}
