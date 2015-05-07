package net.lightbody.bmp.proxy.http;

import com.google.common.collect.Multimap;

import java.util.Collection;

public class HttpResponseHandlerFactory {

    /**
     * Generates a HttpResponseHandler for the given arguments.
     * @throws java.lang.IllegalArgumentException If the given arguments do not match the expected responseHandlerType or responseHandlerType is unknown.
     *
     * TODO: Add additional response handlers
     */
    public static HttpResponseHandler generateResponseHandler(String responseHandlerType, Multimap<String, String> params) throws IllegalArgumentException {

        switch (responseHandlerType) {
            case "ScriptAddingHttpResponseHandler" :
                Collection<String> script = params.get("script");

                if (script.size() != 1) {
                    throw new IllegalArgumentException(String.format("%s needs exactly one argument of type script", responseHandlerType));
                }

                return new ScriptAddingHttpResponseHandler(script.iterator().next());

            default:
                //TODO: Add Additional ResponseHandlers
                throw new IllegalArgumentException(String.format("Unhandled ResponseHandlerType %s", responseHandlerType));
        }

    }

}
