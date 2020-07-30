var my = require('./ums_common');
var endOfLine = my.endOfLine;

my.stepPrefix = 'G';
var frisby = require('frisby');
var umsUrls = require('./ums_urls');

var umsUser = require('./ums_user');
var Tester = umsUser.makeUser('MrTester');
var userCRUD = require('./ums_user_crud');
var testUsers = [];
for(i = 0; i < 6; i++)
	testUsers.push(umsUser.makeUser('Tester'+i))

var umsGroup = require('./ums_group');
var groupCRUD = require('./ums_group_crud');
var testGroups = [];
var i;
for(i = 0; i < 2; i++)
  testGroups.push(umsGroup.create( Tester.getParentDn(), 'Testers Group '+i, 'Testers group ' + i + ' description'))

var ctrlFlow = require('./control_flow');

var useOAuth = true;
var oAuth;
var authHeader = {
	Name: '',
	Value: ''
};

// START
(function () {
	if (useOAuth) {
		oAuth = require('./../oauth/oauth_service2');
		var testerAdmin = require('./tester_admin');
		oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
			.then(runSuiteWithOAuth);
		//oAuth = require('./ums_oauth');
		//oAuth.waitAccessToken(runSuiteWithOAuth);
	} else {
		var doAdminSignIn = require('./ums_AdminSignIn');
		doAdminSignIn(runSuiteWithCookieAuth);
	}
}());

function runSuiteWithCookieAuth(cookie_str) {
	authHeader.Name = 'Cookie';
	authHeader.Value = cookie_str;
	runSuite();
}

function runSuiteWithOAuth() {
	authHeader.Name = 'Authorization';
	authHeader.Value = oAuth.access_token;
	runSuite();
}

function runSuite() {
	ctrlFlow.series([
		function(next) {groupCRUD.createGroup( authHeader, testGroups[0], next ); },
		function(next) {tryToCreateExistingGroup( testGroups[0], next )},
		function(next) {groupCRUD.getGroup( authHeader, testGroups[0], next )},
		function(next) {tryToFindNonexistentGroup( testGroups[1], next )},
		function(next) {groupCRUD.deleteGroup( authHeader, testGroups[0], next )},
		function(next) {deleteNonexistentGroup( testGroups[0], next )}
	], createGroups);
}

function createGroups(prev_results) {

	if (my.resultsHaveErrors(prev_results)) {
		logResults(prev_results,'createGroups is cancelled');
		return;
	}

	var createCalls = [];
	testGroups.forEach(function(ums_group){
		createCalls.push(
			function(next) {groupCRUD.createGroup( authHeader, ums_group, next )}
		);
	});
	ctrlFlow.series(createCalls, createUsers);

}

function createUsers(prev_results){

	if (my.resultsHaveErrors(prev_results)) {
		logResults(prev_results,'createUsers is cancelled');
		deleteGroups();
	}
	else {
		var createCalls = [];
		testUsers.forEach(function(ums_user){
			createCalls.push(
				function(next) {userCRUD.createUser(authHeader, ums_user, next ); }
			);
		});
		ctrlFlow.series(createCalls, addMembersToGroups);
	}
}

function addMembersToGroups(prev_results) {

	if (my.resultsHaveErrors(prev_results)) {
		logResults(prev_results,'addMembersToGroup is cancelled');
		deleteAllUsers();
		return;
	}

	ctrlFlow.series([
		function(next) {groupCRUD.addMemberToGroup( authHeader, testUsers[0], testGroups[0], next ); },
		function(next) {groupCRUD.addMemberToGroup( authHeader, testUsers[1], testGroups[0], next ); },
		function(next) {groupCRUD.addMemberToGroup( authHeader, testUsers[2], testGroups[0], next ); },
		function(next) {groupCRUD.addMemberToGroup( authHeader, testUsers[3], testGroups[1], next ); },
		function(next) {groupCRUD.addMemberToGroup( authHeader, testUsers[4], testGroups[1], next ); },
		function(next) {groupCRUD.addMemberToGroup( authHeader, testUsers[5], testGroups[1], next ); }
	], deleteMembers);
}

function deleteMembers(){
	ctrlFlow.series([
		function(next) {groupCRUD.deleteMemberFromGroup( authHeader, testUsers[1], testGroups[0], next ); }
	], deleteSomeUsers);
}

function deleteSomeUsers(prev_results) {
	if (my.resultsHaveErrors(prev_results)) {
		logResults(prev_results,'deleteSomeUsers is cancelled');
		deleteAllUsers();
		return;
	}
	var user_4 = testUsers[4];
	// delete user 4 from users list
	delete testUsers[4];
	// delete user_4 from group[1]
	testGroups[1].deleteExpectedMember(user_4);

	ctrlFlow.series([
		function(next) {userCRUD.deleteUser(authHeader, user_4, next); }
	], tryDeleteNonexistentMembers);

}

function tryDeleteNonexistentMembers(prev_results){
	if (my.resultsHaveErrors(prev_results)) {
		logResults(prev_results,'tryDeleteNonexistentMembers is cancelled');
		deleteAllUsers();
		return;
	}

	ctrlFlow.series([
		function(next) {tryToDeleteNonexistentMemberFromGroup(testUsers[1], testGroups[0], next); }
	], checkMembers);
}

function checkMembers(prev_results) {
	ctrlFlow.series([
		function(next) {groupCRUD.getGroupMembers(authHeader, testGroups[0], next ); },
		function(next) {groupCRUD.getGroupMembers(authHeader, testGroups[1], next ); },
		function(next) {testGroups[0].checkMembers(next); },
		function(next) {testGroups[1].checkMembers(next); }
	], deleteAllUsers);
}

function deleteAllUsers(prev_results) {
	var createCalls = [];
	testUsers.forEach(function(user){
		if (user !== 'undefined')
			createCalls.push(
				function (next) {
					userCRUD.deleteUser(authHeader, user, next);
				}
			);
	});
	ctrlFlow.series(createCalls, deleteGroups);
}

function deleteGroups(prev_results) {

	var createCalls = [];
	testGroups.forEach(function(ums_group){
		createCalls.push(
			function(next) {groupCRUD.deleteGroup(authHeader, ums_group, next )}
		);
	});
	ctrlFlow.series(createCalls, final);
}

function logResults(results, comment) {
	if (typeof comment === 'string')
		console.log(comment+' ',results);
	else
		console.log(results);
}

function final(){};

function tryToCreateExistingGroup(ums_group, callback) {

	const EXPECTED_STATUS = 400;

	frisby.create(my.getStepNumStr()+' Try To Create Existing Group')
		.post( umsUrls.getCreateGroup_Url(),
			ums_group.getCreateGroup_json(),
			{json: true})
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.expectJSON(ums_group.getGroup_json())
		.after(function (err, response, body) {
			console.log(endOfLine + 'Try To Create Existing Group - check response status code and content.');

			expect(body.errors).toBeDefined();

			callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}

function tryToFindNonexistentGroup(ums_group, callback){

	const EXPECTED_STATUS = 404;

	frisby.create(my.getStepNumStr()+' Try To Find Nonexistent Group')
		.get(umsUrls.getGroup_Url( ums_group.getDn() ))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(endOfLine + 'Try to find Nonexistent group - check response status code and message.');
			console.log(body);

			expect(body).toContain('Cannot find group');

			callback( jasmine.getEnv().currentSpec.results().failedCount );
		})
		.toss();
}

function deleteNonexistentGroup(ums_group, callback){

	const EXPECTED_STATUS = 404;

	frisby.create(my.getStepNumStr()+' Try To Delete Nonexistent Group')
		.delete(umsUrls.getGroup_Url( ums_group.getDn() ))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(endOfLine + 'Try to delete Nonexistent group - check response status code and message.');
			//console.log(body);

			expect(body).toContain('Cannot find group');

			//var fCount = jasmine.getEnv().currentSpec.results().failedCount;
			//console.log('Failed Count '+fCount);
			callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}

function tryToDeleteNonexistentMemberFromGroup(ums_user, ums_group, callback){

	const EXPECTED_STATUS = 404;

	frisby.create(my.getStepNumStr()+' Try To Delete Nonexistent Member From Group')
		.get(umsUrls.getDeleteMemberFromGroup_Url( ums_group.getDn(), ums_user.getDn() ))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(endOfLine + 'Try to delete member ' + ums_user.getName() + ' from group ' + ums_group.name + ' - check response status code.');
			console.log(body);

			callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
};