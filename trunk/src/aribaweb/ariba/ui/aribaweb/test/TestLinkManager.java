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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/TestLinkManager.java#2 $
*/
package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.util.AWJarWalker;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import ariba.util.test.TestLink;
import ariba.util.test.TestStager;
import ariba.util.test.TestInspector;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestLinkManager
{
    static final Class _AnnotationClasses[] = {TestLink.class, ariba.util.test.TestStager.class};
    static final Class _AnnotationValidatorClasses[] = {TestInspector.class};

    static final Class _allTestAnnotationClasses[] =
            {TestLink.class, TestStager.class, TestParam.class,
             TestInspector.class};
    
    static TestLinkManager _instance = new TestLinkManager();
    Set<String> _classesWithAnnotations = new HashSet();
    Set<String> _classesWithValidatorAnnotations = new HashSet();

    Map <String, Map<Class, Object>> _annotationMapByClassName = new HashMap();
    Map <Class, List<TestInspectorLink>> _testInspectorMap = new HashMap();
    
    private TestSessionSetup _testSessionSetup = null;

    public static void forceClassLoad()
    {
        // method gets called to force the class to load and
        // invoke its static initializers.
    }

    public static void initialize (TestSessionSetup testSessionSetup)
    {
        _instance._testSessionSetup = testSessionSetup;
    }

    public void registerTestSessionSetup (TestSessionSetup testSessionSetup)
    {
        _testSessionSetup = testSessionSetup;
    }

    public TestSessionSetup getTestSessionSetup()
    {
        return _testSessionSetup;
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
        for (Class annotationClass : _AnnotationValidatorClasses) {
            AWJarWalker.registerAnnotationListener(annotationClass, new AWJarWalker.AnnotationListener() {
                public void annotationDiscovered(String className, String annotationType)
                {
                    _classesWithValidatorAnnotations.add(className);
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

    public boolean hasObjectInspectors (Object object)
    {
        List list = _testInspectorMap.get(object);
        return list != null && list.size() > 0;
    }

    public List<TestInspectorLink> getObjectInspectors (Object object)
    {
        return _testInspectorMap.get(object);
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
            for (Object key : keys) {
                Annotation annotation = (Annotation)key;
                if (annotation.annotationType().isAssignableFrom(TestLink.class) ||
                        annotation.annotationType().isAssignableFrom(ariba.util.test.TestStager.class)) {
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
        for (String className  :_classesWithValidatorAnnotations) {
            Map<Class, Object> annotations = annotationsForClass(className);
            Set keys = annotations.keySet();
            for (Object key : keys) {
                Annotation annotation = (Annotation)key;
                if (annotation.annotationType().isAssignableFrom(TestInspector.class)) {
                    TestInspector testInspector = (TestInspector)annotation;
                    Object annotationRef = annotations.get(annotation);
                    TestInspectorLink inspectorLink = new TestInspectorLink(testInspector, annotationRef);
                    if (inspectorLink.isValid()) {
                        addTestInspectorToMap(inspectorLink);
                    }
                }
            }
        }
    }

    private void addTestInspectorToMap (TestInspectorLink inspectorLink)
    {
        List list = _testInspectorMap.get(inspectorLink.getObjectClass());
        if (list == null) {
            list = ListUtil.list();
            _testInspectorMap.put(inspectorLink.getObjectClass(), list);
        }
        list.add(inspectorLink);
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
        String superType = AnnotationUtil.getAnnotationSuperType(annotation);
        if (!StringUtil.nullOrEmptyOrBlankString(superType)) {
            Set<String> subTypes = getTestSessionSetup().resolveSuperType(superType);
            for (String subType : subTypes) {
                testLinks.add(new TestLinkHolder(annotation, ref, subType));
            }
        }
        return testLinks;
    }
}
