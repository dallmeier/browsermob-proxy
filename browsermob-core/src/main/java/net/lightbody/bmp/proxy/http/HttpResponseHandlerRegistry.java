package net.lightbody.bmp.proxy.http;

import com.sun.istack.internal.NotNull;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This registry is provided to a http client. The callback method is invoked upon receiving an http-response.
 * @author Andreas Rau (amras.tf@gmail.com)
 */

public class HttpResponseHandlerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(HttpResponseHandlerRegistry.class);

    private ConcurrentHashMap<UUID, HttpResponseHandler> responseHandlers;

    public HttpResponseHandlerRegistry() {
        responseHandlers = new ConcurrentHashMap<>();
    }

    /**
     * Adds an additional response handler to every incoming http response. If a http-response is currently processed, it
     * is not guaranteed that the response handler is executed on this request.
     *
     * @return an <i>unique id</i>, identifying the added response handler for later removal
     */
    public UUID addResponseHandler(@NotNull HttpResponseHandler rh){
        UUID uuid = UUID.randomUUID();
        responseHandlers.put(uuid, rh);
        LOG.info("Registering responsehandler " + rh.getClass().getSimpleName() + " under id " + uuid);
        return uuid;
    }

    /**
     * Method invoked by http-client upon receiving a response. It invokes all registered responsehandlers consecutively
     * without a predefined order.
     * @param request corresponding request to given response
     * @param response the request to be processed
     * @throws IOException
     */
    public void callBackHandlers(HttpRequest request, HttpResponse response) throws IOException {
        for(HttpResponseHandler rh: responseHandlers.values()){
            rh.handleHttpResonse(request, response);
        }
    }

    /**
     * Removes a registered response handler identified by given <i>id</i>
     * @return <b>false</b>, if no response handler with given id is registered.
     */
    public boolean removeResponseHandler(UUID id) {
        return id != null && responseHandlers.remove(id) != null;
    }

}
