/**
 * Created by Vlad on 12.11.2015.
 */

var Q = require('q');
var my = require('../Common/ghap-lib');
var prjRequests = require('../prj-prov/prj-requests');

exports.getGrants4AllStashProjects = function (auth_header, all_stash_projects) {
    //var promises = [];
    // Q: q promise one by another
    // A: http://stackoverflow.com/questions/24586110/resolve-promises-one-after-another-i-e-in-sequence
    var p = Q();
    all_stash_projects.forEach(function(stash_prj){
        //promises.push(
        p = p.then(function() {
            return prjRequests.getAllStashGrants4Project(auth_header, stash_prj)
                .then(function () {
                    console.log(" '%s' have %d grants in STASH.", stash_prj.key, stash_prj.grants.length);
                    stash_prj.grants = stash_prj.grants.map(function (grant_obj) {
                        return {
                            "name": grant_obj.name,
                            "slug": grant_obj.slug
                        }
                    });
                })
        });
        //)
    });

    //return Q.all(promises);
    return p;
};

exports.getStashPermissions = function(auth_header, all_stash_projects) {
    //var promises = [];
    var p = Q();
    all_stash_projects.forEach(function(stash_prj){
        //promises.push(
        p = p.then(function() {
            return prjRequests.getStashProjectUsersPermissions(auth_header, stash_prj)
                .then(function () {
                    return getStashGrantsPermissions(auth_header, stash_prj)
                })
                .catch(my.reportError)
        });
        //)
    });

    //return Q.all(promises);
    return p;

};

function getStashGrantsPermissions(auth_header, stash_prj){
    //var promises = [];
    var p = Q();
    stash_prj.grants.forEach(function(stash_grant) {
        p = p.then(function() {
        //promises.push(
            return prjRequests.getStashGrantUsersPermissions(auth_header, stash_prj, stash_grant)
        });
        //)
    });

    //return Q.all(promises);
    return p;
}

/**
 * @typedef {Object} StashUserPermission
 * @property {string} prj_key - project key
 * @property {string} prj_name - project name
 * @property {string} prj_permission - 'PROJECT_WRITE' or 'PROJECT_READ'
 * @property {[StashGrantUserPermission]} grants_permissions -
 */

/**
 * @typedef {Object} StashGrantUserPermission
 * @property {string} grant_name
 * @property {string} grant_permission - 'REPO_READ' or 'REPO_WRITE'
 */

exports.getStashPermissions4User = function(ghap_user, all_stash_projects) {
    var results = [];
    all_stash_projects.forEach(function(stash_prj){
        var user_perm = my.findElementInArray(stash_prj.permissions, 'username', ghap_user.name);
        if (!user_perm) {
            user_perm = {"permission" : 'missed'};
        }
        results.push({
            "prj_key" : stash_prj.key,
            "prj_name" : stash_prj.name,
            "prj_permission" : user_perm.permission,
            "grants_permissions" : getStashGrantsPermissions4User(ghap_user, stash_prj)
        })
    });
    results.sort(function(a, b){
        if (a.prj_name < b.prj_name) return -1;
        return 1;
    });
    return results;
};

function getStashGrantsPermissions4User(ghap_user, stash_project) {
    var results = [];
    stash_project.grants.forEach(function(stash_grant){
        var user_perm = my.findElementInArray(stash_grant.permissions, 'username', ghap_user.name);
        if (!user_perm) {
            user_perm = {"permission" : 'missed'};
        }
        results.push({
            "grant_name" : stash_grant.name,
            "grant_permission" : user_perm.permission,
        })
    });
    results.sort(function(a, b){
        if (a.grant_name < b.grant_name) return -1;
        return 1;
    });
    return results;
}