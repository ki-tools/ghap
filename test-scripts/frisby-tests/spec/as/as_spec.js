var my = require('../Common/ghap-lib');
my.stepPrefix = 'ActivitySrv';
my.logModuleName(module.filename);

var umsRole = require('../ums/ums_role');
var roleCRUD = require('../ums/ums_role_crud');

var asActivity = require('./as_activity');
var asRequests = require('./as_requests');

var ctrlFlow = require('../Common/control_flow');

var allActivities = [];
var allRoles = [];
var dataAnalystRole;

var testActivities = [];
testActivities.push(asActivity.makeActivity('Testing & Analysis. Test activity.'));
testActivities.push(asActivity.makeActivity(''));

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_service2');
oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
	.then(runSuite);

function runSuite(){
	roleCRUD.getAllRoles(oAuth.header, allRoles, function(err_count){
		if (err_count === 0){
			dataAnalystRole = umsRole.findRoleByName(allRoles, 'Data Analyst');
			expect(dataAnalystRole).not.toBeNull();
			if (dataAnalystRole !== null) runAS_tests();
		}
	});
}

function runAS_tests() {

	ctrlFlow.series([
		function (next) {asRequests.getARAssociationsForRole(oAuth.header, dataAnalystRole, next);},
		function(next) {asRequests.getActivityById(oAuth.header,dataAnalystRole.ar_associations[0].activityId, testActivities[1], next) },
		function(next) {asRequests.createActivity(oAuth.header, testActivities[0], next ); },
		function(next) {asRequests.getActivityByName(oAuth.header, testActivities[0].activityName, testActivities[1], next ); },
		function(next) {asRequests.getAllActivities(oAuth.header, allActivities, next ); },
		function(next) {asRequests.deleteActivity( oAuth.header, testActivities[0], next ); }
	], function () {
		console.log(allActivities)
	});
}