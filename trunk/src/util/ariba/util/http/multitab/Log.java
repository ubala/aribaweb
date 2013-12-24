/*
    Copyright (c) 2013 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/http/multitab/Log.java#1 $

    Responsible: fkolar
 */
package ariba.util.http.multitab;

import ariba.util.log.Logger;

public class Log extends ariba.util.log.Log
{

    /**
     Log category for the multitab functionality
     */
    public static final Logger multiTab = (Logger)Logger.getLogger("util.multitab");

    /**
     Log category for the multiTab functionality on servlet side
     */
    public static final Logger multiTabServlet = (Logger)Logger.getLogger
            ("servletadaptor.multitab");
}
