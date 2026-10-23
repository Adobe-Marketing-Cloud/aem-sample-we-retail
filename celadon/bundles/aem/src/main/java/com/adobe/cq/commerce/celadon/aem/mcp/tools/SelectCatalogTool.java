/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.commerce.celadon.aem.mcp.tools;

import com.adobe.cq.commerce.celadon.aem.mcp.McpTool;
import com.adobe.cq.commerce.celadon.aem.mcp.guard.GatedTool;
import com.google.gson.JsonObject;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Changes which catalog the GraphQL servlet serves at its bare endpoint by
 * updating its OSGi {@code defaultCatalog}. The servlet has @Activate and no
 * @Modified, so the update triggers a full re-activation that reloads the
 * default catalog and the manifest.
 */
@Component(service = McpTool.class)
public class SelectCatalogTool extends GatedTool {

    static final String PID = "com.adobe.cq.commerce.celadon.aem.CeladonGraphqlServlet";

    @Reference
    private ConfigurationAdmin configAdmin;

    @Override
    public String name() {
        return "select_catalog";
    }

    @Override
    public String description() {
        return "Change which catalog the GraphQL endpoint serves at its bare path by updating the "
                + "servlet's OSGi defaultCatalog, the catalog name under /content/dam/celadon (e.g. "
                + "'we-retail'). Instance-wide; triggers a servlet re-activation that reloads the manifest. "
                + "Gated (preview/confirm).";
    }

    @Override
    public String inputSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"defaultCatalog\":{\"type\":\"string\",\"description\":\"catalog name under /content/dam/celadon, e.g. we-retail\"},"
                + "\"confirmToken\":{\"type\":\"string\"}},"
                + "\"required\":[\"defaultCatalog\"],\"additionalProperties\":false}";
    }

    @Override
    protected String catalogOf(JsonObject args) {
        return args.has("defaultCatalog") ? args.get("defaultCatalog").getAsString() : "";
    }

    @Override
    protected String preview(ResourceResolver resolver, JsonObject args) throws Exception {
        Configuration cfg = configAdmin.getConfiguration(PID, null);
        Object current = cfg.getProperties() == null ? "(default)" : cfg.getProperties().get("defaultCatalog");
        return "Will change the served default catalog from '" + current + "' to '"
                + ToolArgs.require(args, "defaultCatalog") + "'. The GraphQL servlet will re-activate.";
    }

    @Override
    protected String execute(ResourceResolver resolver, JsonObject args) throws Exception {
        String defaultCatalog = ToolArgs.require(args, "defaultCatalog");
        Configuration cfg = configAdmin.getConfiguration(PID, null);
        Dictionary<String, Object> props = cfg.getProperties();
        Dictionary<String, Object> merged = new Hashtable<>();
        if (props != null) {
            Enumeration<String> keys = props.keys();
            while (keys.hasMoreElements()) {
                String k = keys.nextElement();
                merged.put(k, props.get(k));
            }
        }
        merged.put("defaultCatalog", defaultCatalog);
        cfg.update(merged);
        return "served default catalog set to '" + defaultCatalog + "'";
    }
}
