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

    $Id: //ariba/platform/util/core/ariba/util/core/TokenTable.java#5 $
*/

package ariba.util.core;

import ariba.util.log.Log;
import java.util.Random;
import java.util.List;

/**
    A table of tokens that map to objects. Objects can be stored in the
    TokenTable. A token is returned that can be used to remove the object.
    The object is invalidated after the expireTime milliseconds.

    @aribaapi private
*/
public class TokenTable
{
    /*-----------------------------------------------------------------------
        Private Constants
      -----------------------------------------------------------------------*/

        // number of attempts to generate unique random token
    private static final int MaxTries = 10;

        // number of millseconds that token is valid
    private static final long DefaultExpiredTime = Date.MillisPerMinute * 6;


    /*-----------------------------------------------------------------------
        Private Fields
      -----------------------------------------------------------------------*/

        // the list of token objects
    private List tokens;

        // the expiration time for this token table
    private long expireTime;

        // a random number generator
    private Random random;

        // random object used for locking
    private Object lock;


    /*-----------------------------------------------------------------------
        Constructors
      -----------------------------------------------------------------------*/

    /** Creates a new TokenTable with the default expiration time. */
    public TokenTable ()
    {
        this(DefaultExpiredTime);
    }

    /**
        Creates a new TokenTable.  The expiration time is set to
        <b>expireTime</b>, which should be expressed in milliseconds.
    */
    public TokenTable (long expireTime)
    {
        this.lock   = new Object();
        this.tokens = ListUtil.list();
        this.random = new Random();
        this.expireTime = expireTime;
    }

    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/

    /**
        This method inserts the given object into the token table and assigns
        it a unique random string for lookup.  If we are unable to generate a
        unique token (which is extremely unlikely), return null.  Otherwise,
        return the unique string which can be used to retrieve the object.
    */
    public String insert (Object object)
    {
        boolean looking = true;
        String  token   = null;
        int i;

        for (i = 0; i < MaxTries && looking; i++)
        {
                // generate a random token
            long l = random.nextLong();
            if (l < 0) {
                l = -l;
            }
            token = Long.toString(l, 36);
            token = token.toUpperCase();

                // check that it is unique
            looking = false;
            synchronized (lock) {
                for (int j=0; j < tokens.size(); j++) {
                    TokenEntry entry = (TokenEntry)tokens.get(j);
                    if (entry.token.equals(token)) {
                        looking = true;
                        break;
                    }
                }
            }
        }

            // something is very wrong if we didn't get a unique token
            // in MaxTries tries
        if (i > MaxTries) {
            Log.serverOps.debug("insert() couldn't add token for %s", object);
            return null;
        }

            // insert the object into our list
        TokenEntry tokenEntry = new TokenEntry(token, object);
        synchronized (lock) {
            tokens.add(tokenEntry);
        }
        Log.serverOps.debug("insert() added token object (%s, %s)", token, object);

        return token;
    }

    /**
        This method looks up the object stored in our table for the given
        token string.  It also removes any objects in the table that have
        expired.
    */
    public Object lookup (String token)
    {
        TokenEntry found = null;

        synchronized (lock) {
            long now = System.currentTimeMillis();
            for (int i=0; i < tokens.size(); i++) {
                TokenEntry temp = (TokenEntry)tokens.get(i);
                if (now - temp.timestamp > expireTime) {
                    removeTokenAtIndex(i, now, temp);
                    i--;
                }
                else if (temp.token.equalsIgnoreCase(token)) {
                    found = temp;
                    Log.serverOps.debug("lookup() found token %s", temp.token);
                    break;
                }
            }
        }

        if (found == null) {
            return null;
        }
        else {
            return found.object;
        }
    }

    /**
        This method removes and returns the object stored in our table for the
        given token string.  It also removes any objects that have expired.
    */
    public Object remove (String token)
    {
        TokenEntry removed = null;

        synchronized (lock) {

            long now = System.currentTimeMillis();
            for (int i=0; i < tokens.size(); i++) {
                TokenEntry temp = (TokenEntry)tokens.get(i);

                if (now - temp.timestamp > expireTime) {
                    removeTokenAtIndex(i, now, temp);
                    i--;

                }
                else if (temp.token.equalsIgnoreCase(token)) {
                    removed = (TokenEntry)tokens.get(i);
                    tokens.remove(i);
                    Log.serverOps.debug("remove() found token %s", temp.token);
                    break;
                }
            }
        }


        if (removed == null) {
            return null;
        }

        return removed.object;
    }

    private void removeTokenAtIndex (int i, long now, TokenEntry entry)
    {
        Log.serverOps.debug("removed token '%s' because it expired " +
                        "(current = %s, timestamp = %s, expireTime = %s)",
                        entry.token, Long.toString(now),
                        Long.toString(entry.timestamp),
                        Long.toString(expireTime));
        tokens.remove(i);
    }

    public String toString ()
    {
        FastStringBuffer result = new FastStringBuffer();
        synchronized (lock) {
            for (int j=0; j < tokens.size(); j++) {
                TokenEntry temp = (TokenEntry)tokens.get(j);
                result.append(temp.toString());
            }
        }
        return result.toString();
    }

}

/** An entry in the token table. */
class TokenEntry
{
    String token;
    long   timestamp;
    Object object;

    TokenEntry (String token, Object object)
    {
        this.timestamp = System.currentTimeMillis();
        this.token     = token;
        this.object    = object;
    }

    public String toString ()
    {
        String time = Long.toString(timestamp);
        return Fmt.S("(%s,%s,%s)", token, time, object);
    }
}
