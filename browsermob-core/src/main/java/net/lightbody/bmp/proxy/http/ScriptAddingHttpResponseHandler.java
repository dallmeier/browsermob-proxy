package net.lightbody.bmp.proxy.http;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.util.Arrays;

/**
 * Adds javascript code at the top of a text/html page.
 * @author Andreas Rau (amras.tf@gmail.com)
 */

public class ScriptAddingHttpResponseHandler extends ContentModifyingHttpResponseHandler {

    public ScriptAddingHttpResponseHandler(String js) {
        super("<head>", String.format("<script> %s </script>", js), Arrays.<Header>asList(new BasicHeader("Content-Type", "text/html")), false, true);
    }
}
