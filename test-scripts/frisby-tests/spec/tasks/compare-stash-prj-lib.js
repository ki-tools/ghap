/**
 * Created by Vlad on 06.11.2015.
 */

var util = require('util');
var my = require('../Common/ghap-lib');

/**
 * Return array of projects presented in projects_1 but missed in projects_2
 * slice returned projects from projects_1
 * @param {Array} projects_1
 * @param {Array} projects_2
 * @returns {Array}
 */
exports.getMissedProjects = function (projects_1, projects_2) {
    var missed_projects = [];
    for (var i = projects_1.length-1; i >=0; i--) {
        var prj_1 = projects_1[i];
        if (!my.findElementInArray(projects_2, 'key', prj_1.key)) {
            missed_projects.push( projects_1.splice(i,1)[0] )
        }
    }
    return missed_projects;
};

/**
 * List grants of projects_1 missed in projects_2
 * @param {Array} projects_1
 * @param {Array} projects_2
 * @returns {missedGrantsArray}
 */
exports.getMissedGrants = function ( projects_1, projects_2 ){
    /**
     * @typedef {Array} missedGrantsArray
     * @property {string] prjKey
     * @property {Array} grants
     */
    var missed_grants = [];
    for (var prj1_index = 0; prj1_index < projects_1.length; prj1_index++) {
        var prj_1 = projects_1[prj1_index];
        var prj_2 = my.findElementInArray(projects_2, 'key', prj_1.key);
        if (prj_2 === null)
            throw new Error(prj_1.key + ' not found in projects array.');
        for (var grant_index = 0; grant_index < prj_1.grants.length; grant_index++) {
            var prj1_grant = prj_1.grants[grant_index];
            if (!my.findElementInArray(prj_2.grants, 'name', prj1_grant.name)) {
                var el = my.findElementInArray(missed_grants, 'prjKey', prj_1.key);
                if (el == null) {
                    el = {
                        "prjKey" : prj_1.key,
                        "grants" : []
                    };
                    missed_grants.push(el);
                }
                el.grants.push(prj1_grant);
            }
        }
    }
    return missed_grants;
};


/**
 * @typedef {Object} ProjectPermissionsDiff
 * @property {string} prj_key - project key
 * @property {string} permissions_diff_str - description of difference
 */

/**
 * @param {[StashUserPermission]} stash_perms
 * @param {[GhapUserPermission]} ghap_perms
 * @returns {[ProjectPermissionsDiff]} - differences
 */
exports.getProjectsPermissionsDiffs = function (stash_perms, ghap_perms) {
    var projects_permissions_diffs = [];

    var keys = [];
    for( var i=0; i < stash_perms.length; i++) {
        keys.push(stash_perms[i].prj_key);
    }
    for( i=0; i < ghap_perms.length; i++) {
        if (!my.findElementInArray(keys, ghap_perms[i].prj_key))
        keys.push(ghap_perms[i].key)
    }

    for( i=0; i < keys.length; i++) {
        var diff_str = getProjectsPermsDiffStr(stash_perms, ghap_perms, keys[i]);
        if (diff_str) {
            projects_permissions_diffs.push( {
                "prj_key" : keys[i],
                "permissions_diff_str" : diff_str
            })
        }
    }

    return projects_permissions_diffs;
};

/**
 *
 * @param {[StashUserPermission]} stash_perms
 * @param {[GhapUserPermission]} ghap_perms
 * @param {string} prj_key
 * @returns {string} description of permissions difference or ''
 */
function getProjectsPermsDiffStr(stash_perms, ghap_perms, prj_key) {
    var stash_perm_str;
    var stash_perm = my.findElementInArray(stash_perms, 'prj_key', prj_key);
    if (stash_perm) {
        stash_perm_str = stash_perm.prj_permission;
        if (!stash_perm_str) stash_perm_str = 'INVALID_PROJECT_PERMISSION';
    } else {
        stash_perm_str = 'STASH_PROJECT_NOT_FOUND'
    }

    var ghap_perm_str;
    var ghap_perm = my.findElementInArray(ghap_perms, 'prj_key', prj_key);
    if (ghap_perm) {
        ghap_perm_str = ghap_perm.prj_permission;
    } else {
        ghap_perm_str = 'GHAP_PROJECT_NOT_FOUND'
    }

    var perms_are_equal = (ghap_perm_str === 'READ') && (stash_perm_str === 'PROJECT_READ') ||
        (ghap_perm_str === 'READ,WRITE') && (stash_perm_str === 'PROJECT_WRITE') ||
        (ghap_perm_str === stash_perm_str);

    if ( perms_are_equal) {
        return '';
        //return 'STASH: ' + stash_perm_str + '; GHAP: ' + ghap_perm_str;
    } else {
        return 'STASH: ' + stash_perm_str + '; GHAP: ' + ghap_perm_str;
    }
}

exports.getGrantsPermissionsDiffs = function (stash_perms, ghap_perms) {
    var grants_permissions_diffs = [];

    var prj_keys = getKeyValues(stash_perms, ghap_perms, 'prj_key');
    for (var i=0; i < prj_keys.length; i++) {

        var stash = my.findElementInArray(stash_perms, 'prj_key', prj_keys[i]);
        var stash_grants = [];
        if (stash) stash_grants = stash.grants_permissions;

        var ghap = my.findElementInArray(ghap_perms, 'prj_key', prj_keys[i]);
        var ghap_grants = [];
        if (ghap) ghap_grants = ghap.grants_permissions;

        var grants_names = getKeyValues(stash_grants, ghap_grants, 'grant_name');

        for (var j=0; j < grants_names.length; j++) {
            var stash_grant = my.findElementInArray(stash_grants, 'grant_name', grants_names[j]);
            var ghap_grant = my.findElementInArray(ghap_grants, 'grant_name', grants_names[j]);

            //if (!stash_grant || !ghap_grant) {
            //    throw new Error('Internal error: grant not found in the all grants list.')
            //}

            var diff_str = '';
            //if (!stash_grant)
            //    diff_str += 'grant not found in stash; ';
            //if (!ghap_grant)
            //    diff_str += 'grant not found in PS table;';
            if (stash_grant && ghap_grant)
                diff_str = getGrantsPermsDiffStr(stash_grant, ghap_grant);

            if (diff_str) {
                grants_permissions_diffs.push(
                    util.format("grant '%s/%s' Diffs: %s", prj_keys[i], grants_names[j], diff_str)
                )
            }
        }
    }

    return grants_permissions_diffs;
};

function getKeyValues(array_1, array_2, key) {
    var values = [];
    for( var i=0; i < array_1.length; i++) {
        values.push(array_1[i][key]);
    }
    for( i=0; i < array_2.length; i++) {
        if (!my.findElementInArray(values, array_2[i][key]))
            values.push(array_2[i][key]);
    }
    return values
}

function getGrantsPermsDiffStr(stash_grant, ghap_grant) {

    var stash_perm_str;
    if (!stash_grant) {
        stash_perm_str = 'GRANT_NOT_FOUND'
    } else {
        stash_perm_str = stash_grant.grant_permission;
        if (stash_perm_str === 'missed') {
            stash_perm_str = 'NO'
        } else if (stash_perm_str === 'REPO_WRITE') {
            stash_perm_str = 'RW'
        } else if (stash_perm_str === 'REPO_READ') {
            stash_perm_str = 'RO'
        } else
            stash_perm_str = 'INVALID_GRANT_PERMISSION';
    }

    var ghap_perm_str;
    if (!ghap_grant) {
        ghap_perm_str = 'GRANT_NOT_FOUND'
    } else {
        ghap_perm_str = ghap_grant.grant_permission.toString();
        if (ghap_perm_str === 'missed') {
            ghap_perm_str = 'NO'
        } else if (ghap_perm_str === 'READ,WRITE') {
            ghap_perm_str = 'RW'
        } else if (ghap_perm_str === 'READ') {
            ghap_perm_str = 'RO'
        }
    }

    if (stash_perm_str === ghap_perm_str)
        return '';

    return 'STASH: ' + stash_perm_str + '; GHAP: ' + ghap_perm_str;
}


/**
 * List values of objects with specified key as comma separated sting.
 * If object does not have key the '[???]' string appears to the result list.
 * @param {Array} array_of_objects
 * @param {string} key_name
 * @returns {string}
 */
exports.listValues = function (array_of_objects, key_name) {
    var result_str = '';
    for (var i= 0; i < array_of_objects.length; i++) {
        var obj = array_of_objects[i];
        if (obj.hasOwnProperty(key_name)) {
            result_str += "'"+array_of_objects[i][key_name]+"'";
        } else {
            result_str += '[????]';
        }
        if (i !== array_of_objects.length-1)
            result_str += ", ";
    }
    return result_str;
};