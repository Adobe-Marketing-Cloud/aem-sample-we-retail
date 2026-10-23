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
package com.adobe.cq.commerce.celadon.aem.mcp;

import com.google.gson.JsonObject;
import org.apache.sling.api.resource.ResourceResolver;

/** A single MCP tool. Implementations are OSGi services collected by the registry. */
public interface McpTool {
    /** Unique tool name (e.g. "list_catalogs"). */
    String name();

    /** One-paragraph description written for an LLM audience. */
    String description();

    /** JSON Schema (as a JSON string) for the tool's arguments object. */
    String inputSchema();

    /**
     * Execute the tool and return the text payload for the MCP tool result.
     * Throwing signals a tool-execution error (mapped to {@code isError:true}).
     */
    String call(ResourceResolver resolver, JsonObject arguments) throws Exception;
}
