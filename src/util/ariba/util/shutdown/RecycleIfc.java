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

    $Id: //ariba/platform/util/core/ariba/util/shutdown/RecycleIfc.java#2 $
*/

package ariba.util.shutdown;

/**
 * The Recycle Interface
 * Classes implementing this interface needs to register with the RecycleManger.
 * When a recycle request is received, the RecycleManager will call back every
 * registerd implementor through this interface
 */
public interface RecycleIfc
{

    /**
     * @return true if it is safe to recycle
     * @aribaapi private
     */
    public boolean isSafeToRecycle ();
    /**
     * Call back function to perform pre-shutdown tasks
     * @return true if initiation tasks are ran successfully.
     * @aribaapi private
     */
    public boolean initiateRecycle ();
    /**
     * Call back to perform abort recycle tasks
     * @aribaapi private
     */
    public void abortRecycle ();

    /**
     * @return true if the Recycle can be aborted. This is normally true only if the
     * initiateRecyle tasks can be undone
     * @aribaapi private
     */
    public boolean isAbortable ();
}
