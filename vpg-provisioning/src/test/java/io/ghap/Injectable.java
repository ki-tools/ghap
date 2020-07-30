package io.ghap;

import com.google.inject.Injector;

/**
 * This interface describes an API to acquire an Injector from an implementing class.
 * This is primarily used to obtain and replace an injector in the class in a convienent
 * manner.
 */
public interface Injectable {
  /**
   * Get the primary injector for the implementing class
   * @return
   */
  Injector getInjector();

  /**
   * Get a child injector if one exists for the implementing class
   * @return
   */
  Injector getChildInjector();

  /**
   * Set the child injector for the implementing class, if a child injector is
   * set it is what is returned by the @{link whichInjector} method.
   * @param injector
   */
  void setChildInjector(Injector injector);

  /**
   * Return the preferred injector of a class to inherit from, this is either the primary injector, or
   * the child injector if it has been set.
   * @return
   */
  Injector whichInjector();
}
