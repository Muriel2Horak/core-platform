package cz.muriel.core.web;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * HttpServletRequestWrapper that caches the request body for multiple reads.
 * Useful when we need to read the body in a filter for validation and then make
 * it available again for the controller.
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

  private final byte[] cachedBody;

  public CachedBodyHttpServletRequest(HttpServletRequest request, byte[] body) {
    super(request);
    this.cachedBody = body != null ? body.clone() : new byte[0];
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    return new CachedBodyServletInputStream(cachedBody);
  }

  @Override
  public BufferedReader getReader() throws IOException {
    String characterEncoding = getCharacterEncoding();
    if (characterEncoding == null) {
      characterEncoding = StandardCharsets.UTF_8.name();
    }
    return new BufferedReader(new InputStreamReader(getInputStream(), characterEncoding));
  }

  public byte[] getCachedBody() {
    return cachedBody.clone();
  }

  private static class CachedBodyServletInputStream extends ServletInputStream {
    private final ByteArrayInputStream inputStream;

    public CachedBodyServletInputStream(byte[] body) {
      this.inputStream = new ByteArrayInputStream(body);
    }

    @Override
    public boolean isFinished() {
      return inputStream.available() == 0;
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
      throw new UnsupportedOperationException("Read listener not supported");
    }

    @Override
    public int read() throws IOException {
      return inputStream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      return inputStream.read(b, off, len);
    }
  }
}
