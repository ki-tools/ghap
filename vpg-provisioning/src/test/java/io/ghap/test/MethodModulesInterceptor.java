package io.ghap.test;

import com.google.inject.Injector;
import com.google.inject.Module;
import io.ghap.Injectable;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.util.ArrayList;
import java.util.List;

public class MethodModulesInterceptor implements MethodInterceptor {

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {

    MethodModules methodModuleInjectorAnnotation = methodInvocation.getMethod().getAnnotation(MethodModules.class);
    if(methodModuleInjectorAnnotation != null) {
      List<Module> modules = new ArrayList<Module>();
      for(Class<? extends Module> moduleClass : methodModuleInjectorAnnotation.modules()) {
        Module module = moduleClass.newInstance();
        modules.add(module);
      }
      Object obj = methodInvocation.getThis();
      if(obj instanceof Injectable) {
        Injectable injectable = (Injectable)obj;
        Injector injector = injectable.whichInjector();
        Injector childInjector = injector.createChildInjector(modules);
        injectable.setChildInjector(childInjector);
        Object result = methodInvocation.proceed();       // invoke the method
        return result;
      }
    }
    return methodInvocation.proceed();
  }
}
