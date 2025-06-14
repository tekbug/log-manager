package com.practices.loggingcore.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * A method-level annotation to declaratively add information to the logging context.
 *
 * <p>It uses Spring Expression Language (SpEL) to extract values from method arguments. Each
 * expression should be in the format "key=SpEL_expression".
 *
 * <p>Example:
 *
 * <pre>{@code
 * @LogContext(expressions = {"userId=#user.id", "tenant=#user.tenantId"})
 * public void processUser(User user) {
 *   // Logs inside this method will automatically have "userId" and "tenant"
 * }
 * }</pre>
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogContext {
  /**
   * An array of SpEL expressions to evaluate against the method arguments.
   *
   * @return the expressions to add to the context
   */
  String[] expressions() default {};
}
