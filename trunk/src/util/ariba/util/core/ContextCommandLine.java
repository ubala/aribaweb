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

    $Id: //ariba/platform/util/core/ariba/util/core/ContextCommandLine.java#1 $
*/

package ariba.util.core;

/**
    This interface provides a general mechanism for its clients to set up
    the command line arguments that it is expecting, to process the arguments,
    and finally to run with the input arguments. In addition to the command
    line arguments, the command may be passed additional context, such as
    other streams or processes.

    Normally, a class implementing this interface implements its <code>main</code>
    method which calls the <code>create</code> method of the <code>ArgumentParser</code>
    class, passing in its class name and its command line arguments. Then
    the <code>setupArguments</code> method is called to allow the client to
    register the command line arguments. Then the <code>processArguments</code>
    method is called to allow the client to process its arguments. Finally,
    the <code>startup</code> method is invoked to allow the client to be
    executed.

    @see ArgumentParser

    @aribaapi documented
*/
public interface ContextCommandLine extends CommandLine
{
    /**
     * Hook for invoker to pass information other than what is in the arguments
     * @aribaapi documented
     */
    public void setContext (Object context);

}
