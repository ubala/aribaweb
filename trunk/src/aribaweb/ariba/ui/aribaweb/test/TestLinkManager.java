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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/TestLinkManager.java#1 $
*/
package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.ui.aribaweb.util.AWJarWalker;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestLinkManager
{
    static final Class _AnnotationClasses[] = {TestLink.class};

    static final Class _allTestAnnotationClasses[] = {TestLink.class, TestLinkParam.class};
    
    static TestLinkManager _instance = new TestLinkManager();
    Set<String> _classesWithAnnotations = new HashSet();

    Map <String, Map<Class, Object>> _annotationMapByClassName = new HashMap();

    public static void initialize ()
    {
        if (_instance == null) {
            _instance = new TestLinkManager();
            AWConcreteServerApplication.sharedInstance().resourceManager().registerPackageName("ariba.ui.aribaweb.test", true);
        }
    }

    public TestLinkManager()
    {
        for (Class annotationClass : _AnnotationClasses) {
            AWJarWalker.registerAnnotationListener(annotationClass, new AWJarWalker.AnnotationListener() {
                public void annotationDiscovered(String className, String annotationType)
                {
                    _classesWithAnnotations.add(className);
                }
            });
        }
    }

    public static final TestLinkManager instance ()
    {
        return _instance;
    }
    
    public Map<Class, Object> annotationsForClass (String className)
    {
        // lazily get annotations for known annotation bearing class
        Map<Class, Object> annotationMap = _annotationMapByClassName.get(className);
        if (annotationMap == null) {
            annotationMap = AWJarWalker.annotationsForClassName(className, _allTestAnnotationClasses);
            _annotationMapByClassName.put(className,  annotationMap);
        }
        return annotationMap;
    }

    private List _allTestLinks = null;
    private List<Category> _categoryList = null;
    
    synchronized void initializeTestLinks ()
    {

        if (_allTestLinks == null) {
            buildAllLinks();
            Map<String, List> firstCategoryMap;
            firstCategoryMap = AWUtil.groupBy(_allTestLinks,
                    new AWUtil.ValueMapper() {
                        public Object valueForObject (Object o) {
                            return ((TestLinkHolder)o).getFirstLevelCategoryName();
                        }
                    });
            Collection<String> firstLevelCategoryNames;
            firstLevelCategoryNames = firstCategoryMap.keySet();
            _categoryList = ListUtil.list();            
            for (String key : firstLevelCategoryNames) {
                Map<String, List> secondCategoryMap;
                secondCategoryMap = AWUtil.groupBy(firstCategoryMap.get(key),
                        new AWUtil.ValueMapper() {
                            public Object valueForObject (Object o) {
                                return ((TestLinkHolder)o).getSecondLevelCategoryName();
                            }
                        });
                Category category = new Category(key);
                _categoryList.add(category);
                Set<String> testUnitKeys = secondCategoryMap.keySet();
                for (String testUnitKey : testUnitKeys ) {
                    TestUnit testUnit = new TestUnit(testUnitKey, secondCategoryMap.get(testUnitKey));
                    category.add(testUnit);
                }
            }
        }
    }

    public List<Category> getTestCategoryList ()
    {
        if (_categoryList == null) {
            initializeTestLinks();
        }
        return _categoryList;
    }

    private void buildAllLinks ()
    {
        _allTestLinks = ListUtil.list();
        for (String className  :_classesWithAnnotations) {
            Map<Class, Object> annotations = annotationsForClass(className);
            Set keys = annotations.keySet();
            for (Object annotation : keys) {
                Object annotationRef = annotations.get(annotation);
                if (shouldExpandTestLink((Annotation)annotation)) {
                    List<TestLinkHolder> testLinks = expandTestLink((Annotation)annotation, annotationRef);
                    _allTestLinks.addAll(testLinks);
                }
                else {
                    _allTestLinks.add(new TestLinkHolder((Annotation)annotation, annotationRef));
                }
            }
        }
    }


    private boolean shouldExpandTestLink (Annotation annotation)
    {
        String typeList = AnnotationUtil.getAnnotationTypeList(annotation) ;
        String superType = AnnotationUtil.getAnnotationSuperType(annotation);
        return (!StringUtil.nullOrEmptyOrBlankString(typeList) ||
                !StringUtil.nullOrEmptyOrBlankString(superType))
                ? true : false;
    }

    private List expandTestLink (Annotation annotation, Object ref)
    {
        List testLinks = ListUtil.list();
        String typeList = AnnotationUtil.getAnnotationTypeList(annotation) ;
        if (!StringUtil.nullOrEmptyOrBlankString(typeList)) {
            String[] types = typeList.split(";");
            for (String type : types) {
                testLinks.add(new TestLinkHolder(annotation, ref, type));
            }
        }
        return testLinks;
    }
}
