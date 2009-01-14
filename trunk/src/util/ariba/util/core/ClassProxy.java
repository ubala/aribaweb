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

    $Id: //ariba/platform/util/core/ariba/util/core/ClassProxy.java#2 $
*/

package ariba.util.core;

/**
 * Objects that implement ClassProxy will be able to get the Class of the 
 * object they represent, and answer convenient instanceOf questions.  Proxies 
 * will return the Class of their target object, and non-proxies will return 
 * their own Class.
 * @aribaapi private
 */
public interface ClassProxy
{
    /**
     * Return Class for the real object that a proxy represents, or the same
     * as getClass() if called on the real object and it implements ClassProxy.
     * java.lang.Object.getClass() is final and always returns the Class of the
     * object it is called on.
     * @aribaapi private
     */
    public Class getRealClass ();
    
    /**
     * Return true if the real Class for this object is an instanceof the given
     * Java Class (which may represent a class or an interface). 
     * For a ClusterRoot it is about the ClusterRoot itself, but for a proxy 
     * ClusterRootValueSource, which extends ClassProxy, it is about the 
     * ClusterRoot the proxy represents.  Returns false silently if the given 
     * Java Class is null.
     * @aribaapi private
     */
    public boolean instanceOf (Class cl);

    /**
     * Return true if the real Class for this object is an instanceof the given
     * className; for a ClusterRoot it is about the ClusterRoot itself, but for
     * a proxy ClusterRootValueSource, it is about the ClusterRoot the proxy 
     * represents.  For ClusterRoots, it should handle Ariba dynamic type names
     * as well as Java class and interface names.  Returns false silently in all
     * other cases.
     * @aribaapi private
     */
    public boolean instanceOf (String className);
}
