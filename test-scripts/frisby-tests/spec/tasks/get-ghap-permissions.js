/**
 * Created by Vlad on 12.11.2015.
 */

var Q = require('q');
var my = require('../Common/ghap-lib');

var umsUser = require('../ums/ums_user');
var prjRequests = require('../prj-prov/prj-requests');


exports.getGrants4AllGhapProjects = function(auth_header, all_ghap_projects) {
    //var promises = [];
    // Q: q promise one by another
    // A: http://stackoverflow.com/questions/24586110/resolve-promises-one-after-another-i-e-in-sequence
    var p = Q();
    all_ghap_projects.forEach(function(prj){
        //promises.push(
        p = p.then(function() {
            return prjRequests.getAllGrants(auth_header, prj)
                .then(function(){
                    console.log(" '%s' have %d grants in GHAP.", prj.key, prj.grants.length);
                })
        });
        //)
    });

    //return Q.all(promises);
    return p;
};

exports.getGhapPermissions = function(auth_header, all_ghap_projects, all_users) {
    //var promises = [];
    var p = Q();
    all_ghap_projects.forEach(function(ghap_prj){
        //promises.push(
        p = p.then(function() {
            return prjRequests.getProjectUsersPermissions(auth_header, ghap_prj, all_users)
                .then(function(){
                    return getGhapGrantsPermissions(auth_header, ghap_prj, all_users)
                })
                .catch(my.reportError)
        });
        //)
    });

    //return Q.all(promises);
    return p;

};

function getGhapGrantsPermissions(auth_header, project, all_users){
    //var promises = [];
    var p = Q();
    project.grants.forEach(function(grant) {
        //promises.push(
        p = p.then(function() {
            return prjRequests.getGrantUsersPermissions(auth_header, grant, all_users)
        });
        //)
    });

    //return Q.all(promises);
    return p;
}

/*---------------------------------------------------------------------------------
 API for retrieve users permissions from all_ghap_projects
 ------------------------------------------------------------------------------------ */
/**
 * @typedef {Object} GhapUserPermission
 * @property {string} prj_key - project key
 * @property {string} prj_name - project name
 * @property {string} prj_permission -
 * @property {[GhapGrantUserPermission]} grants_permissions -
 */

/**
 * @typedef {Object} GhapGrantUserPermission
 * @property {string} grant_name
 * @property {string} grant_permission -
 */

exports.getGhapUserPermissions = function(ghap_user, all_ghap_projects) {
    var results = [];
    all_ghap_projects.forEach(function(ghap_prj){
        var user_perm = my.findElementInArray(ghap_prj.permissions, 'username', ghap_user.name);
        if (!user_perm) {
            user_perm = {"permission" : 'missed'};
        }
        results.push({
            "prj_key" : ghap_prj.key,
            "prj_name" : ghap_prj.name,
            "prj_permission" : user_perm.permission,
            "grants_permissions" : getGhapUsersGrantsPermissions(ghap_user, ghap_prj)
        })
    });
    results.sort(function(a, b){
        if (a.prj_name < b.prj_name) return -1;
        return 1;
    });
    return results;
};

function getGhapUsersGrantsPermissions(ghap_user, project) {
    var results = [];
    project.grants.forEach(function(grant){
        var user_perm = my.findElementInArray(grant.permissions, 'username', ghap_user.name);
        if (!user_perm) {
            user_perm = {"permission" : 'missed'};
        }
        results.push({
            "grant_name" : grant.name,
            "grant_permission" : user_perm.permission
        })
    });
    results.sort(function(a, b){
        if (a.grant_name < b.grant_name) return -1;
        return 1;
    });
    return results;
}

/*---------------------------------------------------------------------------------
   API for retrieve users permissions via REST requests
------------------------------------------------------------------------------------ */

/**
 * @typedef {Object} GhapPermission
 * @property {string} id - project id
 * @property {string} name - project name
 * @property {string} key - project key
 * @property {array} permissions - ['READ','WRITE'] or ['READ']
 */

exports.getGhapPermissions4User = function(auth_header, ghap_user, all_ghap_projects) {
    var ums_user = umsUser.makeUserFromGhapUser(ghap_user);
    return prjRequests.getAllProjects4User(auth_header, ums_user)
        .then(function(){
            appendGhapProjectsWithEmptyPermissions(all_ghap_projects, ums_user.projects)
        })
        .then(function(){
            return getGhapGrantsPermissions4User(auth_header, all_ghap_projects, ums_user)
        })
        .then(function(){return ums_user.projects})
}

function appendGhapProjectsWithEmptyPermissions(all_ghap_projects, projects){
    for (var i=0; i < all_ghap_projects.length; i++) {
        var ghap_project = all_ghap_projects[i];
        if (!my.findElementInArray(projects, 'id', ghap_project.id)) {
            var clone = my.cloneObject(ghap_project);
            clone.permissions = [];
            projects.push(clone)
        }
    }
}

function getGhapGrantsPermissions4User(auth_header, all_ghap_projects, ums_user) {
    var promises = [];
    ums_user.projects.forEach(function(project){
        promises.push(
            prjRequests.getAllGrantsOfProject4User(auth_header, project, ums_user)
                .then(function(grants){
                    project.grants = grants;
                    appendGhapGrantsWithEmptyPermissions(all_ghap_projects, project);
                    project.grants.sort(function(a, b){
                        if (a.name < b.name) return -1;
                        return 1;
                    })
                })
        )
    });
    return Q.all(promises)
}

function appendGhapGrantsWithEmptyPermissions(all_ghap_projects, project){
    var ghap_project = my.findElementInArray(all_ghap_projects, 'id', project.id);
    if (!ghap_project)
        throw new Error(project.name + ' not found in all_ghap_projects.');
    for (var i=0; i < ghap_project.grants.length; i++) {
        var ghap_grant = ghap_project.grants[i];
        if (!my.findElementInArray(project.grants, 'id', ghap_grant.id)) {
            var clone = my.cloneObject(ghap_grant);
            clone.permissions = [];
            project.grants.push(clone)
        }
    }
}
