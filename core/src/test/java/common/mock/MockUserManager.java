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

import java.security.Principal;
import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.AuthorizableTypeException;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.Query;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;

public class MockUserManager implements UserManager{

	@Override
	public Authorizable getAuthorizable(String id) throws RepositoryException {
		return new Authorizable() {
			
			@Override
			public void setProperty(String relPath, Value[] value) throws RepositoryException {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void setProperty(String relPath, Value value) throws RepositoryException {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public boolean removeProperty(String relPath) throws RepositoryException {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public void remove() throws RepositoryException {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public Iterator<Group> memberOf() throws RepositoryException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public boolean isGroup() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean hasProperty(String relPath) throws RepositoryException {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public Iterator<String> getPropertyNames(String relPath) throws RepositoryException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Iterator<String> getPropertyNames() throws RepositoryException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Value[] getProperty(String relPath) throws RepositoryException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Principal getPrincipal() throws RepositoryException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getPath() throws UnsupportedRepositoryOperationException, RepositoryException {
				return StringUtils.EMPTY;
			}
			
			@Override
			public String getID() throws RepositoryException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Iterator<Group> declaredMemberOf() throws RepositoryException {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	@Override
	public <T extends Authorizable> T getAuthorizable(String id, Class<T> authorizableClass)
			throws AuthorizableTypeException, RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Authorizable getAuthorizable(Principal principal) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Authorizable getAuthorizableByPath(String path)
			throws UnsupportedRepositoryOperationException, RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<Authorizable> findAuthorizables(String relPath, String value) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<Authorizable> findAuthorizables(String relPath, String value, int searchType)
			throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<Authorizable> findAuthorizables(Query query) throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public User createUser(String userID, String password) throws AuthorizableExistsException, RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public User createUser(String userID, String password, Principal principal, String intermediatePath)
			throws AuthorizableExistsException, RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public User createSystemUser(String userID, String intermediatePath)
			throws AuthorizableExistsException, RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Group createGroup(String groupID) throws AuthorizableExistsException, RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Group createGroup(Principal principal) throws AuthorizableExistsException, RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Group createGroup(Principal principal, String intermediatePath)
			throws AuthorizableExistsException, RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Group createGroup(String groupID, Principal principal, String intermediatePath)
			throws AuthorizableExistsException, RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAutoSave() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void autoSave(boolean enable) throws UnsupportedRepositoryOperationException, RepositoryException {
		// TODO Auto-generated method stub
		
	}
	
}
