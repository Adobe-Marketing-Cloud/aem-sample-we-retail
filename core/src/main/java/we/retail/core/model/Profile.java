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
package we.retail.core.model;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;

@Model(adaptables = {Resource.class})
public class Profile {

    @Inject
    @Named("profile/givenName")
    @Optional
    public String givenName;

    @Inject
    @Named("profile/familyName")
    @Optional
    public String familyName;

    @Inject
    @Named("profile/aboutMe")
    @Optional
    public String aboutMe;

    @Inject
    @Named("profile/photos/primary/image")
    @Optional
    public Resource profileImage;

    public String getName() {
        List<String> ret = new ArrayList<String>();
        if (givenName != null) {
            ret.add(givenName);
        }

        if (familyName != null) {
            ret.add(familyName);
        }

        return StringUtils.join(ret, " ");
    }
}
