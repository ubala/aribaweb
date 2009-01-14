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

    $Id: //ariba/platform/util/core/ariba/util/core/SignalHandler.java#3 $
*/

package ariba.util.core;

import ariba.util.log.Log;
import sun.misc.Signal;

/**
 * Wrapper around the sun.misc.SignalHandler to avoid having the rest
 * of the application to depend on this undocumented class and as such
 * can be changed in any Java release.
 * 
 * Note that this implementation guarantees that if another handler was registered 
 * for the same signal, it will be invoked after this one. In other words, you
 * can register multiple handlers for the same signal in the reverse order
 * in which you want them to be invoked.  
 * @aribaapi ariba
 */
public abstract class SignalHandler implements sun.misc.SignalHandler
{        
    /**
     * Registers a new SignalHandler for the given signal name
     * <b>Note:</b> this method is not thread safe<br/>
     * @param className name of the subclass of SignalHandler
     * @param signalName the name of the signal handled
     * @return the newly registered SignalHandler, or <code>null</code>
     * if the registration did not succeed.
     * @aribaapi ariba
     */
    public static SignalHandler registerSignalHandler (String className, 
                                                       String signalName)
    {
        SignalHandler handler =
            (SignalHandler)ClassUtil.newInstance(className, SignalHandler.class, true);
        if (handler == null) {
            Log.util.warning(9157, className);
            return null;
        }
        return registerSignalHandler(handler, signalName);
    }
    
    /**
     * Registers the given SignalHandler for the given signal
     * <b>Note:</b> an handler can be registered for only one signal !<br/>
     * <b>Note:</b> this method is not thread safe<br/>
     * @param handler the handler to register
     * @param signalName the signal to register with
     * @return the handler which has been registered or <code>null</code
     * if the registration did not succeed.
     * @aribaapi ariba
     */
    public static SignalHandler registerSignalHandler (SignalHandler handler,
                                                       String signalName)
    {
        // Note: synchronizing the access to _oldHandler should be
        // done to prevent two threads from registering the same handler
        // but it seems overkill so I'm not doing it.
        if (handler._oldHandler != null) {
            Log.util.warning(9158, handler, signalName);
            return null;
        }
        try {
            Signal signal = new Signal(signalName);
            handler._oldHandler = Signal.handle(signal, handler);
        }
        catch (IllegalArgumentException e) {
            Log.util.warning(9159, handler, signalName, e.getMessage());
            return null;
        }
        return handler;
    }
    
    /**
     * Raises a signal in the current process.
     * @param signalName the name of the signal to raise
     * @aribaapi ariba
     */
    public static void raiseSignal (String signalName)
    {
        try {
            Signal signal = new Signal(signalName);  
            Signal.raise(signal);
        }
        catch (IllegalArgumentException e) {
            Log.util.warning(9160, signalName, e.getMessage());
        }
    }

    /**
     * The previous handler registered for the same signal
     */
    private sun.misc.SignalHandler _oldHandler;
    
    /**
     * Specifies whether the previous handler should
     * receive the signal after this handler. 
     */
    private boolean _followSignalChain = true;
    
    /**
     * Specifies whether the previous handler should
     * receive the signal after this handler.
     * @aribaapi ariba 
     */
    protected void setFollowSignalChain (boolean value)
    {
        _followSignalChain = value;
    }
    
    /**
     * Method from the sun's interface which is called to handle a signal.
     * Our implementation guarantees that if another handler was registered 
     * for the same signal, it will be invoked after this.
     * @aribaapi private
     */
    public final void handle (Signal signal)
    {
        Log.util.info(9161, signal.getName());
        try {
            handle(signal.getName());
        }
        finally {
            // Chain back to previous handler, if one exists
            if (_followSignalChain && 
                _oldHandler != SIG_DFL && 
                _oldHandler != SIG_IGN)
            {
                _oldHandler.handle(signal);
            }
        }        
    }

    /**
     * Handles the given signal
     * @param signalName the name of the signal
     * @aribaapi ariba
     */
    protected abstract void handle (String signalName);    
}
