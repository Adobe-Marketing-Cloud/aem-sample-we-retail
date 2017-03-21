/*******************************************************************************
 * Copyright 2016 Adobe Systems Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package apps.weretail.components.content.articleslist;

import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.sightly.WCMUsePojo;

public class ArticleList extends WCMUsePojo {

    private static final String PN_TYPE = "displayAs";
    private static final String TYPE_DEFAULT = "default";

    private String type;
    
    @Override
    public void activate() throws Exception {
        ValueMap properties = getProperties();
        type = properties.get(PN_TYPE, TYPE_DEFAULT);
    }

    public String getType() {
        return type;
    }
}
