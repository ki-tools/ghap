/**
 * Created by vruzov on 25.09.2015.
 */

var Q = require('q');
var util = require('util');

var ctrlFlow = require('../Common/control_flow');

var my = require('../Common/ghap-lib');
my.stepPrefix = 'ValidateAssign';
my.logModuleName(module.filename);

var allProjects = [];
var prjResources = require('../prj-prov/prj-prov_resources');
var prjRequests = require('../prj-prov/prj-requests');


var umsUser = require('../ums/ums_user');
var umsRequests = require('../ums/ums_requests');
//var testerUser = umsUser.makeUser('PSTester');
var testerUser = umsUser.makeUser('Tester', '2');

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
    .then(umsRequests.validateToken)
    .then(pullUserData)
    .then(getAllProjects)
    .then(runSuite)
    .catch(my.reportError)
    .finally(finalCase);

function pullUserData(){
    return umsRequests.pullUserData(oAuth.header, testerUser,
        null,
        function () {  // on Error
            new Error('Can`t pull user data for '+testerUser.getName());
        }
    );
}

function getAllProjects(){ return prjRequests.getAllProjects(oAuth.header, allProjects) }

function runSuite(){
    var deferred = Q.defer();
    var calls = [];

    var max_num_of_handled_projects = 20;
    var num_of_handled_projects = 0;

    allProjects.forEach(function(prj){
        if (num_of_handled_projects++ < max_num_of_handled_projects)
            calls.push(function (next) {
                console.log("\n\n----------------- Program '%s' -----------------", prj.name)
                prjRequests.grantPermissionsOnProject(oAuth.header, prj, testerUser, ["READ", "WRITE"])
                    .then(function () {
                        return prjRequests.revokeProjectPermissions(oAuth.header, prj, testerUser, ["READ", "WRITE"])
                    })
                    .finally(function () {
                        handleGrants(prj).then(next);
                    })
            })
    });
    ctrlFlow.series(calls, function(){ deferred.resolve(); });
    return deferred.promise;
}

function handleGrants(prj) {
    return prjRequests.getAllGrants(oAuth.header, prj)
        .then(function(){
            var deferred = Q.defer();
            var calls = [];

            console.log("\n---------------- handle %d grants", prj.grants.length);
            prj.grants.forEach(function(grant){
                calls.push(function(next){
                    prjRequests.grantPermissionsOnGrant(oAuth.header, grant, testerUser, ["READ", "WRITE"])
                        .then(function(){
                            prjRequests.revokeGrantPermissions(oAuth.header, grant, testerUser, ["READ", "WRITE"])
                        })
                        .finally(next)
                })
            });

            ctrlFlow.series(calls, function(){ deferred.resolve(); });
            return deferred.promise;
        });

}

function finalCase(){
    console.log('\nTest case finished.');
}
