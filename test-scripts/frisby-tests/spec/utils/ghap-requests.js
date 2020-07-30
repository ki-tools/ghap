/**
 * Created by Vlad on 29.10.2015.
 */

var requestPromise = require('request-promise');
var util = require('util');

var my = require('../Common/ghap-lib');
var cfg = require('../Common/ghap-config');

var umsUrls = require('../ums/ums_urls');
var umsUser = require('../ums/ums_user');

var psUrls = require('../ps/ps_urls');
var psResources = require('../ps/ps_resources');

var udsUrls = require('../uds/uds_urls');

var asUrls = require('../as/as_urls');

var prjUrls = require('../prj-prov/prj-prov_urls');
var prjResources = require('../prj-prov/prj-prov_resources');

var verbose = true;
exports.verbose = verbose;
function log(){
    if (verbose) process.stdout.write(util.format.apply(this,arguments));
}

var requestsCount = 0;
exports.resetRequestsCount = function () {
  requestsCount = 0;
};
exports.getRequestsCount = function () {
    return requestsCount;
};

exports.sendCorsRequests = false;

function rpGetHeaders(authHeader) {
    var headers= {};
    headers['content-type'] = 'application/json';
    headers[authHeader.Name] = authHeader.Value;
    return headers;
}

function rpGetOptions(authHeader, url) {
    return {
        url: url,
        method: 'GET',
        headers: rpGetHeaders(authHeader)
    }
}

function rpDeleteOptions(authHeader, url) {
    return {
        url: url,
        method: 'DELETE',
        headers: rpGetHeaders(authHeader)
    }
}

function rpOptionsOptions(url, origin_url) {
    return {
        url: url,
        method: 'OPTIONS',
        headers: {
            'origin' : origin_url
        },
        resolveWithFullResponse: true
    }
}

function corsOptionsRequest(rpOptions) {
    var options = rpOptionsOptions(rpOptions.url, cfg.origin);
    log("OPTIONS -> ");
    requestsCount++;
    return requestPromise(options)
        .then(function (response) {
            // validate OPTIONS response
            var header = response.headers['access-control-allow-origin'];
            if (header !== cfg.origin) {
                log("Invalid 'access-control-allow-origin' header ('%s') in OPTIONS response at %s %s\n",
                    header, rpOptions.method, rpOptions.url);
                throw new Error("Invalid response header on OPTIONS request.")
            }
        })
        .catch(function (err) {
            log("Error get OPTIONS for %s request at '%s'\n", rpOptions.method, rpOptions.url)
            throw new Error("OPTIONS request " + err.toString());
        })

}

function rp_(options) {
    requestsCount++;
    if (exports.sendCorsRequests) {
        return corsOptionsRequest(options)
            .then(function () {
                return requestPromise(options);
            })
    } else
        return requestPromise(options);
}

/**
 * Get All GHAP users
 * @param authHeader
 * @param parentDn_str
 * @returns {Promise<U>} fulfilled with all_ghap_users array.
 */
exports.getAllUsers = function(authHeader, parentDn_str) {
    var options = rpGetOptions(authHeader, umsUrls.getFindAllUsers_Url(parentDn_str));
    log('Get All users.');
    return rp_(options)
        .then(function(body){
            var res = my.jsonParse(body);
            log('%d users found.\n', res.length);
            return res;
        })
        .catch(function(error){
            throw new Error('Can not get all users.\n' + error.toString())
        })
};

/**
 * Get Current user.
 * @param authHeader
 * @returns {Promise} fulfilled with current ums_user or null if authorization failed.
 */
exports.getCurrentUser = function(authHeader) {
    var options = rpGetOptions(authHeader, umsUrls.getCurrentUser_Url());
    var ums_user = umsUser.makeUser('any');
    log('Get current user.\n');
    return rp_(options)
        .then(function(body){
            var ghap_user = my.jsonParse(body);
            umsUser.makeUserFromGhapUser(ghap_user, ums_user);
            log("Current user is '%s'\n", ums_user.getName());
            return ums_user;
        })
        .catch(function(error){
            if (error.statusCode == 401) {
                log(' Current user not found. Token can be stale: try to log in again.\n');
            }
            throw new Error('Can not get current user users.\n' + error.toString())
        })
};

/**
 * Get user roles. Call ums_user.setRoles on success response.
 * @param {GhapAuthHeader} authHeader
 * @param {UmsUserType} ums_user
 * @returns {Promise} fulfilled with empty value or rejected on error.
 */
exports.getUserRoles = function (authHeader, ums_user) {
    var url = umsUrls.getUserRoles_Url( ums_user.getDn() );
    var options = rpGetOptions(authHeader, url);
    log("Get roles of user '%s'\n", ums_user.getName());
    return rp_(options)
        .then(function(body){
            ums_user.setRoles(my.jsonParse(body));
            log("User '%s' has %d roles.\n", ums_user.getName(), ums_user.getRoles().length);
        })
};


/**
 * Delete user
 * @param {GhapAuthHeader} authHeader
 * @param {UmsUserType} ums_user
 */
exports.deleteUser = function(authHeader, ums_user) {
    var options = rpDeleteOptions(authHeader, umsUrls.getUser_Url( ums_user.getDn() ));
    log("Delete user '%s'.", ums_user.getName());
    return rp_(options)
        .then(function(body){
            if (body) log(" <%s>", body);
            log(" Ok.\n");
            return true;
        })
        .catch(function(error){
            throw new Error('Can not delete user.\n' + error.toString())
        })
};

/**
 * Validate current token and re-login if required
 * @param o_auth
 * @returns {Promise}
 */
exports.validateToken = function (o_auth) {
    return exports.getCurrentUser(o_auth.header)
        .then( function(current_user){
            if (current_user == null){
                return o_auth.login(o_auth.username, o_auth.password)
            } else
                return true;
        })
};

/**
 * Get personal_storage for the specified user.
 * @param {GhapAuthHeader} authHeader
 * @param {String} user_id
 * @returns {Promise} fulfilled with personal_storage or null if storage not found.
 */
exports.getPersonalStorage = function(authHeader, user_id) {
    var options = rpGetOptions(authHeader, psUrls.getGetStorage_Url(user_id));
    log("Get personal_storage for user with id '%s'.", user_id);
    return rp_(options)
        .then(function(body){
            var ps_obj = my.jsonParse(body);
            if (ps_obj) {
                var ps_storage = psResources.makeStorage();
                my.copyProperties(ps_obj, ps_storage);
                log(" Found. ID = '%s'\n", ps_storage.id);
                return ps_storage;
            } else {
                log(" Response body is empty.\n");
                return null;
            }
        })
        .catch(function(error){
            if (error.statusCode == 404) {
                log(' Not found.\n');
                return null;
            }
            throw new Error('Can not get personal storage.\n' + error.toString())
        })
};

exports.deletePersonalStorage = function(authHeader, user_id) {
    var options = rpGetOptions(authHeader, psUrls.getDeleteStorage_Url(user_id));
    log("Delete personal storage for user with id '%s'.", user_id);
    return rp_(options)
        .then(function(body){
            if (body) log(" <%s>", body);
            log(" Ok.\n");
            return true;
        })
        .catch(function(error){
            if (error.statusCode == 404) {
                log(' Not found.\n');
                return false;
            }
            throw new Error('Can not delete personal storage.\n' + error.toString())
        })
};

/**
 * Gets all stacks provisioned for the user
 * @param {GhapAuthHeader} authHeader
 * @param {UmsUserType} ums_user
 * @param {vpgStack[]} vpg_array - RESULT
 * @returns {Promise} resolved with empty value or rejected
 */
exports.multiVpgGetStacks4User = function(authHeader, ums_user, vpg_array) {
    var url = psUrls.multiVPG_getStack4user(ums_user.getGuid());
    var options = rpGetOptions(authHeader, url);
    log("Get VP stacks provisioned for user '%s' (user id '%s')\n", ums_user.getName(), ums_user.getGuid());
    return rp_(options)
        .then(function (body) {
            var ghap_vpg_array = my.jsonParse(body);
            if (Array.isArray(ghap_vpg_array)){
                log("User '%s' has %d VPG.\n", ums_user.getName(), ghap_vpg_array.length);
                vpg_array.length =0;
                ghap_vpg_array.forEach(function(ghap_vpg){
                    var vpg = psResources.makeVPG();
                    my.copyProperties(ghap_vpg,vpg);
                    vpg_array.push(vpg);
                })
            }
        })
};

/**
 * Get the compute resources that are associated with a given user
 * @param {GhapAuthHeader} authHeader
 * @param {UmsUserType} ums_user
 * @param {computeResource[]} comp_res_array - RESULTS
 * @returns {Promise} resolved with empty value or rejected
 */
exports.multiVpgGetComputeResources4User = function(authHeader, ums_user, comp_res_array){
    var url = psUrls.multiVPG_getComputeResources4user(ums_user.getGuid());
    var options = rpGetOptions(authHeader, url);
    log("Get compute resources provisioned for user '%s'\n", ums_user.getName());
    return rp_(options)
        .then(function (body) {
            my.moveArray(my.jsonParse(body), comp_res_array);
            log(" User '%s' has %d compute resources.\n", ums_user.getName(), comp_res_array.length);
        })
};

/**
 * Get user workspace content at path
 * @param {GhapAuthHeader} authHeader
 * @param {UmsUserType} ums_user
 * @param {String} path - path to content folder. Root is ''.
 * @returns {Promise}
 */
exports.dir = function(authHeader, ums_user, path){
    var url = udsUrls.dir_url(ums_user.getGuid(), path);
    var options = rpGetOptions(authHeader, url);
    log("List user workspace files at path '%s' for user '%s'\n", path, ums_user.getName());
    return rp_(options)
        .then(function (body) {
            var result = my.jsonParse(body);
            log("Dir response array has %d entities.\n", result.length);
            return result;
        })
};

/* Activity service requests /

/**
 * Get activities for the specified role and save them to ums_role.ar_associations.
 * @param {GhapAuthHeader} authHeader
 * @param {Object} ums_role
 * @returns {Promise}
 */
exports.getARAssociationsForRole = function(authHeader, ums_role){
    var url = asUrls.getARAssociationsForRole_Url(ums_role.guid);
    var options = rpGetOptions(authHeader, url);
    log("Get ActivityRole-Associations for '%s' role\n", ums_role.name);
    return rp_(options)
        .then(function(body){
            if (!ums_role.hasOwnProperty("ar_associations"))
                ums_role.ar_associations = [];
            my.moveArray(my.jsonParse(body),ums_role.ar_associations);
            log("'%s' role has %d activities assigned.\n", ums_role.name, ums_role.ar_associations.length);
        })
};

/**
 * Get ghap activity by Id
 * @param {GhapAuthHeader} authHeader
 * @param {String}activity_id
 * @returns {Promise} resolved with {ghapActivity} or rejected.
 */
exports.getActivityById = function(authHeader, activity_id){
    var url = asUrls.getActivityById_Url(activity_id);
    var options = rpGetOptions(authHeader, url);
    log("Get Activity By ID '%s'\n", activity_id);

    return rp_(options)
        .then(function(body){
            var activity = my.jsonParse(body);
            log("Activity with is '%s' has name '%s'.\n",activity_id, activity.activityName);
            return activity;
        })
};

/* Project provisioning service requests */

/**
 * Get all projects (GHAP programs)
 * @param {GhapAuthHeader} authHeader
 * @param {Array} all_projects - RESULT
 * @returns {Promise} resolved with empty value or rejected
 */
exports.getAllProjects = function(authHeader, all_projects){
    var url = prjUrls.getAllProjects_Url();
    var options = rpGetOptions(authHeader, url);
    log("Get all projects (GHAP programs)\n");
    return rp_(options)
        .then(function (body) {
            var parsed_array = my.jsonParse(body);
            if (parsed_array instanceof Array) {
                all_projects.length = 0;
                var element = parsed_array.shift();
                while(element) {
                    var prj_res =  prjResources.makeProject();
                    my.copyProperties(element, prj_res);
                    all_projects.push( prj_res );
                    element = parsed_array.shift();
                }
                log("GHAP storage has %d programs.\n", all_projects.length)
            }
        })
};

/**
 * Get all project (GHAP programs) for which the user has permissions defined.
 * @param {GhapAuthHeader} authHeader
 * @param {UmsUserType} ums_user
 * @returns {Promise} resolved with array of projects or rejected on error
 */
exports.getAllProjects4User = function(authHeader, ums_user){
    var url = prjUrls.getAllProjects4User_Url(ums_user.getGuid());
    var options = rpGetOptions(authHeader, url);
    log("Get All projects (GHAP programs) for user '%s'", ums_user.getName());

    return rp_(options)
        .then(function (body) {
            var parsed_array = my.jsonParse(body);
            log("User '%s' has %d projects (GHAP programs) with defined permissions.\n",
                ums_user.getName(), parsed_array.length);
            return parsed_array;
        })
};

/**
 * Get all grants of the specified project (GHAP program)
 * @param {GhapAuthHeader} authHeader
 * @param {GhapProjectType} prj_resource - RESULT in grants attribute.
 * @returns {Promise} resolved with empty value or rejected
 */
module.exports.getAllGrants = function(authHeader, prj_resource){
    var url = prjUrls.getAllGrants_Url(prj_resource.id);
    var options = rpGetOptions(authHeader, url);
    log("Get all grants of program '%s'\n", prj_resource.name);

    return rp_(options)
        .then(function (body) {
            var parsed_array = my.jsonParse(body);
            if (parsed_array instanceof Array) {
                prj_resource.grants.length = 0;
                var element = parsed_array.shift();
                while(element) {
                    var grant_res =  prjResources.makeGrant();
                    my.copyProperties(element,grant_res);
                    prj_resource.addGrant( grant_res );
                    element = parsed_array.shift();
                }
                log("Program '%s' has %d grants.\n", prj_resource.name, prj_resource.grants.length)
            }
        })
};

/**
 * Get all project grants for which the user has permissions defined.
 * @param {GhapAuthHeader} authHeader
 * @param {GhapProjectType} prj
 * @param {UmsUserType} ums_user
 * @returns {Promise} resolved with array of grants or rejected on error
 */
exports.getAllGrantsOfProject4User = function(authHeader, prj, ums_user){
    var url = prjUrls.getAllGrantsOfProject4User_Url(prj.id, ums_user.getGuid());
    var options = rpGetOptions(authHeader, url);
    log("Get all Grants of project '%s' for which user '%s' has permissions defined.\n", prj.name, ums_user.getName());

    return rp_(options)
        .then(function (body) {
            var parsed_array = my.jsonParse(body);
            log("User '%s' has %d grants in project '%s' with defined permissions.\n",
                ums_user.getName(), parsed_array.length, prj.name);
            return parsed_array;
        })
};

/* Visualization Publisher service requests */

var vzUrls = require('../vz/vz_urls');

exports.getRegisteredApps = function(authHeader){
    var token = authHeader.Value.substr(7);
    var url = vzUrls.getRegistry_url(token);
    var options = rpGetOptions(authHeader, url);
    log("Get list of registered visualization applications.\n");

    return rp_(options)
        .then(function (body) {
            var parsed_array = my.jsonParse(body);
            log("%d applications found.\n", parsed_array.length);
            return parsed_array;
        })
};
