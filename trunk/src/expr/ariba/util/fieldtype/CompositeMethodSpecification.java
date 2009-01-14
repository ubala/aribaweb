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

    $Id: //ariba/platform/util/expr/ariba/util/fieldtype/CompositeMethodSpecification.java#2 $
*/

package ariba.util.fieldtype;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import ariba.util.core.ListUtil;

/**
*/
public class CompositeMethodSpecification extends MethodSpecification
{
	List/*<MethodSpecification>*/ _methodSpecifications;
	
	public CompositeMethodSpecification (List collection)
	{
	    _methodSpecifications = ListUtil.list();
	    _methodSpecifications.addAll(collection);
	}
	
	public boolean isSatisfiedBy (Method method)
	{
		for (Iterator i = _methodSpecifications.iterator(); i.hasNext();) {
			MethodSpecification spec = (MethodSpecification)i.next();
			if (spec.isSatisfiedBy(method)) {
				return true;
			}
		}
		return false;
	}
}
