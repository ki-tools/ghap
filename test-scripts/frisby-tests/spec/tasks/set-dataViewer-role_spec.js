/**
 * Created by Vlad on 28.09.2015.
 */

var Q = require('q');
var ctrlFlow = require('../Common/control_flow');

var my = require('./../Common/ghap-lib');
my.stepPrefix = 'SetDataViewerRole';
my.logModuleName(module.filename);

var cfg = require('./../Common/ghap-config');

var prjResources = require('./../prj-prov/prj-prov_resources');
var prjRequests = require('./../prj-prov/prj-requests');
var allProjects = [];
var hbgdProject = null;
var hbgdCommonGrant = null;
var hbgdCommonGrantUsers = [];

var umsUser = require('./../ums/ums_user');
var umsRequests = require('./../ums/ums_requests');
var umsRole = require('./../ums/ums_role');
var roleRequests = require('./../ums/ums_role_crud');
var allGhapUsers = [];
var dataViewerRole = umsRole.makeRole('Data Viewer');

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
    .then(umsRequests.validateToken)
    .then(runSuite)
    .catch(my.reportError)
    .finally(final);

function runSuite() {
    return umsRequests.getAllUsers(oAuth.header, testerAdmin.getParentDn(), allGhapUsers)
        .then(function(){
            return prjRequests.getAllProjects(oAuth.header, allProjects)
        })
        .then(function(){
           hbgdProject = prjResources.findProjectByKey(allProjects, "HBGD");
            if ( hbgdProject ) {
                return prjRequests.getAllGrants(oAuth.header,hbgdProject)
            } else {
                throw Error("HBGD project not found.")
            }
        })
        .then(function(){
            hbgdCommonGrant = prjResources.findGrantByName(hbgdProject.grants, 'Common');
            if (hbgdCommonGrant) {
                console.log("\nHBGD/Common grant id '%s'", hbgdCommonGrant.id)
            } else {
                throw Error("HBGD/Common grant not found.")
            }
        })
        .then(function(){
            return prjRequests.getAllUsers4Grant(oAuth.header,hbgdCommonGrant, hbgdCommonGrantUsers)
        })
        .then(function(){
            var deferred = Q.defer();
            var calls = [];
            var users = [];
            console.log("\n%d users have permissions for grant '%s'", hbgdCommonGrantUsers.length, hbgdCommonGrant.name)
            for (var i=0; i<hbgdCommonGrantUsers.length; i++) {
                var ghap_user = my.findElementInArray(allGhapUsers, 'guid', hbgdCommonGrantUsers[i].guid);
                if (ghap_user) {
                    var ums_user = umsUser.makeUserFromGhapUser(ghap_user);
                    console.log("Set 'Data Viewer' role to user '%s'.\t\t(HBGD/Common grant permissions '%s').",
                        ums_user.getName(), hbgdCommonGrantUsers[i].permissions.toString());
                    users.push(ums_user);
                } else {
                    console.error("Can`t find user with guid '%s'", hbgdCommonGrantUsers[i].guid)
                }
            }
            users.forEach(function(ums_user){
               calls.push(function(next){
                   roleRequests.setRoleToUser(oAuth.header, ums_user, dataViewerRole).finally(next);
               })
            });
            ctrlFlow.series(calls, function(){
                deferred.resolve( waits(300) )
            });
            return deferred.promise;
        })
}

function final(){
    console.log('Script finished');
}