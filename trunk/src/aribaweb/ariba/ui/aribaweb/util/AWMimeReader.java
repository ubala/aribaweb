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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWMimeReader.java#22 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.MIME;
import ariba.util.core.ProgressMonitor;
import ariba.util.core.StringUtil;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;

public final class AWMimeReader extends AWBaseObject
{
    private static final int BUFFER_SIZE = 1024;
    private final InputStream  _inputStream;
    private final byte[] _boundaryMarker;
    private boolean _isAtEnd = false;
    private int _contentLength;
    private int _position = 0;
        // The maximum number of bytes that are allowed within a single chunk (parameter value) in a multipart form request
    private static int _MaxBytesPerChunk = -1;
        // True if the max size of a chunk in the request has been exceeded when processing the request input
    private boolean _MaxChunkSizeExceeded = false;
        // Location of file upload temp files. If null, uploaded data files are stored in memory.
    private static String _fileUploadDirectory = null;

    public AWMimeReader (InputStream inputStream,
                         int contentLength,
                         String contentType) throws IOException
    {
        super();
        _inputStream = inputStream;
        _contentLength = contentLength;
        String parameterBoundary = mimeArgumentValue(contentType, MIME.ParameterBoundary);
        String boundaryString = Fmt.S("--%s", parameterBoundary);
        _boundaryMarker = boundaryString.getBytes();
        //scan off the preamble
        readToBoundary(_MaxBytesPerChunk);
    }

    public int contentLength ()
    {
        return _contentLength;
    }

    public int currentPosition ()
    {
        return _position;
    }

    /**
        Find the MIME argument value for the name provided.
        @param  value the complete MIME value from the header
        @param  name the name of the argument that we're looking for
        @return the value or null if there is no such argument
    */
    public static String mimeArgumentValue (String value, String name)
    {
        if ((value == null) || (name == null)) {
            return null;
        }
        StringTokenizer tokens = new StringTokenizer(value, ";");
        String token = tokens.nextToken();
        while (tokens.hasMoreTokens()) {
            token = tokens.nextToken().trim();

            if (StringUtil.startsWithIgnoreCase(token, name)) {
                int pos = token.indexOf("=");
                if (pos < 0) {
                    return null;
                }
                token = token.substring(pos+1);
                token.trim();

                if (token.startsWith("\"") && token.endsWith("\"")) {
                    token = token.substring(1, token.length() -1);
                }

                return token;
            }
        }
        return null;
    }

    // Todo: The two versions of readToMarker should be merged into one. This can be done by
    // modifying the method so that it uses a byte or file output stream to which the input bytes are written

    /**
        Reads input from the multipart form post until the marker bytes are reached.
        @param markerBytes the byte array containing the marker which signals the end of the chunk
        @param ostream the output stream to which the chunk bytes are written
        @param maxBytes maximum number of bytes to read
        @return number of bytes appended to the buffer
    */
    private int readToMarker (byte[] markerBytes,
                              BufferedOutputStream ostream,
                              int maxBytes)
        throws IOException
    {
        int bytesAppendedToBuffer = 0;
        int byteArraySize = 16 * BUFFER_SIZE;
        AWByteArray byteArray = new AWByteArray(byteArraySize);
        int markerLength = markerBytes.length;
        byte markerBytesEnd = markerBytes[markerLength - 1];

        while (_position < _contentLength) {
            int intRead = _inputStream.read();
            _position++;
            if (_position % 1024 == 0) {
                ProgressMonitor.instance().setCurrentCount(currentPosition() / 1024);
            }

            if (intRead == -1) {
                break;
            }
            byte byteRead = (byte)intRead;
            byteArray.addElement(byteRead);
            bytesAppendedToBuffer++;
            if ((maxBytes != -1) && (bytesAppendedToBuffer == (maxBytes + markerLength))) {
                // if the number of bytes has exceeded the maximum, copy what has been processed so far
                // and process the rest of the input without adding it to the buffer
                int byteLength = byteArray.inUse - markerLength;
                ostream.write(byteArray.array(), 0, byteLength);
                processOversizedChunkInRequest(byteArray, markerLength, markerBytesEnd, markerBytes);
                break;
            }
            if ((byteRead == markerBytesEnd) && byteArray.endsWith(markerBytes)) {
                int byteLength = byteArray.inUse - markerLength;
                if (byteLength != 0) {
                    // If this chunk is non-empty, there is a carriage return and line feed (blank line) between the end
                    // input data and the beginning of the marker. We need to strip off two characters to remove them.
                    byteLength = byteLength - 2;
                    // Adjust the count of bytes appended to buffer to account for marker and extra CRNL
                    bytesAppendedToBuffer = bytesAppendedToBuffer - markerLength - 2;
                }
                else {
                    // Adjust the count of bytes appended to buffer to account for marker
                    bytesAppendedToBuffer -= markerLength;
                }

                ostream.write(byteArray.array(), 0, byteLength);
                intRead = _inputStream.read();
                intRead = _inputStream.read();
                if ((byte)intRead == '-') {
                    _isAtEnd = true;
                }
                break;
            }
            if (byteArray.inUse >= (byteArraySize - 16)) {
                // If the buffer is full, we write out the current bytes and left shift the buffer so that
                // the marker and the extra carriage return and new line which follow the chunk data and precede the
                // marker remain in the buffer.
                ostream.write(byteArray.array(), 0, (byteArray.inUse - markerLength - 2));
                byteArray.leftShiftElements(byteArray.inUse - markerLength - 2);
            }
        }

        return bytesAppendedToBuffer;
    }

    private byte[] readToMarker (byte[] markerBytes, int maxBytes) throws IOException
    {
        int bytesAppendedToBuffer = 0;
        byte[] buffer = null;
        AWByteArray byteArray = new AWByteArray(16 * BUFFER_SIZE);
        int markerLength = markerBytes.length;
        byte markerBytesEnd = markerBytes[markerLength - 1];
        while (_position < _contentLength) {
            int intRead = _inputStream.read();
            _position++;
            if (_position % 1024 == 0) {
                ProgressMonitor.instance().setCurrentCount(currentPosition() / 1024);
            }

            if (intRead == -1) {
                break;
            }
            byte byteRead = (byte)intRead;
            byteArray.addElement(byteRead);
            bytesAppendedToBuffer++;
            if ((maxBytes != -1) && (bytesAppendedToBuffer == (maxBytes + markerLength))) {
                // if the number of bytes has exceeded the maximum, copy what has been processed so far
                // and process the rest of the input without adding it to the buffer
                int byteLength = byteArray.inUse - markerLength;
                buffer = new byte[byteLength];
                System.arraycopy(byteArray.array(), 0, buffer, 0, byteLength);
                processOversizedChunkInRequest(byteArray, markerLength, markerBytesEnd, markerBytes);
                break;
            }
            if ((byteRead == markerBytesEnd) && byteArray.endsWith(markerBytes)) {
                int byteLength = byteArray.inUse - markerLength;
                if (byteLength != 0) {
                    // If this chunk is non-empty, there is a carriage return and line feed (blank line) between the end
                    // input data and the beginning of the marker. We need to strip off two characters to remove them.
                    byteLength = byteLength - 2;
                }
                buffer = new byte[byteLength];
                System.arraycopy(byteArray.array(), 0, buffer, 0, byteLength);
                intRead = _inputStream.read();
                intRead = _inputStream.read();
                if ((byte)intRead == '-') {
                    _isAtEnd = true;
                }
                break;
            }
        }
        return buffer;
    }

    public static int maxBytesPerChunk ()
    {
        return _MaxBytesPerChunk;
    }

    public static void setMaxBytesPerChunk (int value)
    {
        _MaxBytesPerChunk = value;
    }

    // This method is called when the maximum size of a chunk (parameter value) has been exceeded while processing the
    // request input. It continues to process the input in order to scan for the marker, but it will dispose of
    // data in byteArray to keep it from growing.

    private void processOversizedChunkInRequest (AWByteArray byteArray,
                                                 int markerLength, byte markerBytesEnd,
                                                 byte[]markerBytes) throws IOException
    {
        _MaxChunkSizeExceeded = true;
        int sizeOfByteArray = byteArray.array().length;
        while (true) {
            if (byteArray.inUse >= sizeOfByteArray) {
                byteArray.leftShiftElements(sizeOfByteArray - markerLength);
            }
            int intRead = _inputStream.read();
            _position++;
            if (intRead == -1) {
                throw new IOException("Malformed request - unexpected end of input.");
            }
            byte byteRead = (byte)intRead;
            byteArray.addElement((byte)intRead);
            if ((byteRead == markerBytesEnd) && byteArray.endsWith(markerBytes)) {
                intRead = _inputStream.read();
                intRead = _inputStream.read();
                if ((byte)intRead == '-') {
                    _isAtEnd = true;
                }
                break;
            }
        }
    }

    public boolean maxChunkSizeExceeded ()
    {
        return _MaxChunkSizeExceeded;
    }

    public byte[] readToBoundary () throws IOException
    {
        return readToMarker(_boundaryMarker, _MaxBytesPerChunk);
    }

    public byte[] readToBoundary (int maxBytes) throws IOException
    {
        return readToMarker(_boundaryMarker, maxBytes);
    }

    public Parameters nextHeaders () throws IOException
    {
        if (_isAtEnd || (_position >= _contentLength)) {
            return null;
        }
        return AWRFC822Header.parse(_inputStream, AWCharacterEncoding.Default);
    }

    public AWFileData nextChunk (String fileName, String mimeType) throws IOException
    {
        return nextChunk(fileName, mimeType, _MaxBytesPerChunk, false);
    }

    public AWFileData nextChunk (String fileName, String mimeType, int maxBytes, boolean encrypted)
        throws IOException
    {
        _MaxChunkSizeExceeded = false;
        AWFileData fileData = null;
        if (_isAtEnd || (_position >= _contentLength)) {
            Log.aribaweb_request.debug(
                "nextChunk _position: %s, _contentLenght %s, _isAtEnd",
                _position, _contentLength, Boolean.toString(_isAtEnd));
            return null;
        }
        if (StringUtil.nullOrEmptyString(_fileUploadDirectory)) {
            byte[] byteArray = readToBoundary(maxBytes);
            Log.aribaweb_request.debug("nextChunk byteArray.length: %s",
                                       byteArray.length);
            if (byteArray.length != 0) {
                fileData =
                    new AWFileData(fileName, byteArray, mimeType, maxChunkSizeExceeded());
            }
        }
        else {
            File uploadDirectory = new File(_fileUploadDirectory);
            File uploadFile = File.createTempFile("awupload", ".tmp", uploadDirectory);
            OutputStream tempStream = new FileOutputStream(uploadFile);
            if (encrypted) {
                tempStream = AWEncryptionProvider.getProvider().getCipherOutputStream(tempStream);
            }
            BufferedOutputStream ostream =
                new BufferedOutputStream(tempStream);
            int bytesRead = readToMarker(_boundaryMarker, ostream, maxBytes);
            Log.aribaweb_request.debug("nextChunk byteRead: %s", bytesRead);
            ostream.close();
            if (!StringUtil.nullOrEmptyOrBlankString(fileName)) {
                fileData = new AWFileData(fileName, uploadFile, mimeType,
                                          maxChunkSizeExceeded(), bytesRead, encrypted);
            }
            else {
                // Need to remove temporary file because no data bytes were read
                uploadFile.delete();
            }
        }
        return fileData;
    }

    public byte[] nextChunk () throws IOException
    {
        return nextChunk(_MaxBytesPerChunk);
    }

    public byte[] nextChunk (int maxBytes) throws IOException
    {
        if (_isAtEnd || (_position >= _contentLength)) {
            Log.aribaweb_request.debug("nextChunk2 _position: %s, _contentLenght %s, _isAtEnd", _position, _contentLength, Boolean.toString(_isAtEnd));
            return null;
        }
        return readToBoundary(maxBytes);
    }

    public static void setFileUploadDirectory (String directory)
    {
        _fileUploadDirectory = directory;
    }

    public static String fileUploadDirectory ()
    {
        return _fileUploadDirectory;
    }
}

/**
    Note:  This code taken directly from ariba.util.net.RFC822Header and modified to
    accept a character encoding.  We'll need to get the util team to support some sort
    of character encoding handling.
 */
final class AWRFC822Header
{
    public static Parameters parse (InputStream in, AWCharacterEncoding characterEncoding) throws IOException
    {
            // for performance
        byte charBuf[] = new byte[256];
            // Prime the pump
        Parameters headers = new Parameters();
        String lookahead = readLine(in, charBuf, characterEncoding);
        if (lookahead.equals("")) {
            return headers;
        }

            // Outer loop once per header
    outer:
        while (true) {
            FastStringBuffer header = new FastStringBuffer(lookahead);
                // Inner loop once per line
            while (true) {
                lookahead = readLine(in, charBuf, characterEncoding);

                    // A blank line indicates the end of the headers
                if (StringUtil.nullOrEmptyOrBlankString(lookahead)) {
                    add(headers, header.toString());
                    break outer;
                }
                if ((lookahead.charAt(0) == ' ') ||
                    (lookahead.charAt(0) == '\t')) {
                        // the header continues to the next line
                    for (int i = 1; i < lookahead.length(); i++) {
                        if (!((lookahead.charAt(i) == ' ') ||
                              (lookahead.charAt(i) == '\t'))) {
                            header.append(" ");
                            header.append(lookahead.substring(i));
                            break;
                        }
                    }
                }
                else {
                        // We found the end of the header
                        // lookahead has the start of the next
                    add(headers, header.toString());
                    continue outer;
                }
            }
        }
        return headers;
    }

    private static void add (Parameters headers, String header)
    {
        int i = header.indexOf(':');
        String name  = header.substring(0, i).toUpperCase();
        String value = header.substring(i+2);
        headers.putParameter(name, value);
    }


    /**
        This code taken directly from IOUtil.java and modified to accept a characterEncoding.
        We'll need to get the util folks to support some sort of character encoding argument.
     */

    //////////
    // IOUtil
    //////////
    public static String readLine (InputStream in, AWCharacterEncoding characterEncoding) throws IOException
    {
        return readLine(in, new byte[128], characterEncoding);
    }

    public static String readLine (InputStream in,
                                   byte[]      lineBuffer,
                                   AWCharacterEncoding characterEncoding)
      throws IOException
    {
        byte buf[] = lineBuffer;

        int room = buf.length;
        int offset = 0;
        int c;

    loop:
        while (true) {
            switch (c = in.read()) {
              case -1:
              case '\n': {
                  break loop;
              }

              case '\r': {
                  int c2 = in.read();
                  if (c2 != '\n') {
                      throw new IOException(
                          "Expected newline after carriage return");
                  }
                  break loop;
              }

              default: {
                  if (--room < 0) {
                      buf = new byte[offset + 128];
                      room = buf.length - offset - 1;
                      System.arraycopy(lineBuffer, 0, buf, 0, offset);
                      lineBuffer = buf;
                  }
                  buf[offset++] = (byte)c;
                  break;
              }
            }
        }
        if ((c == -1) && (offset == 0)) {
            return null;
        }
        byte[] bytes = new byte[offset];
        System.arraycopy(buf, 0, bytes, 0, offset);
        return characterEncoding.newString(bytes);
    }
}
