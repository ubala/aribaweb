AribaWeb README file, May 2008

INTRODUCTION

AribaWeb is a component-oriented framework for building web application
user interfaces.  The AW distribution includes both the (relatively) low-level
core rendering and request handling framework (ariba.aribaweb) as well
as a higher level library of ui components (ariba.widgets, ariba.metaui, ...)


INTENDED AUDIENCE
    IF YOU HAVEN'T ALREADY USED ARIBAWEB OR DON'T HAVE A PERSONAL RELATIONSHIP WITH
    ONE OF THE CORE DEVELOPERS OF ARIBAWEB AT ARIBA, THIS RELEASE IS NOT INTENDED FOR YOU.
    There is presently no documentation or tutorials available.  This situation will be
    remedied in the next few months.  Until then, if you don't already know how to use
    AribaWeb, you're likely to get discouraged if you try now.  Check back later!  :-)

Version 0.8 (BETA)
    Although most of the libraries in this distribution has been incorporated in a
    variety of Ariba commercial application releases over as many as 9 years, this
    is its first release in OpenSource form and there are, therefore, likely issues
    that will arise from its use in non-Ariba contexts.  Also, the documentation
    is not ready for release and the samples are limited.  As such, we're calling it
    beta for the time being...  (That said, the majority of aribaweb and widgets APIs
    are likely to be very stable from here foreward)

Running Examples
    - Install the Java5 or Java6 JDK (http://java.sun.com/javase/downloads/index_jdk5.jsp)
        - set your JAVA_HOME
    - Install Apache Tomcat 5.5 Core (http://tomcat.apache.org/download-55.cgi)
        - set your CATALINA_HOME environment variable to your tomcat install directory
    - Install Ant v1.7+ (http://ant.apache.org/)
        - set your ANT_HOME environment variable to your Ant install directory
    I.e.:
        export JAVA_HOME='d:/jdk1.5'
        export ANT_HOME="d:/ant"
        export CATALINA_HOME="d:/tomcat5.5"
        export PATH="$JAVA_HOME/bin;$ANT_HOME/bin;$PATH"

    - from a shell cd'd to the main AW directory do:
            % ant -f src/build.xml tomcat
            
    - You can now access the samples in your browser)via:
        http://localhost:8080/Simple/AribaWeb
        http://localhost:8080/Demo/AribaWeb
        http://localhost:8080/BusObjUI/AribaWeb

    - See src/BUILD.txt for more instructions on building and debugging
    
Directories
    lib/        The aribaweb jars
    lib-ext/    Third-party libraries used either at build time or runtime
    src/        Source code for the AribaWeb jars, samples, and build files
    webapps/    WAR files for AW sample/demo applications (fully self-contained, with jars and resources)

Lib Details:
    ariba.util.jar
        Low-level utility classes used by the other AW components.  Includes logging,
        collection wrappers, performance metric and debugging tracing utilities, as well
        as "ClassExtension" and "FieldValue" support for AOP-like external class
        extension, and JavaBeans-like high-performance property access.

    ariba.aribaweb.jar
        The core AribaWeb framework.  Includes servlet adaptors to bind AW into a
        container, the AWComponent and AWElement hierarchy, template parser, and
        built-in tags for control flow (AWIf, AWFor, etc) as well as HTML constructs
        (AWTextField, AWPopup, ...) as well at the client-side javascript libraries
        to deliver an AJAX user experience (incremental refresh, drag/drop, ...)

        Note: this jar contains webserver resources in docroot/** that need to be
        copied to any application deployment WAR file.

    ariba.widgets.jar
        Higher level UI components for building AribaWeb applications.  Includes:
            - Layout components (*PageWrapper, TabSet, SectionHead/Body, PortletWrapper)
            - Controls (PopupMenu, Chooser, Calendar)
            - Rich DataTable, PivotTable and Tree/Outline support
            - Validation display / navigation UI (ErrorFlag, ...)
            - Wizard framework (multi-step UIs)
        Several of these controls provide a rich user interface via associated client-side
        JavaScript libraries (e.g. type-ahead choosers, scrolling and dynamic data retrieval
        in tables).

        Note: this jar contains webserver resources in docroot/** that need to be
        copied to any application deployment WAR file.

    ariba.expr.jar
        A simple expression parser/interpreter that can be used in AW tag bindings
        and metaui property definitions.  Based originally on OGNL code, but with a
        more Java/Groovy-inspired syntax and using ariba.util.fieldvalue for high-performance
        property access.  (Also supports interfaces for type-safe validation of
        restricted subsets of APIs exposed for business application end-user scripting)

    ariba.metaui.jar                    [pre-Alpha!]
        Meta-data driven UI framework, built on the full AribaWeb stack for generating
        complete user interfaces "on the fly" based on various sources of meta data
        (java class introspection, annotations, and ".oss" files).  Uses CSS-like
        "multi-dimensional selectors" to contextually specify properties.
        Currently supports generating forms (with validation), tables, as well as
        global application navigation (e.g. nav tabs) and Action (global and instance-level)

    ariba.demoshell
        An application prototyping environment built on the full AW stack.  Enables
        rapid development of functional prototypes using AW components and server-side
        JavaScript (Rhino) or Groovy scripting.

        This source directory includes a "site" directory containing many examples
        of using popular Widgets (e.g. DataTable, PivotTable, etc)

      Note: this jar contains webserver resources in docroot/** that need to be
      copied to any application deployment WAR file.


Src Details
    BUILD.txt
        Read this for instructions on how to build the AribaWeb distribution from source

    util, aribaweb, widgets, expr, metaui, demoshell/
        Correspond to the above-described jars

    samples/
        Contains a few simple AW sample applications
            simple/
                A single-component "Guest Book" mini app (see Main.{awl, java})
            BusObjUI
                Defines a few business objects (in busobj package) and then uses
                metaui (in UserForm.*) and hand coded (in StartPage.*) to manipulate them.

                Also includes ExplorerPage.*: a Outline/Table-based file system browser.

    build.xml / build-support
        Ant build files.  Each source sub-directory has a build.xml file that references
        shared build rules in the build-support directory


This software is covered under the Apache License Version 2.0 (see LICENSE.txt)
