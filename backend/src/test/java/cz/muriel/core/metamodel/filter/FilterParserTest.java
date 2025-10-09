package cz.muriel.core.metamodel.filter;

import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * ✅ Filter Parser Tests
 */
class FilterParserTest {

  @Test
  void testSimpleEquality() {
    Condition condition = FilterParser.parse("status eq 'active'");
    assertThat(condition).isNotNull();
    assertThat(condition.toString()).contains("status", "active");
  }

  @Test
  void testNumericComparison() {
    Condition condition = FilterParser.parse("age gt 18");
    assertThat(condition).isNotNull();
    assertThat(condition.toString()).contains("age", "18");
  }

  @Test
  void testLikeOperator() {
    Condition condition = FilterParser.parse("name like '%John%'");
    assertThat(condition).isNotNull();
    assertThat(condition.toString()).contains("name", "like");
  }

  @Test
  void testInOperator() {
    Condition condition = FilterParser.parse("status in ('active', 'pending')");
    assertThat(condition).isNotNull();
    assertThat(condition.toString()).contains("status", "in");
  }

  @Test
  void testAndOperator() {
    Condition condition = FilterParser.parse("age gt 18 and status eq 'active'");
    assertThat(condition).isNotNull();
    assertThat(condition.toString()).contains("age", "status");
  }

  @Test
  void testOrOperator() {
    Condition condition = FilterParser.parse("name like '%Smith%' or email like '%@example.com'");
    assertThat(condition).isNotNull();
    assertThat(condition.toString()).contains("name", "email", "or");
  }

  @Test
  void testParentheses() {
    Condition condition = FilterParser.parse("(age gt 18) and (status eq 'active')");
    assertThat(condition).isNotNull();
    assertThat(condition.toString()).contains("age", "status");
  }

  @Test
  void testComplexExpression() {
    Condition condition = FilterParser
        .parse("(age gte 18 and age lte 65) and status ne 'inactive'");
    assertThat(condition).isNotNull();
    assertThat(condition.toString()).contains("age", "status");
  }

  @Test
  void testEmptyFilter() {
    Condition condition = FilterParser.parse("");
    assertThat(condition).isEqualTo(DSL.trueCondition());
  }

  @Test
  void testNullFilter() {
    Condition condition = FilterParser.parse(null);
    assertThat(condition).isEqualTo(DSL.trueCondition());
  }

  @Test
  void testInvalidSyntax() {
    assertThatThrownBy(() -> FilterParser.parse("invalid filter syntax"))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid");
  }

  @Test
  void testBooleanValue() {
    Condition condition = FilterParser.parse("is_active eq true");
    assertThat(condition).isNotNull();
    assertThat(condition.toString()).contains("is_active", "true");
  }

  @Test
  void testNotInOperator() {
    Condition condition = FilterParser.parse("status notIn ('deleted', 'archived')");
    assertThat(condition).isNotNull();
    assertThat(condition.toString()).contains("status", "not in");
  }
}
