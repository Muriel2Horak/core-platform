package cz.muriel.core.test;

import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation to disable tests when running with 'test' profile.
 * 
 * Use this to temporarily skip tests that require features not available
 * in the test profile (e.g., specific DB features, external services).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@DisabledIfSystemProperty(named = "spring.profiles.active", matches = ".*test.*")
public @interface DisabledOnTestProfile {
  String reason() default "Not supported in test profile";
}
