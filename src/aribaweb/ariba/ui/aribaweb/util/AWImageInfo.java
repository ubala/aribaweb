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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWImageInfo.java#6 $
*/

package ariba.ui.aribaweb.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

abstract class AWImageReader extends AWBaseObject
{
    static final byte M_SOF0  = (byte)0xC0;    // Start Of Frame N
    static final byte M_SOF1  = (byte)0xC1;    // N indicates which compression process
    static final byte M_SOF2  = (byte)0xC2;    // Only SOF0-SOF2 are now in common use
    static final byte M_SOF3  = (byte)0xC3;
    static final byte M_SOF5  = (byte)0xC5;    // NB: codes C4 and CC are NOT SOF markers
    static final byte M_SOF6  = (byte)0xC6;
    static final byte M_SOF7  = (byte)0xC7;
    static final byte M_SOF9  = (byte)0xC9;
    static final byte M_SOF10 = (byte)0xCA;
    static final byte M_SOF11 = (byte)0xCB;
    static final byte M_SOF13 = (byte)0xCD;
    static final byte M_SOF14 = (byte)0xCE;
    static final byte M_SOF15 = (byte)0xCF;
    static final byte M_SOI   = (byte)0xD8;    // Start Of Image (beginning of datastream)
    static final byte M_EOI   = (byte)0xD9;    // End Of Image (end of datastream)
    static final byte M_SOS   = (byte)0xDA;    // Start Of Scan (begins compressed data)
    static final byte M_COM   = (byte)0xFE;    // COMment
    static final byte hexByteFF   = (byte)0xFF;

    protected InputStream _inputStream;
    protected int width;
    protected int height;

    AWImageReader (InputStream inputStream)
    {
        super();
        _inputStream = inputStream;
    }

    public void finalize ()
    {
        try {
            _inputStream.close();
            _inputStream = null;
            super.finalize();
        }
        catch (Throwable throwable) {
            // swallow
            throwable = null;
        }
    }

    protected final int extractWord (byte bytesBuffer[], int offset)
    {
        return (bytesBuffer[offset] & 0xFF) | ((bytesBuffer[offset + 1] & 0xFF) << 8);
    }

    protected final int extractInt (byte bytesBuffer[], int offset)
    {
        return (bytesBuffer[offset + 1] & 0xFF) | ((bytesBuffer[offset] & 0xFF) << 8);
    }

    protected static int readBytes (InputStream inputStream, byte bytesBuffer[], int offset, int length)
    {
        while (length > 0) {
            try {
                int numberRead = inputStream.read(bytesBuffer, offset, length);
                if (numberRead < 0) {
                    break;
                }
                offset += numberRead;
                length -= numberRead;
            }
            catch (IOException exception) {
                break;
            }
        }
        return length;
    }

    private static boolean isGifFile (InputStream inputStream)
    {
        boolean isGifFile = false;
        byte bytesBuffer[] = new byte[13];
        readBytes(inputStream, bytesBuffer, 0, 13);
        isGifFile = ((bytesBuffer[0] == 'G') && (bytesBuffer[1] == 'I') && (bytesBuffer[2] == 'F'));
        return isGifFile;
    }

    private static boolean isJpegFile (AWResource resource)
    {
        return (resource.url().endsWith(".jpg") || resource.url().endsWith(".jpeg"));
    }

    public static AWImageReader imageReaderForFilepath (AWResource resource)
    {
        InputStream inputStream = null;
        try {
            inputStream = resource.inputStream();
            AWByteArray byteArray = AWUtil.byteArrayForInputStream(inputStream);
            AWImageReader imageReader = null;
            ByteArrayInputStream byteInput = new ByteArrayInputStream(byteArray.array());
            boolean isGif = isGifFile(byteInput);
            try {
                byteInput.close();
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
            byteInput = new ByteArrayInputStream(byteArray.array());
            if (isGif) {
                imageReader = new AWGifImageReader(byteInput);
            }
            else if (isJpegFile(resource)) {
                imageReader = new AWJpegImageReader(byteInput);
            }
            return imageReader;
        }
        finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
            catch (IOException ex){
                ex = null;
            }
        }
    }
}

final class AWGifImageReader extends AWImageReader
{
    AWGifImageReader (InputStream inputStream)
    {
        super(inputStream);
        byte bytesBuffer[] = new byte[13];
        readBytes(bytesBuffer, 0, 13);
        width = extractWord(bytesBuffer, 6);
        height = extractWord(bytesBuffer, 8);
    }

    protected final int readBytes (byte bytesBuffer[], int offset, int length)
    {
        return readBytes(_inputStream, bytesBuffer, offset, length);
    }

}

final class AWJpegImageReader extends AWImageReader
{
    private byte _bytesBuffer[] = new byte[2];

    // Ported from rdjpgcom.c

    AWJpegImageReader (InputStream inputStream)
    {
        super(inputStream);
        scan_JPEG_header();
    }

    private byte read_1_byte ()
    {
        try {
            _inputStream.read(_bytesBuffer, 0, 1);
        }
        catch (IOException exception) {
            // swallow
            exception = null;
        }
        return _bytesBuffer[0];
    }

    private int read_2_bytes ()
    {
        try {
            _inputStream.read(_bytesBuffer, 0, 2);
        }
        catch (IOException exception) {
            // swallow
            exception = null;
        }
        return extractInt(_bytesBuffer, 0);
    }

    void skip_variable ()
    {
        /* Skip over an unknown or uninteresting variable-length marker */
        /* Get the marker parameter length count */
        int length = read_2_bytes();
        /* Length includes itself, so must be at least 2 */
        if (length < 2) {
            logString("** Error: Erroneous marker length");
        }
        length -= 2;
        /* Skip over the remaining bytes */
        while (length > 0) {
            read_1_byte();
            length--;
        }
    }

    private void process_SOFn ()
    {
        // usual parameter length count
        int length = read_2_bytes();
        read_1_byte();

        // ** This is what we're actually after!
        height = read_2_bytes();
        width = read_2_bytes();

        byte num_components = read_1_byte();
        if (length != (8 + num_components * 3)) {
            logString("** Error bogus SOF marker length.");
        }
        for (byte ci = 0; ci < num_components; ci++) {
            // Component ID code
            read_1_byte();
            // H, V sampling factors
            read_1_byte();
            // Quantization table number
            read_1_byte();
        }
    }

    private byte first_marker ()
    {
        byte c1;
        byte c2;

        c1 = read_1_byte();
        c2 = read_1_byte();
        if (c1 != hexByteFF || c2 != M_SOI) {
            logString("** Not a JPEG file.");
        }
        return c2;
    }

    private byte next_marker ()
    {
        byte markerCodeByte;
        int discarded_bytes = 0;

        // Find hexByteFF byte; count and skip any non-FFs.
        markerCodeByte = read_1_byte();
        while (markerCodeByte != hexByteFF) {
            discarded_bytes++;
            markerCodeByte = read_1_byte();
        }
        // Get marker code byte, swallowing any duplicate FF bytes.  Extra FFs
        // are legal as pad bytes, so don't count them in discarded_bytes.
        do {
            markerCodeByte = read_1_byte();
        }
        while (markerCodeByte == hexByteFF);

        if (discarded_bytes != 0) {
            //logString("Warning: garbage data found in JPEG file\n");
        }
        return markerCodeByte;
    }

    private int scan_JPEG_header ()
    {
        byte marker = 0;
        // Expect SOI at start of file
        if (first_marker() != M_SOI) {
            throw new AWGenericException("Bad header in jpeg image");
        }
        // Scan miscellaneous markers until we reach SOS.
        for (;;) {
            marker = next_marker();
            switch (marker) {
                case M_SOF0:        // Baseline
                case M_SOF1:        // Extended sequential, Huffman
                case M_SOF2:        // Progressive, Huffman
                case M_SOF3:        // Lossless, Huffman
                case M_SOF5:        // Differential sequential, Huffman
                case M_SOF6:        // Differential progressive, Huffman
                case M_SOF7:        // Differential lossless, Huffman
                case M_SOF9:        // Extended sequential, arithmetic
                case M_SOF10:       // Progressive, arithmetic
                case M_SOF11:       // Lossless, arithmetic
                case M_SOF13:       // Differential sequential, arithmetic
                case M_SOF14:       // Differential progressive, arithmetic
                case M_SOF15:       // Differential lossless, arithmetic
                    process_SOFn();
                    break;
                case M_COM:
                    break;
                case M_EOI:            // in case it's a tables-only JPEG stream
                case M_SOS:            // stop before hitting compressed data
                    return marker;
                default:
                    skip_variable();
                    break;
            }
        }
    }
}

public final class AWImageInfo extends AWBaseObject
{
    public final int width;
    public final int height;
    public final AWEncodedString widthString;
    public final AWEncodedString heightString;
    private final AWEncodedString _url;
    private final AWResource _resource;

    public AWImageInfo (AWResource resource)
    {
        super();
        if (resource == null) {
            width = -1;
            height = -1;
            widthString = null;
            heightString = null;
            _resource = null;
            _url = null;
        }
        else {
            _resource = resource;
            AWImageReader imageReader = (_resource.url()== null) ? null : AWImageReader.imageReaderForFilepath(resource);
            if (imageReader != null) {
                width = imageReader.width;
                height = imageReader.height;
                widthString = AWEncodedString.sharedEncodedString(AWUtil.toString(width));
                heightString = AWEncodedString.sharedEncodedString(AWUtil.toString(height));
            }
            else {
                width = -1;
                height = -1;
                widthString = null;
                heightString = null;
            }
            _url = (_resource.canCacheUrl()) ? AWEncodedString.sharedEncodedString(_resource.url()) : null;
        }
    }

    public AWEncodedString url ()
    {
        return _url != null ? _url : AWEncodedString.sharedEncodedString(_resource.url());
    }
}
