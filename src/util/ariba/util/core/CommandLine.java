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

    $Id: //ariba/platform/util/core/ariba/util/core/CommandLine.java#5 $
*/

package ariba.util.core;

/**
    This interface provides a general mechanism for its clients to set up
    the command line arguments that it is expecteing, to process the arguments,
    and finally to run with the input arguments.

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
public interface CommandLine
{
    /**
        registers the arguments this class expects. This can include
        the name of the command line option, the type of input
        arguments, and whether the option is optional or not. 
        
        @param arguments the ArgumentParser object that stores the
        registered information. The implementation of this method
        can expect this ArgumnentParser object to be non null.
        
        @see ArgumentParser

        @aribaapi documented
    */
    public void setupArguments (ArgumentParser arguments);

    /**
        processes the arguments previously registered during
        setupArguments.

        @param arguments the ArgumentParser object that provides
        the registered information to be processed.

        @see ArgumentParser

        @aribaapi documented
    */
    public void processArguments (ArgumentParser arguments);

    /**
        Called to allow client to begin running

        @aribaapi documented
    */
    public void startup ();
}
