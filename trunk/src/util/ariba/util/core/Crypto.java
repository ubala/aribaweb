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

    $Id: //ariba/platform/util/core/ariba/util/core/Crypto.java#8 $
*/

package ariba.util.core;

import ariba.util.log.Log;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
    Disclaimer. This is not production security! This is a weak
    attempt to thwart direct or accidental packet sniffing from
    obtaining cleartext passwords. Of course real encryption is the
    solution but at this moment is not achievable due to schedule
    constraints.

    @aribaapi private
*/
public class Crypto implements CryptoInterface
{
    private CryptoChar cryptoChar;

    private boolean passThrough = false;

    public Crypto (Object key)
    {
        this.cryptoChar = new CryptoChar(key.hashCode());
    }

    public Crypto (Object key, boolean enabled)
    {
        this.passThrough = !enabled;
        this.cryptoChar = new CryptoChar(key.hashCode());
    }

    public Object encrypt (Object target)
    {
        if (this.passThrough) {
            return target;
        }
            // Log.runtime.debug("encrypt %s", target);
        if (target instanceof String) {
            return this.encrypt((String)target);
        }
        if (target instanceof Map) {
            return this.encrypt((Map)target);
        }
        if (target instanceof List) {
            return this.encrypt((List)target);
        }
        return target;
    }

    public char encrypt (char ch)
    {
        return this.cryptoChar.encrypt(ch);
    }

    public String encrypt (String string)
    {
        int strLen = string.length();
        char [] charBuffer = new char[strLen];
        string.getChars(0, strLen, charBuffer, 0);
        reverse(charBuffer, strLen);
        for (int idx = 0; idx < strLen; idx++) {
            charBuffer[idx] = this.encrypt(charBuffer[idx]);
        }

        return new String(charBuffer);
    }

    public static char [] reverse (char [] charBuffer, int charBufferLength)
    {
        int halfStrLen = charBufferLength/2;
        for (int idx = 0; idx < halfStrLen; idx++) {
            char temp = charBuffer[idx];
            int revIdx = charBufferLength-idx-1;
            charBuffer[idx] = charBuffer[revIdx];
            charBuffer[revIdx] = temp;
        }
        return charBuffer;
    }


    public Map encrypt (Map map)
    {
        int size = map.size();
        // calling new Map with size zero is a no-no
        Map result = (size>0)?MapUtil.map(size):MapUtil.map();
        Iterator e = map.keySet().iterator();
        while (e.hasNext()) {
            Object key = e.next();
            Object value = map.get(key);
            result.put(this.encrypt(key), this.encrypt(value));
        }
        return result;
    }

    public List encrypt (List vector)
    {
        int size = vector.size();
        // calling ListUtil.list with size zero is a no-no
        List result = (size>0)?ListUtil.list(size):ListUtil.list();
        for (int idx = 0; idx < size; idx++) {
            Object value = vector.get(idx);
            result.add(this.encrypt(value));
        }
        return result;
    }

    public Object decrypt (Object target)
    {
        if (this.passThrough) {
            return target;
        }
            // Log.runtime.debug("decrypt %s", target);
        if (target instanceof String) {
            return this.decrypt((String)target);
        }
        if (target instanceof Map) {
            return this.decrypt((Map)target);
        }
        if (target instanceof List) {
            return this.decrypt((List)target);
        }
        return target;
    }

    public char decrypt (char ch)
    {
        return this.cryptoChar.decrypt(ch);
    }

    public String decrypt (String string)
    {
        int strLen = string.length();
        char [] charBuffer = new char[strLen];
        string.getChars(0, strLen, charBuffer, 0);
        reverse(charBuffer, strLen);
        for (int idx = 0; idx < strLen; idx++) {
            charBuffer[idx] = this.decrypt(charBuffer[idx]);
        }

        return new String(charBuffer);
    }

    public Map decrypt (Map map)
    {
        int size = map.size();
        // calling new Map with size zero is a no-no
        Map result = (size>0)?MapUtil.map(size):MapUtil.map();
        Iterator e = map.keySet().iterator();
        while (e.hasNext()) {
            Object key = e.next();
            Object value = map.get(key);
            result.put(this.decrypt(key), this.decrypt(value));
        }
        return result;
    }

    public List decrypt (List vector)
    {
        int size = vector.size();
        // calling ListUtil.list with size zero is a no-no
        List result = (size>0)?ListUtil.list(size):ListUtil.list();
        for (int idx = 0; idx < size; idx++) {
            Object value = vector.get(idx);
            result.add(this.decrypt(value));
        }
        return result;
    }

    /*
    public static void main (String [] args)
    {
        test(0,(char)0xFFFF);
        test(0,'a');
        for (int idx = 0; idx < 16; idx++) {
            bigtest(idx, args);
        }
    }

    public static void test (int key, char input)
    {
        PrintStream out = System.out;
        Random random = new Random();
        out.println("input is " + input);
        Crypto captCrunch = new Crypto(key);
        char x = captCrunch.encrypt(input);
        out.println("crypted is " + x);
        char y = captCrunch.decrypt(x);
        out.println("decrypted is " + y);
    }

    public static String formatArray (String [] args)
    {
        String output = Constants.EmptyString;
        for (int idx = 0; idx < args.length; idx++) {
            output =
                output +
                " arg[" +
                idx +
                "] = " +
                args[idx];

        }
        return output;
    }

    public static void bigtest (int key, String [] args)
    {
        PrintStream out = System.out;
        out.println("Crypting " + formatArray(args));

        Crypto captCrunch = new Crypto(key);

        List vector = ListUtil.list();
        for (int idx = 0; idx<args.length - 1; idx+=2) {
            vector.add(Constants.getInteger(idx));

            Map map = new Map();
            map.put(args[idx], args[idx+1]);
            vector.add(map);
        }


        out.println("List - precrypt: " + vector);
        vector = captCrunch.encrypt(vector);
        out.println("List - prostcrypt: " + vector);
        vector = (List) captCrunch.decrypt(vector);
        out.println("List - prostdecrypt: " + vector);
    }
    */
}

class CryptoChar
{
    static final int CharBitLength = 16;
    static final int MinShift = 5;

    int encryptLowerCharShiftCount;
    int encryptUpperCharShiftCount;

    char encryptLowerCharMask;
    char encryptUpperCharMask;

    char decryptLowerCharMask;
    char decryptUpperCharMask;

    int decryptLowerCharShiftCount;
    int decryptUpperCharShiftCount;

    public CryptoChar (int key)
    {
        this.encryptLowerCharShiftCount =
            Math.abs(key % (CharBitLength-MinShift)) +
            MinShift;

        Log.util.debug("CryptoChar - key: %s", this.encryptLowerCharShiftCount);
        this.init();
    }

    private void init ()
    {
        this.encryptUpperCharShiftCount =
            CharBitLength - this.encryptLowerCharShiftCount;

        this.encryptLowerCharMask = (char)
            (0xFFFF >>> this.encryptLowerCharShiftCount);
        this.encryptUpperCharMask = (char)
            (0xFFFF << this.encryptUpperCharShiftCount);

        this.decryptLowerCharMask = (char)
            (this.encryptUpperCharMask >>> this.encryptUpperCharShiftCount);
        this.decryptUpperCharMask = (char)
            (this.encryptLowerCharMask << this.encryptLowerCharShiftCount);

        this.decryptLowerCharShiftCount = this.encryptUpperCharShiftCount;
        this.decryptUpperCharShiftCount = this.encryptLowerCharShiftCount;
    }

    public char encrypt (char val)
    {
        char lowerShifted = (char)
            (val << this.encryptLowerCharShiftCount);
        char upperShifted = (char)
            ((val & this.encryptUpperCharMask) >>>
             this.encryptUpperCharShiftCount);
        char retVal = (char)(lowerShifted | upperShifted);
        return retVal;
    }

    /*
    public void dumpChar (char x)
    {
        PrintStream out = System.out;
        byte upper = (byte)(x << CharBitLength);
        byte lower = (byte)x;
        out.println(this.dumpByte(upper)+this.dumpByte(lower));
    }

    public String dumpByte (byte x)
    {
        String output = Constants.EmptyString;
        for (int idx = 0; idx < 16; idx++) {
            boolean on = ((x & 0x80)==0);
            if (on) {
                output = output + "1";
            }
            else {
                output = output + "0";
            }
            x = (byte)(x << 1);
        }
        return output;
    }
    */

    public char decrypt (char val)
    {
        char lowerShifted = (char)
            (val << this.decryptLowerCharShiftCount);
        char upperShifted = (char)
            ((val & this.decryptUpperCharMask) >>>
             this.decryptUpperCharShiftCount);
        char retVal = (char)(lowerShifted | upperShifted);
        return retVal;
    }
}

