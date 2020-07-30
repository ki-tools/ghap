/**
 * Created by Vlad on 29.05.2015.
 */

var numOfTesters = 1;

var my = require('./../Common/ghap-lib');
var endOfLine = my.endOfLine;

var ctrlFlow = require('../Common/control_flow');

my.stepPrefix = 'CT';
var umsUrls = require('./../ums/ums_urls');

var umsUser = require('./../ums/ums_user');
var testUsers = [];
var umsRequests = require('./../ums/ums_requests');

var umsRole = require('../ums/ums_role');
var roleCRUD = require('../ums/ums_role_crud');
dataCuratorRole = umsRole.makeRole( 'Data Curator', 'Data Curator description');

var psResources = require('./../ps/ps_resources');
var psRequests = require('./../ps/ps_requests');

for(i = 0; i < numOfTesters; i++)
	testUsers.push(umsUser.makeUser('AutoTester'+i))

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
	.then(createTesters);

function createTesters() {
	var createCalls = [];
	testUsers.forEach(function(ums_user){
		createCalls.push(
			function(next) {umsRequests.createUser(oAuth.header, ums_user, next ); }
		);
		createCalls.push(
			function(next) {
				var tester_storage = psResources.makeStorage(ums_user,500);
				psRequests.createPersonalStorage(oAuth.header, tester_storage, next ); }
		);
		createCalls.push(
			function(next) {roleCRUD.setRoleToUser(oAuth.header, ums_user, dataCuratorRole, next ); }
		);
	});
	ctrlFlow.series(createCalls, function(){console.log('Finished.')});
}
