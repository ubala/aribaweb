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

    $Id: //ariba/platform/util/core/ariba/util/core/Parameters.java#13 $
*/

package ariba.util.core;

import ariba.util.formatter.DateFormatter;
import ariba.util.formatter.DoubleFormatter;
import ariba.util.formatter.IntegerFormatter;
import ariba.util.formatter.LongFormatter;
import ariba.util.log.Log;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
    Interface to access easily configuration parameters

    @aribaapi ariba
*/
public abstract class Parameters
{
    /**
        Returns the value of the parameter
        @param parameter fully qualified name of the parameter
        @return the value of the parameter
        @aribaapi ariba
    */
    public Object getParameter (String parameter)
    {
        return getParameter(parameter, true, true);
    }

    protected Object getParameter (String parameter, boolean warning)
    {
        return getParameter(parameter, warning, true);
    }

    /**
        DO NOT CALL THIS UNLESS YOU ARE IN THE SERVER INTERNALS.

        BaseService and Admin UI  calls with warnings and useDefault disabled
        so it test and do defaulting quietly.

        Be careful with useDefault !!! If you call once with useDefault=true
        then the next calls for the same parameter will return the default
        value even if useDefault is later false. This is because the value
        is cached once for all.

        @aribaapi private
    */
    public abstract Object getParameter (String parameter,
                                         boolean warning,
                                         boolean useDefault);

    public abstract Object getUncachedParameter (String parameter,
                                         boolean warning,
                                         boolean useDefault);

    public String getStringParameter (String parameter)
    {
        Object object = getParameter(parameter, true);

        if ((object == null) || (object instanceof String)) {
            return (String)object;
        }
        Log.util.warning(2880, "String", parameter, object);
        return null;
    }

    /**
        @deprecated use getMapParameter(String)
    */
    public Map getHashtableParameter (String parameter)
    {
        return getMapParameter(parameter);
    }

    public Map getMapParameter (String parameter)
    {
        Object object = getParameter(parameter, true);
        if ((object == null) || (object instanceof Map)) {
            return (Map)object;
        }
        Log.util.warning(2880, "Map", parameter, object);
        return null;
    }

    /**
        @deprecated use getListParameter(String)
    */
    public List getVectorParameter (String parameter)
    {
        return getListParameter(parameter);
    }

    public List getListParameter (String parameter)
    {
        Object object = getParameter(parameter, true);
        if ((object == null) || (object instanceof List)) {
            return ListUtil.immutableList((List)object);
        }
        Log.util.warning(2880, "List", parameter, object);
        return null;
    }

    public boolean getBooleanParameter (String parameter)
    {
        Object object = getParameter(parameter, true);

        if (object instanceof Boolean) {
            return ((Boolean)object).booleanValue();
        }
        if (object instanceof String) {
            Assert.assertNonFatal(false,
                                  "Parameter %s should be defined with the type Boolean",
                                  parameter);
            return Constants.getBoolean((String)object).booleanValue();
        }
        Log.util.warning(2880, "Boolean", parameter, object);
        return false;
    }

    public int getIntParameter (String parameter)
    {
        Object object = getParameter(parameter, true);

        if (object instanceof Integer) {
            return ((Integer)object).intValue();
        }
        if (object instanceof String) {
            Assert.assertNonFatal(false,
                                  "Parameter %s should be defined with the type Integer",
                                  parameter);
            try {
                return IntegerFormatter.parseInt((String)object);
            }
            catch (ParseException pe) {
                Log.util.warning(2883, parameter, object);
                return 0;
            }
        }
        Log.util.warning(2880, "Integer", parameter, object);
        return 0;
    }

    public long getLongParameter (String parameter)
    {
        Object object = getParameter(parameter, true);

        if (object instanceof Long) {
            return ((Long)object).longValue();
        }
        if (object instanceof String) {
            Assert.assertNonFatal(false,
                                  "Parameter %s should be defined with the type Long",
                                  parameter);
            try {
                return LongFormatter.parseLong((String)object);
            }
            catch (ParseException pe) {
                Log.util.warning(2883, parameter, object);
                return 0;
            }
        }
        Log.util.warning(2880, "Long", parameter, object);
        return 0;
    }

    public double getDoubleParameter (String parameter)
    {
        Object object = getParameter(parameter, true);

        if (object instanceof Double) {
            return ((Double)object).doubleValue();
        }
        if (object instanceof String) {
            Assert.assertNonFatal(false,
                                  "Parameter %s should be defined with the type Double",
                                  parameter);
            try {
                return DoubleFormatter.parseDouble((String)object);
            }
            catch (ParseException pe) {
                Log.util.warning(2884, parameter, object);
                return 0;
            }
        }
        Log.util.warning(2880, "Double", parameter, object);
        return 0;
    }

    public Date getDateParameter (String parameter)
    {
        Object object = getParameter(parameter, true);

        if (object == null) {
            return new Date();
        }
        if (object instanceof Date) {
            return (Date)object;
        }
        if (object instanceof String) {
            Assert.assertNonFatal(false,
                                  "Parameter %s should be defined with the type Date",
                                  parameter);
            try {
                return DateFormatter.parseDate((String)object);
            }
            catch (ParseException pe) {
                Log.util.warning(2885, parameter, object);
                return new Date();
            }
        }
        Log.util.warning(2880, "Date", parameter, object);
        return new Date();
    }

    /**
        Sets the value of the parameter.
        @param parameter dotted name of the parameter
        @param value new value for the parameter
    */
    public abstract void setParameter (String parameter, Object value);

    public abstract void reloadParameters (Object scope);
}
