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

    $Id: //ariba/platform/util/expr/ariba/util/fieldtype/TypeInfo.java#9 $
*/

package ariba.util.fieldtype;

import java.util.List;
import java.util.Set;

/**
 * TypeInfo is an abstraction of a type of an underlying type system.
 * @aribaapi private
*/
public interface TypeInfo
{
    /**
     * The accessiblity code for the type.<ol>
     * <li>AccessibilityPrivate - private accessiblity.  This indicates that
     * access is limited to within Ariba shipped codeline.</li>
     * <li>AccessibilityPublic - public accessibility.  This indicates that
     * the access is open for customization with possible future upgrade or
     * compatibility issues.</li>
     * <li>AccessibilitySafe - safe accessbility.  This indicates that the
     * access is open for customization without future upgrade or
     * compatbility issue.<li>
     */
    public static final int AccessibilityPrivate = 0;
    public static final int AccessibilityPublic = 1;
    public static final int AccessibilitySafe = 2;

    /**
     * Get the type name.
     * @return the name of the type
     */
    public String     getName ();

    /**
     * Get the implementation name.
     * @return the implementation name of the type.  It is typically the class
     * name of the type.
     */
    public String     getImplementationName ();

    /**
     * If this type is a collection of element type, this method returns
     * the <code>TypeInfo</code> of the element type.  Otherwise, this method
     * return false.
     * @return the <code>TypeInfo</code> of element type. Otherwise, return null.
     */
    public TypeInfo   getElementType ();

    /**
     * This method tests if <code>this</code> type is asssignable from
     * <code>other</code> type.  This method should also handle
     * auto-boxing and auto-unboxing for primitive types.
     * @param other - the type to assign to <code>this</code> type.
     * @return true if the test condition passes.
     */
    public boolean    isAssignableFrom (TypeInfo other);

    /**
     * This method returns true if one type is assignable to another. This
     * implies that one type is convertible to another through reference
     * widening or narrowing.
     * For primitive type, this method can also return true if the two
     * types can be promoted into a common type.  This method should also handle
     * auto-boxing and auto-unboxing for primitive types.
     * @param other - the other type to compare with <code>this</code> type.
     * @return true if the test condition passes.
     */
    public boolean    isCompatible (TypeInfo other);

    /**
     * This method returns true if one type can be widening to another.
     * This typically checks using referencing widening where <code>this</code>
     * type is a superclass of <code>other</code> type.
     * For primitive type, this method can also return true if the
     * <code>other</code> type can be promoted to <code>this</code> type using
     * java primitive widening. This method should also handle auto-boxing and
     * auto-unboxing for primitive types.
     * @param other - the other type to compare with <code>this</code> type.
     * @return true if the test condition passes.
     */
    public boolean    isWideningTypeOf (TypeInfo other);

    /**
     * Return the accessiblity code of this type.
     * @return Return the value of AccessibilityPrivate, AccessibilityPublic,
     * or AccessibilitySafe.
     */
    public int        getAccessibility ();

    /**
     * Get the field belongs to this <code>type</code>.
     * @param retriever - A <code>TypeRetriever</code> for retriever type
     * information.
     * @param name - the name of the field
     * @return A <code>FieldInfo</code> object representing the field.
     */
    public FieldInfo  getField (TypeRetriever retriever, String name);

    /**
     * Get the method belongs to this <code>type</code>.
     * @param retriever - A <code>TypeRetriever</code> for retriever type
     * information.
     * @param name - the name of the method
     * @param parameters - list of parameter type names
     * @param staticOnly will be true when used in a class static context such as
     * className.staticMethod(), will be false for non static usages e.g.
     * instance.staticMethod() or instance.nonStaticMethod()
     * @return A <code>MethodInfo</code> object representing the method.
     */
    public MethodInfo getMethodForName (TypeRetriever retriever,
                                        String name,
                                        List<String> parameters,
                                        boolean staticOnly);
    
    
    /**
     * Get all methods belong to this <code>type</code>.
     * @param retriever - A <code>TypeRetriever</code> for retriever type
     * information.
     * @return A <code>Set</code> of <code>MethodInfo</code> objects 
     * representing all methods.
     */
    public Set/*<MethodInfo>*/ getAllMethods (TypeRetriever retriever);


    /**
     * Create a <code>PropertyResolver</code> that can be used to resolve
     * property name.
     * @return A <code>PropertyResovler</code>
     */
    public PropertyResolver getPropertyResolver ();
}
