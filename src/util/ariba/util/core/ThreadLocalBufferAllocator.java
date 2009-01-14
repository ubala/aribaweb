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

    $Id: //ariba/platform/util/core/ariba/util/core/ThreadLocalBufferAllocator.java#4 $
*/

package ariba.util.core;

/**
    package private
*/
final class ThreadLocalBufferAllocator implements BufferAllocator
{
    private final ThreadLocal bufferStorage = new ThreadLocal();
    public FormatBuffer getBuffer ()
    {
        FormatBuffer fmb = (FormatBuffer)bufferStorage.get();
            // support multiple calls to get prior to a call to
            // release.
        bufferStorage.set(null);
        return (fmb == null) ? new FormatBuffer() : fmb;
    }

    public void freeBuffer (FormatBuffer buf)
    {
        buf.truncateToLength(0);
        if (buf.getBuffer().length > BufSizeLimit) {
            buf.setCapacity(BufSize);
        }
        bufferStorage.set(buf);
    }
}
