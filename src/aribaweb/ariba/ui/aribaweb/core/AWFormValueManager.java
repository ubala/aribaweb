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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWFormValueManager.java#10 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.util.core.ListUtil;
import ariba.util.core.Assert;
import java.util.List;

/**
    Mechanism for deferring certain kinds of work.
    The fieldsui layer needs to be able to defer some kinds of work - we allow
    this by queuing up that work and executing it later.  The kinds of work
    that need to be deferred are to set object values, to run object triggers
    and to run behaviors (visibility, editability & validity).  These different
    kinds of work are queued up during the applyValues phase and then executed
    in invokeAction just as we are about to actually invoke the action.
    <br>
    The whole reason for the deferral is that we need the back-end data to remain
    untouched from the end of renderResponse, through applyValues and through the
    navigation of invoke - up to when we actually call the action

    @aribaapi private
*/
public class AWFormValueManager extends AWBaseObject
{
    /** queue for object model field setting */
    public static final int DeferredValues = 0;
    /** queue for running triggers */
    public static final int Triggers = DeferredValues + 1;
    /** queue for computed value */
    public static final int ComputedValue = Triggers + 1;
    /** queus for running behaviors */
    public static final int Validations = ComputedValue + 1;

    // the queues
    private final List _queues;
    // we may supress the execution of some queues
    private boolean[] _suppressQueueExecution = null;

    // flag to track whether we've processed the queues
    private boolean _queuesPushed;

    // the page this form value manager is associated with.
    private AWPage _page;

    /**
        Create the form value manager
        @aribaapi private
    */
    public AWFormValueManager (AWPage page)
    {
        _queues = ListUtil.list();
        for (int index = 0; index <= Validations; index++) {
            List list = ListUtil.list();
            _queues.add(list);
        }

        _page = page;
    }

    /**
        Clear all the queues.
        @aribaapi  private
    */
    public void clear ()
    {
        for (int index = 0; index <= Validations; index++) {
            clearQueue(index);
        }
    }

    /**
     * Suppress the execution of one of the queues.
     * If a queue is suppressed, we won't process the work in it at the normal
     * time.  The caller is then responsible for explicitly calling
     * processQueue if the work should get done.
     * <p>
     * <b>WARNING!</b> using this method will cause serious problems in
     * subtle ways.  Don't use it unless you thoroughly understand the
     * use of the queues
     * </p>
     * @param queueId  which queue should be suppressed.  Should be one of
     * DeferredValues, Triggers or Validations
     * @aribaapi private
     */
    public void suppressWork (int queueId)
    {
        if (_suppressQueueExecution == null) {
            _suppressQueueExecution = new boolean[Validations];
            for (int ii=0; ii<Validations; ii++) {
                _suppressQueueExecution[ii] = false;
            }
        }
        _suppressQueueExecution[queueId] = true;
    }

    /**
        Add a piece of work to one of the queues.
        This method should normally only be called by internals of fieldsui.
        @param queueId  which queue this work should go in.  Should be one of
        DeferredValues, Triggers or Validations
        @param formValueOperation The work to do.  Cannot be null.
        @see #processQueue
        @aribaapi private
    */
    public void add (int queueId, AWFormValueOperation formValueOperation)
    {
        Assert.that(formValueOperation != null,
                    "null operation to form value manager");
        List queue = (List)_queues.get(queueId);
        queue.add(formValueOperation);

        // remember the error manager for the deferred work so that
        // errors are reported to the correct error manager.  This is
        // needed for nested error manager support.
        formValueOperation.setErrorManager(_page.errorManager());
    }

    /**
        Process a single queue.
        Every form value operation in the queue will have its <code>perform()</code>
        method called.  This method ignores queue suppression, so that callers
        may use this method to run a previously suppressed queue.
        @param queueId  which queue we are running.  Should be one of
        DeferredValues, Triggers or Validations
        @aribaapi private
    */
    public void processQueue (int queueId)
    {
        List queue = (List)_queues.get(queueId);
        for (int index = 0, length = queue.size(); index < length; index++) {
            AWFormValueOperation formValueOperation =
                (AWFormValueOperation)queue.get(index);

            // before performing the deferred work, swizzle in
            // the error manager that was in effect at the time
            // the deferred work is registered.
            AWErrorManager curErrorManager = _page.errorManager();
            Object oldErrorManager =
                (curErrorManager != formValueOperation.getErrorManager())
                ? _page.pushErrorManager(formValueOperation.getErrorManager())
                : null;
            formValueOperation.perform();
            if (oldErrorManager != null) {
                _page.popErrorManager(oldErrorManager);
            }
        }
    }

    /**
        Clear a single queue.
        @param index  which queue we are clearing.  Should be one of
        DeferredValues, Triggers or Validations
        @aribaapi private
     */
    public void clearQueue (int index)
    {
        List oneQueue = (List)_queues.get(index);
        oneQueue.clear();
    }

    /**
        Process all the queues in the form value manager.
        We run the queues in a specific order: deferredValues, triggers,
        then validations.  If one of the queues has been suppressed, we don't
        run it.
        @see #processQueue
        @aribaapi private
    */
    protected void processAllQueues ()
    {
        for (int queueId = 0; queueId <= Validations; queueId++) {
            if (_suppressQueueExecution != null &&
                _suppressQueueExecution[queueId]) {
                // we've suppressed the execution of this queue, skip it
                continue;
            }
            processQueue(queueId);
        }

        // remember that we ran the queues
        _queuesPushed = true;
    }

    /**
        Called at the beginning of invoke to indicate that we will want queues to run
        @aribaapi private
    */
    protected void waitingToPushQueues ()
    {
        _queuesPushed = false;
    }

    /**
        Check to see if we called processAllQueues since the last call to waitingToPushQueues()
        @return true if we ran processAllQueues.  Will not return true because of individual
        calls to processQueue
        @aribaapi private
    */
    protected boolean didPushAllQueues ()
    {
        return _queuesPushed;
    }
}
