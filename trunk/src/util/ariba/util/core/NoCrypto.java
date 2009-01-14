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

    $Id: //ariba/platform/util/core/ariba/util/core/NoCrypto.java#5 $
*/

package ariba.util.core;

import java.util.List;
import java.util.Map;

/**
    Disclaimer. This is not production security! This is a weak
    attempt to thrawrt direct or accidental packet sniffing from
    obtaining cleartext passwords. Of course real encryption is the
    solution but at this moment is not achievable due to scheule
    constraints.

    @aribaapi private
*/
public class NoCrypto implements CryptoInterface
{
    public Object encrypt (Object target)
    {
        return target;
    }

    public Map encrypt (Map target)
    {
        return target;
    }

    public List encrypt (List target)
    {
        return target;
    }

    public String encrypt (String target)
    {
        return target;
    }

    public Object decrypt (Object target)
    {
        return target;
    }

    public Map decrypt (Map target)
    {
        return target;
    }

    public List decrypt (List target)
    {
        return target;
    }

    public String decrypt (String target)
    {
        return target;
    }

}

