/**
 * Created by Vlad on 12.06.2015.
 */
var my = require('./../Common/ghap-lib');
var endOfLine = my.endOfLine;

var ctrlFlow = require('../Common/control_flow');

my.stepPrefix = 'DT';
var frisby = require('frisby');
var umsUrls = require('./../ums/ums_urls');

var allGhapUsers = [];
var autoTesters = [];
var umsUser = require('./../ums/ums_user');
var umsRequests = require('./../ums/ums_requests');

var allProjects = [];
var programKey = 'COMMON'
var programPrjRes = null;
var prjRequests = require('./../prj-prov/prj-requests');
var prjResources = require('./../prj-prov/prj-prov_resources');

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_service2');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
	.then(runSuite);

function runSuite() {
	ctrlFlow.series([
		function(next) {umsRequests.getAllUsers(oAuth.header, testerAdmin.getParentDn(), allGhapUsers, next); },
		function(next) {
			fillAutoTesters();
			if (autoTesters.length == 0) {
				console.log('No AutoTesters found.');
				final();
			} else
				next(0);
		},
		function(next) { prjRequests.getAllProjects(oAuth.header, allProjects, next); },
		function(next) {
			programPrjRes = prjResources.findProjectByKey(allProjects, programKey);
			if (programPrjRes === null) {
				console.log("'%s' program not found.", programKey);
			} else
				next(0);
		},
		function(next) { prjRequests.getAllGrants(oAuth.header, programPrjRes, next); }
	], function(results) {
		if (my.resultsHaveErrors(results))
			final();
		else
			granPermissionsOnTesters();
	});
}

function fillAutoTesters() {
	var tester_name_pattern = new RegExp(/^AutoTester\d+$/);
	allGhapUsers.forEach( function(ghap_user){
		var res = ghap_user.name.match(tester_name_pattern);
		if (res !== null) {
			var ums_user = umsUser.makeUserFromGhapUser(ghap_user);
			autoTesters.push(ums_user);
		}
	} );
}

function granPermissionsOnTesters(){
	var projectPermissionsCalls = [];
	autoTesters.forEach(function(ums_user){
		projectPermissionsCalls.push(
			function(next) {prjRequests.grantPermissionsOnProject(oAuth.header, programPrjRes, ums_user, ["READ"],
				function(err_count){
					if (err_count == 0) {
						var grantPermissionsCalls = [];
						programPrjRes.grants.forEach(function (grant_res) {
							grantPermissionsCalls.push(
								function (next_grant_call) {
									prjRequests.grantPermissionsOnGrant(oAuth.header, grant_res, ums_user, ["READ"], next_grant_call);
								}
							);
						});
						ctrlFlow.series(grantPermissionsCalls, next)
					}
				});}
		);
	});
	ctrlFlow.series(projectPermissionsCalls, final);
}

function final() {
	console.log('Script finished.')
}