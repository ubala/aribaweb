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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWBookmarker.java#6 $
*/

package ariba.ui.aribaweb.util;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.AnnotatedElement;
import java.lang.annotation.Annotation;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWDirectActionUrl;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWSessionValidationException;
import ariba.ui.aribaweb.util.AWEvaluateTemplateFile.FieldValue;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.fieldvalue.FieldValueAccessorUtil;

public class AWBookmarker
{
    private static String PageName = "page";
    private static Class<?>[] BookmarkType = {AWBookmarkState.class};

    protected BookmarkEncrypter _encrypter = new DefaultEncrypterImpl();

    public boolean isBookmarkable (AWRequestContext requestContext)
    {
        if (requestContext.page() != null) {
            AWComponent comp = requestContext.pageComponent();
            if (comp instanceof AWPublicBookmarkable) {
                return true;
            }
            else if (comp instanceof AWBookmarkURLProvider) {
                return true;
            }
        }

        return false;
    }

    public String getURLString (AWRequestContext requestContext)
    {
        AWComponent comp = requestContext.pageComponent();

        if (comp instanceof AWBookmarkURLProvider) {
            return ((AWBookmarkURLProvider)comp).bookmarkURL();
        }
        Map<String, Object> properties = null;
        List<String> aprop = 
            ListUtil.collectionToList(getAnnotedProperties(comp.getClass()).keySet());
        properties = MapUtil.map();

        for (String prop : aprop) {
            properties.put(prop, FieldValue.getFieldValue(comp, prop));
        }

        StringBuffer queryBuff = new StringBuffer();
        queryBuff.append(Fmt.S("%s=%s", PageName, comp.componentReference().tagName()));

        for (String key : properties.keySet()) {
            Object val = properties.get(key);
            String sval = AWBookmarkFormatter.format(val);
            sval = AWUtil.encodeString(sval);
            queryBuff.append(Fmt.S("&%s=%s", key, sval));
        }
        String urlString = 
            AWDirectActionUrl.fullUrlForDirectAction("restore", requestContext);
        urlString = _encrypter.getEncryptedUrl(urlString, queryBuff.toString());
        return urlString;
    }

    public Map<String, Class<?>> getAnnotedProperties(Class<?> aclass){
        Map<String,Class<?>> properties = MapUtil.map();
        Class<?> currClass = aclass;

        while (currClass != null && !currClass.getName().equals(AWComponent.class.getName())) {
            Map<Annotation, AnnotatedElement> annotations = AWJarWalker.annotationsForClassName(
                currClass.getName(), BookmarkType);

            for (AnnotatedElement element : annotations.values()) {
                String fname = FieldValueAccessorUtil.normalizedFieldPathForMember((Member)element);
                Class<?> type = null;

                if (element instanceof Method) {
                    Method mtd =(Method)element;
                    type = mtd.getReturnType();
                }
                else if (element instanceof Field) {
                    Field fld = (Field) element;
                    type = ((Field)element).getType();
                }

                Assert.that(fname != null && type != null, 
                    "Unable to access (possibly private): " + element.toString());
                properties.put(fname, type);
            }
            currClass = currClass.getSuperclass();
        }
        return properties;
    }


    public AWComponent getComponent(AWRequestContext requestContext) {
        String page = requestContext.request().formValueForKey(PageName);
        if (page == null) return null; 
        AWComponent compRet = requestContext.pageWithName(page);
        if (compRet instanceof AWProtectedBookmarkable) {
            ((AWProtectedBookmarkable)compRet).check();
        }
        Map<String,Class<?>> aprop = getAnnotedProperties(compRet.getClass());
        try {
            for (String prop : aprop.keySet()) {
                Object val = AWBookmarkFormatter.parse(
                    requestContext.request().formValueForKey(prop), aprop.get(prop));
                FieldValue.setFieldValue(compRet, prop, val);
            }
            ((AWPublicBookmarkable)compRet).bookmarkPostInit();
            if (compRet instanceof AWProtectedBookmarkable) {
                if (!((AWProtectedBookmarkable)compRet).check()) {
                    throw new AWSessionValidationException();
                }
            }
        }
        catch (ParseException e) {
            requestContext.application().handleException(requestContext, e);
        }
        return compRet;
    }
    
    /**
     * Returns the current encrypter i.e. the implementation of BookmarkEncrypter
     * 
     * @return BookmarkEncrypter 
     */
    public BookmarkEncrypter getEncrypter ()
    {
        return _encrypter;
    }

    /**
     * Set the encrypter to be used for encrypting and decrypting urls. 
     * 
     * @param encryptor concrete implementation of BookmarkEncrypter.
     */
    public void setEncrypter (BookmarkEncrypter encryptor)
    {
        this._encrypter = encryptor;
    }

    /**
     * This interface is intended to be implemented by the Application.
     * It should provide a way to encrypt the query string of the urls to prevent hacking.
     * 
     */
    public static interface BookmarkEncrypter
    {
        public boolean isEncrypted(AWRequestContext requestContext);
        public String getEncryptedUrl (String url, String queryToAppend);
        public String getDecryptedUrl (AWRequestContext requestContext);
    }

    class DefaultEncrypterImpl implements BookmarkEncrypter
    {
        public boolean isEncrypted (AWRequestContext requestContext)
        {
            return false;
        }

        public String getEncryptedUrl (String url, String query)
        {
            return Fmt.S("%s&%s", url, query);
        }

        public String getDecryptedUrl (AWRequestContext requestContext)
        {
            return null;
        }
    }
}
