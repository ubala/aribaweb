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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWResponseBuffer.java#25 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.AWCharacterEncoding;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWPagedVector;
import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * High-level notes on the algorithm:
 *
 * A responseBuffer defines a region of the responseContents (ie the encodedStrings appended to the response during
 * renderResponse).  ResponseBuffers can be nested.  There are two types: scoped and regular.  Let's first
 * describe how a regular buffer works then talk about the scoped.
 *
 * RegularBuffers:  The entire page is in a regular buffer.  Within that page there can be regions (div/spans)
 * that can change when cycling the page.  If we make each of these into a sub-buffer, we'll have nested regular
 * buffers.  To detect the minimum amount of text that needs to be written to the response, we need to find the
 * smallest buffers that changed.  If a buffer is determined to be changed, all its content must be re-written.
 *
 * We talk in terms of the "top-level" content of a buffer.  This is all the encodedStrins in the buffer but not the
 * contents of the subbuffers, which are treated independently.  However, we do concern ourselves with the names of
 * the subbuffers because if these change, we will have had a deletion or insertion and the only way to handle that
 * in general is to rewrite the entire buffer.
 *
 * We employ checksums to substitute for actual string comparisons.  The checksum for a regularBuffer is computed
 * by including all the encodedStrings at the top-level of a given regular buffer, plus the names of the subbuffers.
 * If any top-level string of subbuffer name changed, we detect a change and must write the entire buffer.
 *
 * If we do not detect a change in the top-level content of the buffer (ie the checksums are the same), we still need
 * to recurse down to the next level to see if any of the contents of the subbuffers changed and needs to be written
 * out.  We simply iterate through the children of the current buffer and repeat the aforementioned process.
 *
 * However, there is a second type of buffer: scoped buffers.  An example of a scoped buffer is a <table></table>.
 * In a table, we can detect and react to insertions, deletions, and modifications of rows, but this requires a
 * different algorithm than regular buffers.
 *
 * First, to know if we need to write the entire scoped buffer, we compare its top-level content to its predecessor.
 * In this case, we only care about the encodedStrings at the top level and not the names of the children buffers.
 * Changes in child buffers names are an indication of insertions or deletions and we have a way to deal with that.
 * In any case, if we detect a change in the top level content of a scoped buffer, we simply write out the entire
 * buffer.
 *
 * However, if we detect a change in a child buffer (either insertion, deletion, or modification), we must still
 * write out the top-level contents of the scoped buffer as this is generally required to be legitimate html (that is,
 * a <tr/> cannot exist in the absence of a <table/>).  So to write the children of a scoped buffer, we iterate through
 * the encodedStrings at the top level which are intermingled with the subbuffers.  We write all encodedStrings,
 * but must determine what to do with each subbuffer.  If the subbuffer is a simple modification at the top level, its
 * checksum will differ from its predecessor and we simply write the entire child buffer.
 *
 * If the child buffer doesn't exist in the previous response, then we have an insertion and this means we must
 * write out the entire buffer and generate some javascript to tell the client side code that this entry is an
 * insertion.  For insertions, we must indicate which row it comes after so the client side code can insert the
 * row in the proper place.
 *
 * Once we've iterated through all children in the scoped buffer and written all the top-level content of the
 * scoped buffer, we must iterate through its predecessor to determine if any rows were present in the predecessor
 * but not in the current scoped buffer -- this would be a deletion.  All deletions can be written after the scoped
 * buffer has been written.
 *
 * Finally, we must write out any sub-subbuffers nested within a child of a scoped buffer which wasn't written in
 * the previous passes.  That is, if a given child had no changes and didn't require writing within the scoped buffer,
 * we need to propagate the writeTo function to each of the nested buffers within those children.
 *
 * Notes on the implementation of AWResponseBuffer:
 *
 * To minimize garbage generation, we chose to use a single "contents" buffer and have the AWResponseBuffers point
 * into this PagedVector.  So the BaseResponse allocates a PagedVector which is shared by all responseBuffers and
 * the responseBuffers simply maintin indexes to their start and ending positions.  Each time a new subbuffer
 * is required by the AWRefreshRegion component, a new one is created and initialized with the index of the globalContents
 * buffer.  The baseResponse makes this buffer the current target buffer and all "append" is done to this buffer.
 * Of course, the target buffer puts the appended content in the shared globalCotents buffer, but it keep track
 * of what's going on by updating its checksum as content is appended.
 *
 * When a buffer is appended it updates its _children list by adding the buffer to the end of the list.  Each
 * responseBuffer has a _children pointer which points to the head of the children, and a _next pointer which
 * points to the next child in the current list.  We also maintain a _tail which allows for rapid append to the
 * end of the list without requiring iteration to the end to simply append a new child.
 *
 * If a buffer is a scoped buffer, appending a child does something a bit different than a regular buffer.
 * In a regular buffer, we don't really care to know where the begin/end of a given child buffer is because we
 * are either going to write the entire buffer or merely its children.  With the proper use of checksums,
 * we can determine which to do quite easily and quickly (se discussion above).
 *
 * However, with a scoped buffer, we are required to write the top-level content of the scoped buffer and optionally
 * write some of the children.  This means we must iterate through the top-level encodedStrings writing them out
 * but, at the appropriate point, conditionally write out a child buffer.  Hence, we must put the buffers themselves
 * in the globalContents to keep track of when to compare/write them.  Once we've determined that some child
 * requires writing, we must render the wrapper of the scoped buffer (eg the <table>...</table> tags) and all
 * the interstitial content between the rows of the table (ie the children).  As we go, we encounter child buffers
 * and optionally write them out (by comparing their checksums to their predecesor).
 *
 * So, when we append a child buffer to a scoped buffer, we add this buffer to the content so it can be invovled
 * in the rendering of the wrapper.  However, we do not include its name in the scoped buffer's checksum as with
 * regular buffers because we will deal with each child buffer in situ as either a modification or insertion.
 *
 * To make the determination of whether or not a child buffer was an insertion or deletion, we keep a HashMap
 * of all scoped child buffers in both the current and previous responseBuffers.  Thus, we can easily compare
 * the current children with the previous and determine if it existed before or not.  Of course, we can do the same
 * from the other direction to determine if there were deletions, and this is done after the scoped buffer is written
 * in its entirety.
 *
 * Finally, we must make a pass throught he children of the scoped buffer and, for those children which were
 * not written out in the previous pass, give them an opportunity to write out any of their subbuffers which may have
 * changed.  Again, this is done outside the rendering of the scoped buffer to result in legitimate html.
 *
 * Other notes:  This whole operation is an interactive dance with the BaseResponse.  BaseResponse maintains a stack
 * of buffers and the current buffer so it knows where to direct the next append operation.  As it pops buffers
 * off its stack, it sends a close() message to that buffer so that buffer can cleanup (ie release its CRC32) and,
 * most importantly, take note of the size of the globalContents buffer so it knows where its end is.
 *
 * Pooling: To avoid too much garbage generation, we use a recycle pool for the CRC32 objects and for
 * the AWPagedVectorIterator.  In both cases, the number of objects required at any one time is a function of the
 * depth of the stack of responseBuffers, so the pool neededn't be too large.  However, they come and go quite
 * frequently, so its make sense to pool these.  Also, they clean up nicely, so its easy to pool them.
 *
 * I do not pool AWResponseBuffers since they are quite numerous and have a fairly long life.
 *
 * Also, once a BaseResponse has been written to the client, we jettison the globalContent to free up that memory.
 */
public final class AWResponseBuffer extends AWBaseObject
{
    private final AWEncodedString _name;
    private final boolean _alwaysRender;
    private final Type _type;
    private final AWBaseResponse _baseResponse;
    private final HashMap _globalScopeChildren;
    private int _contentsStartIndex;
    private int _contentsEndIndex;
    private boolean _ignoreWhitespaceDiffs;

    private AWPagedVector _globalContents;

    public enum Type { Normal, Scope, ScopeChild };

    // AWChecksum is a java implementation of the CRC32 checksum, which does not use
    // the java.util.zip.Checksum interface and thus does not require creating instances
    // of the Checksum object (and thus additional overhead of GC / pooling / etc.).
    private long _awchecksumValue;
    private int _byteCount = 0;

    // The children are managed as a linked list.
    private AWResponseBuffer _children;
    private AWResponseBuffer _next;
    private AWResponseBuffer _tail;

    protected static class WriteContext {
        OutputStream _outputStream;
        AWCharacterEncoding _characterEncoding;
        int _nestingLevel;
        boolean _didWrite;
        boolean _writeRefreshRegionBoundaryMarkers;
        int _bytesWritten = 0;

        public WriteContext (OutputStream outputStream, AWCharacterEncoding characterEncoding,
                             boolean writeRefreshRegionBoundaryMarkers)
        {
            _outputStream = outputStream;
            _characterEncoding = characterEncoding;
            _writeRefreshRegionBoundaryMarkers = writeRefreshRegionBoundaryMarkers;
        }

        public WriteContext (OutputStream outputStream, AWCharacterEncoding characterEncoding)
        {
            this(outputStream, characterEncoding, false);
        }

        public void write (byte[] bytes)
        {
            try {
                _outputStream.write(bytes);
                _bytesWritten += bytes.length;
            } catch (IOException e) {
                throw new AWGenericException(e);
            }
        }

        public void write (AWEncodedString encodedString)
        {
            write(encodedString.bytes(_characterEncoding));
            _didWrite = true;
        }

        private static final AWEncodedString TopLevelMarker = new AWEncodedString("<!--@&@-->");

        private int _nestLevel = 0;
        void pushLevel ()
        {
            // we don't write makers for the first entry (or the last)
            if (_nestLevel++ == 0 && _didWrite && _writeRefreshRegionBoundaryMarkers) {
                write(TopLevelMarker);
            }
        }

        void popLevel ()
        {
            Assert.that(_nestLevel > 0, "Unbalanced nesting level");
            _nestLevel--;
        }


    }

    protected AWResponseBuffer (AWEncodedString name, Type type, boolean alwaysRender, AWBaseResponse baseResponse)
    {
        _baseResponse = baseResponse;
        _globalContents = baseResponse.globalContents();
        _name = name;
        Assert.that(_name != null, "name may not be null.");
        _type = type;
        _ignoreWhitespaceDiffs = type == Type.Scope;
        _globalScopeChildren = type == Type.Scope ? _baseResponse.scopeChildren() : null;
        _alwaysRender = alwaysRender;
        _contentsStartIndex = _globalContents.size();
        _contentsEndIndex = _contentsStartIndex;
    }

    private AWResponseBuffer (AWEncodedString name)
    {
        _name = name;
        _alwaysRender = false;
        _type = Type.Normal;
        _baseResponse = null;
        _globalScopeChildren = null;
    }

    protected void close ()
    {
        _contentsEndIndex = _globalContents.size();
    }

    public void updateParentChecksum (AWResponseBuffer parent)
    {
        // used to attribute our checksum to the rootBuffer to force FPR for globalScope
        // noRefresh buffers.
        // We encode both our data (checksum, length) but not our name so that a change
        // in our contents will trigger an FPR, but not a change in our position (elementId)
        parent._byteCount += _byteCount;
        parent._awchecksumValue = AWChecksum.crc32(parent._awchecksumValue, _awchecksumValue);
    }

    protected boolean isEqual (AWResponseBuffer otherBuffer)
    {
        return (_awchecksumValue == otherBuffer._awchecksumValue) &&
                (_byteCount == otherBuffer._byteCount);
    }

    private void updateChecksum (AWEncodedString encodedString)
    {
        byte[] bytes = encodedString.bytes(AWCharacterEncoding.UTF8);
        int bytesLength = bytes.length;
        _awchecksumValue = AWChecksum.crc32(_awchecksumValue, bytes, bytesLength);
        _byteCount += bytesLength;
    }

    public void setIgnoreWhitespaceDiffs (boolean yn)
    {
        _ignoreWhitespaceDiffs = yn;
    }

    //////////////////
    // Accessors
    //////////////////
    protected AWEncodedString getName ()
    {
        return _name;
    }

    protected boolean isScopeChild ()
    {
        return _type == Type.ScopeChild;
    }

    protected boolean isAlwaysRender ()
    {
        return _alwaysRender;
    }

    protected int getContentStartIndex ()
    {
        return _contentsStartIndex;
    }
    
    protected int getContentEndIndex ()
    {
        return _contentsEndIndex;
    }

    protected AWPagedVector getGlobalContents ()
    {
        return _globalContents;
    }

    protected AWResponseBuffer getFirstChild ()
    {
        return _children;
    }

    protected AWResponseBuffer getNext ()
    {
        return _next;
    }

    //////////////////
    // Child Handling
    //////////////////
    private AWResponseBuffer addNext (AWResponseBuffer responseBuffer)
    {
        if (_next == null) {
            _next = _tail = responseBuffer;
        }
        else {
            _tail = _tail.addNext(responseBuffer);
        }
        return _tail;
    }

    private void addChild (AWResponseBuffer responseBuffer)
    {
        if (_children == null) {
            _children = responseBuffer;
        }
        else {
            _children.addNext(responseBuffer);
        }
    }

    ////////////////////
    // External methods
    ////////////////////
    protected void append (AWEncodedString encodedString)
    {
        if (encodedString != null) {
            _globalContents.add(encodedString);
            if (!_ignoreWhitespaceDiffs || !StringUtil.nullOrEmptyOrBlankString(encodedString.string())) {
                // For scoped buffers, we do not include pure whitespace in its checksum
                updateChecksum(encodedString);

                // remember the size for perf measurement
                _baseResponse._fullSize += encodedString.bytes(AWCharacterEncoding.UTF8).length;
            }
        }
    }

    protected void append (AWResponseBuffer responseBuffer)
    {
        if (responseBuffer != null) {
            Assert.that(responseBuffer._type != Type.Scope || _type != Type.Scope, "Attempt to nest scoped RefreshRegion directly inside another");
            addChild(responseBuffer);
            // add the response buffer to the global contents and increment the
            // response buffer's _contentsStartIndex so it doesn't point to itself
            _globalContents.add(responseBuffer);
            responseBuffer._contentsStartIndex++;
            if (_type == Type.Scope) {
                // For scoped buffers, we do not include the children buffers in its checksum
                _globalScopeChildren.put(responseBuffer._name, responseBuffer);
                if (responseBuffer._type != Type.ScopeChild) updateChecksum(responseBuffer._name);
            }
            else {
                updateChecksum(responseBuffer._name);
            }
        }
    }

    protected boolean isScope ()
    {
        return _type == Type.Scope;
    }

    static class ScopeChanges {
        List inserts, updates, deletes;
        int total;
    }


    // Number of modifications that should trigger a full table replace instead of an piece-wise update
    protected static int ScopeChildCountUpdateAllThreshhold = 50;
    
    /**
     * This is the eternal entry point called by the AWBaseResponse instance.
     * @param otherBuffer
     */
    protected void writeTo (WriteContext context, AWResponseBuffer otherBuffer)
    {
        if (_alwaysRender || otherBuffer == null || !isEqual(otherBuffer)) {
            renderAll(context);
            if (_type == Type.Scope) {
                writeScopeUpdate(context);
            }
        }
        else if (_type == Type.Scope) {
            // We either need the wrapper or we do not.  We do not need the wrapper if all children checksums match,
            // which means there were no insertions, deletions, or modifications to the top-level content of any row.
            // Note: it may be more efficient to simply extract the inserted/deleted/modified/unmodified lists rather
            // than doing this boolean check -- at least we'd get some work done during the iteration.
            ScopeChanges changes = checkScopeChanges(otherBuffer);
            if (changes != null) {
                if (changes.total > ScopeChildCountUpdateAllThreshhold) {
                    // write out the whole table
                    renderAll(context);
                    writeScopeUpdate(context);
                } else {
                    if (changes.inserts != null || changes.updates != null) {
                        // Now write out the table and its modified children, including insertions.  At the end, write out
                        // the deltedChildren as javascript calls.
                        writeScopedBuffer(context, otherBuffer);

                        // Now, outside the table, write any buffers which changed within rows that didn't change.
                        writeUnmodifiedChildren(context, otherBuffer);
                    } else {
                        // write changes within rows (if any)
                        writeNextSublevel(context, otherBuffer);
                    }
                    // JS to execute inserts and deletes
                    if (changes.inserts != null || changes.deletes != null) {
                        writeScopeChangeScript(context, changes.inserts, changes.deletes);
                    }
                }
            }
            else {
                writeNextSublevel(context, otherBuffer);
            }
        }
        else {
            writeNextLevel(context, otherBuffer);
        }
    }

    /**
     * When its determined that a given buffer should be rendered in its entirety, call this.
     */
    private void renderAll (WriteContext context)
    {
        context.pushLevel();
        AWPagedVector.AWPagedVectorIterator elements = _globalContents.elements(_contentsStartIndex, _contentsEndIndex);
        while (elements.hasNext()) {
            Object element = elements.next();
            if (element instanceof AWEncodedString) {
                context.write(((AWEncodedString)element));
            }
            else {
                AWResponseBuffer childBuffer = (AWResponseBuffer)element;
                childBuffer.renderAll(context);
                elements.skipTo(childBuffer._contentsEndIndex);
            }
        }
        elements.release();
        context.popLevel();
    }


    /**
     * This should ONLY be used by regular buffers.  It skips all the string content at the top level and
     * asks each child to determine if it requires writing.
     */
    private void writeNextLevel (WriteContext context, AWResponseBuffer otherBuffer)
    {
        Assert.that(_type != Type.Scope, "writeNextLevel(...) cannot be used by scoped buffers");
        AWResponseBuffer childBuffer = _children;
        AWResponseBuffer otherChildBuffer = otherBuffer._children;
        while (childBuffer != null) {
            childBuffer.writeTo(context, otherChildBuffer);
            childBuffer = childBuffer._next;
            otherChildBuffer = otherChildBuffer._next;
        }
    }

    /**
     * This is used to write the buffers contained within the children of a scoped buffer (ie a div within a tablecell)
     * This should only be called on scoped children when its determined that the wrapper scope doesn't require
     * writing (ie all children are same in terms of their checksums)
     */
    private void writeNextSublevel (WriteContext context, AWResponseBuffer otherBuffer)
    {
        Assert.that(_type == Type.Scope, "writeNextSublevel(...) can only be used by scoped buffers");
        AWResponseBuffer childBuffer = _children;
        HashMap otherScopeChildren = otherBuffer._globalScopeChildren;
        while (childBuffer != null) {
            AWEncodedString childBufferName = childBuffer._name;
            AWResponseBuffer otherChildBuffer = (AWResponseBuffer)otherScopeChildren.get(childBufferName);
            if (childBuffer._type == Type.ScopeChild) {
                childBuffer.writeNextLevel(context, otherChildBuffer);
            } else {
                childBuffer.writeTo(context, otherChildBuffer);
            }
            childBuffer = childBuffer._next;
        }
    }

    private static AWResponseBuffer _NullResponseRef = new AWResponseBuffer(AWConstants.Null);

    ScopeChanges checkScopeChanges (AWResponseBuffer otherBuffer)
    {
        ScopeChanges changes = null;
        int total = 0;
        List inserts = null, updates = null, deletes = null;

        // insertions and updates
        AWPagedVector.AWPagedVectorIterator elements =
                _globalContents.elements(_contentsStartIndex, _contentsEndIndex);
        HashMap otherScopeChildren = otherBuffer._globalScopeChildren;
        AWResponseBuffer previousChild = _NullResponseRef;
        while (elements.hasNext()) {
            Object element = elements.next();
            if (!(element instanceof AWEncodedString)) {
                AWResponseBuffer childBuffer = (AWResponseBuffer)element;
                if (childBuffer._type == Type.ScopeChild) {
                    AWEncodedString childBufferName = childBuffer._name;
                    AWResponseBuffer otherChildBuffer = (AWResponseBuffer)otherScopeChildren.get(childBufferName);
                    if (otherChildBuffer == null) {
                        if (inserts == null) inserts = ListUtil.list();
                        inserts.add(previousChild);
                        inserts.add(childBuffer);
                        total++;
                    } else if (!childBuffer.isEqual(otherChildBuffer) || childBuffer._alwaysRender) {
                        if (updates == null) updates = ListUtil.list();
                        updates.add(childBuffer);
                        total++;
                    }
                    elements.skipTo(childBuffer._contentsEndIndex);
                    previousChild = childBuffer;
                }
            }
        }
        elements.release();

        // deletions
        AWResponseBuffer otherChildBuffer = otherBuffer._children;
        while (otherChildBuffer != null) {
            if (otherChildBuffer._type == Type.ScopeChild) {
                AWEncodedString otherChildBufferName = otherChildBuffer._name;
                if (_globalScopeChildren.get(otherChildBufferName) == null) {
                    if (deletes == null) deletes = ListUtil.list();
                    deletes.add(otherChildBuffer);
                    total++;
                }
            }
            otherChildBuffer = otherChildBuffer._next;
        }

        if (total > 0) {
            changes = new ScopeChanges();
            changes.total = total;
            changes.inserts = inserts;
            changes.updates = updates;
            changes.deletes = deletes;
        }

        return changes;
    }

    /**
     * This is called when it has been determined that the scope wrapper is required due to at least on of the scoped
     * children being different at the top level.  Thus we write the wrapper itself and all changed children, including
     * insertions.
     */
    private void writeScopedBuffer (WriteContext context, AWResponseBuffer otherBuffer)
    {
        context.pushLevel();
        AWResponseBuffer previousChild = _NullResponseRef;
        AWPagedVector.AWPagedVectorIterator elements =
                _globalContents.elements(_contentsStartIndex, _contentsEndIndex);
        HashMap otherScopeChildren = otherBuffer._globalScopeChildren;
        while (elements.hasNext()) {
            Object element = elements.next();
            if (element instanceof AWEncodedString) {
                context.write(((AWEncodedString)element));
            }
            else {
                AWResponseBuffer childBuffer = (AWResponseBuffer)element;
                AWEncodedString childBufferName = childBuffer._name;
                AWResponseBuffer otherChildBuffer = (AWResponseBuffer)otherScopeChildren.get(childBufferName);
                if (otherChildBuffer == null || !childBuffer.isEqual(otherChildBuffer) || childBuffer._alwaysRender) {
                    childBuffer.renderAll(context);
                    if (otherChildBuffer == null) {
                        // This was an insertion -- output some javascript to denote that fact.
                        // AWEncodedString previousChildName = previousChild._name;
                        // writeInsertion(context, previousChildName, childBufferName);
                    }
                }
                elements.skipTo(childBuffer._contentsEndIndex);
                previousChild = childBuffer;
            }
        }
        elements.release();
        context.popLevel();
    }
    
    private static final AWEncodedString WriteScopeUpdate1 = new AWEncodedString("<script>ariba.Refresh.registerScopeUpdate('");
    private static final AWEncodedString Separator = new AWEncodedString("','");
    private static final AWEncodedString EndScript = new AWEncodedString("');</script>");
    private static final AWEncodedString WriteChanges = new AWEncodedString("<script>ariba.Refresh.registerScopeChanges('");
    private static final AWEncodedString ChangeNoInsSeparator = new AWEncodedString("',null,");
    private static final AWEncodedString ChangeInsStartSeparator = new AWEncodedString("',['");
    private static final AWEncodedString ChangeInsEndSeparator = new AWEncodedString("'],");
    private static final AWEncodedString ChangeDelStartSeparator = new AWEncodedString("['");
    private static final AWEncodedString ChangeDelEndSeparator = new AWEncodedString("']);</script>");
    private static final AWEncodedString ChangeNoDelSeparator = new AWEncodedString("null);</script>");

    private void writeScopeUpdate (WriteContext context)
    {
        context.write(WriteScopeUpdate1);
        context.write(_name);
        context.write(EndScript);
    }

    private void writeScopeChangeScript (WriteContext context,
            List <AWResponseBuffer>insertions, List <AWResponseBuffer>deletions)
    {
        // registerScopeChanges('tableId', [ insertions ], [ deletions ]);
        context.write(WriteChanges);
        context.write(_name);

        // insertions
        if (insertions != null) {
            context.write(ChangeInsStartSeparator);
            for (int i=0, c=insertions.size(); i < c; i+=2) {
                context.write(insertions.get(i)._name);
                context.write(Separator);
                context.write(insertions.get(i+1)._name);
                if (i + 2 < c) context.write(Separator);
            }
            context.write(ChangeInsEndSeparator);
        } else {
            context.write(ChangeNoInsSeparator);
        }
        // deletions
        if (deletions != null) {
            context.write(ChangeDelStartSeparator);
            for (int i=0, c=deletions.size(); i < c; i++) {
                context.write(deletions.get(i)._name);
                if (i + 1 < c) context.write(Separator);
            }
            context.write(ChangeDelEndSeparator);
        } else {
            context.write(ChangeNoDelSeparator);
        }
    }

    /**
     * This is used to write the changed contents of unmodified rows in a table.  Many of these will not end up writing
     * anything, but if there's a domsync block within a row that is changed, this will write that out.  This is called
     * after the table is finished rendering so that these changes do not end up within the <table></table> tags.
     */
    private void  writeUnmodifiedChildren (WriteContext context, AWResponseBuffer otherBuffer)
    {
        HashMap otherScopeChildren = otherBuffer._globalScopeChildren;
        AWResponseBuffer childBuffer = _children;
        while (childBuffer != null && !childBuffer._alwaysRender) {
            AWResponseBuffer otherChildBuffer = (AWResponseBuffer)otherScopeChildren.get(childBuffer._name);
            if (childBuffer._type == Type.ScopeChild) {
                if (otherChildBuffer != null && childBuffer.isEqual(otherChildBuffer)) {
                    childBuffer.writeNextLevel(context, otherChildBuffer);
                }
            } else {
                childBuffer.writeTo(context, otherChildBuffer);
            }
            childBuffer = childBuffer._next;
        }
    }

    protected void debug_writeTopLevelOnly (WriteContext context)
    {
        int totalLength = _globalContents.size();
        int index = 0;
        AWResponseBuffer child = _children;
        while (index < totalLength) {
            int endIndex = child == null ? totalLength : child._contentsStartIndex - 1;
            Iterator iterator = _globalContents.elements(index, endIndex);
            while (iterator.hasNext()) {
                AWEncodedString string = (AWEncodedString)iterator.next();
                context.write(string);
            }
            if (child != null) {
                index = child._contentsEndIndex;
                child = child._next;
            }
            else {
                index = totalLength;
            }
            index += 1;
        }
    }
}
