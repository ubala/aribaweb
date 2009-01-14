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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWDisposable.java#5 $
*/

package ariba.ui.aribaweb.util;

/**
    Any component that implements AWDisposable is a candidate for having dispose() called on it when a session is terminated.  The implementation of dispose() should clear out instance variables that might contribute to memory leaks through indirect references.  Only objects that are known to be scoped to a session should implement AWDisposable.  You can call AWUtil.dispose(...) to flush out collection type objects and propagate AWDispose as far as possible.
*/
public interface AWDisposable
{
    public void dispose ();
}
