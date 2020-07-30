var my = require('./../ums/ums_common');

var userCRUD = require('./../ums/ums_user_crud');
var admin_Tester = require('../ums/tester_admin');

var frisby = require('frisby');

var ctrlFlow = require('./../ums/control_flow');

var umsRole = require('./../ums/ums_role');
var roleCRUD = require('./../ums/ums_role_crud');
var dataAnalystRole       = umsRole.makeRole( 'Data Analyst');
var dataCuratorRole       = umsRole.makeRole( 'Data Curator');
var dataContributorRole   = umsRole.makeRole( 'Data Contributor');
var bmgfAdministratorRole = umsRole.makeRole( 'BMGF Administrator');
var ghapAdministratorRole = umsRole.makeRole( 'GHAP Administrator');

//admin_Tester.addRole(dataAnalystRole);
//admin_Tester.addRole(dataCuratorRole);
//admin_Tester.addRole(dataContributorRole);
admin_Tester.addRole(bmgfAdministratorRole);
//admin_Tester.addRole(ghapAdministratorRole);

var umsGroup = require('./../ums/ums_group');
var groupCRUD = require('./../ums/ums_group_crud');
var ghapAdministratorsGroup = umsGroup.create( admin_Tester.getParentDn(), 'GhapAdministrators', '');

//var oAuth = require('../ums/ums_oauth');
//oAuth.waitAccessToken(runSuite);
var oAuth = require('./../oauth/oauth_service');
var testerAdmin = require('./../ums/tester_admin');
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword()).then(runSuite);


function runSuite() {
	ctrlFlow.series([
		//function(next) {userCRUD.createUser(oAuth.header, admin_Tester, next ); },
		//function(next) {roleCRUD.getRole(oAuth.header, dataAnalystRole, next); },
		//function(next) {roleCRUD.getRole(oAuth.header, dataCuratorRole, next); },
		//function(next) {roleCRUD.getRole(oAuth.header, dataContributorRole, next); },
		//function(next) {roleCRUD.getRole(oAuth.header, bmgfAdministratorRole, next); },
		function(next) {roleCRUD.getRole(oAuth.header, ghapAdministratorRole, next); }
	], final);
}

function setRoles(prev_results) {
	if (my.resultsHaveErrors(prev_results)) {
		console.log('setRoles is cancelled.' + prev_results);
		return;
	}

	var setRoleToUserCalls = [];
	admin_Tester.getRoles().forEach(function (ums_role) {
		setRoleToUserCalls.push(
			function (next) {
				roleCRUD.setRoleToUser(oAuth.header, admin_Tester, ums_role, next);
			}
		);
	});
	ctrlFlow.series(setRoleToUserCalls, addUserToGroup);

}

function addUserToGroup(){
	ctrlFlow.series([
		function(next) {groupCRUD.addMemberToGroup(oAuth.header, admin_Tester, ghapAdministratorsGroup, next) }
	], final);
}

function final() {
	console.log('Suite is finished.')
}