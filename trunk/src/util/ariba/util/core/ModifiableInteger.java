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

    $Id: //ariba/platform/util/core/ariba/util/core/ModifiableInteger.java#2 $
*/

package ariba.util.core;

/**
    This is a handy class to use in cases where Integer needs to be part of a
    Collection object, yet needs to be modified very frequently. Especially when
    the integer value does not fall between Constants.MinSavedInt and 
    Constants.MaxSavedInt

    @aribaapi documented
*/
public class ModifiableInteger
{
    private int n;

    public ModifiableInteger ()
    {
        n = 0;
    }

    public ModifiableInteger (int i)
    {
        n = i;
    }

    public void setInt (int newValue)
    {
        n = newValue;
    }

    public int getInt ()
    {
        return n;
    }

    public int addOne ()
    {
        n++;
        return n;
    }

    public String toString ()
    {
        return Fmt.S("ModifiableInteger[%s]" + Integer.toString(n));
    }
}
