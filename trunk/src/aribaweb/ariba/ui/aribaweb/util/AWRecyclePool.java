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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWRecyclePool.java#8 $
*/

package ariba.ui.aribaweb.util;

import java.util.List;
import ariba.util.core.ListUtil;

abstract public class AWRecyclePool extends AWBaseObject
{
    private static final int MaxSize = 16;
    private final int _maxSize;
    private final List _pool;
    private Object _object;
    private boolean _debugEnabled;

    protected AWRecyclePool (int maxSize, boolean debugEnabled)
    {
        _maxSize = maxSize == -1 ? MaxSize : maxSize;
        _debugEnabled = debugEnabled;
        _pool = ListUtil.list();
    }

    public static AWRecyclePool newPool (int maxSize, boolean requiresThreadSafety, boolean debugEnabled)
    {
        AWRecyclePool newPool = null;
        if (requiresThreadSafety) {
            newPool = new AWSynchronizedPool(maxSize, debugEnabled);
        }
        else {
            newPool = new AWUnsynchronizedPool(maxSize, debugEnabled);
        }
        return newPool;
    }

    public Object checkout ()
    {
        Object object = null;
        if (_object == null) {
            if (_pool.size() > 0) {
                object = ListUtil.removeLastElement(_pool);
            }
        }
        else {
            object = _object;
            _object = null;
        }
        return object;
    }

    public void checkin (Object object)
    {
        if (object != null) {
            if (_debugEnabled && object instanceof AWBaseObject) {
                ((AWBaseObject)object).ensureFieldValuesClear();
            }
            if (_object == null) {
                _object = object;
            }
            else {
                if (_pool.size() < _maxSize) {
                    _pool.add(object);
                }
            }
        }
    }
}

final class AWUnsynchronizedPool extends AWRecyclePool
{
    protected AWUnsynchronizedPool (int maxSize, boolean debugEnabled)
    {
        super(maxSize, debugEnabled);
    }
}

final class AWSynchronizedPool extends AWRecyclePool
{
    protected AWSynchronizedPool (int maxSize, boolean debugEnabled)
    {
        super(maxSize, debugEnabled);
    }

    public synchronized Object checkout ()
    {
        return super.checkout();
    }

    public synchronized void checkin (AWBaseObject object)
    {
        super.checkin(object);
    }
}
