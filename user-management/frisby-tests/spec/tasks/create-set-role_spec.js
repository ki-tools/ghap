var my = require('./../ums/ums_common');

var frisby = require('frisby');

var ctrlFlow = require('./../ums/control_flow');

var umsRole = require('./../ums/ums_role');
var roleCRUD = require('./../ums/ums_role_crud');
var ghapAdministratorRole = umsRole.makeRole( 'GHAP Administrator','GHAP Administrator role');

var oAuth = require('./../oauth/oauth_service');
var testerAdmin = require('./../ums/tester_admin');
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword()).then(runSuite);


function runSuite() {
	ctrlFlow.series([
		//function(next) {roleCRUD.createRole(oAuth.header, ghapAdministratorRole, next);},
		function(next) {roleCRUD.getRole(oAuth.header, ghapAdministratorRole, next);},
		//function(next) {roleCRUD.setRoleToUser(oAuth.header, testerAdmin, ghapAdministratorRole, next);}
	], final);
}

function final() {
	console.log('Suite is finished.')
}