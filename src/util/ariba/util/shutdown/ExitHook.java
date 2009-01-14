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

    $Id: //ariba/platform/util/core/ariba/util/shutdown/ExitHook.java#2 $
*/

package ariba.util.shutdown;

/**
 * The implementation of this interface which is registered
 * with the ShutdownManager will be invoked at the end of the
 * shutdown sequence to force the VM to exit.
 * @aribaapi ariba
 */
public interface ExitHook
{
    /**
     * Forces the VM to exit
     * @param exitCode the exit code to return
     * @aribaapi ariba
     */
    public void exit (int exitCode);
}
