package net.lightbody.bmp.proxy.http;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.*;

/**
 * Replaces arbitrary content of http/https responses.
 * @author Andreas Rau [amras.tf@gmail.com]
 */
public class ContentModifyingHttpResponseHandler implements HttpResponseHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ContentModifyingHttpResponseHandler.class);

    private final String replacePattern;
    private final String replaceString;
    private final List<String> contentTypeValues = new LinkedList<>();
    private final Boolean doOnce;

    /**
     * Init ContentModifyingHttpResponseHandler
     * @param replacePattern String to search
     * @param replaceString String replacing replacePattern
     * @param contentType A whitelist of http content types (header fields) to be processed. Empty list corresponds to wildcard
     * @param replace If <i>true</i>, replacePattern is removed from the response and replaced by replaceString. If <i>false</i>, replaceString is appended to replacePattern
     * @param doOnce If <i>true</i>, only the first occurence of replacePattern is modified
     */
    public ContentModifyingHttpResponseHandler(String replacePattern, String replaceString, List<Header> contentType, Boolean replace, Boolean doOnce) {

        this.replacePattern = replacePattern;
        for (Header h: contentType) {
            contentTypeValues.add(h.getValue());
        }
        if (!replace) {
            this.replaceString = replacePattern + replaceString;
        } else {
            this.replaceString = replaceString;
        }
        this.doOnce = doOnce;
    }

    @Override
    public void handleHttpResonse(HttpRequest request, HttpResponse response) throws IOException {
        HttpEntity oldEntity = response.getEntity();
        if (oldEntity != null && oldEntity.getContentType()!=null) {
            boolean gzipping = false;
            boolean deflating = false;

            if (hasMatchingContentType(oldEntity)) {

                Charset charset = ContentType.getOrDefault(oldEntity).getCharset();

                if (charset == null) {
                    charset = Charset.defaultCharset();
                    LOG.info(String.format("Could not detect charset of http resonse. Assuming %s", charset));

                }

                InputStream is = oldEntity.getContent();

                Header contentEncodingHeader = response.getFirstHeader("Content-Encoding");
                if(contentEncodingHeader != null) {
                    if ("gzip".equalsIgnoreCase(contentEncodingHeader.getValue())) {
                        gzipping = true;
                    } else if ("deflate".equalsIgnoreCase(contentEncodingHeader.getValue())) {
                        deflating = true;
                    }
                }

                // deal with GZIP content!
                if(response.getEntity().getContentLength() != 0) { //getContentLength<0 if unknown
                    if (gzipping) {
                        is = new GZIPInputStream(is);
                    } else if (deflating) {
                        // RAW deflate only
                        // WARN : if system is using zlib<=1.1.4 the stream must be append with a dummy byte
                        // that is not requiered for zlib>1.1.4 (not mentioned on current Inflater javadoc)
                        is = new InflaterInputStream(is, new Inflater(true));
                    }
                }

                LOG.info(String.format("Manipulating Webpage with content type %s", oldEntity.getContentEncoding()));

                BasicHttpEntity newEntity = new BasicHttpEntity();
                newEntity.setContentEncoding(oldEntity.getContentEncoding());
                newEntity.setContentType(oldEntity.getContentType());

                String s = toStringAndClose(is, charset);
                if (doOnce) {
                    s = s.replaceFirst(replacePattern, replaceString);
                } else {
                    s = s.replaceAll(replacePattern, replaceString);
                }
                byte[] bytes = null;
                try {
                    byte[] loadedBytes = s.getBytes(charset);
                    if (gzipping) {
                        LOG.info("Detected GZIP encoded content");
                        ByteArrayOutputStream os = new ByteArrayOutputStream(s.length());
                        GZIPOutputStream gos = new GZIPOutputStream(os);
                        gos.write(loadedBytes);
                        gos.close();
                        byte[] compressed = os.toByteArray();
                        os.close();
                        bytes = compressed;

                    } else if (deflating) {
                        LOG.info("Detected Deflating encoded content");
                        ByteArrayOutputStream os = new ByteArrayOutputStream(s.length());

                        InflaterOutputStream inflaterOutputStream = new InflaterOutputStream(os, new Inflater(true));
                        inflaterOutputStream.write(loadedBytes);
                        inflaterOutputStream.close();
                        os.close();
                        bytes = os.toByteArray();

                    } else {
                        bytes = loadedBytes;
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }


                newEntity.setContent(new ByteArrayInputStream(bytes));
                newEntity.setContentEncoding(newEntity.getContentEncoding());
                response.setEntity(newEntity);

            } else {
                LOG.debug(String.format("Ignoring content of unprocessed entity type %s", oldEntity.getContentType().getValue()));
            }

        }

    }

    /**
     * Checks if the given entity matches the expected content encoding Type.
     */
    private boolean hasMatchingContentType(HttpEntity entity) {
        if (entity == null || entity.getContentType() == null) return false;

        if(contentTypeValues.size() == 0) return true;

        String[] split = entity.getContentType().getValue().split(";");
        for (String s: split){
            for(String ctv: contentTypeValues){
                if(ctv.equalsIgnoreCase(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Extracts given inputstream to String, before closing it. Neither must be null.
     */
    private String toStringAndClose(InputStream in, Charset charset) throws IOException {
        try {
            return org.apache.commons.io.IOUtils.toString(in, charset);
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(in);
        }
    }
}
