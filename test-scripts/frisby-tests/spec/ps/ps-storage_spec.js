/**
 * Created by Vlad on 12.06.2015.
 */
var my = require('../Common/ghap-lib');
my.stepPrefix = 'PsStorage';
my.logModuleName(module.filename);

var frisby = require('frisby');
frisby.globalSetup({timeout:40000});

var ctrlFlow = require('../Common/control_flow');

var testerAdmin = require('../ums/tester_admin');
var umsResources = require('./../ums/ums_user');
var umsRequests = require('./../ums/ums_requests');

var psResources = require('./ps_resources');
var psRequests = require('./ps_requests');

var psTesterUser;

var oAuthService = require('./../oauth/oauth_service2');
var adminOAuth = oAuthService.makeOAuthClient();
var psTesterOAuth = oAuthService.makeOAuthClient();
adminOAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
	.then(createPSTesterUser);

function createPSTesterUser() {
	psTesterUser = umsResources.makeUser('PStorage','tester','success@simulator.amazonses.com');
	umsRequests.createUser(adminOAuth.header, psTesterUser, function(err_count){
		if (err_count === 0) {
			psTesterOAuth.login(psTesterUser.getName(), psTesterUser.getPassword())
				.then(runSuite);
		} else
			umsRequests.deleteUser(adminOAuth.header, psTesterUser, final);
	})
}

function runSuite () {
	var testerStorage = psResources.makeStorage(psTesterUser,500);

	ctrlFlow.series([
		function(next) {psRequests.createPersonalStorage(adminOAuth.header, testerStorage, next); },
		function(next) {psRequests.getPersonalStorage(adminOAuth.header, testerStorage, next); },
		function(next) {umsRequests.deleteUser(adminOAuth.header, psTesterUser, next); },
		function(next) {psRequests.getPersonalStorage(adminOAuth.header, testerStorage, next); },
		function(next) {psRequests.deletePersonalStorage(adminOAuth.header, testerStorage, next); },
		function(next) {psRequests.getPersonalStorage(adminOAuth.header, testerStorage,
			function () {
				expect(testerStorage.id).toBeNull();
				if (testerStorage.id )
					console.error("Personal storage for user '%s' should be deleted.", psTesterUser.getName());
				next();
			});
		}
	], final);
}

function final(){
	console.log("\nScript '%s' finished.",my.getModuleName(module.filename));
}