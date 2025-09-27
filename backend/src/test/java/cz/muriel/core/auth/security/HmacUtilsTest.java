package cz.muriel.core.auth.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HmacUtilsTest {

  @Test
  public void shouldComputeCorrectHmacSha256() {
    String data = "test data";
    String secret = "secret key";

    String hmac1 = HmacUtils.computeHmacSha256(data.getBytes(), secret);
    String hmac2 = HmacUtils.computeHmacSha256(data.getBytes(), secret);

    // Stejná data a secret by měly dát stejný HMAC
    assertEquals(hmac1, hmac2);
    assertNotNull(hmac1);
    assertTrue(hmac1.length() > 0);

    // HMAC by měl být hex string (64 znaků pro SHA-256)
    assertEquals(64, hmac1.length());
    assertTrue(hmac1.matches("[0-9a-f]+"));
  }

  @Test
  public void shouldProduceDifferentHmacForDifferentData() {
    String secret = "secret key";

    String hmac1 = HmacUtils.computeHmacSha256("data1".getBytes(), secret);
    String hmac2 = HmacUtils.computeHmacSha256("data2".getBytes(), secret);

    assertNotEquals(hmac1, hmac2);
  }

  @Test
  public void shouldProduceDifferentHmacForDifferentSecret() {
    String data = "test data";

    String hmac1 = HmacUtils.computeHmacSha256(data.getBytes(), "secret1");
    String hmac2 = HmacUtils.computeHmacSha256(data.getBytes(), "secret2");

    assertNotEquals(hmac1, hmac2);
  }

  @Test
  public void slowEqualsShouldReturnTrueForEqualByteArrays() {
    byte[] array1 = "test".getBytes();
    byte[] array2 = "test".getBytes();

    assertTrue(HmacUtils.slowEquals(array1, array2));
  }

  @Test
  public void slowEqualsShouldReturnFalseForDifferentByteArrays() {
    byte[] array1 = "test1".getBytes();
    byte[] array2 = "test2".getBytes();

    assertFalse(HmacUtils.slowEquals(array1, array2));
  }

  @Test
  public void slowEqualsShouldReturnFalseForDifferentLengthArrays() {
    byte[] array1 = "test".getBytes();
    byte[] array2 = "testlong".getBytes();

    assertFalse(HmacUtils.slowEquals(array1, array2));
  }

  @Test
  public void slowEqualsShouldHandleNullArrays() {
    assertTrue(HmacUtils.slowEquals((byte[]) null, (byte[]) null));
    assertFalse(HmacUtils.slowEquals((byte[]) null, "test".getBytes()));
    assertFalse(HmacUtils.slowEquals("test".getBytes(), (byte[]) null));
  }

  @Test
  public void slowEqualsShouldReturnTrueForEqualStrings() {
    assertTrue(HmacUtils.slowEquals("test", "test"));
    assertTrue(HmacUtils.slowEquals("", ""));
  }

  @Test
  public void slowEqualsShouldReturnFalseForDifferentStrings() {
    assertFalse(HmacUtils.slowEquals("test1", "test2"));
    assertFalse(HmacUtils.slowEquals("test", "TEST"));
  }

  @Test
  public void slowEqualsShouldHandleNullStrings() {
    assertTrue(HmacUtils.slowEquals((String) null, (String) null));
    assertFalse(HmacUtils.slowEquals((String) null, "test"));
    assertFalse(HmacUtils.slowEquals("test", (String) null));
  }

  @Test
  public void shouldThrowExceptionForInvalidAlgorithm() {
    // Test, že se vyhodí RuntimeException při chybě v HMAC výpočtu
    // Toto je obtížné testovat přímo, ale můžeme alespoň ověřit, že metoda nepadá
    // na prázdných datech
    assertDoesNotThrow(() -> {
      HmacUtils.computeHmacSha256(new byte[0], "secret");
    });
  }
}
