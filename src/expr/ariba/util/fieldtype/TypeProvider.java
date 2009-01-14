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

    $Id: //ariba/platform/util/expr/ariba/util/fieldtype/TypeProvider.java#2 $
*/

package ariba.util.fieldtype;

import ariba.util.core.Assert;

/**
 * TypeProvider provides type lookup for a single "type source".  A type itself
 * is a logical concept which can have multiple physical representations.  Each
 * representation is captured in a type source which captures a certain asepct
 * (slice of properties and behaviors) of this type.   For example,
 * the AML type source is a collection of AML files.  A Java type source is
 * a collection of Java classes.   In another words, a TypeProvider is an
 * adaptor to a particular physical representation of a "type".
 *
 * @aribaapi private
*/
public abstract class TypeProvider implements TypeRetriever
{
    protected static final int PrimitiveTypeProviderId = 1;
    protected static final int AMLTypeProviderId = 2;
    protected static final int JavaTypeProviderId = 3;

    private int _providerId;

    protected TypeProvider (int providerId)
    {
        _providerId = providerId;
    }

    public boolean equals (Object thatObj)
    {
        // This method must work across different types of TypeProvider.
        Assert.that(thatObj instanceof TypeProvider,
            "The provided object is not a type provider.");
        TypeProvider thatProvider = (TypeProvider)thatObj;
        return thatProvider.getProviderId() == getProviderId();
    }

    public int getProviderId ()
    {
        return _providerId;
    }
}

