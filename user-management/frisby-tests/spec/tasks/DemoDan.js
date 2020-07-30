var my = require('../ums/ums_common');

var frisby = require('frisby');
frisby.globalSetup({timeout:40000});

var asUrls = require('./../as/as_urls');
var asActivity = require('./../as/as_activity');

var ctrlFlow = require('../ums/control_flow');

var testActivities = [];

testActivities.push(asActivity.makeActivity('Modelling & Analysis via Remote Desktop (Windows OS)'));
testActivities.push(asActivity.makeActivity('Modelling & Analysis via SSH Client (Linux OS)'));

var dataAnalystRole_uuid = '4461F479-F133-4A27-903B-11ADA5A90EFB';
var dataContributorRole_uuid = '6D9066D2-BF15-4EA0-9FCD-7FC9962992D9';

var oAuth = require('../ums/ums_oauth');
oAuth.waitAccessToken(runSuite);

function runSuite() {
	ctrlFlow.series([
		function(next) {CreateActivity( testActivities[0], next ); },
		function(next) {CreateActivity( testActivities[1], next ); },
		function(next) {createActivityRoleAssociation( testActivities[0], dataAnalystRole_uuid, next ); },
		function(next) {createActivityRoleAssociation( testActivities[0], dataContributorRole_uuid, next ); },
		function(next) {createActivityRoleAssociation( testActivities[1], dataAnalystRole_uuid, next ); },
		function(next) {createActivityRoleAssociation( testActivities[1], dataContributorRole_uuid, next ); }
	], function(){});
}

function CreateActivity(as_activity, callback){
	var start = new Date();
	frisby.create(my.getStepNumStr() + ' Create Activity')
		.put( asUrls.getCreateActivity_Url(),
		as_activity.getCreateActivity_json(), {json: true}
	)
		.addHeader(oAuth.header.Name, oAuth.header.Value)
		.expectStatus(200)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Create Activity - check response status code and content.');
			//console.log(body);

			var exec_time_ms = new Date() - start;
			if (exec_time_ms > 5000)
				console.info("WARNING: Execution time is too long: %dms", exec_time_ms);

			if (this.current.response.status == 200) {
				expect(typeof body).toBe('object');
				if (typeof body === 'object') {
					var created_activity = my.jsonParse(body);
					expect(created_activity.id).toBeDefined();
					as_activity.id = created_activity.id
					console.log('Activity '+as_activity.activityName+' is created with id '+as_activity.id);
				}
			}

			callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}

function createActivityRoleAssociation(as_activity, role_uuid, callback){
	var start = new Date();
	frisby.create(my.getStepNumStr() + ' Associate Activity With Role')
		.put( asUrls.getAssociateActivityWithRole_Url(as_activity.activityName, role_uuid	) )
		.addHeader(oAuth.header.Name, oAuth.header.Value)
		.expectStatus(200)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Associate Activity With Role - check response status code and content.');
			console.log(body);

			var exec_time_ms = new Date() - start;
			if (exec_time_ms > 5000)
				console.info("WARNING: Execution time is too long: %dms", exec_time_ms);

			callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}
