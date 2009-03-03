/*
    Copyright 2008 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/metaui-jpa/examples/appcore/ariba/appcore/Global.java#3 $
*/
package ariba.appcore;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;

import java.util.Date;
import ariba.ui.meta.persistence.ObjectContext;

import java.util.Collections;

@Entity
public class Global
{
    @Id @GeneratedValue
    private Long id;

    @Column(name="thekey")
    String key;

    Date lastModified;

    long longValue;

    String stringValue;

    public Date getLastModified ()
    {
        return lastModified;
    }

    protected void modified ()
    {
        // bogus: should update with pre-commit observer
        lastModified = new Date();
    }

    public long getLongValue ()
    {
        return longValue;
    }

    public void setLongValue (long longValue)
    {
        modified();
        this.longValue = longValue;
    }

    public String getStringValue ()
    {
        return stringValue;
    }

    public void setStringValue (String stringValue)
    {
        modified();
        this.stringValue = stringValue;
    }

    public static Global find (String key)
    {
        ObjectContext context = ObjectContext.get();
        return context.findOne(Global.class, Collections.singletonMap("key", (Object)key));
    }

    public static Global create (String key)
    {
        ObjectContext context = ObjectContext.get();
        Global global = context.create(Global.class);
        global.key = key;
        global.modified();
        return global;
    }

    public static Global findOrCreate (String key)
    {
        Global global = find(key);
        return  (global != null) ? global : create(key);
    }
}
