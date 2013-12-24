/*
    Copyright 1996-2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/ObjectUtil.java#1 $
*/
package ariba.util.core;

/**
    This is a class of static methods useful for Objects in general. Currently it has
    helper methods that help in building equals and hashCode methods for Objects.
    @aribaapi private
*/
public class ObjectUtil
{
    /**
        Returns true if the two objects are both null, or both the same instance, or if
        they are both non-null and their equals method returns true. This method is
        helpful when building an equals method for an object, based on equality of its
        fields, where the fields may be null.
    */
    public static boolean equalsObj (Object obj1, Object obj2)
    {
        return (obj1 == obj2) || (obj1 != null && obj1.equals(obj2));
    }

    /**
        Returns int hash code based on the previous hash code combined with the given int
        value, following the algorithm outlined in Sun's Javadoc for java.util.List
        hashCode. By convention, the initial value of hashCode should be 1, and the values
        corresponding to significant fields of the class should be combined in a canonical
        order, because order matters to this algorithm. This method is helpful when
        building a hashCode method for an object, based on combining int values
        corresponding to its fields.
    */
    public static int hashCodeNext (int hashCode, int value)
    {
        return (31 * hashCode) + value;
    }

    /**
        Returns int hash code based on the previous hash code combined with the given long
        value, splitting the long value into two int values and doing the little-endian int
        first, and the big-endian int second.
    */
    public static int hashCodeNext (int hashCode, long value)
    {
        hashCode = hashCodeNext(hashCode, (int)(value & 0xffffffff));
        hashCode = hashCodeNext(hashCode, (int)((value >>> 32) & 0xffffffff));
        return hashCode;
    }

    /**
        Returns int hash code based on the previous hash code combined with the given
        boolean value, using int 1 for true and 0 for false.
    */
    public static int hashCodeNext (int hashCode, boolean value)
    {
        return hashCodeNext(hashCode, (value ? 1 : 0));
    }

    /**
        Returns int hash code based on the previous hash code combined with the hashcode
        for the given Object, or 0 if the Object is null. This method is helpful when
        building a hashCode method for an object, based on combining int values
        corresponding to its Object fields, where the fields may be null.
    */
    public static int hashCodeNext (int hashCode, Object obj)
    {
        return hashCodeNext(hashCode, (obj != null ? obj.hashCode() : 0));
    }
}
