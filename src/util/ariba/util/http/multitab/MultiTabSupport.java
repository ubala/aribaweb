/*
    Copyright (c) 2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/http/multitab/MultiTabSupport.java#1 $

    Responsible: fkolar
 */

package ariba.util.http.multitab;


import ariba.util.core.Assert;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

/**
 This interface defines a new functionality for MultiTab support. These methods exposes
 hooks into AW application that let you implement your own multi tab functionality.
 */
public interface MultiTabSupport
{

    /**
     Inserts the tab into the untabbedUrl, if a tab was found
     in tabbedUrl. Noop, if onlyWhenPositive is true and tabIndex is zero.

     @param untabbedUrl An untabbed relative or absolute request URI.
     @param tabIndex    The tab to insert.
     @param isNotZero   Only calls replace when the tabIndex is not ZERO.
     @return The modified URI.
     */
    public String insertTabInUri (String untabbedUrl, int tabIndex, boolean isNotZero);

    /**
     Removes the tab # from the URI.

     @param uri The URI to evaluate.
     @return A URI without any tab info.
     */
    public String stripTabFromUri (String uri);

    /**
     Defines max number of tabs system allows to be opened at the same time

     @return Max defined number of tabs.
     */
    public int maximumNumberOfTabs ();

    /**
     Attempts to parse the tab number from the provided URI.
     Returns tab defaultTab if UnexpectedTabException was thrown.

     @param uri        The URI to evaluate.
     @param defaultTab The default tab, when an exception happens.
     @return The tab number, default, or 0.
     */
    public int uriToTabNumber (String uri, int defaultTab);

    /**
     If the tab index is more than ZERO, apply your customer naming scheme to identify
     your tab URI

     @param servletName           The name of the servlet.
     @param applicationNameSuffix Usually, "/", but allows for modifications
     to the servlet name.
     @param tabIndex              The tab index to evaluate.
     @param uri
     @return The concatenated tab-aware servlet-name.
     */
    public String tabNumberToUri (String servletName, String applicationNameSuffix,
                                  int tabIndex, String uri);

    /**
     Provides information if the multitab is enabled in the system

     @return true if MultiTab is enabled
     */
    public boolean isMultiTabEnabled ();

    /**
     Determine the state of the request and return what multi-tab related
     action should be taken by the caller.

     @param data                    A data wrapper.
     @param multiTabHandlerCallback A class implementing MultiTab.
     */
    public void processRequest (MultiTabHandler.RequestInfo data,
                                MultiTabHandler multiTabHandlerCallback)
            throws IOException;

    /**
     Returns a string that help us differentiate between Tabbed and non Tabbed URI.

     @return
     */
    public String defaultTabPrefix ();

    /**
     creates request specific handler. This method stores current handler in the
     ThreadLocal which is
     not the best way how to handle temp info for the request. Probably next task will
     be some
     refactoring to this. Especially for servlet 3.0 where the same thread can be
     reused for
     processing another concurrent request. something like NIO.
     */
    public MultiTabHandler initHandler (RequestProcessor requestProcessor,
                                        HttpServletRequest request)
            throws IOException;

    /**
     Factory methods to register a handler and to retrieve. We had to introduce this
     due to the
     package dependencies. Therefore we have a common place usually in the APP that
     registers handlers
     and one place we do lookup.
     */
    public void registerHandlerClassForName (String name,
                                             Class<? extends MultiTabHandler> handler);

    public MultiTabHandler handlerClassForName (String name);

    /**
     This is replacement of the AWApplication supposed to hold current instance of
     MultiTabSupport.
     Since its not accessible from everywhere so we had to take it out.
     */
    public static class Instance
    {
        //In case we do not provide some impl. here is the dummy one.
        public static final NoMultiTabSupport DefaultImplementation = new
                NoMultiTabSupport();
        // Store info for the initHandler() method please see above
        private static final ThreadLocal<MultiTabHandler> CurrentHandler = new
                ThreadLocal<MultiTabHandler>();
        // This instead does not change so there is not problem.
        private static MultiTabSupport Instance;

        public static MultiTabSupport get ()
        {
            if (Instance == null) {
                set(DefaultImplementation);
            }
            return Instance;
        }

        /**
         this is called only once when app is initialized.

         @param multiTabSupport
         */
        public static void set (MultiTabSupport multiTabSupport)
        {
            Assert.that(Instance == null || multiTabSupport == null,
                    "You can initialize MultiTabSupport only once!");
            Instance = multiTabSupport;
        }

        // threadlocal storage for handlers
        public static void saveHandler (MultiTabHandler handler)
        {

            CurrentHandler.set(handler);
        }

        public static MultiTabHandler currentHandler ()
        {
            MultiTabHandler multiTabHandler = CurrentHandler.get();
            Assert.that(multiTabHandler != null, "Nobody registered handler for current" +
                    " thread");
            return multiTabHandler;
        }

        public static void releaseHandler ()
        {
            // do not call .remove()
            CurrentHandler.set(null);
        }
    }

}