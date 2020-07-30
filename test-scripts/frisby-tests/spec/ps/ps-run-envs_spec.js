/**
 * Created by Vlad on 07.08.2015.
 */

var Q = require('q');

var my = require('../Common/ghap-lib');
my.stepPrefix = 'RunhMultiEnv';
my.logModuleName(module.filename);

var umsRequests = require('../ums/ums_requests');

var umsRole = require('../ums/ums_role');
var roleRequests = require('../ums/ums_role_crud');
var allRoles = [];
var dataAnalystRole = null;

var asActivity = require('../as/as_activity');
var asRequests = require('../as/as_requests');
var dataAnalystActivities = [];
var linuxHostActivity = null;
var linuxVPGActivity = null;
var winActivity = null;

var psRequests = require('./ps_requests');
var psResources = require('./ps_resources');
var Tester = require('./ps_tester').make();
var testerVPGs = [];
var psUtil = require('./ps-terminate-envs');

var ctrlFlow = require('../Common/control_flow');

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
var adminOAuth = oAuthService.makeOAuthClient();
adminOAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
	.then(umsRequests.validateToken)
	.then(startSuite)
	.catch(my.reportError);

function startSuite(){
	return roleRequests.getAllRoles(adminOAuth.header, allRoles)
		.then(findDataAnalystRole)
		.then(setTesterAndRun)
}

function findDataAnalystRole(){
	var deferred = Q.defer();
	describe('Check if Data Analyst role defined.', function(){
		dataAnalystRole = umsRole.findRoleByName(allRoles, 'Data Analyst');
		it('The role should be present in allRoles array.', function(){
			console.log(my.endOfLine+this.getFullName());
			expect(dataAnalystRole).not.toBeNull();
		});
		if (dataAnalystRole) deferred.resolve(); else	deferred.reject();
	});
	return deferred.promise;
}

function setTesterAndRun() {

	umsRequests.pullUserData(adminOAuth.header, Tester,

		// Tester user exists
		function(){
			console.log();
			oAuth.login(Tester.getName(), Tester.getPassword())
				.then(umsRequests.validateToken)
				.then(runSuite);
		},

		// Tester user missed
		function(response_status){

			if (response_status !== 404) return false;  // indicate that error do not handled

			var tester_password = Tester.getPassword();
			Tester.setPassword('');

			umsRequests.createUser(adminOAuth.header, Tester)
				.then( function(){return roleRequests.setRoleToUser(adminOAuth.header, Tester, dataAnalystRole)} )
				.then( function(){
					var testerStorage = psResources.makeStorage(Tester, 500);
					return psRequests.createPersonalStorage(adminOAuth.header, testerStorage)
				})
				.then( function(){
					return oAuth.login(Tester.getName(), Tester.getPassword())
				})
				.then(function(){
					return umsRequests.resetUserPassword(oAuth.header, Tester, tester_password)
				})
				.then(function(){
					Tester.setPassword(tester_password);
					console.log();
					return oAuth.login(Tester.getName(), Tester.getPassword());
				})
				.then(runSuite)
				.catch(my.reportError);

			return true;  // indicate that error is handled
		}
	);
}

function runSuite(){

	umsRequests.getUserRoles(oAuth.header, Tester)
		.then(validateTesterRole)
		.then( function(){return asRequests.getARAssociationsForRole(oAuth.header, dataAnalystRole)} )
		.then(fillDataAnalystActivities)
		.then( function(){return psUtil.terminateEnvironments(oAuth.header, Tester)})
		.then(launchWinCE)
		.then(launchLinuxSshCE)
		.then(waitVPGsRunningStatus)
		.catch(my.reportError)
		.finally(my.finalTest);

}

function validateTesterRole(){
	var deferred = Q.defer();
	describe('Validate Tester role.', function(){
		it('Tester should have 1 role. The role should be Data Analyst.', function(){
			console.log(my.endOfLine+this.getFullName());
			var tester_roles = Tester.getRoles();
			expect(tester_roles.length).toEqual(1);
			if (tester_roles.length === 1)
				expect(tester_roles[0].name).toEqual('Data Analyst');
			var err_count = jasmine.getEnv().currentSpec.results().failedCount;
			if (err_count === 0) deferred.resolve(); else deferred.reject(err_count);
			waits(10);
		});
	});
	return deferred.promise;
}

function fillDataAnalystActivities(){
	var deferred = Q.defer();
	var activity = asActivity.makeActivity('');
	var calls = [];
	dataAnalystRole.ar_associations.forEach( function(ar){
		calls.push(
			asRequests.getActivityById(oAuth.header, ar.activityId, activity)
				.then(function(){
					var activity_clone = my.cloneObject(activity);
					dataAnalystActivities.push(activity_clone);
					var activity_name = activity_clone.activityName.toUpperCase();
					if (activity_name.indexOf('SSH CLIENT') > -1) linuxHostActivity = activity_clone;
					else if (activity_name.indexOf('LINUX ANALYSIS') > -1) linuxHostActivity = activity_clone;  // for samba.ghap.io
					else if (activity_name.indexOf('- LINUX HOST') > -1) linuxHostActivity = activity_clone;  // for production
					else if (activity_name.indexOf('LINUX VIRTUAL') > -1) linuxVPGActivity = activity_clone;
					else if (activity_name.indexOf('WINDOWS') > -1) winActivity = activity_clone;
					else console.error("Unknown activity '%s' for 'Data Analyst' role.", activity_name);
				})
		)
	});
	Q.all(calls)
		.then(function(){
			if ( linuxHostActivity && linuxVPGActivity && winActivity)
				deferred.resolve();
			else
				deferred.reject(new Error('One or more of expected Linux or Windows activity not found.'));
		})
		.catch(function() {	deferred.reject( new Error('Can\'t get activities for DataAnalyst role') ) });

	return deferred.promise;
}

function launchWinCE(){
	var vpg = psResources.makeVPG(Tester);
	return psRequests.multiVpgCreateStack(oAuth.header, winActivity, vpg)
}

function launchLinuxSshCE(){
	var vpg = psResources.makeVPG(Tester);
	return psRequests.multiVpgCreateStack(oAuth.header, linuxHostActivity, vpg)
}

/**
 *
 * @returns {Promise<T>}
 */
function waitVPGsRunningStatus(){
	var deferred = Q.defer();
	const DELAY_BETWEEN_ATTEMPTS = 30 * 1000;
	const MAX_EXEC_TIME_MS = 20 * 60 * 1000;
	var start = new Date();
	var exec_time_ms;

	function check() {
		psRequests.multiVpgGetStatuses4User(oAuth.header, Tester, testerVPGs)
			.then(function(){
				describe("Check VPGs 'running' statuses", function(){

					var allStatusesIsRunning = true;
					var invalidStatusPresent = false;
					it('- tester should have 2 active VPGs', function(){
						console.log(this.getFullName());
						expect(testerVPGs.length).toBe(2);

						testerVPGs.forEach(function(vpg){
							var activity = my.findElementInArray(dataAnalystActivities, 'id', vpg.activityId);
							if (vpg.computeResources.length) {
								console.log("%s environment has %d nodes.", activity.activityName, vpg.computeResources.length);
								vpg.computeResources.forEach(function (comp_res) {
									console.log("   '%s' node has '%s' status.", comp_res.instanceOsType, comp_res.status);
									allStatusesIsRunning = allStatusesIsRunning && (comp_res.status === 'running');
									invalidStatusPresent = invalidStatusPresent || (comp_res.status === 'terminated');
								});
							} else {
								console.log("%s environment has no nodes.", activity.activityName);
								allStatusesIsRunning = false;
							}
						});

					});

					if (testerVPGs.length !== 2) {
						deferred.reject(new Error('Required VPG missed in waitVPGsRunningStatus.'));
						return;
					}

					it('- terminated status should no happen', function(){
						console.log(this.getFullName());
						expect(invalidStatusPresent).toBeFalsy();
						if (invalidStatusPresent) {
							console.error("One of tester`s node have 'terminated' status.");
							deferred.reject(new Error('waitVPGsRunningStatus invalid node status detected.'));
						}
					});

					it('- awaiting time should be less than '+MAX_EXEC_TIME_MS/1000/60+' minutes', function(){
						console.log(this.getFullName());
						exec_time_ms = new Date() - start;
						console.log("Awaiting time is %s", my.logTime(exec_time_ms));

						if (invalidStatusPresent) return;

						if (allStatusesIsRunning) {
							console.log("\nAll environments got 'running' status. Awaiting time was %s.", my.logTime(exec_time_ms));
							deferred.resolve();
							return;
						}

						expect(exec_time_ms).toBeLessThan(MAX_EXEC_TIME_MS);

						if (exec_time_ms < MAX_EXEC_TIME_MS) {
							console.log("Pause for %d seconds...",DELAY_BETWEEN_ATTEMPTS/1000);
							waits(DELAY_BETWEEN_ATTEMPTS);
							check();
						}
						else {
							console.error("\nAwaiting time for 'running' statuses exceeded %d minutes", MAX_EXEC_TIME_MS / 1000 / 60);
							deferred.reject(new Error('waitVPGsRunningStatus timeout.'));
						}
					})

				});

			})
	}

	check();

	return deferred.promise;
}
