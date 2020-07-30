/**
 * Created by Vlad on 29.05.2015.
 */
var my = require('./../Common/ghap-lib');
var endOfLine = my.endOfLine;

var ctrlFlow = require('../Common/control_flow');

my.stepPrefix = 'DT';
var frisby = require('frisby');
var umsUrls = require('./../ums/ums_urls');

var umsUser = require('./../ums/ums_user');
var umsRequests = require('./../ums/ums_requests');
var testUsers = [];
var checkDeleteListOnly = true;

var allGhapUsers = [];

var psResources = require('./../ps/ps_resources');
var psRequests = require('./../ps/ps_requests');

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_service2');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
	.then(runSuite);

function runSuite() {
	ctrlFlow.series([
		function(next) {umsRequests.getAllUsers(oAuth.header, testerAdmin.getParentDn(), allGhapUsers, next); }
	], deleteTesters);
}

function deleteTesters(){
	var testers = [];
	// var tester_name_pattern = new RegExp(/^autotester\d+$/);
	// var tester_name_pattern = new RegExp(/^(A|B)\d$/i);
	var tester_name_pattern = new RegExp(/^a\.0322$/i);
	checkDeleteListOnly = false;
	allGhapUsers.forEach( function(ghap_user){
		var res = ghap_user.name.match(tester_name_pattern);
		if (res !== null) {
			testUsers.push(umsUser.makeUserFromGhapUser(ghap_user))
		}
	} );

	console.log();
	if (testUsers.length === 0){
		console.log("No users found matching RegExp pattern '%s'", tester_name_pattern.source);
	}

	var deleteCalls = [];
	testUsers.forEach(function(ums_user){
		console.log("Delete user '%s' with GUID '%s'", ums_user.getName(), ums_user.getGuid());
		deleteCalls.push(
			function (next) {
				var personal_storage = psResources.makeStorage(ums_user, 500);
				psRequests.getPersonalStorage(oAuth.header, personal_storage, function () {
					// if (personal_storage.id){
					// 	psRequests.deletePersonalStorage(oAuth.header, personal_storage);
					// }
					next();
				})
			},
			function(next) {
				//umsRequests.deleteUser(oAuth.header, ums_user, next );
				next();
			}
		);
	});
	if (!checkDeleteListOnly)
		ctrlFlow.series(deleteCalls, function(){console.log("\nFinished.")});
}