NOTE:

The jmti implementation of the class loader is intended for use with JDK 1.5(+) VMs.  As of 1.5,
a new Java-base API (Instrumentation) is available:
    http://java.sun.com/j2se/1.5.0/docs/api/java/lang/instrument/Instrumentation.html

To us this we need to build the instrumentation code in its own jar with a special manifest entry of the form:
    Premain-Class: ariba.awreload.Agent
    Can-Redefine-Classes: true

And then launch the VM with a special -javaagent:<path to>/awreload-jmti.jar argument
(along with -Xdebug).
