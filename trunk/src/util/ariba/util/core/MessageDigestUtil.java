/*
    Copyright 1996-2009 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/MessageDigestUtil.java#3 $
*/

package ariba.util.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
     A utility class for generating message digests, suitable for
     password hashing.

     @aribaapi ariba
*/
public class MessageDigestUtil
{
    private static final int Sha256DigestLength            = 32;

    private static SecureRandom _secureRandom;

    public static final String InvalidDigest               = "Invalid digest";

    static {
        try {
            _secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            Assert.fail(Fmt.S("Cannot instantiate secure random with algorithm SHA1PRNG: %s",
                              SystemUtil.stackTrace()));
        }
    }

    public static byte[] secureRandomBytes (int n)
    {
        byte[] b = new byte[n];
        _secureRandom.nextBytes(b);
        return b;
    }

    /**
        Digest a message with salt & iterations. Note two calls to this method with the same
        arguments will NOT return the same result. Instead, to compare, use compareDigest. The salt
        will be generated using a SecureRandom generator. The result will be base64 encoded.
        This method uses the SHA-256 algorithm.

        @param saltLength    - length of the salt (extra bytes in salt are ignored)
        @param iterations    - number of iterations - makes cracking more computationally expensive. Should choose
                               a reasonably large number here (thousands) to make this worthwhile.
        @param message       - string
        @return Base64 encoded message

        @throws SecurityException shouldn't happen
    */
    public static String digestWithSalt (int saltLength,
                                         int iterations,
                                         String message)
    {
        byte[] salt = secureRandomBytes(saltLength);

        return digestWithSalt(salt,
                              saltLength,
                              iterations,
                              message);
    }

    /**
        Digest a message with salt & iterations. The result will be base64 encoded.
        This method uses the SHA-256 algorithm.

        @param salt          - salt for this digest
        @param saltLength    - length of the salt (extra bytes in salt are ignored)
        @param iterations    - number of iterations - makes cracking more computationally expensive. Should choose
                               a reasonably large number here (thousands) to make this worthwhile.
        @param message       - string
        @return Base64 encoded message

        @throws SecurityException shouldn't happen
    */
    public static String digestWithSalt (byte[] salt,
                                         int saltLength,
                                         int iterations,
                                         String message)
    {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Initial seeding of hash with salt||password
            md.update(salt, 0, saltLength);
            md.update(StringUtil.getBytesUTF8(message));

            // Run the hashing operation "iterations" times,
            // supplying as input the result of the previous digesting.
            byte[] digest = new byte[md.getDigestLength()];
            for (int i = 0; i < iterations; i++) {
                md.digest(digest, 0, digest.length);
                md.reset();
                md.update(digest);
            }
            md.reset();

            ByteArrayOutputStream o =
                    new ByteArrayOutputStream(saltLength + digest.length);
            o.write(salt, 0, saltLength);
            o.write(digest);
            return Base64.encodeToString(o.toByteArray());
        }
        catch (IOException e) {
            throw new SecurityException(e);
        }
        catch (NoSuchAlgorithmException e) {
            throw new SecurityException(e);
        }
        catch (DigestException e) {
            throw new SecurityException(e);
        }
    }


    /**
        compares a digested message with a cleartext version, using the salt from the digested
        message.

        @param saltLength    - salt length used for the digest
        @param iterations    - number of iterations - makes cracking more computationally expensive
        @param hashedMessage - the hashed message
        @param candidate     - the candidate cleartext to be compared
        @return true if the digests match
    */
    public static boolean compareWithSalt (int saltLength,
                                           int iterations,
                                           String hashedMessage,
                                           String candidate)
    {
        byte[] hashedMessageBytes = Base64.decodeFromString(hashedMessage);
        if (hashedMessageBytes == null ||
            hashedMessageBytes.length != saltLength + Sha256DigestLength) {
            throw new IllegalArgumentException(InvalidDigest);
        }
        String hashedCandidate = digestWithSalt(hashedMessageBytes, saltLength, iterations, candidate);

        return hashedMessage.equals(hashedCandidate);
    }

}
