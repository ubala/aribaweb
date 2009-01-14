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

    $Id: //ariba/platform/util/core/ariba/util/core/NamedList.java#4 $
*/

package ariba.util.core;

import java.io.Externalizable;
import java.util.List;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
    @aribaapi private
*/
public class NamedList extends Vector implements Externalizable
{
    private String name;

    public NamedList (String name)
    {
        this.name = name;
    }

    /** This constructor creates NamedList of NamedLists
        from a vector of vectors.
    */
    public NamedList (List original)
    {
        super(original.size()-1);

        for (int i=0;i< original.size();i++) {
            Object element = original.get(i);
            if (i == 0) {
                this.name = (String)element;
            }
            else if (element instanceof List &&
                     (ListUtil.firstElement((List)element)) instanceof String)
            {
                add(new NamedList ((List)element));
            }
        }
    }

    public String name ()
    {
        return name;
    }

    public NamedList namedListNamed (String vectorName)
    {
        for (int i=0; i< size(); i++) {
            Object element = get(i);
            if (element instanceof NamedList) {
                NamedList nv = (NamedList)element;
                if ((nv.name == vectorName) ||
                    ((vectorName != null) && (vectorName.equals(nv.name()))))
                {
                    return nv;
                }
            }
        }
        return null;
    }

    public String toString ()
    {
        FastStringBuffer result = new FastStringBuffer("(Name = ");
        result.append(name);
        result.append(';');
        result.append(' ');
        for (int i=0; i<size(); i++) {
            result.append(' ');
            result.append(get(i).toString());
        }
        result.append(')');
        return result.toString();
    }

    //--- implements the Externalizable interface -----------------------------

    /* Constructor with no arguments */
    public NamedList ()
    {
    }

    /**
        @aribaapi private
    */
    public void writeExternal (ObjectOutput output) throws IOException
    {
        super.writeExternal(output);
        output.writeObject(name);
    }
    /**
        @aribaapi private
    */
    public void readExternal (ObjectInput input)
      throws IOException, ClassNotFoundException
    {
        super.readExternal(input);
        name = (String)input.readObject();
    }
}
