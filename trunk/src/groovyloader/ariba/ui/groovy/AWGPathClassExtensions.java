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

    $$
*/
package ariba.ui.groovy;

import ariba.util.fieldvalue.FieldValue_Object;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.fieldvalue.OrderedList;
import ariba.util.core.Assert;
import groovy.util.slurpersupport.GPathResult;
import groovy.util.slurpersupport.NodeChild;
import java.util.ArrayList;

/**
    FieldValue support for Groovy GPath (e.g. XMLSlurper) results.
    Makes working with GPath results in AW bindings consistent with
    binding to XML DOM Nodes
  */
public class AWGPathClassExtensions
{
    static {
        // register class extensions
        FieldValue.registerClassExtension(GPathResult.class, new GPath_FieldValue());
        FieldValue.registerClassExtension(NodeChild.class, new NodeChild_FieldValue());
        OrderedList.registerClassExtension(GPathResult.class, new GPathResult_OrderedList());
    }

    static void initialize () {}
    
    static class GPath_FieldValue extends FieldValue_Object
    {
        public Object getFieldValuePrimitive (Object receiver, FieldPath keyPath)
        {
            GPathResult node = (GPathResult)receiver;
            String key = keyPath.car();

            // magic keys...
            if (key.equals("tagName")) {
                return node.name();
            }

            if (key.equals("children")) {
                return node.children();
            }

            if (key.equals("text")) {
                return node.text();
            }

            if (key.endsWith("[]")) {
                key = key.substring(0, key.length()-2);
                return node.getProperty(key);
            }

            // Expect single value
            Object val = node.getProperty(key);

            if (val instanceof GPathResult) {
                int size = ((GPathResult)val).size();
                val = size > 0 ? ((GPathResult)val).getAt(0) : null;
            }

            return val;
        }
    }

    static class NodeChild_FieldValue extends GPath_FieldValue
    {
        public Object getFieldValuePrimitive (Object receiver, FieldPath keyPath)
        {
            NodeChild node = (NodeChild)receiver;
            String key = keyPath.car();

            // try a string value
            Object attrValue = node.attributes().get(key);
            if (attrValue != null) return attrValue;

            return super.getFieldValuePrimitive(receiver, keyPath);
        }
    }

    static class GPathResult_OrderedList extends OrderedList
    {
        public int size(Object receiver)
        {
            return ((GPathResult)receiver).size();
        }

        public Object elementAt(Object receiver, int elementIndex) {
            return ((GPathResult)receiver).getAt(elementIndex);
        }

        public java.util.Iterator elements (Object receiver)
        {
            return ((GPathResult)receiver).iterator();
        }

        public Object mutableInstance(Object receiver) {
            return new ArrayList();
        }

        public void setElementAt(Object receiver, Object element, int elementIndex) {
            Assert.that(false, "Not supported");
        }

        public void addElement(Object receiver, Object element) {
            Assert.that(false, "Not supported");
        }

        public void insertElementAt(Object receiver, Object element, int elementIndex) {
            Assert.that(false, "Not supported");
        }
    }
}