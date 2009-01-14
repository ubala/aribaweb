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

    $Id: //ariba/platform/util/expr/ariba/util/fieldtype/ContainerTypeInfo.java#5 $
*/

package ariba.util.fieldtype;

import ariba.util.core.Assert;

/**
 * A generic <code>TypeInfo</code> for specifying a container type as well
 * as its element type.
 * @aribaapi private
*/
public class ContainerTypeInfo extends JavaTypeProvider.JavaTypeInfo
{
    protected TypeInfo  _containerType;
    protected TypeInfo  _elementType;

    protected ContainerTypeInfo ()
    {
    }

    public ContainerTypeInfo (TypeInfo containerType, TypeInfo elementType)
    {
        setContainerTypeInfo(containerType);
        setElementTypeInfo(elementType);
    }

    /**
     * @see TypeInfo#getElementType()
     */
    public TypeInfo   getElementType ()
    {
        return _elementType;
    }

    /**
     * @see TypeInfo#isAssignableFrom
     */
    public boolean    isAssignableFrom (TypeInfo other)
    {
        if (other instanceof NullTypeInfo) {
            return true;
        }

        if (!(other instanceof ContainerTypeInfo)) {
            return false;
        }

        ContainerTypeInfo otherContainer = (ContainerTypeInfo)other;

        return (_containerType.isAssignableFrom(otherContainer._containerType) &&
                _elementType.isAssignableFrom(otherContainer._elementType));
    }

    /**
     * @see TypeInfo#isCompatible
     */
    public boolean    isCompatible (TypeInfo other)
    {
        if (!(other instanceof ContainerTypeInfo)) {
            return false;
        }

        ContainerTypeInfo otherContainer = (ContainerTypeInfo)other;

        return (_containerType.isCompatible(otherContainer._containerType) &&
                _elementType.isCompatible(otherContainer._elementType));
    }

    /**
     * @see TypeInfo#isWideningTypeOf
     */
    public boolean    isWideningTypeOf (TypeInfo other)
    {
        if (!(other instanceof ContainerTypeInfo)) {
            return false;
        }

        ContainerTypeInfo otherContainer = (ContainerTypeInfo)other;

        return (_containerType.isWideningTypeOf(otherContainer._containerType));
    }

    /**
     *
     */
    protected TypeInfo getContainerTypeInfo ()
    {
        return _containerType;
    }

    /**
     *
     */
    protected void setContainerTypeInfo (TypeInfo containerType)
    {
        Assert.that(containerType instanceof JavaTypeProvider.JavaTypeInfo,
             "container type must be a java type. Container type is '%s'.",
            containerType.getName());
        setProxiedClass(((JavaTypeProvider.JavaTypeInfo)containerType).getProxiedClass());
         _containerType = containerType;

    }

    /**
     *
     */
    protected TypeInfo getElementTypeInfo ()
    {
        return _elementType;
    }

    /**
     *
     */
    public void setElementTypeInfo (TypeInfo elementType)
    {
         _elementType = elementType;
    }

}
