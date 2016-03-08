/*
 *  Copyright 2016 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

use(function () {  

	'use strict';

	// Checking if the Community area has been setup or not on this instance
	var communityEnabled = false;
	var communitySetup = '/bin/SetupCommunities?contentPath=/etc/community/we-retail';
	var communitySignin = '/content/we-retail/community/en/signin';
	var communityHome = '/content/we-retail/community/en';
	var communityBlog = '/content/we-retail/community/en/blog';
	var communityForum = '/content/we-retail/community/en/forum';
	var communityQA = '/content/we-retail/community/en/questions';
	var communityRoot = resolver.getResource('/content/we-retail/community');
	if (communityRoot != null) {
		communityEnabled = true;
	} else {
		communityHome = communitySetup + '&returnURL=' + communityHome + '.html#top';
		communitySignin = communitySetup + '&returnURL=' + communitySignin + '.html#top';
		communityBlog = communitySetup + '&returnURL=' + communityBlog + '.html#top';
		communityForum = communitySetup + '&returnURL=' + communityForum + '.html#top';
		communityQA = communitySetup + '&returnURL=' + communityQA + '.html#top';
	}

	var Calendar = Packages.java.util.Calendar;
	var currentYear = Calendar.getInstance().get(Calendar.YEAR);
	
	return {
		communityEnabled: communityEnabled,
		communityHome: communityHome,
		communityBlog: communityBlog,
		communityForum: communityForum,
		communityQA: communityQA,
		year: currentYear
	};

});