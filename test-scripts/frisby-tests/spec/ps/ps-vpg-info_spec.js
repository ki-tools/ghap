/**
 * Created by Vlad on 08.06.2015.
 */
var my = require('../Common/ghap-lib');
my.stepPrefix = 'PsVpgInfo';
my.logModuleName(module.filename);

var frisby = require('frisby');
frisby.globalSetup({timeout:40000});

var ctrlFlow = require('../Common/control_flow');
var myConsole = require('../Common/my_console');

var testerUser = require('../ums/tester_admin');

var userRequests = require('./../ums/ums_requests');

var allGhapUsers = [];

var psResources = require('./ps_resources');
var psRequests = require('./ps_requests');

var allGhapVPG = [];
var allPsVPG =[];

var oAuthService = require('./../oauth/oauth_service2');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerUser.getName(), testerUser.getPassword())
	.then(getData);

function getData() {
	ctrlFlow.series([
		function(next) {psRequests.getAllVPGs(oAuth.header, allGhapVPG, next);},
		function(next) {userRequests.getAllUsers(oAuth.header, testerUser.getParentDn(),allGhapUsers,next); },
		//function(next) {allGhapUsers.forEach( function(user){console.log(user.dn)}); next(); }
	], runSuite);
}

function runSuite () {
	var getVPG_calls = [];
	allGhapVPG.forEach(function(ghap_vpg, index){
		allPsVPG[index] = psResources.makeVPG(ghap_vpg.userId);
		getVPG_calls.push(
			function(next) {
				psRequests.getVPG(oAuth.header, allPsVPG[index], next);
			}
		)
	});
	myConsole.setConsoleLogOff();
	ctrlFlow.series(getVPG_calls, listStatuses);
}

function listStatuses(){
	myConsole.setConsoleLogOn();
	allPsVPG.forEach( function(ps_vpg) {
		var ghap_user = findUser(ps_vpg.userId);
		if (ghap_user === null )
			console.log("User NOT FOUND for VPG '%s' ( user id '%s', status '%s') ", ps_vpg.id, ps_vpg.userId, ps_vpg.status);
		else
			console.log("User '%s' have VPG with status '%s' ",ghap_user.name, ps_vpg.status);
		console.log(ps_vpg.computeResources);
	})
}

function findUser(user_id) {
	var result = null;
	allGhapUsers.forEach(function(ghap_user){
			if (ghap_user.guid === user_id)
			result = ghap_user;
		}
	);
	return result;
}