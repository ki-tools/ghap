var Q = require('q');

var my = require('../Common/ghap-lib');
my.stepPrefix = 'PrjPrvSrv';
my.logModuleName(module.filename);

var testerUser = require('../ums/tester_admin');
var umsRequests = require('../ums/ums_requests');

var allProjects = [];
var prjResources = require('./prj-prov_resources');
var prjRequests = require('./prj-requests');
var prjName = "TstP" + Date.now();
var testPrj = prjResources.makeProject(prjName, prjName+'KEY', prjName + ' description');
var testGrant = prjResources.makeGrant('Grant1');

var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerUser.getName(), testerUser.getPassword())
    .then(umsRequests.validateToken)
    .then(runSuite)
    .catch(my.reportError)
    .finally(my.finalTest);

function runSuite () {
    return Q()
        .then(function () {
            return prjRequests.createProject(oAuth.header, testPrj);
        })
        .then(function () {
            return prjRequests.createGrant(oAuth.header, testPrj, testGrant);
        })
        .then(function () {
            return prjRequests.getAllProjects(oAuth.header, allProjects);
        })
        .then(function () {
            return prjRequests.getAllGrants(oAuth.header, testPrj);
        })
        .then(function () {
            return prjRequests.deleteGrant(oAuth.header, testGrant);
        })
        .then(function () {
            return prjRequests.deleteProject(oAuth.header, testPrj);
        })
        .then(function () {
            return prjRequests.isFileExistsInStash(oAuth.header, "GHAP Standard Folders.docx");
        });

	/*ctrlFlow.series([
		function(next) {prjRequests.createProject(oAuth.header, testPrj, next); },
		function(next) {prjRequests.createGrant(oAuth.header, testPrj, testGrant, next); },
		function(next) {prjRequests.getAllProjects(oAuth.header, allProjects, next); },
		function(next) {prjRequests.getAllGrants(oAuth.header, testPrj, function(){
			console.log(testPrj.grants);
			next();
		}); },
		function(next) {prjRequests.deleteGrant(oAuth.header, testGrant, next); },
		function(next) {prjRequests.deleteProject(oAuth.header, testPrj, next); }
	], final);*/

}
