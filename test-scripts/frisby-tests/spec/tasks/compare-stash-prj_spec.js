/**
 * Created by Vlad on 03.11.2015.
 */

var Q = require('q');
var util = require('util');

var my = require('../Common/ghap-lib');
my.stepPrefix = 'StashPrjCompare';
my.logModuleName(module.filename);

var lib = require('./compare-stash-prj-lib');
var ctrlFlow = require('../Common/control_flow');

var allGhapUsers = [];
var umsRequests = require('../ums/ums_requests');
var umsUser = require('../ums/ums_user');

var allStashProjects = [];
var allGhapProjects = [];
var prjRequests = require('../prj-prov/prj-requests');

var limitUsers = 200;
var limitProjects = 200;

var stashApi = require('./get-stash-permissions');
var ghapApi = require('./get-ghap-permissions');

/**
 * @type {UmsUser}
 */
var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_promise');
var adminOAuth = oAuthService.makeOAuthClient();
adminOAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
    .then(umsRequests.validateToken)
    .then(runSuite)
    .catch(my.reportError);

function runSuite() {
    console.log('---------------- start projects handling --------------------');
    return prjRequests.getAllStashProjects(adminOAuth.header, allStashProjects)
        .then(function(){
            console.log(" %d projects received from STASH.", allStashProjects.length);
            allStashProjects = allStashProjects.map(function(stash_prj_obj){
               return {
                   "key" : stash_prj_obj.key,
                   "name" : stash_prj_obj.name
               }
            });
            allStashProjects.sort(function(a, b){
                if (a.key < b.key) return -1;
                return 1;
            })
        })
        .then(function(){
            return prjRequests.getAllProjects(adminOAuth.header, allGhapProjects)
        })
        .then(function(){
            console.log(' %d projects received from Projects DB.', allGhapProjects.length);
            allGhapProjects.sort(function(a, b){
                if (a.key < b.key) return -1;
                return 1;
            })
        })
        .then(compareProjectsLists)
        .then(function(){
            console.log('%d projects should be handled', allGhapProjects.length);
            if (limitProjects < allGhapProjects.length) {
                console.log("Limit to %d users.", limitUsers);
                allGhapProjects.length = limitProjects;
                allStashProjects.length = limitProjects;
            }
        })
        .then(function(){
            console.log('---------------- get grants for all projects in Stash  ----------------------------');
            return stashApi.getGrants4AllStashProjects(adminOAuth.header, allStashProjects)
        })
        .then(function(){
            console.log('---------------- get grants for all programs in Project service table --------------------');
            return ghapApi.getGrants4AllGhapProjects(adminOAuth.header, allGhapProjects)
        })
        .then(compareGrantsLists)
        .then(reportResultsOfProjectsAndGrantsListsComparison)
        .then(function(){
            console.log('---------------- get STASH permissions ----------------------------');
            return stashApi.getStashPermissions(adminOAuth.header, allStashProjects)
                //.then(function(){
                //    console.log();
                //    my.log(allStashProjects);
                //    process.exit(0);
                //})
        })
        .then(function(){
            console.log('\n-------------------- get GHAP permissions --------------------');
            return umsRequests.getAllUsers(adminOAuth.header, testerAdmin.getParentDn(), allGhapUsers)
                .then(function(){
                    console.log(" %d users found.", allGhapUsers.length);
                    if (limitUsers < allGhapUsers.length) {
                        console.log("Limit processing to %d users.", limitUsers);
                        allGhapUsers.length = limitUsers;
                    }
                })
        })
        .then(function(){
            return ghapApi.getGhapPermissions(adminOAuth.header, allGhapProjects, allGhapUsers)
            //.then(function(){
            //    console.log();
            //    my.log(allGhapProjects);
            //    process.exit(0);
            //})
        })
        .then(comparePermissionsByUsers)
        .then(reportResultsOfPermissionsComparison)
}

var prjMissedInDb = [];
var prjMissedInStash = [];
var grantsMissedInDb = [];
var grantsMissedInStash = [];

var projectsPermissionsDiffs = [];
var grantsPermissionsDiffs = [];

function compareProjectsLists(){
    prjMissedInDb = lib.getMissedProjects(allStashProjects, allGhapProjects);
    prjMissedInStash = lib.getMissedProjects(allGhapProjects, allStashProjects);
}

function compareGrantsLists(){
    grantsMissedInDb = lib.getMissedGrants(allStashProjects, allGhapProjects);
    grantsMissedInStash = lib.getMissedGrants(allGhapProjects, allStashProjects);
}

function comparePermissionsByUsers() {
    var deferred = Q.defer();

    var calls = [];
    var count = allGhapUsers.length;
    var call = function(ghap_user) {
        return function(next) {
            console.log("--------------------- compare permissions for '%s'", ghap_user.name);
            return comparePermissions4GhapUser(ghap_user, next);
        }
    };
    console.log();
    for( var i=0; i < count; i++) {
        calls.push( call(allGhapUsers[i]) );
    }
    ctrlFlow.series(calls, deferred.resolve);

    return deferred.promise;
}

function comparePermissions4GhapUser(ghap_user, done) {
    var stash_permissions, ghap_permissions;
    stash_permissions = stashApi.getStashPermissions4User(ghap_user, allStashProjects);
    ghap_permissions = ghapApi.getGhapUserPermissions(ghap_user, allGhapProjects);

    var diffs = lib.getProjectsPermissionsDiffs(stash_permissions, ghap_permissions);
    if (diffs.length) {
        projectsPermissionsDiffs.push({
            "username": ghap_user.name,
            "projects_diffs": diffs
        });
    };

    diffs = lib.getGrantsPermissionsDiffs(stash_permissions, ghap_permissions);
    if (diffs.length) {
        grantsPermissionsDiffs.push({
            "username": ghap_user.name,
            "grants_diffs": diffs
        })
    }

    done();
}

function reportResultsOfProjectsAndGrantsListsComparison() {
    console.log('============================ REPORT ==================================');

    if (prjMissedInDb.length) {
        console.log('The following projects are presented in STASH but missed in DB:');
        console.log(lib.listValues(prjMissedInDb, 'key'));
    } else {
        console.log('No projects presented in stash but missed in DB detected.')
    }

    if (prjMissedInStash.length) {
        console.log('\nThe following projects are presented in DB but missed in STASH:');
        console.log(lib.listValues(prjMissedInStash, 'key'));
    } else {
        console.log('No projects presented in DB but missed in Stash detected.')
    }

    if (grantsMissedInDb.length) {
        console.log('\nThe following grants are presented in STASH but missed in DB:');
        for (var i = 0; i < grantsMissedInDb.length; i++) {
            var el = grantsMissedInDb[i];
            var str = util.format("Project '%s' grants: %s ", el.prjKey, lib.listValues(el.grants, 'name'));
            console.log(str);
        }
    } else {
        console.log('No grants presented in STASH but missed in DB detected.')
    }

    if (grantsMissedInStash.length) {
        console.log('\nThe following grants are presented in DB but missed in STASH:');
        for (i = 0; i < grantsMissedInStash.length; i++) {
            el = grantsMissedInStash[i];
            str = util.format("Project '%s' grants: %s ", el.prjKey, lib.listValues(el.grants, 'name'));
            console.log(str);
        }
    } else {
        console.log('No grants presented in DB but missed in STASH detected.')
    }
}

function reportResultsOfPermissionsComparison() {
    console.log('\n============================ REPORT ==================================');

    if (projectsPermissionsDiffs.length) {
        console.log('\nList of differences for projects permissions between STASH and DB:');
        for (var i = 0; i < projectsPermissionsDiffs.length; i++) {
            var user_el = projectsPermissionsDiffs[i];
            for (var j=0; j < user_el.projects_diffs.length; j++) {
                var projects_el = user_el.projects_diffs[j];
                var str = util.format("User '%s' Project '%s' Diffs: '%s'", user_el.username, projects_el.prj_key, projects_el.permissions_diff_str);
                console.log(str);
            }
        }
    } else {
        console.log('No differences in projects permissions for users are detected.')
    }

    if (grantsPermissionsDiffs.length) {
        console.log('\nList of differences for grants permissions between STASH and DB:');
        for (i = 0; i < grantsPermissionsDiffs.length; i++) {
            user_el = grantsPermissionsDiffs[i];
            for (j=0; j < user_el.grants_diffs.length; j++) {
                str = util.format("User '%s' '%s'", user_el.username, user_el.grants_diffs[j]);
                console.log(str);
            }
        }
    } else {
        console.log('No differences in grants permissions for users are detected.')
    }

}