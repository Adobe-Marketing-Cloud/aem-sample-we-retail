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
package com.adobe.cq.commerce.celadon.core.impl;

import java.util.ArrayList;
import java.util.List;

final class CatalogCategory {
    private final String path;
    private final String displayName;
    private final String imagePath;
    private final List<String> childPaths = new ArrayList<>();

    CatalogCategory(String path, String displayName, String imagePath) {
        this.path = path;
        this.displayName = displayName;
        this.imagePath = imagePath;
    }

    String path() {
        return path;
    }

    String displayName() {
        return displayName;
    }

    String imagePath() {
        return imagePath;
    }

    List<String> childPaths() {
        return childPaths;
    }
}
