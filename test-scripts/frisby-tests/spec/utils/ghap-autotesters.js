/**
 * Created by Vlad on 12.06.2015.
 */

var ctrlFlow = require('../Common/control_flow');
var oAuthService = require('./ghap-oauth');
var umService = require('./ghap-usermanagement-service');
var umsUser = require('../ums/ums_user');

var testerAdmin = require('../ums/tester_admin');

function AutoTesters() {
	this.allGhapUsers = [];
	this.usersMap = [];
	this.loggingIsFinished = false;
	this.doAfterLogin = null;

	return this;
}

AutoTesters.prototype.login = function(){
	var self = this;
	if (self.usersMap.length === 0)
		self.getAutoTesters( function () {
			loginTesters(self)
		});
	else loginTesters(self);
	return self;
};

AutoTesters.prototype.then = function(do_after) {
	this.doAfterLogin = do_after;
	if (this.loggingIsFinished && (this.doAfterLogin !== null))	this.doAfterLogin();
};

AutoTesters.prototype.getAutoTesters = function(do_after){
	var self = this;
	var oAuth = oAuthService.makeOAuthClient();
	oAuth.login( testerAdmin.getName(), testerAdmin.getPassword())
		.then(function () {
			umService.getAllUsers( oAuth.header, testerAdmin.getParentDn(), self.allGhapUsers, function (result) {
				if (result.is_successful) {
					fillUsersMap(self);
					do_after();
				}
			});
		});
};

function fillUsersMap(auto_testers) {
	var tester_name_pattern = new RegExp(/^AutoTester\d+$/);
	auto_testers.allGhapUsers.forEach( function(ghap_user){
		var res = ghap_user.name.match(tester_name_pattern);
		if (res !== null) {
			var ums_user = umsUser.makeUserFromGhapUser(ghap_user);
			auto_testers.usersMap.push(
				{
					name: ghap_user.name,
					oAuth: oAuthService.makeOAuthClient(),
					umsUser: ums_user
				});
		}
	} );
	console.log("%d AutoTesters found.",auto_testers.usersMap.length);
}

function loginTesters(auto_testers){
	var loginCalls = [];
	auto_testers.usersMap.forEach(function(tester_map){
		loginCalls.push(
			function(next) {
				tester_map.oAuth.login(tester_map.umsUser.getName(), tester_map.umsUser.getPassword()).then(next);
			}
		);
	});
	ctrlFlow.fullParallel(loginCalls, function(){
		console.log('All authorization requests are finished.');
		auto_testers.loggingIsFinished = true;
		if (auto_testers.doAfterLogin !== null)	auto_testers.doAfterLogin();
	});
}

module.exports = new AutoTesters();