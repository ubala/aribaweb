/*
    Copyright (c) 1999-2009 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/Aggregator.java#2 $

    Responsible: dfinlay
*/
package ariba.util.core;

import java.util.List;

/**
    @aribaapi ariba
*/
public abstract class Aggregator<W, V>
{
    public abstract W aggregate (W aggregate, V value);

    public boolean mutatesAggregate ()
    {
        return false;
    }

    public static final Aggregator<Number,Number> Summer = new Aggregator<Number, Number>()
    {
        public Number aggregate (Number aggregate, Number value)
        {
            if (aggregate == null) {
                return value;
            }
            return ArithmeticUtil.add(aggregate, value);
        }
    };

    public static final Aggregator<Integer,Object> Count = new Aggregator<Integer, Object>()
    {
        public Integer aggregate (Integer aggregate, Object value)
        {
            if (aggregate == null) {
                return 1;
            }
            return aggregate + 1;
        }
    };

    public static class Collector<X> extends Aggregator<List<X>,X>
    {
        public List<X> aggregate (List<X> aggregate, X value)
        {
            if (aggregate == null) {
                return ListUtil.list(value);
            }
            aggregate.add(value);
            return aggregate;
        }

        public boolean mutatesAggregate ()
        {
            return false;
        }
    }

    private static final Collector COLLECTOR = new Collector();

    public static final <X> Collector<X> collector ()
    {
        return Aggregator.COLLECTOR;
    }
}
