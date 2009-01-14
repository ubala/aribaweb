/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/test/TestValidator.java#3 $
*/
package ariba.util.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.FIELD,ElementType.METHOD})
@Inherited

/**
 * This annotation is used to mark methods to be used by the Test Automation
 * framework as Test Validators.
 *
 * Test validators are used to generate a baseline set of data values that are stored as part of
 * our Selenium UI Automation test scripts and then compared to the values produced by the system during
 * replay.
 *
 * Methods annotated as Validators must return the data structure List<TestValidationParameter>.
 *
 * The following conventions are used when validating test results:
 * <ol>
 *      <li/>The order of parameters in the list does not matter.
 *      <li/>If any parameter key in the baseline is missing from the list from the test run then
 *           it is considered a test failure.
 *      <li/>If any baseline parameter's value does not match exaclt with the value from the current run
 *           it is considered a test failure.
 *      <li/>New parameters in the list form the current run which are not in the baseline are <b>NOT</b>
 *           considered a test failure.  This is to allow validators to grow over time without breaking existing tests.
 * </ol>
 *
 * Some important guidelines when building validators:
 * <ol>
 *      <li/>Use human friendly names for parameters - these need to make sense to system users who are not developers.
 *      <li/>Provide the list in a consistent and useful order.  While the test infrastructure doe not depend on the order
 *           testers who look at the validation screen will find it frustrating to have the order changing from run to run.
 *      <li/>Provide a name for your method which is meaningful to testers.  The name will be displayed in the UI as part
 *           of a validator list, it needs to make sense. 
 * </ol>
 */
public @interface TestValidator
{
    public String classname() default "";
    public String name() default "";
    public String superType() default "";
    public String typeList () default "";
    public String description () default "";
    
}
