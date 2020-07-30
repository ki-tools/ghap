package io.ghap.guice;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * A simple interceptor responsible for recording when unit tests are entered into and exited from.
 * The <code>UnitTestTrackerModule</code> configures this interceptor to work on methods annotated with <code>@Test</code>
 */
public class UnitTestTracker implements MethodInterceptor {
    private final static Logger LOG = LoggerFactory.getLogger(UnitTestTracker.class);


    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();

        //TODO: Need to get this to work with the logger, instead of using system.out
        System.out.println(String.format("Entering unit test <%s.%s>%n", method.getDeclaringClass().getName(), method.getName()));

        try {
            return invocation.proceed();
        } finally {
            System.out.println(String.format("Exiting unit test <%s.%s>%n", method.getDeclaringClass().getName(), method.getName()));
        }
    }
}
