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
    String sql = condition.toString();
    assertThat(sql).containsIgnoringCase("status");
  }

  @Test
  void testNumericComparison() {
    Condition condition = FilterParser.parse("age gt 18");
    assertThat(condition).isNotNull();
    String sql = condition.toString();
    assertThat(sql).containsIgnoringCase("age");
  }

  @Test
  void testLikeOperator() {
    Condition condition = FilterParser.parse("name like '%John%'");
    assertThat(condition).isNotNull();
    String sql = condition.toString();
    assertThat(sql).containsIgnoringCase("name");
  }

  @Test
  void testInOperator() {
    Condition condition = FilterParser.parse("status in ('active', 'pending')");
    assertThat(condition).isNotNull();
    String sql = condition.toString();
    assertThat(sql).containsIgnoringCase("status");
  }

  @Test
  void testAndOperator() {
    Condition condition = FilterParser.parse("age gt 18 and status eq 'active'");
    assertThat(condition).isNotNull();
    String sql = condition.toString();
    assertThat(sql).containsIgnoringCase("age");
  }

  @Test
  void testOrOperator() {
    Condition condition = FilterParser.parse("name like '%Smith%' or email like '%@example.com'");
    assertThat(condition).isNotNull();
    String sql = condition.toString();
    assertThat(sql).containsIgnoringCase("name");
  }

  @Test
  void testParentheses() {
    Condition condition = FilterParser.parse("(age gt 18) and (status eq 'active')");
    assertThat(condition).isNotNull();
    String sql = condition.toString();
    assertThat(sql).containsIgnoringCase("age");
  }

  @Test
  void testComplexExpression() {
    Condition condition = FilterParser
        .parse("(age gte 18 and age lte 65) and status ne 'inactive'");
    assertThat(condition).isNotNull();
    String sql = condition.toString();
    assertThat(sql).containsIgnoringCase("age");
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
    String sql = condition.toString();
    assertThat(sql).containsIgnoringCase("is_active");
  }

  @Test
  void testNotInOperator() {
    Condition condition = FilterParser.parse("status notIn ('deleted', 'archived')");
    assertThat(condition).isNotNull();
    String sql = condition.toString();
    assertThat(sql).containsIgnoringCase("status");
  }
}
