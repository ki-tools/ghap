var my = require('./../Common/ghap-lib');

var umsRequests = require('./../ums/ums_requests');
var testerAdmin = require('../ums/tester_admin');

var frisby = require('frisby');

var ctrlFlow = require('./../Common/control_flow');

var umsRole = require('./../ums/ums_role');
var roleCRUD = require('./../ums/ums_role_crud');
var dataAnalystRole       = umsRole.makeRole( 'Data Analyst');
var dataCuratorRole       = umsRole.makeRole( 'Data Curator');
var dataContributorRole   = umsRole.makeRole( 'Data Contributor');
var bmgfAdministratorRole = umsRole.makeRole( 'BMGF Administrator');
var ghapAdministratorRole = umsRole.makeRole( 'GHAP Administrator');

testerAdmin.addRole(dataAnalystRole);
testerAdmin.addRole(dataCuratorRole);
testerAdmin.addRole(dataContributorRole);
testerAdmin.addRole(bmgfAdministratorRole);
testerAdmin.addRole(ghapAdministratorRole);

var umsGroup = require('./../ums/ums_group');
var groupCRUD = require('./../ums/ums_group_crud');
var ghapAdministratorsGroup = umsGroup.create( testerAdmin.getParentDn(), 'GhapAdministrators', '');

var oAuthService = require('./../oauth/oauth_service2');
var oAuth = oAuthService.makeOAuthClient();
var adminUser = require('./admin_user');
oAuth.login(adminUser.getName(), adminUser.getPassword()).then(runSuite);


function runSuite() {
	ctrlFlow.series([
		function(next) {umsRequests.createUser(oAuth.header, testerAdmin, next ); },
		function(next) {roleCRUD.getRole(oAuth.header, dataAnalystRole, next); },
		function(next) {roleCRUD.getRole(oAuth.header, dataCuratorRole, next); },
		function(next) {roleCRUD.getRole(oAuth.header, dataContributorRole, next); },
		function(next) {roleCRUD.getRole(oAuth.header, bmgfAdministratorRole, next); },
		function(next) {roleCRUD.getRole(oAuth.header, ghapAdministratorRole, next); }
	], setRoles);
}

function setRoles(prev_results) {
	if (my.resultsHaveErrors(prev_results)) {
		console.log('setRoles is cancelled.' + prev_results);
		return;
	}

	var setRoleToUserCalls = [];
	testerAdmin.getRoles().forEach(function (ums_role) {
		setRoleToUserCalls.push(
			function (next) {
				roleCRUD.setRoleToUser(oAuth.header, testerAdmin, ums_role, next);
			}
		);
	});
	ctrlFlow.series(setRoleToUserCalls, addUserToGroup);

}

function addUserToGroup(){
	ctrlFlow.series([
		function(next) {groupCRUD.addMemberToGroup(oAuth.header, testerAdmin, ghapAdministratorsGroup, next) }
	], final);
}

function final() {
	console.log('Suite is finished.')
}