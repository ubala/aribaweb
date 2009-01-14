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

    $Id: //ariba/platform/util/core/ariba/util/core/CryptoInterface.java#5 $
*/

package ariba.util.core;

import java.util.List;
import java.util.Map;

/**
    @aribaapi private
*/
public interface CryptoInterface
{
    public Object encrypt (Object target);
    public Map encrypt (Map target);
    public List encrypt (List target);
    public String encrypt (String target);

    public Object decrypt (Object target);
    public Map decrypt (Map target);
    public List decrypt (List target);
    public String decrypt (String target);
}

