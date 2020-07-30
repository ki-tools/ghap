/**
 * Created by Vlad on 23.06.2015.
 */

var my = require('./../Common/ghap-lib');
var ctrlFlow = require('../Common/control_flow');
my.stepPrefix = 'CreateRealUser';
my.logModuleName(module.filename);

var cfg = require('./../Common/ghap-config');

var frisby = require('frisby');
var umsUrls = require('./../ums/ums_urls');

var umsUser = require('./../ums/ums_user');
var umsRequests = require('./../ums/ums_requests');

var umsRole = require('../ums/ums_role');
var roleCRUD = require('../ums/ums_role_crud');
dataAnalystRole = umsRole.makeRole( 'Data Analyst', '');
dataCuratorRole = umsRole.makeRole( 'Data Curator', '');
ghapAdministratorRole = umsRole.makeRole( 'GHAP Administrator', '');

var psResources = require('./../ps/ps_resources');
var psRequests = require('./../ps/ps_requests');

var ghapUsersMap = require('./GHAPUsersOnQA.json');
var ghapUsers = [];
for(i = 0; i < ghapUsersMap.length; i++) {
	if (cfg.environment !== 'prod') ghapUsersMap[i].EmailAddress = 'success@simulator.amazonses.com';
	var ums_user = umsUser.makeUser(ghapUsersMap[i].FirstName, ghapUsersMap[i].LastName,
		ghapUsersMap[i].EmailAddress, '', ghapUsersMap[i].GHAPUsernameCleansed);
	ums_user.roles = ghapUsersMap[i].GHAPRole.split(',');
	ghapUsers.push(ums_user);
}

// uncomment line below this to run the test
// var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_service2');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
	.then(createTesters);

function createTesters() {
	var createCalls = [];

	ghapUsers.forEach(function(ums_user) {
		createCalls.push(
			function (next) {
				umsRequests.createUser(oAuth.header, ums_user,
				function(createUsersErrCount){
					if (createUsersErrCount !== 0)
						next();
					else {
						var tester_storage = psResources.makeStorage(ums_user, 500);
						psRequests.createPersonalStorage(oAuth.header, tester_storage,
							function(createPsErrCount){

								if (ums_user.roles.length === 0) {
									console.log("No roles defined for user %s", ums_user.getName());
									next();
									return;
								}

								ums_user.roles.forEach(function (role_name) {

									switch (role_name.trim()) {

										case 'Data Analyst':
											roleCRUD.setRoleToUser(oAuth.header, ums_user, dataAnalystRole, next);
											break;

										case 'Data Curator':
											roleCRUD.setRoleToUser(oAuth.header, ums_user, dataCuratorRole, next);
											break;

										case 'GHAP Administrator':
											roleCRUD.setRoleToUser(oAuth.header, ums_user, ghapAdministratorRole, next);
											break;

										default :
											console.error("Undefined role '%s' for user '%s'", role_name, ums_user.getName());
											next();
									}

								});

							});
					}
				});
			}
		);

	});

	ctrlFlow.series(createCalls, final);
}

function final(){
	console.log('Script finished');
}
