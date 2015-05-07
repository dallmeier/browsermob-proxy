package net.lightbody.bmp.proxy.http;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import java.io.IOException;

/**
 * @author Andreas Rau [amras.tf@gmail.com]
 */
public interface HttpResponseHandler {

    /**
     * The given response may or may not be modified, without terminating the inputstreams.
     * @param request the corresponding request to the given response
     * @param response response to be handled
     * @throws IOException
     */
    public void handleHttpResonse(HttpRequest request, HttpResponse response) throws IOException;

}
