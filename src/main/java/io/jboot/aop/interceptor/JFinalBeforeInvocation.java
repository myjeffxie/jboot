/**
 * Copyright (c) 2015-2018, Michael Yang 杨福海 (fuhai999@gmail.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jboot.aop.interceptor;

import com.jfinal.aop.Interceptor;
import com.jfinal.aop.InterceptorManager;
import com.jfinal.aop.Invocation;
import io.jboot.aop.interceptor.cache.JbootCacheEvictInterceptor;
import io.jboot.aop.interceptor.cache.JbootCacheInterceptor;
import io.jboot.aop.interceptor.cache.JbootCachePutInterceptor;
import io.jboot.aop.interceptor.cache.JbootCachesEvictInterceptor;
import io.jboot.aop.interceptor.metric.*;
import io.jboot.support.metric.JbootMetricManager;
import io.jboot.exception.JbootException;
import io.jboot.kits.ClassKits;

import java.lang.reflect.Method;


public class JFinalBeforeInvocation extends Invocation {


    private Interceptor[] inters;
    private Invocation originInvocation;

    private int index = 0;


    private static final Interceptor[] ALL_INTERS = {
            new JbootMetricCounterAopInterceptor(),
            new JbootMetricConcurrencyAopInterceptor(),
            new JbootMetricMeterAopInterceptor(),
            new JbootMetricTimerAopInterceptor(),
            new JbootMetricHistogramAopInterceptor(),
            new JbootCacheEvictInterceptor(),
            new JbootCachesEvictInterceptor(),
            new JbootCachePutInterceptor(),
            new JbootCacheInterceptor()
    };

    private static final Interceptor[] NO_METRIC_INTERS = {
            new JbootCacheEvictInterceptor(),
            new JbootCachesEvictInterceptor(),
            new JbootCachePutInterceptor(),
            new JbootCacheInterceptor()
    };


    private static boolean metricConfigOk = JbootMetricManager.me().isConfigOk();


    public JFinalBeforeInvocation(Invocation originInvocation) {
        this.originInvocation = originInvocation;

        Class targetClass = ClassKits.getUsefulClass(originInvocation.getTarget().getClass());
        Method method = originInvocation.getMethod();

        this.inters = metricConfigOk
                ? InterceptorManager.me().buildServiceMethodInterceptor(ALL_INTERS, targetClass, method)
                : InterceptorManager.me().buildServiceMethodInterceptor(NO_METRIC_INTERS, targetClass, method);

    }


    @Override
    public void invoke() {
        if (index < inters.length) {
            inters[index++].intercept(this);
        } else if (index++ == inters.length) {    // index++ ensure invoke action only one time
            try {
                originInvocation.invoke();
                setReturnValue(originInvocation.getReturnValue());
            } catch (Throwable throwable) {
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                } else {
                    throw new JbootException(throwable.getMessage(), throwable);
                }
            }
        }
    }


    @Override
    public Method getMethod() {
        return originInvocation.getMethod();
    }

    @Override
    public String getMethodName() {
        return getMethod().getName();
    }

    @Override
    public <T> T getTarget() {
        return (T) originInvocation.getTarget();
    }

    @Override
    public Object getArg(int index) {
        return originInvocation.getArg(index);
    }

    @Override
    public void setArg(int index, Object value) {
        originInvocation.setArg(index, value);
    }

    @Override
    public Object[] getArgs() {
        return originInvocation.getArgs();
    }
}
