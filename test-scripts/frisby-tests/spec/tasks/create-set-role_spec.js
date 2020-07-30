var my = require('./../Common/ghap-lib');

var frisby = require('frisby');

var ctrlFlow = require('./../Common/control_flow');

var umsRole = require('./../ums/ums_role');
var roleCRUD = require('./../ums/ums_role_crud');
var ghapAdministratorRole = umsRole.makeRole( 'GHAP Administrator','GHAP Administrator role');

var oAuthService = require('./../oauth/oauth_service2');
var oAuth = oAuthService.makeOAuthClient();
var testerAdmin = require('./../ums/tester_admin');
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword()).then(runSuite);


function runSuite() {
	roleCRUD.getRole(oAuth.header, ghapAdministratorRole, final);
	//ctrlFlow.series([
	//	//function(next) {roleCRUD.createRole(oAuth.header, ghapAdministratorRole, next);},
	//	function(next) {roleCRUD.getRole(oAuth.header, ghapAdministratorRole, next);},
	//	//function(next) {roleCRUD.setRoleToUser(oAuth.header, adminUser, ghapAdministratorRole, next);}
	//], final);
}

function final(err_count) {
	console.log("Suite is finished. Errors in last test: %s", err_count);
}