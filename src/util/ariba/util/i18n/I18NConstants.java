/*
    Copyright (c) 2013-2013 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id$
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

*/

package ariba.util.i18n;


/**
    Class I18NConstants contains all public constants used by I18N.

    @aribaapi ariba
*/

public final class I18NConstants extends I18NEncodingConstants
{
    /*
        Encoding strings for email header
    */
    public static final String EncodingQ  = "Q";
    public static final String EncodingB  = "B";

    private static int      count         = 0;


    public static int count ()
    {
        return count;
    }

}
