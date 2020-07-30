var my = require('../Common/ghap-lib');
my.stepPrefix = 'PsVpg';
my.logModuleName(module.filename);

var cfg = require('./../Common/ghap-config');

var ctrlFlow = require('../Common/control_flow');

var testerUser = require('../ums/tester_admin');
var userRequests = require('./../ums/ums_requests');

var psUrls = require('./ps_urls');
var psResources = require('./ps_resources');
var psRequests = require('./ps_requests');

var allVPGs = [];

var asRequests = require('./../as/as_requests');
var allGhapActivities = [];

var oAuthService = require('./../oauth/oauth_service2');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerUser.getName(), testerUser.getPassword())
	.then(getUserData);

function getUserData() {
	userRequests.pullUserData(oAuth.header, testerUser, runSuite)
}

function runSuite () {
	var testerVPG = psResources.makeVPG(testerUser);

	ctrlFlow.series([
		function(next) {asRequests.getAllActivities(oAuth.header, allGhapActivities, next)},
		//function(next) {console.log(allGhapActivities); next(); },
		function(next) {psRequests.getAllVPGs(oAuth.header, allVPGs, next); },
		//function(next) {console.log(allVPGs); next(); },
		function(next) {psRequests.createVPG(oAuth.header, findLinuxVPGActivity(), testerVPG, next); },
		function(next) {psRequests.getVPG(oAuth.header, testerVPG, next); },
		function(next) {psRequests.terminateVPG(oAuth.header, testerVPG, next); }
	], function(){});
}

function findLinuxVPGActivity() {
    var activityNamePart = 'linux - vpg';
    if (cfg.environment == 'samba') activityNamePart = 'linux virtual private grid';
	for (var i = 0; i < allGhapActivities.length; i++) {
		if (allGhapActivities[i].activityName.toLowerCase().indexOf(activityNamePart) > -1) {
			return allGhapActivities[i]
		}
	}
	return null;
}