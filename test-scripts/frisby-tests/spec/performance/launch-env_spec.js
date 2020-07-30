/**
 * Created by Vlad on 08.06.2015.
 */

var my = require('./../Common/ghap-lib');
var endOfLine = my.endOfLine;

var ctrlFlow = require('../Common/control_flow');

my.stepPrefix = 'LE';
var frisby = require('frisby');

var umsUser = require('./../ums/ums_user');
var umsRequests = require('./../ums/ums_requests');

var allGhapUsers = [];
var testUsersMap = [];

var psResources = require('./../ps/ps_resources');
var psRequests = require('./../ps/ps_requests');

var asRequests = require('./../as/as_requests');
var allGhapActivities = [];

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_service2');
var oAuth = oAuthService.makeOAuthClient();

var util = require('util');
var log_stdout = process.stdout;
var consoleLogIsOn = true;
console.log = function() {
	if (consoleLogIsOn)
		log_stdout.write(util.format.apply(this,arguments) + '\n');
};
function setConsoleLogOn(){
	consoleLogIsOn = true;
	console.log('Console.log switched ON.')
}
function setConsoleLogOff(){
	consoleLogIsOn = true;
	console.log('Console.log switched OFF.');
	consoleLogIsOn = false;
}

var StartTime = new Date();
my.setLongExecutionInterval(60000);

oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
	.then(runSuite);

function runSuite() {
	ctrlFlow.series([
		function(next) {umsRequests.getAllUsers(oAuth.header, testerAdmin.getParentDn(), allGhapUsers, next); },
		function(next) {fillTestUsersMap(next); },
		function(next) {asRequests.getAllActivities(oAuth.header, allGhapActivities, next)},
		//function(next) {console.log(allGhapActivities); next(); },
	], loginTesters);
}

function fillTestUsersMap(callback) {
	var tester_name_pattern = new RegExp(/^AutoTester\d+$/);
	allGhapUsers.forEach( function(ghap_user){
		var res = ghap_user.name.match(tester_name_pattern);
		if (res !== null) {
			var ums_user = umsUser.makeUserFromGhapUser(ghap_user);
			testUsersMap.push(
				{
					oAuth: oAuthService.makeOAuthClient(),
					user: ums_user,
					psVpg: psResources.makeVPG(ums_user)
				});
		}
	} );
	callback();
}

function loginTesters(){
	var loginCalls = [];
	testUsersMap.forEach(function(ums_user_map){
		loginCalls.push(
			function(next) {
				ums_user_map.oAuth.login(ums_user_map.user.getName(), ums_user_map.user.getPassword()).then(next);
			}
		);
	});
	ctrlFlow.fullParallel(loginCalls, function(results){
		console.log('All authorization requests are finished.');
		//console.log(results);
		getVPGstates(terminateActiveVPGrids);
	});
}

function getVPGstates(callback){
	var getVPG_calls =[];
	testUsersMap.forEach(function(ums_user_map){
		getVPG_calls.push(
			function(next) {
				psRequests.getVPG(oAuth.header, ums_user_map.psVpg, next);
			}
		);
	});
	console.log("Get VPG statuses for %d users. Please wait.",testUsersMap.length);
	setConsoleLogOff();
	ctrlFlow.fullParallel(getVPG_calls, function(){
		setConsoleLogOn();
		callback();
	});
}

function terminateActiveVPGrids(){
	var terminateVPG_calls =[];
	var terminate_ON = true;
	testUsersMap.forEach(function(ums_user_map){
		if (terminate_ON && ums_user_map.psVpg.status)
			terminateVPG_calls.push(
				function(next) {
					psRequests.terminateVPG(oAuth.header, ums_user_map.psVpg, next);
				});
	});
	terminateVPG_calls.push( function(next) {	next();	})

	ctrlFlow.fullParallel(terminateVPG_calls, function(results){
		if (terminateVPG_calls.length === 1) {
			console.log('No active VPG for AutoTesters was found.');
			createVPGrids();
		}	else {
			console.log('All active VPG for AutoTesters are terminated.');
			final();
		}
	});
}

function createVPGrids(results){

	var createVPGCalls = [];
	var create_ON = true;
	testUsersMap.forEach(function(ums_user_map){
		if (create_ON)
		createVPGCalls.push(
			function(next) {
				psRequests.createVPG(oAuth.header, allGhapActivities[1], ums_user_map.psVpg, next);
			});
	});
	createVPGCalls.push( function(next){ next(); });
	ctrlFlow.fullParallel(createVPGCalls, waitCompleteStatuses);
}

var statusesIsChecked;
function waitCompleteStatuses(){
	//frisby.create('test')
	//	.get('http://google.com')
	//	.after(function(){
	//		checkCompleteStatus();
	//		waitsFor(function(){return statusesIsChecked}, "Checking of VPG statuses is not complete in 60 sec.", 60000);
	//	})
	//.toss();
	//describe("Check of VPG statuses", function(){
	//	it("VPG checking should finished in time", function(){
	//		checkCompleteStatus();
	//		waitsFor(function(){return statusesIsChecked}, "Checking of VPG statuses is not complete in 60 sec.", 60000);
	//	})
	//})
	checkCompleteStatus();
}

function checkCompleteStatus(){
	//statusesIsChecked = false;
	getVPGstates(checkVPGstates);
}

var checkStatesAttemptsCount=0;
const maxCheckStatesAttempts = 40;
const checkStatesAttemptsDelay = 15000;

function checkVPGstates(){
	var allVPG_IsCreated = true;

	var exec_time_ms = new Date() - StartTime;
	console.log("Execution time %dms. VPG statuses for AutoTesters:",exec_time_ms);
	testUsersMap.forEach(function(ums_user_map){
		var user_vpg = ums_user_map.psVpg;
		var node_status = 'not defined';
		if ( (user_vpg.computeResources.length === 1) && user_vpg.computeResources[0].hasOwnProperty('status'))
			node_status = user_vpg.computeResources[0].status;
		console.log("For user '%s' VPG '%s' status is '%s'. Node status is %s.", ums_user_map.user.getName(), user_vpg.id, user_vpg.status, node_status);
		allVPG_IsCreated = allVPG_IsCreated && (node_status == 'running')
	});
	if (allVPG_IsCreated)
		final();
	else {
		if (++checkStatesAttemptsCount > maxCheckStatesAttempts-1){
			final()
		}
		else {
			console.log('Pause. Will try to check VPG statuses after %d seconds',checkStatesAttemptsDelay/1000);

			describe("Check of VPG statuses", function(){
				statusesIsChecked = false;
				it("VPG checking should finished in time", function(){
					runs(function(){
						setTimeout( function() {
							statusesIsChecked = true;
							waitCompleteStatuses();
						}, checkStatesAttemptsDelay);
					});
					waitsFor(function(){return statusesIsChecked}, "Checking of VPG statuses is not complete in 60 sec.", checkStatesAttemptsDelay+1000);
				})
			})
		}
	}
}

function final(){
	statusesIsChecked = true;
	console.log('Script finished.')
}