package io.holbein.ephor.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

@Slf4j
public class GzipDecompressionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String contentEncoding = request.getHeader("Content-Encoding");

        if ("gzip".equalsIgnoreCase(contentEncoding)) {
            log.debug("Decompressing gzip request body for {}", request.getRequestURI());
            filterChain.doFilter(new GzipRequestWrapper(request), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private static class GzipRequestWrapper extends HttpServletRequestWrapper {

        private final HttpServletRequest original;

        public GzipRequestWrapper(HttpServletRequest request) {
            super(request);
            this.original = request;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            GZIPInputStream gzipStream = new GZIPInputStream(original.getInputStream());
            return new DelegatingServletInputStream(gzipStream);
        }

        @Override
        public String getHeader(String name) {
            if ("Content-Encoding".equalsIgnoreCase(name)) {
                return null;
            }
            return super.getHeader(name);
        }
    }

    private static class DelegatingServletInputStream extends ServletInputStream {

        private final InputStream delegate;

        public DelegatingServletInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return delegate.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        @Override
        public boolean isFinished() {
            try {
                return delegate.available() == 0;
            } catch (IOException e) {
                return true;
            }
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException("ReadListener not supported");
        }
    }
}
