var my = require('../ums/ums_common');

var frisby = require('frisby');
frisby.globalSetup({timeout:40000});

var umsRole = require('../ums/ums_role');
var roleCRUD = require('../ums/ums_role_crud');

var asActivity = require('./as_activity');
var asRequests = require('./as_requests');

var ctrlFlow = require('../ums/control_flow');

var allActivities = [];

var testActivities = [];
testActivities.push(asActivity.makeActivity('Testing & Analysis. Instance 1'));
testActivities.push(asActivity.makeActivity(''));

var dataAnalystRole = umsRole.makeRole('Data Analyst');

//var oAuth = require('../ums/ums_oauth');
//oAuth.waitAccessToken(runSuite);

var testerUser = require('../ums/tester_admin');
var oAuth = require('./../oauth/oauth_service2');
oAuth.login(testerUser.getName(), testerUser.getPassword())
	.then(runSuite);

function runSuite() {
	ctrlFlow.series([
		function(next) {roleCRUD.getRole(oAuth.header, dataAnalystRole, next); },
		function(next) {asRequests.getARAssociationsForRole(oAuth.header, dataAnalystRole, next); },
		//function(next) {asRequests.getActivityById(oAuth.header,dataAnalystRole.ar_associations[0].activityId, testActivities[1], next) },
		function(next) {asRequests.createActivity(oAuth.header, testActivities[0], next ); },
		function(next) {asRequests.getActivityByName(oAuth.header, testActivities[0].activityName, testActivities[1], next ); },
		function(next) {asRequests.getAllActivities(oAuth.header, allActivities, next ); },
		function(next) {asRequests.deleteActivity( oAuth.header, testActivities[0], next ); }
	], function(){});

}

function findActivityIdInAllActivities (as_activity) {
	if (!as_activity.id){
		// is empty
		var filtered_activities = allActivities.filter(function(activity){
			return (activity.activityName == as_activity.activityName)
		});
		expect(filtered_activities.length).toBe(1);
		if (filtered_activities.length === 1) {
			as_activity.id = filtered_activities[0].id;
			console.log(as_activity.activityName + ' activity is found. ID = ' + as_activity.id)
		}
		else return false;
	}
	return true;
}
