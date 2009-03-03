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

    $Id: //ariba/platform/util/core/ariba/util/core/MasterPasswordClient.java#6 $
*/

package ariba.util.core;

import java.io.IOException;

/**
    Manages master password. For security reasons, it may be required to pass in the
    master password via std in. Whether this is the case is specified by 1 of the 2
    command line options: -readMasterPassword and -masterPasswordNoPrompt. When a commmand
    line tool is invoked, {@link ariba.util.core.ArgumentParser} will invoke methods
    from this class to read the master password from the stdin. Whether the user is
    prompted or not depends on which of the above options are present. If
    -masterPasswordNoPrompt is present, then the user will not be prompted (in such
    cases the 'user' is most likely some parent process executing the command line).
    Otherwise, if -readMasterPassword is present, then the user will be prompted.

    The master password is then stored and made available for the process via the
    call {@link #getMasterPassword}.

    @aribaapi ariba
*/
public final class MasterPasswordClient
{
    public static final String OptionMasterPassword = "readMasterPassword";
    public static final String OptionMasterPasswordNoPrompt = "masterPasswordNoPrompt";

    public static final String DashOptionMasterPassword = "-" + OptionMasterPassword;
    public static final String DashOptionMasterPasswordNoPrompt =
        "-" + OptionMasterPasswordNoPrompt;

    private boolean useMasterPassword = false;
    private String masterPassword = null;
    private static MasterPasswordClient client;

    /**
        Get the master password client. Note that this is a singleton.
        Also this method is <b>not synchronized</b>. If multiple threads
        call this method, the last one wins, and previously instantiated
        instances of this class are simply garbage collected.

        @return the master password client.
        @aribaapi ariba
    */
    public static MasterPasswordClient getMasterPasswordClient ()
    {
        if (client == null) {
            client = new MasterPasswordClient();
        }
        return client;
    }

    /*
        A simple wrapper on top of the standard setupArguments method from
        ariba.util.core.CommandLine
    */
    void setupArguments (
        CommandLine client,
        ArgumentParser arguments)
    {
        arguments.addOptionalBoolean(OptionMasterPassword, false, null);
        arguments.addOptionalBoolean(OptionMasterPasswordNoPrompt, false, null);
        client.setupArguments(arguments);
    }

    /*
        A simple wrapper on top of the standard processArguments method from
        ariba.util.core.CommandLine
    */
    void processArguments (
        CommandLine client,
        ArgumentParser arguments)
    {
        boolean useMasterPasswordNoPrompt =
            arguments.getBoolean(OptionMasterPasswordNoPrompt);
        useMasterPassword =
            useMasterPasswordNoPrompt || arguments.getBoolean(OptionMasterPassword);

        if (masterPassword == null && useMasterPassword) {
            if (!useMasterPasswordNoPrompt) {
                promptUserForPassword();
            }
            try {
                masterPassword = SystemUtil.in().readLine();
            }
            catch (IOException e) {
                SystemUtil.out().println(
                    "MasterPasswordClient Caught unexpected IOException: " +
                    e);
                SystemUtil.out().flush();
                Assert.that(false, "Caught unexpected IOException");
            }
        }
        client.processArguments(arguments);
    }

    /**
        Returns the master password. Should be called after processArguments. It is
        incorrect to call this method before prcoessArguments is called, in which
        case null will be returned.
        
        @return the master password, <b>null</b> if no masterPassword options are
        specified in the command line when {@link #processArguments} is called.

        @aribaapi ariba
    */
    public String getMasterPassword ()
    {
        if (masterPassword == null) {
            return null;
        }
        SecurityHelper.validateUnscriptedCaller();
        return masterPassword;
    }

    private void promptUserForPassword ()
    {
        SystemUtil.out().println("Please enter the master password: ");
        SystemUtil.out().flush();
    }


}
