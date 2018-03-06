/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2017 Adobe Systems Incorporated
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
package common.mock;

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.tenant.Tenant;

import com.adobe.cq.social.community.api.CommunityContext;
import com.adobe.cq.social.community.api.CommunityUserGroup;
import com.adobe.cq.social.srp.config.SocialResourceConfiguration;

public class MockCommunityContext implements CommunityContext{

	@Override
	public Tenant getTenant() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTenantId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSiteId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCommunityGroupId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCommunityGroupUniqueId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContentRootPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTenantConfigPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSitePayloadPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSitePagePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDesignRootPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSiteThemePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPageThemePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSitePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCommunityGroupPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSiteBannerPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isMultiTenantSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getUgcPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUgcSitePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getActivityStreamRootPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTenantUserGroupName(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSiteUserGroupName(CommunityUserGroup arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFullyQualifiedGroupName(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUserGroupRootPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUserRootPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUserGroupPath(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean checkIfUserIsAdmin(UserManager arg0, String arg1) throws RepositoryException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean checkIfUserIsGroupAdmin(UserManager arg0, String arg1) throws RepositoryException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isUserAdmin(UserManager arg0, String arg1) throws RepositoryException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getStorageConfigurationPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SocialResourceConfiguration getStorageConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean checkIfUserIsModerator(UserManager arg0, String arg1) throws RepositoryException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getTenantGroupRootPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTenantUserRootPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCommunityGroupRootPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCommunityUserRootPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSiteResourcesPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCommunityGroupResourcesPath() {
		return null;
	}

	@Override
	public List<String> getSiteContentPaths() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSiteRootPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSiteRootPath(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSiteName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSiteDamRootPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSiteContentFragmentDamRootPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSiteContentFragmentAssociatedMediaDamRootPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSiteDamCollectionPath() {
		// TODO Auto-generated method stub
		return null;
	}

}
