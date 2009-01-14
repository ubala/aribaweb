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

    $Id: //ariba/platform/util/core/ariba/util/core/InternCharToString.java#5 $
*/

package ariba.util.core;

import ariba.util.log.Log;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
    This class implements an instance of a GrowOnlyHashtable with the
    following properties:

    -- implements a version of intern() that does not have the small
    size limitations of the native intern() methods on at least hp and
    nt.

    -- unlike all current implementations of intern, it does not
    require any synchronization for the usual case of a get()

    -- exposes methods to intern a string and "intern" a char[]
    and retrieve a string.

    -- exposes a method to calculate the hashCode of a char[] as
    though it were a string. (there is a varient for the sun and the
    standard implementations of hashCode() The code dynamically
    determines which to use)

    This could be modified to allow a subrange of a char[] to be
    interned.

    Typically the static methods should be used, which will intern the
    strings to a single shared table.  Only create a new instance of
    this class if for some reason you want a separate pool of strings.

    @aribaapi private
*/
public class InternCharToString extends GrowOnlyHashtable
{

    private static int hashFN = -1;
    private static InternCharToString cache = new InternCharToString(1024);

    /**
        Private method that calculates the hash code for the char[]
        <b>val</b> according to the java spec.
    */
    private static final int normalHashCode (char[] val)
    {
        return MathUtil.normalHashCode(val);
    }

    /**
        Private method that calculates the hash code for the char[]
        <b>val</b> according to the sun's unique implementation which
        they say will be standard in 1.2 (love those standards)
    */
    private static final int sunHashCode (char[] val)
    {
        return MathUtil.sunHashCode(val);
    }

    /*
        Method we are currently using to guarantee a stable hashcode
        across all VMS over all time. Do not allow the behavior of
        this to change.
    */
    public static final int jdk12HashCode (String s)
    {
        return sunHashCode(s.toCharArray());
    }
    
    /**
        Private method that calculates the hash code for the char[]
        <b>val</b> using the appropriate hash function. Love those
        standards)
    */
    public static final int hashCodeForChars (char[] val)
    {
        if (hashFN == 0) {
            return normalHashCode(val);
        }
        if (hashFN == 1) {
            return sunHashCode(val);
        }
            // if things go terribly wrong, go the old route of
            // creating a string just to find it's hash code...
        String s = new String(val);
        return s.hashCode();
    }

        // force proper typing by only exposing correct interfaces


    /**
        Public method that interns the specified String <b>s</b>. This
        will not provide objects that compare pointer equals with the
        native String.intern() method as that method is too slow and
        can not accomidate moderate numbers of interned strings.
    */
    public static String intern (String s)
    {
        return cache.privIntern(s);
    }

    /**
        Public method that interns the specified char[] <b>chars</b>.
        This method is identical to the previous method which uses a
        String as an argument, except that it does not require the
        caller to construct a temporary String for lookup.
    */
    public static String intern (char[] chars)
    {
        return cache.privIntern(chars);
    }

    /**
        Public method that interns the specified String <b>s</b>. This
        will not provide objects that compare pointer equals with the
        native String.intern() method as that method is too slow and
        can not accomidate moderate numbers of interned strings.
    */
    public String internUnshared (String s)
    {
        return privIntern(s);
    }

    /**
        Public method that interns the specified char[] <b>chars</b>.
        This method is identical to the previous method which uses a
        String as an argument, except that it does not require the
        caller to construct a temporary String for lookup.
    */
    public String internUnshared (char[] chars)
    {
        return privIntern(chars);
    }

    /**
        Private method for the implementation of intern on Object to
        include both String and char[] in the same
        implementation. This allows and bad calls to fail at compile
        time rather than run time. The object <b>o</b> must be either
        a String or a char[] which is enforced through exposure of
        those public methods.
    */
    private String privIntern (Object o)
    {
        String result = (String)get(o);
        if (result != null) {
            return result;
        }
            // if it is not there on the first pass, synchronize and
            // check again to keep intern semantics
        synchronized (this) {
            result = (String)get(o);
            if (result != null) {
                return result;
            }
            String stringToInsert;
            if (o instanceof String) {
                stringToInsert = (String)o;
            }
            else {
                stringToInsert = new String((char [])o);
            }
            put(stringToInsert, stringToInsert);
            return(stringToInsert);
        }
    }


    private InternCharToString ()
    {
        super();
        Assert.that(false, "do not call default constructor it is private");
    }

    /**
        Constructs an InternCharToString hashtable capable of holding
        at least <b>initialCapacity</b> elements before needing to
        grow. It also determines which is the correct hash function to
        use from the two standards.  Most uses of this class should just
        call the static intern methods, which will make use of a single,
        shared table of strings.  Only use this constructor if you want 
        to make a separate pool of objects for some unusual reason.
    */
    public InternCharToString (int initialCapacity)
    {
        super(initialCapacity);
            // Try out a few hash functions on startup to figure out
            // which hash function is actually used.
        String fooString = "bar";
        char []fooArray = new char[3];
        fooString.getChars(0, 3, fooArray, 0);
        if (fooString.hashCode() == normalHashCode(fooArray)) {
            hashFN = 0;
        }
        if (fooString.hashCode() == sunHashCode(fooArray)) {
            hashFN = 1;
        }
        if (hashFN < 0) {
            hashFN = 100;
            Log.util.warning(2798);
        }
    }

    /**
        Helper function that returns the appropriate hash code for the
        object <b>o</b>. It is overridden to compute the hash value
        for a char[] as though it were a String.
    */
    protected int getHashValueForObject (Object o)
    {
        if (o instanceof char[]) {
            return(hashCodeForChars((char [])o));
        }
        return o.hashCode();
    }

    /**
        Helper function to determine if two objects are equal. This
        method is overriden to allow a char[] to compare equals to a
        String.  It returns true if <b>obj1</b> and <b>obj2</b>
        compare as equal.
    */
    protected boolean objectsAreEqualEnough (Object obj1, Object obj2)
    {
            // obj2 is always a string
        if (obj1 == obj2) {
            return true;
        }
        if (obj1 instanceof char[]) {
            return(StringUtil.charsEqualString((char [])obj1, (String)obj2));
        }
        return obj2.equals(obj1);
    }

    public static void main (String []args)
    {
        Thread []t = new Thread[20];
        ThreadRunner []tr = new ThreadRunner[t.length];
        for (int i=0; i<t.length; i++) {
            tr[i]=new ThreadRunner();
            t[i]=new Thread(tr[i]);
        }
            // start the threads as closely as possible
        for (int i=0; i<t.length; i++) {
            t[i].start();
        }
        for (int i=0; i<t.length; i++) {
            try {
                t[i].join();
            }
            catch (InterruptedException ex) {
                Assert.that(false, "%s", SystemUtil.stackTrace(ex));
            }
        }
        int totalWins=0;
        int totalLines = tr[0].totalLines;
        boolean success = true;
        for (int i=0; i<t.length; i++) {
            Fmt.F(SystemUtil.out(), "thread %s had %s wins\n",
                  Constants.getInteger(i),
                  Constants.getInteger(tr[i].winCount));
            totalWins+=tr[i].winCount;
            String []firstInternedStrings = tr[0].internedStrings;
            String []currentInternedStrings = tr[i].internedStrings;
            for (int j=0; j<totalLines; j++) {
                if (firstInternedStrings[j]!=currentInternedStrings[j]) {
                    success = false;
                    Fmt.F(SystemUtil.out(), 
                          "whoops, didn't work. array %s has string %s " +
                          "with hascode %s rather than %s with hascode %s\n",
                          Constants.getInteger(i), currentInternedStrings[j],
                          Constants.getInteger(
                              System.identityHashCode(currentInternedStrings[j])),
                          firstInternedStrings[j],
                          Constants.getInteger(
                              System.identityHashCode(firstInternedStrings[j])));
                }
            }
        }
        Fmt.F(SystemUtil.out(), "total lines = %s, total wins = %s\n",
              Constants.getInteger(totalLines),
              Constants.getInteger(totalWins));
        Assert.that(totalLines == totalWins, "nope, didn't work right");
        Assert.that(success, "mismatched lines");
        SystemUtil.exit(0);
    }
}
class ThreadRunner implements Runnable
{
    public ThreadRunner ()
    {
    }
    public String [] internedStrings = new String[100000];
    public int winCount=0;
    public int totalLines=0;
    public void run ()
    {
        try {
            InputStream istream = IOUtil.bufferedInputStream(new File("words"));
            char []buf = new char[1024];
            String line=null;
            String internedData;
            while ((line = IOUtil.readLine(istream, buf))!=null) {
                internedData = InternCharToString.intern(line);
                internedStrings[totalLines]=internedData;
                if (internedData == line) {
                        // whoo hoo, I won getting a line into the table.
                    winCount++;
                }
                totalLines++;
            }
        }
        catch (IOException ex) {
            Assert.that(false, "%s", SystemUtil.stackTrace(ex));
        }
    }
}

