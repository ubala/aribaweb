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

    $Id: //ariba/platform/ui/ideplugin/ariba/ideplugin/core/RemoteOpen.java#3 $
*/
package ariba.ideplugin.core;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RemoteOpen implements Runnable
{

    private boolean _enabled;
    private ServerSocket _listener;
    private Opener _opener;

    public RemoteOpen (Opener opener)
    {
        _opener = opener;
        _enabled = true;
    }

    public void start ()
    {
        try {
            _listener = new ServerSocket(28073);
            Thread runner = new Thread(this);
            runner.setPriority(4);
            runner.start();
        }
        catch (IOException e) {
            e.fillInStackTrace();
        }
    }

    public void stop ()
    {
        try {
            if (_listener != null) {
                _listener.close();
            }
        }
        catch(IOException e) {
        }
    }

    public void run () {
        while(_enabled) {
            try
            {
                Socket client = _listener.accept();
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String inputLine = in.readLine();
                String ignore = null;
                while ((ignore = in.readLine()) != null && !"".equals(ignore)) {
                }
                // GET /java/lang/Thread.java:595 HTTP/1.1
                // GET d:/src/java/lang/Thread.java:595 HTTP/1.1
                String fileLocation = inputLine.split(" ")[1];
                String[] loc = fileLocation.split(":");
                out.println("HTTP/1.1 200 OK");
                out.close();
                in.close();
                client.close();
                int line = 0;
                if (loc.length > 1) {
                    line = Integer.parseInt(loc[1]);
                }
                _opener.open(loc[0], line);
            }
            catch(IOException e)
            {
                _enabled = false;
            }
        }
    }

    public static interface Opener {
        public boolean open(String name, int line);
    }
}
