This is a Demoshell "Site" root folder.  It contains script-backed (rather than
Java-backed) components (i.e. the control logic is embedded in the pages and is
written in Groovy or server-side JavaScript).

To run this example access demoshell (e.g. http://localhost:8080/Demo/AribaWeb).  Demoshell
will look for the environment variable ARIBA_DEMOSHELL_HOME -- it should be set in the
environment in which Tomcat is run to point to this directory.  (If you run this example via
the top-level ant build.xml file this is all handled for you by default).

Once you're up an running you can modify the examples and any changes you make will be
automatically detected upon your next request in the browser.
