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

    $Id: //ariba/platform/util/core/ariba/util/log/PrivateLoggers.java#31 $
*/

package ariba.util.log;

import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;
import ariba.util.core.FileUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import ariba.util.io.CSVReader;
import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
    This class retrieves the lists of private loggers from csv files. See
    PrivateLoggers.csv for the format.

    @aribaapi private
*/
class PrivateLoggers
{
    /**
        The csv file that specifies the private loggers and the logger factory
        (if any) used to create the loggers. The factory can be an empty string
        in which case, the default logger factory is used.
    */
    private static final String FileName =
        FileUtil.fixFileSeparators("ariba/util/log/PrivateLoggers.csv");
    private static final File[] PrivateLoggersFiles =
        new File[] {new File(SystemUtil.getInternalDirectory(), FileName),
                    new File(SystemUtil.getInstallDirectory(), FileName)};
    /**
        Expects to see at least 2 columns per entry in the above csv file (other
        than the beginning encoding line, of course. Extra columns are ignored.
    */
    private static final int MinNumColumns = 2;
    /**
        The logger to logger factory map.
    */
    private static Map /*<loggerName, factory>*/ loggersMap = createMap();

    private static Map createMap ()
    {
        try {
            Map map = MapUtil.map();
            for (int i=0; i<PrivateLoggersFiles.length; i++) {
                if (!PrivateLoggersFiles[i].exists()) {
                    continue;
                }
                createMap(PrivateLoggersFiles[i], map);
            }
            return map;
        }
        catch (IOException ioe) {
            Assert.that(false, "Unexpected IOException: %s:%s",
                        ioe, SystemUtil.stackTrace(ioe));
            return null;
        }
    }

    private static void createMap (File file, Map map)
        throws IOException
    {
        List loggerList = CSVReader.readAllLines(file, null, true);
        for (int i=0; i<loggerList.size(); i++) {
            List entry = (List)loggerList.get(i);
            if (entry.size() < MinNumColumns) {
                    // not enough columns, skip it. Since this happens very early
                    // on, we can't even log an error, but the absence of this
                    // entry (if it is indeed used) would arouse the logging mechanism
                    // to scream error, so it won't get unnoticed.
                continue;
            }
            String loggerName = (String)entry.get(0);
            String loggerFactory = (String)entry.get(1);
            map.put(loggerName, loggerFactory);
        }
    }

    /**
        Explicitly make this private since there is no need to instantiate
    */
    private PrivateLoggers ()
    {
    }

    /**
        Returns the logger factory for a given logger specified by the input name
        @param name the name of the logger
        @return the logger factory instance, or null if no such factory can be found.
    */
    static LoggerFactory getLoggerFactory (String name)
    {
        String factoryClassName = (String)loggersMap.get(name);
        if (StringUtil.nullOrEmptyString(factoryClassName)) {
            return null;
        }
            // may not be able to log warnings this early on. So call with false
            // boolean (to turn off warning)
        return (LoggerFactory)ClassUtil.newInstance(
            factoryClassName, LoggerFactory.class, false);
    }
    /**
        Determines if a specified logger (or one of its parents) is in the private logger list,
        @param name the name of the logger
        @return <code>true</code> if the specified logger is in the private logger list,
        <code>false</code> otherwise.
    */
    static boolean contains (String name)
    {
        if (loggersMap.get(name) != null) {
            return true;
        }
        else {
            int idx = name.lastIndexOf('.');
            if (idx == -1) {
                return false;
            }
            else {
                return contains(name.substring(0,idx));
            }
        }
    }
}

