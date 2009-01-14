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

    $Id: //ariba/platform/util/core/ariba/util/shutdown/ShutdownManagerProcessorIfc.java#2 $
*/

package ariba.util.shutdown;

/**
 * Interface for the ShutdownManager code to call the ShutdownManagerProcessor.
 */
public interface ShutdownManagerProcessorIfc
{
    /**
     * Send message about change to node shutdown status.
     * @param newNodeStatus the new node shutdown status.
     * @aribaapi private
     */
    public void newNodeStatus (int newNodeStatus);
}
