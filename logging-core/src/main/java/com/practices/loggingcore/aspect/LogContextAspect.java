package com.practices.loggingcore.aspect;

import com.practices.loggingcore.annotation.LogContext;
import com.practices.loggingcore.core.LoggingContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
@Slf4j
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
   * @return The result of the method execution.
   * @throws Throwable If the intercepted method throws any exception.
   */

  @Around("execution(* *(..)) && (@annotation(com.practices.loggingcore.annotation.LogContext) || @within(com.practices.loggingcore.annotation.LogContext))")
  public Object addInformationFromExpression(ProceedingJoinPoint joinPoint) throws Throwable {
    final List<String> addedKeys = new ArrayList<>();

    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();

    LogContext logContextAnnotation;

    // find method-level annotation
    logContextAnnotation = AnnotationUtils.findAnnotation(method, LogContext.class);

    // check the target class
    if (logContextAnnotation == null && joinPoint.getTarget() != null) {
      Class<?> targetClass = AopUtils.getTargetClass(joinPoint.getTarget());
      try {
        Method targetMethod = targetClass.getMethod(method.getName(), method.getParameterTypes());
        logContextAnnotation = AnnotationUtils.findAnnotation(targetMethod, LogContext.class);
      } catch (NoSuchMethodException e) {
        // method not found on target class, continue
      }
    }

    if (logContextAnnotation == null) {
      Class<?> targetClass = joinPoint.getTarget() != null ?
          AopUtils.getTargetClass(joinPoint.getTarget()) :
          method.getDeclaringClass();
      logContextAnnotation = AnnotationUtils.findAnnotation(targetClass, LogContext.class);
    }

    if (logContextAnnotation == null || logContextAnnotation.expressions().length == 0) {
      return joinPoint.proceed();
    }

    try {
      Object[] args = joinPoint.getArgs();
      String[] paramNames = signature.getParameterNames();

      if (paramNames == null) {
        log.warn("Parameter names are not available for method: {}", method);
        return joinPoint.proceed();
      }

      StandardEvaluationContext evalContext = new StandardEvaluationContext();
      for (int i = 0; i < paramNames.length; i++) {
        evalContext.setVariable(paramNames[i], args[i]);
      }

      Set<String> seenKeys = new HashSet<>();

      for (String expression : logContextAnnotation.expressions()) {
        int index = expression.indexOf('=');
        if (index == -1) {
          log.warn("Invalid expression format '{}'. Expected format: key=#expression", expression);
          continue;
        }

        String key = expression.substring(0, index).trim();
        String raxExpr = expression.substring(index + 1).trim();

        try {
          Expression expressionCompare = expressionParser.parseExpression(raxExpr);
          Object value = expressionCompare.getValue(evalContext);

          log.debug("Evaluating expressionCompare '{}' for key '{}', result: '{}'", raxExpr, key, value);

          if (value != null) {
            if (seenKeys.add(key)) { // only add if it's not seen before
              loggingContext.set(key, String.valueOf(value));
              addedKeys.add(key);
              log.debug("Successfully inserted '{}' = '{}' into LoggingContext", key, value);
            } else {
              log.warn("Duplicate key '{}' detected. Skipping second occurrence.", key);
            }
          } else {
            log.debug("Skipped null value for key '{}'", key);
          }

        } catch (Exception e) {
          log.warn("Failed to evaluate expression '{}' for key '{}': {}", raxExpr, key, e.getMessage());
        }
      }

      return joinPoint.proceed();
    } finally {
      addedKeys.forEach(loggingContext::remove);
    }
  }
}
