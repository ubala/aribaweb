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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/Util.java#11 $
*/

package ariba.ui.widgets;

import java.util.List;
import ariba.util.core.StringUtil;
import ariba.util.core.Fmt;
import ariba.ui.aribaweb.core.AWRequest;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWCharacterEncoding;

import java.lang.reflect.Array;

public final class Util extends Object
{
    private static final String IsIE4Key = "AW_IsIe4";

    public static Object getElements (List vector, Class desiredClass)
    {
        Object[] elements = null;
        int vectorSize = vector.size();
        try {
            elements = (Object[])Array.newInstance(desiredClass, vectorSize);
        }
        catch (NegativeArraySizeException negativeArraySizeException) {
            //swallow -- cannot happen
            elements = null;
        }
        for (int index = 0; index < vectorSize; index++) {
            elements[index] = vector.get(index);
        }
        return elements;
    }

    public static Object removeElementAt (Object array, int index)
    {
        Object[] sourceArray = (Object[])array;
        Class componentType = sourceArray.getClass().getComponentType();
        int destArrayLength = sourceArray.length - 1;
        Object[] newArray = (Object[])Array.newInstance(componentType, destArrayLength);
        System.arraycopy(sourceArray, 0, newArray, 0, index);
        System.arraycopy(sourceArray, index + 1, newArray, index, destArrayLength - index);
        return newArray;
    }

    public static Object addElement (Object array, Object newElement)
    {
        Object[] sourceArray = (Object[])array;
        Class componentType = sourceArray.getClass().getComponentType();
        int sourceArrayLength = sourceArray.length;
        Object[] newArray = (Object[])Array.newInstance(componentType, sourceArrayLength + 1);
        System.arraycopy(sourceArray, 0, newArray, 0, sourceArrayLength);
        newArray[sourceArrayLength] = newElement;
        return newArray;
    }

    public static Object subarray (Object array , int startIndex)
    {
        Object[] sourceArray = (Object[])array;
        Class componentType = sourceArray.getClass().getComponentType();
        int destinationArrayLength = sourceArray.length - startIndex;
        Object destinationArray = Array.newInstance(componentType, destinationArrayLength);
        System.arraycopy(sourceArray, startIndex, destinationArray, 0, destinationArrayLength);
        return destinationArray;
    }

    public static int min (int int1, int int2)
    {
        return int1 < int2 ? int1 : int2;
    }

    public static boolean isIE4 (AWRequest request)
    {
        String userAgent = request.userAgent();
        return userAgent.indexOf("; MSIE 4.") != -1;
    }

    public static Boolean isIE4 (AWSession session)
    {
        Boolean isIE4 = (Boolean)session.httpSession().getAttribute(IsIE4Key);
        if (isIE4 == null) {
            AWRequest request = session.request();
            isIE4 = isIE4(request) ? Boolean.TRUE : Boolean.FALSE;
            session.httpSession().setAttribute(IsIE4Key, isIE4);
        }
        return isIE4;
    }

    public static Boolean isIE4 (AWRequestContext requestContext)
    {
        Boolean isIE4 = (Boolean)requestContext.get(IsIE4Key);
        if (isIE4 == null) {
            AWSession session = requestContext.session();
            isIE4 = isIE4(session);
            requestContext.put(IsIE4Key, isIE4);
        }
        return isIE4;
    }

    /**
         @deprecated Use setHeadersForDownload with requestContext instead
     */
    public static void setHeadersForDownload (AWResponse response, String fileName)
    {
        if (!StringUtil.nullOrEmptyOrBlankString(fileName)) {
            response.setHeaderForKey(
                "attachment; filename=\"" + fileName + "\"", "Content-Disposition");
        }
        response.setHeaderForKey("bytes", "Accept-Ranges");
        response.setHeaderForKey("\"c0109d99ac9cc11:1161\"", "ETag");
        response.setHeaderForKey("Mon, 14 Jan 2002 03:36:06 GMT", "Last-Modified");
        response.setBrowserCachingEnabled(true);
    }

    /**
        Sets the Content-Disposition header in a browser dependent way.
        Encodes filename in UTF-8.
    */
    public static void setHeadersForDownload (AWRequestContext requestContext, AWResponse response, String fileName)
    {
        if (!StringUtil.nullOrEmptyOrBlankString(fileName)) {
            fileName = AWUtil.encodeString(fileName, false, AWCharacterEncoding.UTF8.name);
            if (requestContext.isBrowserMicrosoft()) {
                response.setHeaderForKey(Fmt.S("attachment; filename=%s",
                                         fileName), "Content-Disposition");
            }
            else {
                // http://www.ietf.org/rfc/rfc2231.txt
                response.setHeaderForKey(Fmt.S("attachment; filename*=%s''%s;",
                                         AWCharacterEncoding.UTF8.name,
                                         fileName), "Content-Disposition");

            }
            response.setHeaderForKey("bytes", "Accept-Ranges");
            response.setHeaderForKey("\"c0109d99ac9cc11:1161\"", "ETag");
            response.setHeaderForKey("Mon, 14 Jan 2002 03:36:06 GMT", "Last-Modified");
            response.setBrowserCachingEnabled(true);
        }
    }

}
