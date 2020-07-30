/**
 * Created by Vlad on 23.09.2015.
 */

var ctrlFlow = require('../Common/control_flow');

var my = require('./../Common/ghap-lib');
my.stepPrefix = 'SetDummyEmail';
my.logModuleName(module.filename);

var cfg = require('./../Common/ghap-config');

var frisby = require('frisby');
var umsUrls = require('./../ums/ums_urls');

var umsUser = require('./../ums/ums_user');
var umsRequests = require('./../ums/ums_requests');
var dummyEmail = 'success@simulator.amazonses.com';

var ghapUsersMap = require('./GHAPUsersOnQA.json');
var ghapUsers = [];
for(i = 0; i < ghapUsersMap.length; i++) {
	var ums_user = umsUser.makeUser(ghapUsersMap[i].FirstName, ghapUsersMap[i].LastName,
		ghapUsersMap[i].EmailAddress, '', ghapUsersMap[i].GHAPUsernameCleansed);
	ghapUsers.push(ums_user);
}

// uncomment line below this to run the test
//var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
	.then(umsRequests.validateToken)
	.then(runSuite)
	.catch(my.reportError)

function runSuite() {
	var createCalls = [];

	ghapUsers.forEach(function(ums_user) {
		createCalls.push(
			function (next) {
				umsRequests.pullUserData(oAuth.header, ums_user)
			  .then(function(){
						console.log("%s user found",ums_user.getName());
						umsRequests.updateUser(oAuth.header, ums_user, {email:dummyEmail}, next);
					})
			})
	});

	ctrlFlow.series(createCalls, final);

}

function final(){
	console.log('Script finished');
}
