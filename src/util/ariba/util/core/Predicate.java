/*
    Copyright 2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/Predicate.java#1 $
*/

package ariba.util.core;


public abstract class Predicate<T>
{
    abstract public boolean evaluate(T t);

    public static class Constant<T> extends Predicate
    {
        public static final Constant True = new Constant(true);
        public static final Constant False = new Constant(false);

        public static <V> Constant<V> getTrue ()
        {
            return True;
        }
        public static <V> Constant<V> getFalse ()
        {
            return False;
        }

        boolean _value;

        private Constant (boolean value)
        {
            _value = value;
        }

        @Override
        public boolean evaluate (Object o)
        {
            return _value;
        }
    }

}
