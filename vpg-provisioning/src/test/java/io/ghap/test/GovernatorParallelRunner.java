package io.ghap.test;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.netflix.governator.LifecycleModule;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.lifecycle.LifecycleManager;
import governator.junit.GovernatorAfterStatement;
import governator.junit.GovernatorBeforeStatement;
import governator.junit.LifecycleInjectorParamsExtractor;
import governator.junit.config.LifecycleInjectorParams;
import org.junit.Assume;
import org.junit.experimental.theories.PotentialAssignment;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.internal.Assignments;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.*;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RecursiveAction;
import static com.googlecode.junittoolbox.util.TigerThrower.sneakyThrow;


public class GovernatorParallelRunner extends Theories {
    private Injector injector;
    private LifecycleManager lifecycleManager;
    private LifecycleInjectorParamsExtractor injectorParamsExtractor;

    /**
     * Creates a GovernatorJunit4Runner to run {@code testClass}
     *
     * @param testClass the test class being executed
     * @throws org.junit.runners.model.InitializationError if the test class is malformed.
     */
    public GovernatorParallelRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
        this.injectorParamsExtractor = new LifecycleInjectorParamsExtractor();
        this.injector = createInjectorFromClassAnnotations(testClass);
        this.lifecycleManager = getLifecycleManager(this.injector);
        setScheduler(new ParallelScheduler());

    }

    @Override
    public Statement methodBlock(FrameworkMethod method) {
        return new ParallelTheoryAnchor(method, getTestClass());
    }

    public class ParallelTheoryAnchor extends Theories.TheoryAnchor {
        private final Deque<ForkJoinTask<?>> _asyncRuns = new LinkedBlockingDeque<>();
        private volatile boolean _wasRunWithAssignmentCalled;
        private FrameworkMethod testMethod;

        public ParallelTheoryAnchor(FrameworkMethod method, TestClass testClass) {
            super(method, testClass);
            this.testMethod = method;
        }

        private boolean nullsOk() {
            Theory annotation = testMethod.getMethod().getAnnotation(Theory.class);

            if (annotation == null) {
                return false;
            }
            return annotation.nullsAccepted();
        }

        @Override
        protected void runWithAssignment(Assignments assignments) throws Throwable {
            if (_wasRunWithAssignmentCalled) {
                super.runWithAssignment(assignments);
            } else {
                _wasRunWithAssignmentCalled = true;
                super.runWithAssignment(assignments);
                // This is the first call to runWithAssignment, therefore we need to
                // make sure, that all asynchronous runs have finished, before we return ...
                // Note: Because we have added all asynchronous runs via addFirst to _asyncRuns
                // and retrieve them via removeFirst here, task.join() is able to steal tasks,
                // which have not been started yet, from other worker threads ...
                Throwable failure = null;
                while (failure == null && !_asyncRuns.isEmpty()) {
                    ForkJoinTask<?> task = _asyncRuns.removeFirst();
                    try { task.join(); } catch (Throwable t) { failure = t; }
                }
                if (failure != null) {
                    // Cancel all remaining tasks ...
                    while (!_asyncRuns.isEmpty()) {
                        ForkJoinTask<?> task = _asyncRuns.removeFirst();
                        try { task.cancel(true); } catch (Throwable ignored) {}
                    }
                    // ... and join them, to prevent interference with other tests ...
                    while (!_asyncRuns.isEmpty()) {
                        ForkJoinTask<?> task = _asyncRuns.removeFirst();
                        try { task.join(); } catch (Throwable ignored) {}
                    }
                    throw failure;
                }
            }
        }

        protected void runWithCompleteAssignment(final Assignments complete) throws Throwable {
            (new BlockJUnit4ClassRunner(getTestClass().getJavaClass()) {
                protected void collectInitializationErrors(List<Throwable> errors) {
                }

                public Statement methodBlock(FrameworkMethod method) {
                    final Statement statement = super.methodBlock(method);
                    return new Statement() {
                        public void evaluate() throws Throwable {
                            try {
                                statement.evaluate();
                                handleDataPointSuccess();
                            } catch (AssumptionViolatedException var2) {
                                handleAssumptionViolation(var2);
                            } catch (Throwable var3) {
                                reportParameterizedError(var3, complete.getArgumentStrings(nullsOk()));
                            }

                        }
                    };
                }

                protected Statement methodInvoker(FrameworkMethod method, Object test) {
                    return methodCompletesWithParameters(method, complete, test);
                }

                public Object createTest() throws Exception {
                    Object[] params = complete.getConstructorArguments();
                    if(!nullsOk()) {
                        Assume.assumeNotNull(params);
                    }
                    return injector.getInstance(getTestClass().getJavaClass());
                }
            }).methodBlock(this.testMethod).evaluate();
        }

        private Statement methodCompletesWithParameters(
                final FrameworkMethod method, final Assignments complete, final Object freshInstance) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    final Object[] values = complete.getMethodArguments();

                    if (!nullsOk()) {
                        Assume.assumeNotNull(values);
                    }

                    method.invokeExplosively(freshInstance, values);
                }
            };
        }

        @Override
        protected void runWithIncompleteAssignment(Assignments incomplete) throws Throwable {
            for (PotentialAssignment source : incomplete.potentialsForNextUnassigned()) {
                Assignments nextAssignment = incomplete.assignNext(source);
                ForkJoinTask<?> asyncRun = new RecursiveAction() {
                    @Override
                    protected void compute() {
                        try {
                            ParallelTheoryAnchor.this.runWithAssignment(nextAssignment);
                        } catch (Throwable t) {
                            sneakyThrow(t);
                        }
                    }
                };
                _asyncRuns.addFirst(asyncRun.fork());
            }
        }

        /**
         * Overridden to make the method synchronized.
         */
        @Override
        protected synchronized void handleAssumptionViolation(AssumptionViolatedException e) {
            super.handleAssumptionViolation(e);
        }

        /**
         * Overridden to make the method synchronized.
         */
        @Override
        protected synchronized void handleDataPointSuccess() {
            super.handleDataPointSuccess();
        }
    }
    /**
     * The {@link com.netflix.governator.lifecycle.LifecycleManager} is hooked up to start before the
     * entire test class and closed after all the tests are complete. This may not be an ideal behavior and will
     * be revisited at a later stage.
     *
     * @param notifier parent notifier
     * @return the statement with modified startup and shutdown
     */
    @Override
    protected Statement classBlock(final RunNotifier notifier) {
        Statement fromParent = super.classBlock(notifier);
        Statement withBefore = new GovernatorBeforeStatement(this.lifecycleManager, fromParent);
        Statement withAfter = new GovernatorAfterStatement(this.lifecycleManager, withBefore);
        return withAfter;
    }

    /**
     * Read the {@link LifecycleInjectorParams} and figure out the Guice modules, Governator bootstrap modules
     * and instantiate a {@link com.netflix.governator.lifecycle.LifecycleManager}
     *
     * @param testClass
     * @return the Guice {@link com.google.inject.Injector}
     * @throws InitializationError
     */
    private Injector createInjectorFromClassAnnotations(Class<?> testClass) throws InitializationError {
        LifecycleInjectorParams lifecycleInjectorParamsAnnotation
                = testClass.getAnnotation(LifecycleInjectorParams.class);


        List<Module> modules = this.injectorParamsExtractor.getModules(lifecycleInjectorParamsAnnotation);
        BootstrapModule bootstrapModule = this.injectorParamsExtractor.getBootstrapModule(lifecycleInjectorParamsAnnotation);
        List<BootstrapModule> additionalBootstrapModules = this.injectorParamsExtractor.getAdditionalBootstrapModules(lifecycleInjectorParamsAnnotation);
        String[] scannedPackages = this.injectorParamsExtractor.getScannedPackages(lifecycleInjectorParamsAnnotation);

        LifecycleInjectorBuilder lifecycleInjectorBuilder = LifecycleInjector.builder();

        if (modules != null) {
            lifecycleInjectorBuilder.withModules(modules);
        }

        if (bootstrapModule != null) {
            lifecycleInjectorBuilder.withBootstrapModule(bootstrapModule);
        }

        if (additionalBootstrapModules != null) {
            lifecycleInjectorBuilder.withAdditionalBootstrapModules(additionalBootstrapModules);
        }

        if (scannedPackages != null) {
            lifecycleInjectorBuilder.usingBasePackages(scannedPackages);
        }

        Injector localInjector = lifecycleInjectorBuilder.build().createInjector();

        return localInjector;
    }





    private LifecycleManager getLifecycleManager(Injector injector) {
        return injector.getInstance(LifecycleManager.class);
    }

    /**
     * No-op method, normally junit validates that the test class has a 0 parameter constructor, however
     * a Junit with Governator support can accept a constructor with dependencies injected in.
     */
    @Override
    protected void validateConstructor(List<Throwable> errors) {
    }
}
