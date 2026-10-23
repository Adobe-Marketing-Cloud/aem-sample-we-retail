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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

public class AddProductToCategoryToolTest {

    private final AddProductToCategoryTool tool = new AddProductToCategoryTool();

    @Test
    public void exposesStableToolName() {
        assertEquals("add_product_to_category", tool.name());
    }

    @Test
    public void inputSchemaIsValidJsonWithExpectedShape() {
        JsonObject schema = JsonParser.parseString(tool.inputSchema()).getAsJsonObject();
        assertEquals("object", schema.get("type").getAsString());
        assertFalse("must reject unknown args", schema.get("additionalProperties").getAsBoolean());

        JsonObject props = schema.getAsJsonObject("properties");
        assertTrue("has catalog", props.has("catalog"));
        assertTrue("has sku", props.has("sku"));
        assertTrue("has category", props.has("category"));

        JsonArray required = schema.getAsJsonArray("required");
        assertTrue("sku is required", required.contains(JsonParser.parseString("\"sku\"")));
        assertTrue("category is required", required.contains(JsonParser.parseString("\"category\"")));
        assertEquals("only sku + category are required", 2, required.size());
    }
}
