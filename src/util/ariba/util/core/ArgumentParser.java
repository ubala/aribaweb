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

    $Id: //ariba/platform/util/core/ariba/util/core/ArgumentParser.java#15 $
*/

package ariba.util.core;

import ariba.util.formatter.DoubleFormatter;
import ariba.util.formatter.IntegerFormatter;
import java.util.Map;
import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

/**
    Provide a cleaner way of handling arguments to
    public static void main (String args[]).
    @aribaapi documented
*/
public class ArgumentParser
{
    private static final PrintWriter err = SystemUtil.err();

    /**
        Cache of VM command line arguments

        aribaapi ariba because the util unit test uses it.

        @aribaapi ariba
    */
    public static String[] globalArgs;

        // by default we interpret two flags with different
        // case to be the same.
    /**
        @aribaapi private
    */
    public static boolean IgnoreCaseDefault = true;

    /**
        Indicates if we should ignore the case of the comand line arguments
        aribaapi ariba because unit test uses it.

        @aribaapi ariba
    */
    public static boolean globalIgnoreCase = IgnoreCaseDefault;

    /**
        Create an instance of the command line client. Then invokes the
        command line client's methods in the following order:

        (1) <code>setupArgument</code> to let the client set up its commmand
        line arguments;
        (2) <code>processArguments</code> to let the client process and parse
        its arguments;
        (3) <code>startup</code> to run the client.


        @param className the command line client class
        @param args the command line arguments of the class, must be non null

        @see CommandLine
        @aribaapi documented
    */
    public static void create (String className, String[] args)
    {
        create(className, args, IgnoreCaseDefault);
    }

    /**
        The first part of <code>create</code>: creates a new instance of the
        specified comand line client class. Sometimes we need to create it without
        starting it right away.

        @param className the class name of the command line client
        @param args      its associated command line arguments, must be non-null.

        @aribaapi ariba

    */
    public static CommandLine newInstance (String className, String[] args)
    {
        return newInstance(className, args, IgnoreCaseDefault);
    }

    /**
        Create a command line client. Then invokes the command line client's
        methods in the following order:

        (1) <code>setupArgument</code> to let the client set up its commmand
        line arguments;
        (2) <code>processArguments</code> to let the client process and parse
        its arguments;
        (3) <code>startup</code> to run the client.

        @param className the command line client class
        @param args the command line arguments of the class. Must be non null.
        @param ignoreCase when <b>true</b> flags of differing case compare equal
        @param context information to be passed to the command, in addition to
            the args.

        @see CommandLine
        @aribaapi documented
    */
    public static void create (String className, String[] args, boolean ignoreCase, Object context)
    {
            // we stash the args here, as well as in startup, because
            // sometimes an application may access them during
            // newInstance
        globalArgs = args;
        CommandLine cmd = newInstance(className, args, ignoreCase);
        if (cmd instanceof ContextCommandLine) {
           ((ContextCommandLine)cmd).setContext(context);
        }
        startup(cmd);
    }

    /**
     * Convenience method for creating a command line client without the optional context.
     * @param className the command line client class
     * @param args the command line arguments of the class. Must be non null.
     * @param ignoreCase when <b>true</b> flags of differing case compare equal.
     */
    public static void create (String className, String[] args, boolean ignoreCase)
    {
    	create(className, args, ignoreCase, null);
    }

    public static void create (String className, String[] args, Object context)
    {
    	create(className, args, IgnoreCaseDefault, context);
    }

    /**
        The first part of <code>create</code>: creates a new instance of the
        specified comand line client class. Sometimes we need to create it without
        starting it right away.

        @param className the class name of the command line client
        @param args      its associated command line arguments, must be non-null.
        @param ignoreCase when <b>true</b> flags of differing case compare equal

        @aribaapi ariba
    */
    public static CommandLine newInstance (
        String   className,
        String[] args,
        boolean  ignoreCase)
    {
        globalArgs = args;
        globalIgnoreCase = ignoreCase;
        Object object = ClassUtil.newInstance(className);
        if (object == null) {
            Fmt.F(err, "Problem creating program for %s", className);
            SystemUtil.exit(1);
        }
        if (!(object instanceof CommandLine)) {
            Fmt.F(err,
                  "%s is not an instance of  %s",
                  className,
                  CommandLine.class.getName());
            SystemUtil.exit(1);
        }
        return (CommandLine)object;
    }

    /**
        The second part of create, starting an existing instance.
        Sometimes we need to start a precreated CommandLine.

        @param client the command line client to start executing.
        must be non null.

        aribaapi ariba because unit test uses it.
        @aribaapi ariba
    */
    public static void startup (CommandLine client)
    {
        try {
            ArgumentParser arguments =
                new ArgumentParser(
                    Fmt.S("Usage: %s <option> <option> ...; " +
                          "where <option> is:",
                          client.getClass().getName()),
                    globalArgs,
                    globalIgnoreCase);
            MasterPasswordClient mpClient =
                MasterPasswordClient.getMasterPasswordClient();
            mpClient.setupArguments(client, arguments);
            mpClient.processArguments(client, arguments);
            client.startup();
        }
        finally {
            SystemUtil.flushOutput();
        }
    }

        // Argument types
    public static final int TypeString  = 1 << 0;
    public static final int TypeBoolean = 1 << 1;
    public static final int TypeInteger = 1 << 2;
    public static final int TypeDouble  = 1 << 3;
    public static final int TypeFile    = 1 << 4;
    public static final int TypeURL     = 1 << 5;
    public static final int TypeIntegerList = 1 << 6;

    public static final String RequiredOpen   = "{";
    public static final String RequiredClose  = "}";
    public static final String OptionalOpen   = "[";
    public static final String OptionalClose  = "]";
    public static final String BooleanDefault = "*";
    public static final String ValidChoices   = "|";

    public static final String SwitchPrefix = "-";

    public static final String NoPrefix     = "no";

    /**
        The argument list we are handling
    */
    private String[] args;
    /**
        Controls flag evaluation
    */
    private boolean ignoreCase;

    /**
        Summary line for usage statement
    */
    private String usage;

    /**
        The hashtable of ArgumentHandlers for this parser
    */
    private Map handlerTable = MapUtil.map();

    /**
        Ordering for argument usage statement;
    */
    private List arguments = ListUtil.list();

    /**
        The parsed results
    */
    private MultiValueHashtable results;

    /**
        Toggles the behavior of the parser so that it either throws an IllegalArgumentException
        instead or exit the VM
    */
    private boolean exitUponParsingError = true;

    /**
        Constructor takes the args to parse and user supplied usage string

        @param usage specifies command line option usage
        @param args  the command line arguments

        @aribaapi ariba
    */
    public ArgumentParser (String   usage,
                           String[] args)

    {
        this(usage, args, IgnoreCaseDefault);
    }

    /**
        Constructor takes the args to parse and user supplied usage string

        @param usage specifies command line option usage
        @param args  the command line arguments
        @param ignoreCase when <b>true</b> flags of differing case compare equal

        @aribaapi ariba
    */
    public ArgumentParser (String   usage,
                           String[] args,
                           boolean  ignoreCase)

    {
        this(usage, args, ignoreCase, true);
    }

    /**
        Constructor takes the args to parse and user supplied usage string

        @param usage specifies command line option usage
        @param args  the command line arguments
        @param ignoreCase when <b>true</b> flags of differing case compare equal
        @param exitUponParsingError when <b>false</false> parsing error will be reported
                     by an IllegalArgumentException while when <b>true</b> the VM will exit

        @aribaapi ariba
    */
    public ArgumentParser (String   usage,
                           String[] args,
                           boolean  ignoreCase,
                           boolean  exitUponParsingError)

    {
        this.args = args;
        this.usage = usage;
        this.ignoreCase = ignoreCase;
        this.exitUponParsingError = exitUponParsingError;
    }

    /**
        Return the arguments for purposes such as printing

        @return the command line arguments

        @aribaapi ariba
    */
    public String [] args ()
    {
        return args;
    }

    /**
        Add a required string argument

        @param name name of the command line option
        @param usage  usage of this argument

        @aribaapi documented
    */
    public void addRequiredString (String name, String usage)
    {
        add(name, false, TypeString, null, usage);
    }

    /**
        Add an optional string argument

        @param name name of the command line option
        @param usage  usage of this argument
        @param defaultValue the default value to use if the
        option does not appear in the command line

        @aribaapi documented
    */
    public void addOptionalString (String name,
                                   String defaultValue,
                                   String usage)
    {
        add(name, true, TypeString, defaultValue, usage);
    }

    /**
        Add a required integer argument

        @param name name of the command line option
        @param usage  usage of this argument

        @aribaapi documented
    */
    public void addRequiredInteger (String name, String usage)
    {
        add(name, false, TypeInteger, null, usage);
    }

    /**
        Add an optional integer argument

        @param name name of the command line option
        @param usage  usage of this argument
        @param defaultValue the default value to use if the
        option does not appear in the command line

        @aribaapi documented
    */
    public void addOptionalInteger (String name,
                                    int defaultValue,
                                    String usage)
    {
        add(name,
            true,
            TypeInteger,
            Constants.getInteger(defaultValue),
            usage);
    }

    /**
        Add a required double argument

        @param name name of the command line option
        @param usage  usage of this argument

        @aribaapi documented
    */
    public void addRequiredDouble (String name, String usage)
    {
        add(name, false, TypeDouble, null, usage);
    }

    /**
        Add an optional double argument

        @param name name of the command line option
        @param usage  usage of this argument
        @param defaultValue the default value to use if the
        option does not appear in the command line

        @aribaapi documented
    */
    public void addOptionalDouble (String name,
                                   double defaultValue,
                                   String usage)
    {
        add(name,
            true,
            TypeDouble,
            new Double(defaultValue),
            usage);
    }

    /**
        Add a required boolean argument

        @param name name of the command line option

        @aribaapi documented
    */
    public void addRequiredBoolean (String name)
    {
        add(name, false, TypeBoolean, null, "");
    }

    /**
        Add a required boolean argument

        @param name name of the command line option
        @param usage  usage of this argument

        @aribaapi documented
    */
    public void addRequiredBoolean (String name, String usage)
    {
        add(name, false, TypeBoolean, null, usage);
    }

    /**
        Add an optional boolean argument

        @param name name of the command line option
        @param defaultValue the default value to use if the
        option does not appear in the command line

        @aribaapi documented
    */
    public void addOptionalBoolean (String name, boolean defaultValue)
    {
        add(name,
            true,
            TypeBoolean,
            Constants.getBoolean(defaultValue),
            "");
    }

    /**
        Add an optional boolean argument

        @param name name of the command line option
        @param usage  usage of this argument, if <code>null</code>, the usage will not be displayed.
        @param defaultValue the default value to use if the
        option does not appear in the command line

        @aribaapi documented
    */
    public void addOptionalBoolean (String name, boolean defaultValue, String usage)
    {
        add(name,
            true,
            TypeBoolean,
            Constants.getBoolean(defaultValue),
            usage);
    }

    /**
        Add a required file argument

        @param name name of the command line option
        @param usage  usage of this argument

        @aribaapi documented
    */
    public void addRequiredFile (String name, String usage)
    {
        add(name, false, TypeFile, null, usage);
    }

    /**
        Add an optional file argument

        @param name name of the command line option
        @param usage  usage of this argument
        @param defaultValue the default value to use if the
        option does not appear in the command line

        @aribaapi documented

    */
    public void addOptionalFile (String name,
                                 File   defaultValue,
                                 String usage)
    {
        add(name, true, TypeFile, defaultValue, usage);
    }

    /**
        Add a required url argument

        @param name name of the command line option
        @param usage  usage of this argument

        @aribaapi documented
    */
    public void addRequiredURL (String name, String usage)
    {
        add(name, false, TypeURL, null, usage);
    }

    /**
        Add an optional url argument

        @param name name of the command line option
        @param usage  usage of this argument
        @param defaultValue the default value to use if the
        option does not appear in the command line

        @aribaapi documented
    */
    public void addOptionalURL (String name,
                                URL    defaultValue,
                                String usage)
    {
        add(name, true, TypeURL, defaultValue, usage);
    }

    /**
        Add a required integer range argument

        @param name name of the command line option
        @param usage  usage of this argument

        @aribaapi documented
    */
    public void addRequiredIntegerList (String name, String usage)
    {
        add(name, false, TypeIntegerList, null, usage);
    }

    /**
        Add an optional integer range argument

        @param name name of the command line option
        @param usage  usage of this argument
        @param defaultValue the default value to use if the
        option does not appear in the command line

        @aribaapi documented
    */
    public void addOptionalIntegerList (String name,
                                    List defaultValue,
                                    String usage)
    {
        add(name,
            true,
            TypeIntegerList,
            defaultValue,
            usage);
    }

    /**
        Add a new argument to the list and table of arguments

        @param name name of the command line option
        @param optional <b>true</b> if the option is an optional,
        <b>false</b> if it is required.
        @param type the type of the argument
        @param defaultValue the default value to use if the
        option does not appear in the command line
        @param usage  usage of this argument

    */
    private void add (String  name,
                      boolean optional,
                      int     type,
                      Object  defaultValue,
                      String  usage)
    {
        Assert.that(results == null, "Can't add new arguments after parsing");
        arguments.add(name);
        ArgumentHandler handler = new ArgumentHandler(name,
                                                      optional,
                                                      type,
                                                      defaultValue,
                                                      usage);
        this.putHandler(name, handler);

            // For Boolean arguments register noFoo as well as foo
        if (type == TypeBoolean) {
            Assert.that(!name.startsWith(NoPrefix),
                        "Boolean argument name %s can't start with %s",
                        name,
                        NoPrefix);
            this.putHandler(negate(handler.name), handler);
        }
    }

    /**
        Returns a vector of values for the argument name specified. If
        none have been set, a null vector will be returned.

        @param name the name of the parameter whose value is to be returned

        @return the vector of values as specified above.

        @aribaapi documented
    */
    public List getListOf (String name)
    {
        parse();
        List result =  results.getList(name);
        if ((result != null) && result.contains(Null)) {
            result = null;
        }
        return result;
    }

    /**
        retrieve the value of a string argument

        @param name the name of the parameter whose value is to be returned

        @return the value of a string argument

        @aribaapi documented
    */
    public String getString (String name)
    {
        parse();
        assertHandler(name, TypeString);
        return (String)get(name);
    }

    /**
        retrieve the value of a boolean argument

        @param name the name of the parameter whose value is to be returned

        @return the value fo the boolean argument

        @aribaapi documented
    */
    public boolean getBoolean (String name)
    {
        parse();
        assertHandler(name, TypeBoolean);
        return ((Boolean)get(name)).booleanValue();
    }

    /**
        retrieve the value of a integer argument

        @param name the name of the parameter whose value is to be returned

        @return the value of the integer argument

        @aribaapi documented
    */
    public int getInteger (String name)
    {
        parse();
        assertHandler(name, TypeInteger);
        return ((Integer)get(name)).intValue();
    }

    /**
        retrieve the value of a double argument

        @param name the name of the parameter whose value is to be returned

        @return the value of a double argument

        @aribaapi documented
    */
    public double getDouble (String name)
    {
        parse();
        assertHandler(name, TypeDouble);
        return ((Double)get(name)).doubleValue();
    }

    /**
        retrieve the value of a file argument

        @param name the name of the parameter whose value is to be returned

        @return the value of a file argument

        @aribaapi documented

    */
    public File getFile (String name)
    {
        parse();
        assertHandler(name, TypeFile);
        return (File)get(name);
    }

    /**
        retrieve the value of a url argument

        @param name the name of the parameter whose value is to be returned

        @return the value of a url argument

        @aribaapi documented
    */
    public URL getURL (String name)
    {
        parse();
        assertHandler(name, TypeURL);
        return (URL)get(name);
    }

    /**
        retrieve the value of a integer range argument

        @param name the name of the parameter whose value is to be returned

        @return the value of the integer range argument

        @aribaapi documented
    */
    public List/*<Integer>*/ getIntegerList (String name)
    {
        parse();
        assertHandler(name, TypeIntegerList);
        return (List)get(name);
    }

    /**
        Wrapper function to implement case insensitivity
    */
    private void putHandler (String name, ArgumentHandler handler)
    {
        String key = (this.ignoreCase)?name.toLowerCase():name;
        this.handlerTable.put(key, handler);
    }

    /**
        Wrapper function to implement case insensitivity
    */
    private ArgumentHandler getHandler (String name)
    {
        Assert.that(name != null,
                    "Attempted to retrieve argument with null name");
        String key = (this.ignoreCase)?name.toLowerCase():name;
        return (ArgumentHandler)this.handlerTable.get(key);
    }

    private void assertHandler (String name, int type)
    {
        ArgumentHandler handler = this.getHandler(name);
        Assert.that(handler != null, "Unknown argument %s", name);
        Assert.that(handler.type == type,
                    "Argument %s of type %s not of type %s",
                    name,
                    Constants.getInteger(handler.type), Constants.getInteger(type));
    }

    /**
        Actually process the argument list and return the values
    */
    private void parse ()
    {
            // If we've already parsed skip it
        if (results != null) {
            return;
        }
        results = new MultiValueHashtable();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith(SwitchPrefix)) {
                usage(Fmt.S("Unexpected switch prefix received %s, " +
                            "expecting %s",
                            arg,
                            SwitchPrefix));
            }

            arg = arg.substring(1);
            ArgumentHandler handler = this.getHandler(arg);
            if (handler == null) {
                usage(Fmt.S("Unknown switch %s", arg));
            }

            String value = null;
            if (handler.type != TypeBoolean) {
                i++;
                if (i == args.length) {
                    usage(Fmt.S("Switch %s%s expected argument",
                                SwitchPrefix,
                                arg));
                }
                value = args[i];
            }

            String error = handler.handle(arg, this, value);
            if (error != null) {
                usage(error);
            }
        }

            // Handle defaulting and required arguments
        for (int i = 0; i < arguments.size(); i++) {
            String arg = (String)arguments.get(i);
            ArgumentHandler handler = this.getHandler(arg);
            Object value = get(handler.name);
            if (value == null) {
                if (handler.optional) {
                    put(handler.name, handler.defaultValue);
                }
                else {
                    usage(Fmt.S("Required argument %s missing", arg));
                }
            }
        }
        return;
    }

    /**
        Print a usage statement and exit

        @param error the usage string

        @aribaapi private
    */
    public void usage (String error)
    {
        Fmt.F(err, "%s\n", error);
        if (args.length != 0) {
            Fmt.F(err, "Arguments were:\n", error);
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                Fmt.F(err, "%s: %s\n", Constants.getInteger(i), arg);
            }
        }
        usage();
    }

    /**
        Print a usage statement and exit

        @aribaapi ariba
    */
    public void usage ()
    {
        Fmt.F(err, "%s\n", usage);
        for (int i=0; i < arguments.size(); i++) {
            String arg = (String)arguments.get(i);
            ArgumentHandler handler = this.getHandler(arg);
            String usageForThisHandler = handler.usage();
            if (usageForThisHandler != null) {
                Fmt.F(err, "%s\n", usageForThisHandler);
            }
        }
        if (exitUponParsingError) {
            SystemUtil.exit(1);
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    /**
        Print the argument list
        @aribaapi ariba

        used by test code
    */
    public String image ()
    {
        FastStringBuffer fsb = new FastStringBuffer();
        for (int i=0; i<arguments.size(); i++) {
            String arg = (String)arguments.get(i);
            Object value = this.get(arg);
            ArgumentHandler handler = this.getHandler(arg);
            String usage = handler.usage();
            if (usage != null) {
                fsb.append(handler.usage());
                fsb.append(", value = ");
                fsb.append(value);
                fsb.append("\n");
            }
        }
        return fsb.toString();
    }

    /**
        Constant for lame Map and vector classes.
        @aribaapi private
    */
    public static final Object Null = new Object();
    /**
        Wrapper for lame Maps
    */
    void put (Object key, Object value)
    {
        if (value == null) {
            value =  Null;
        }
        results.put(key, value);
    }
    /**
        Wrapper for lame Maps
    */
    Object get (Object key)
    {
        Object value = results.getLastValue(key);
        if (value == Null) {
            value =  null;
        }
        return value;
    }

    /**
        given foo returns noFoo

        @param name String  to negate
        @return the negated string

        @aribaapi private
    */
    public static String negate (String name)
    {
        char[] array = name.toCharArray();
        array[0] = Character.toUpperCase(array[0]);
        return Fmt.S(NoPrefix + "%s", new String(array));
    }

    /**
        This is needed to get the nodename from the command line
        before the log is created and before most of the rest of the
        setup is done.

        DO not use it without talking to the core server group!

        @param name the name of the parameter
        @return the value of the parameter.
        @aribaapi ariba
    */
    public static String getHackedParameter (String name)
    {
        if (globalArgs == null) {
            return "";
        }
        name = StringUtil.strcat("-", name);
        /* if globalArgs has less than 2 elements, we are still okay because
            the for loop won't iterate.
        */
        for (int i=globalArgs.length-2; i>=0; i--) {
            String arg = globalArgs[i];
            boolean match =
                (globalIgnoreCase)?name.equalsIgnoreCase(arg):name.equals(arg);
            if (match) {
                return globalArgs[i+1];
            }
        }
        return "";
    }
}

/**
    Helper class to store argument information

    @aribaapi private
*/
class ArgumentHandler
{
    private static final char CommaSeparatedDelimiter  = ',';
    private static final char HyphenSeparatedDelimiter  = '-';

    /**
        The name of this argument
    */
    String name;

    /**
        Is this argument optional
    */
    boolean optional;

    /**
        TypeString, TypeBoolean, TypeInteger, TypeDouble, TypeFile, TypeURL,
        TypeIntegerList
    */
    int type;

    /**
        If it is optional the default
    */
    Object defaultValue;

    /**
        Short usage description
    */
    String usage;

    /**
        Simple Constructor
    */
    ArgumentHandler (String  name,
                     boolean optional,
                     int     type,
                     Object  defaultValue,
                     String  usage)
    {
        this.name          = name;
        this.optional      = optional;
        this.type          = type;
        this.defaultValue  = defaultValue;
        this.usage         = usage;
    }

    /**
        Returns null on success, error String otherwise
    */
    String handle (String arg, ArgumentParser results, String value)
    {
        Object object;
        switch (type) {
          case ArgumentParser.TypeString:
            object = value;
            break;
          case ArgumentParser.TypeBoolean:
            if (arg.startsWith(ArgumentParser.NoPrefix)) {
                object = Boolean.FALSE;
            }
            else {
                object = Boolean.TRUE;
            }
            break;
          case ArgumentParser.TypeInteger:
            try {
                object = Constants.getInteger(IntegerFormatter.parseInt(value));
            }
            catch (ParseException e) {
                return Fmt.S("Expected integer argument to %s", name);
            }
            break;
          case ArgumentParser.TypeDouble:
            try {
                double d = DoubleFormatter.parseDouble(
                    value,
                    ResourceService.LocaleOfLastResort);
                object = new Double(d);
            }
            catch (ParseException e) {
                return Fmt.S("Expected double argument to %s", name);
            }
            break;
          case ArgumentParser.TypeFile:
            object = (value == null) ? null : new File(value);
            break;
          case ArgumentParser.TypeURL:
            try {
                object = (value == null) ? null : URLUtil.makeURL(value);
            }
            catch (MalformedURLException e) {
                return Fmt.S("Expected URL argument to %s", name);
            }
            break;
          case ArgumentParser.TypeIntegerList:
            try {
                object = getIntegerListValue((String)value);
            }
            catch (ParseException e) {
                return Fmt.S("Expected integer range argument to %s, " +
                        "got problem: %s", name, e.getMessage());
            }
            break;
          default:
            Fmt.F(SystemUtil.err(),
                  "Unexpected type in ArgumentHandler: %s",
                  Constants.getInteger(type));
            SystemUtil.exit(1);
            return "";
        }
        results.put(name, object);
        return null;
    }

    /**
        Parse the integer list value.
        For example: the input value of 1,3-5,7 will output:
            {1,3,4,5,7}
        Currently only non-negative integers are supported.
    */
    private static List/*<Integer>*/ getIntegerListValue (String input)
      throws ParseException
    {
        List/*<Integer>*/ output = ListUtil.list();
        List ids = ListUtil.delimitedStringToList(input,
                CommaSeparatedDelimiter);
        for (int i = 0; i < ids.size(); i++) {
            String id = (String)ids.get(i);
            List ranges = ListUtil.delimitedStringToList(id,
                    HyphenSeparatedDelimiter);
            if (ranges.size() > 2) {
                throw new ParseException(Fmt.S(
                            "Too many values for range: %s", ranges), i);
            }
            if (ranges.size() == 1) {
                Integer val = getIntegerValueFromList(ranges, 0);
                output.add(val);
            }
            else {
                Integer from = getIntegerValueFromList(ranges, 0);
                Integer to = getIntegerValueFromList(ranges, 1);
                if (from.intValue() > to.intValue()) {
                    throw new ParseException(Fmt.S(
                        "The from value %s needs to be smaller than to: %s",
                        from, to), i);
                }
                for (int r = from.intValue(); r <= to.intValue(); r++) {
                    output.add(Constants.getInteger(r));
                }
            }
        }

        return output;
    }

    private static Integer getIntegerValueFromList (List ranges, int index)
      throws ParseException
    {
        String indexValue = (String)ranges.get(index);
        if (StringUtil.nullOrEmptyString(indexValue)) {
            throw new ParseException(Fmt.S(
                        "Unrecognized value in %s at pos %s",
                        ranges, Constants.getInteger(index)), index);
        }
        Integer value = Constants.getInteger(IntegerFormatter.parseInt(indexValue));
        return value;
    }

    /**
        Returns a usage line for this argument
    */
    public String usage ()
    {
        if (usage == null) {
            return null;
        }
        FastStringBuffer result = new FastStringBuffer();
        if (optional) {
            result.append(ArgumentParser.OptionalOpen);
        }
        result.append(ArgumentParser.SwitchPrefix);
        result.append(name);
        switch (type) {
          case ArgumentParser.TypeString:
          case ArgumentParser.TypeInteger:
          case ArgumentParser.TypeDouble:
          case ArgumentParser.TypeFile:
          case ArgumentParser.TypeURL:
          case ArgumentParser.TypeIntegerList:
            result.append(' ');
            result.append(usage);
            break;
          case ArgumentParser.TypeBoolean:
            if (optional && ((Boolean)defaultValue).booleanValue()) {
                result.append(ArgumentParser.BooleanDefault);
            }
            result.append(ArgumentParser.ValidChoices);
            result.append(ArgumentParser.SwitchPrefix);
            result.append(ArgumentParser.negate(name));
            if (optional && !((Boolean)defaultValue).booleanValue()) {
                result.append(ArgumentParser.BooleanDefault);
            }

            if (!StringUtil.nullOrEmptyOrBlankString(usage)) {
                result.append(' ');
                result.append(usage);
            }
            break;
          default:
            Fmt.F(SystemUtil.err(),
                  "Unexpected type in ArgumentHandler: %s",
                  Constants.getInteger(type));
            SystemUtil.exit(1);
            break;
        }
        if (optional) {
            result.append(ArgumentParser.OptionalClose);
        }
        return result.toString();
    }
}
