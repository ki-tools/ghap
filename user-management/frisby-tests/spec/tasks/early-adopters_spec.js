var my = require('../ums/ums_common');
var endOfLine = my.endOfLine;

var frisby = require('frisby');

var ctrlFlow = require('../ums/control_flow');

var useOAuth = true;
var oAuth;
var authHeader = {
	Name: '',
	Value: ''
};

var umsRole = require('../ums/ums_role');
var roleCRUD = require('../ums/ums_role_crud');
dataAnalystRole       = umsRole.makeRole( 'Data Analyst',       'Data Analyst description');
dataCuratorRole       = umsRole.makeRole( 'Data Curator',       'Data Curator description');
bmgfAdministratorRole = umsRole.makeRole( 'BMGF Administrator', 'BMGF Administrator description');

var umsUser = require('./../ums/ums_user');
var userCRUD = require('./../ums/ums_user_crud');
var earlyAdopters = [];
var i=0;
earlyAdopters.push(umsUser.makeUser( 'Sergey',   'Feldman',    'sergey@data-cowboys.com',            '#4$asDF'));
earlyAdopters[i++].addRole(dataAnalystRole);
earlyAdopters.push(umsUser.makeUser( 'Jonathan', 'French',     'jonathanf@metrumrg.com',             '#4$asDF'));
earlyAdopters[i++].addRole(dataAnalystRole);
earlyAdopters.push(umsUser.makeUser( 'Ryan',     'Hafen',      'rhafen@gmail.com',                   '#4$asDF'));
earlyAdopters[i++].addRole(dataAnalystRole);
earlyAdopters.push(umsUser.makeUser( 'Matt',     'Hutmacher',  'matt.hutmacher@a2pg.com',            '#4$asDF'));
earlyAdopters[i++].addRole(dataAnalystRole);
earlyAdopters.push(umsUser.makeUser( 'Modo',     'Modoran',    'amodoran@qinprop.com',               '#4$asDF'));
earlyAdopters[i++].addRole(dataAnalystRole);
earlyAdopters.push(umsUser.makeUser( 'Samer',    'Mouksassi',  'Samer.Mouksassi@certara.com',        '#4$asDF'));
earlyAdopters[i++].addRole(dataAnalystRole);
earlyAdopters.push(umsUser.makeUser( 'Tom',      'Peppard',    'thomas.peppard@gatesfoundation.org', '#4$asDF'));
earlyAdopters[i++].addRole(dataCuratorRole);
earlyAdopters[i].addRole(bmgfAdministratorRole);
earlyAdopters.push(umsUser.makeUser( 'Amy',      'Racine',     'amy.racine@novartis.com',            '#4$asDF'));
earlyAdopters[i++].addRole(dataAnalystRole);
earlyAdopters.push(umsUser.makeUser( 'Louise',   'Ryan',       'louise.m.ryan@uts.edu.au',           '#4$asDF'));
earlyAdopters[i++].addRole(dataAnalystRole);
earlyAdopters.push(umsUser.makeUser( 'Vishak',   'Subramoney', 'vishak.Subramoney@certara.com',      '#4$asDF'));
earlyAdopters[i++].addRole(dataCuratorRole);
earlyAdopters.push(umsUser.makeUser( 'Darren',   'Vengroff',   'vengroff@nextplaylabs.com',          '#4$asDF'));
earlyAdopters[i++].addRole(dataAnalystRole);


// START
(function () {
	if (useOAuth) {
		oAuth = require('./../ums/oauth_spec');
		oAuth.waitAccessToken(runSuiteWithOAuth);
	} else {
		var doAdminSignIn = require('./../ums/ums_AdminSignIn');
		doAdminSignIn(runSuiteWithCookieAuth);
	}
}());

function runSuiteWithCookieAuth(cookie_str) {
	authHeader.Name = 'Cookie';
	authHeader.Value = cookie_str;
	runSuite();
}

function runSuiteWithOAuth(cookie_str) {
	authHeader.Name = 'Authorization';
	authHeader.Value = oAuth.access_token;
	runSuite();
}

function runSuite(){
	my.stepPrefix = 'EA';

	var createRoleCalls = [];
	//Object.keys(adopterRoles).forEach(function(key){
	//	var ums_role = adopterRoles[key];
	//	createRoleCalls.push(
	//		function(next) {roleCRUD.createRole( authHeader, ums_role, next )}
	//	);
	//});
	ctrlFlow.series(createRoleCalls, createEarlyAdopters);

}

function createEarlyAdopters(prev_results) {

	var createCalls = [];
	earlyAdopters.forEach(function(ums_user){
		createCalls.push(
			function(next) {userCRUD.createUser(authHeader, ums_user, next ); }
		);
	});
	ctrlFlow.series(createCalls, setRolesToAdopters);

}

function setRolesToAdopters(prev_results) {
	var createCalls = [];
	earlyAdopters.forEach(function(ums_user){
		ums_user.getRoles().forEach(function(ums_role){
			console.log('User: '+ums_user.getName()+' Role:'+ums_role.name);
			createCalls.push(
				function(next) {roleCRUD.setRoleToUser(authHeader, ums_user, ums_role, next ); }
			);
		})
	});
	ctrlFlow.series(createCalls, final);
}

function final(prev_results) {
	console.log(prev_results)
}