/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.plugin.schedulerx2.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static org.apache.skywalking.apm.agent.core.plugin.match.HierarchyMatch.byHierarchyMatch;
import static org.apache.skywalking.apm.plugin.schedulerx2.Constants.SCHEDULERX2_JOB_PROCESSOR;

/**
 * Enhance {@link com.alibaba.schedulerx.worker.processor.JobProcessor} instance and intercept {@link com.alibaba.schedulerx.worker.processor.JobProcessor#process(com.alibaba.schedulerx.worker.domain.JobContext)} method
 *
 * @see org.apache.skywalking.apm.plugin.schedulerx2.MethodJobHandlerMethodInterceptor
 */
public class JobProcessorInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    public static final String METHOD_INTERCEPTOR_CLASS = "org.apache.skywalking.apm.plugin.schedulerx2.MethodJobHandlerMethodInterceptor";

    @Override
    protected ClassMatch enhanceClass() {
        return byHierarchyMatch(SCHEDULERX2_JOB_PROCESSOR);
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[] {
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named("process")
                                .and(isPublic())
                                .and(takesArguments(1))
                                .and(takesArgument(0, named("com.alibaba.schedulerx.worker.domain.JobContext")));
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return METHOD_INTERCEPTOR_CLASS;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                }
        };
    }
}
