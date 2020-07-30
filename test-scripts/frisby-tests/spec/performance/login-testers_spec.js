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
var userRequests = require('./../ums/ums_requests');

var testUsers = [];
var allUsers = [];

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_service2');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
	.then(runSuite);

function runSuite() {
	ctrlFlow.series([
		function(next) {userRequests.getAllUsers(oAuth.header, testerAdmin.getParentDn(), allUsers, next); }
	], loginTesters);
}

function loginTesters(){
	var testers = [];
	var tester_name_pattern = new RegExp(/^AutoTester\d+$/);
	allUsers.forEach( function(ums_user){
		var res = ums_user.name.match(tester_name_pattern);
		if (res !== null) {
			testUsers.push(umsUser.makeUser(ums_user.name));
		}
	} );

	var loginCalls = [];
	testUsers.forEach(function(ums_user){
		loginCalls.push(
			function(next) {
				oAuth.login(ums_user.getName(), ums_user.getPassword()).then(next);
			}
		);
	});
	ctrlFlow.series(loginCalls, function(){console.log('Expect responses.')});
}