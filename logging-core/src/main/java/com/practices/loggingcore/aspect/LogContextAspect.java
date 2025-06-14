package com.practices.loggingcore.aspect;

import com.practices.loggingcore.annotation.LogContext;
import com.practices.loggingcore.core.LoggingContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Aspect class that intercepts methods annotated with {@link LogContext} to dynamically evaluate
 * and inject method argument values into the logging context, like MDC,
 * using Spring Expression Language (SpEL).
 *
 * <p>The purpose is to enrich logs with contextual data derived from the method's parameters at
 * runtime, which can aid in tracing, debugging, or correlating logs across services or layers.
 *
 * <p>Example usage of the annotation:
 * <pre>
 * {@code
 * @LogContext(expressions = {"userId=#user.id", "action=#actionName"})
 * public void process(User user, String actionName) { ... }
 * }
 * </pre>
 *
 * <p>The evaluated expressions are inserted into the {@link LoggingContext} before the method
 * execution and removed after it completes to prevent context contamination.
 */

@Aspect
public class LogContextAspect {

  private final LoggingContext loggingContext;
  private final SpelExpressionParser expressionParser = new SpelExpressionParser();

  public LogContextAspect(LoggingContext loggingContext) {
    this.loggingContext = loggingContext;
  }

  /**
   * Intercepts method execution to populate the {@link LoggingContext} with key-value pairs
   * evaluated from SpEL (Spring Expression Language) expressions defined 
   * in the {@link LogContext} annotation.
   *
   * <p>The expressions follow the format {@code key=#expression}, where {@code #expression}
   * references method parameters or their properties. These expressions are evaluated against the
   * method's actual arguments at runtime, and the results are added to the logging context
   * (e.g., for use with MDC in structured logging).
   *
   * <p>All added keys are automatically removed from the context after the method completes,
   * ensuring that context information is not leaked between requests or method calls.
   *
   * @param joinPoint The AOP join point representing the intercepted method call.
   * @param logContextAnnotation The {@link LogContext} annotation instance 
   *                             containing SpEL expressions.
   * @return The result of the method execution.
   * @throws Throwable If the intercepted method throws any exception.
   */
  
  @Around("@annotation(logContextAnnotation)")
  public Object addInformationFromExpression(
      ProceedingJoinPoint joinPoint,
      LogContext logContextAnnotation
  ) throws Throwable {
    final List<String> addedKeys = new ArrayList<>();
    try {
      if (logContextAnnotation.expressions().length > 0) {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final Object[] args = joinPoint.getArgs();
        final String[] paramNames = signature.getParameterNames();

        final StandardEvaluationContext evalContext = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
          evalContext.setVariable(paramNames[i], args[i]);
        }

        for (final String expressionStr : logContextAnnotation.expressions()) {
          final String[] parts = expressionStr.split("=", 2);
          if (parts.length == 2) {
            final String key = parts[0].trim();
            final Expression expression = expressionParser.parseExpression(parts[1].trim());
            final Object value = expression.getValue(evalContext);

            loggingContext.set(key, String.valueOf(value));
            addedKeys.add(key);
          }
        }
      }
      return joinPoint.proceed();
    } finally {
      addedKeys.forEach(loggingContext::remove);
    }
  }
}
