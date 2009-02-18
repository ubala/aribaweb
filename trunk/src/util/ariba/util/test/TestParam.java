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

    $Id: //ariba/platform/util/core/ariba/util/test/TestParam.java#3 $
*/

package ariba.util.test;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Target({ElementType.TYPE,ElementType.FIELD,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TestParam
{
    /**
     * This annotation should be used to indicate a required parameter.<p>
     * <p>
     * <u>ex</u>:
     * <pre>
     *      class MyTestParams implements StagerArgs {
     *          <b>&#64;Required</b>
     *          int repetitionCount;
     *      }
     * </pre>
     * @aribaapi private
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD,ElementType.METHOD})
    @Inherited
    public @interface Required {}

    /**
     * This annotation should be used to specify a control for a valid value for a parameter.<p>
     * <p>
     * <u>ex</u>:
     * <pre>
     *      class MyTestParams implements StagerArgs {
     *          <b>&#64;Valid("value > 0")</b>
     *          int repetitionCount;
     *      }
     * </pre>
     * @aribaapi private
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD,ElementType.METHOD})
    @Inherited
    public @interface Valid
    {
        public abstract String value();
    }

    /**
     * This annotation should be used to add a set of properties to a field.<p>
     * <p>
     * <u>ex</u>:
     * <pre>
     *      class MyTestParams implements StagerArgs {
     *          //Indicates the enumeration type and the list of values available for the field
     *          <b>&#64;Properties("trait:enumeration; choices: [10, 20, 30, 40, 50, 60, 70, 80, 90, 100];")</b>
     *          int resultsPerPage;
     *      }
     * </pre>
     * @aribaapi private
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE,ElementType.FIELD,ElementType.METHOD})
    @Inherited
    public @interface Properties
    {
        public abstract String value();
    }
}
