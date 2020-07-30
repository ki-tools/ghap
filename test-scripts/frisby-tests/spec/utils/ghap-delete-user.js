/**
 * Created by Vlad on 29.10.2015.
 */

var util = require('util');
var verbose = true;
function log(){
    if (verbose) process.stdout.write(util.format.apply(this,arguments));
}

var cfg = require('./../Common/ghap-config');

var environment = null;
if (typeof process.argv[2] === 'string'){
    environment = process.argv[2];
}
var userName = null;
if (typeof process.argv[3] === 'string'){
    userName = process.argv[3];
}

if (environment === null || userName === null) {
    console.log('Use: ghap-delete-user <ENVIRONMENT> <USER_NAME>');
    process.exit(1);
}

if (environment !== cfg.environment) {
    if (!cfg.setConfig(environment)) {
        console.log("Configuration for '%s' environment not found. ", environment);
        process.exit(1);
    }
}

var my = require('../Common/ghap-lib');
var ghapRq = require('./ghap-requests');
var umsUser = require('../ums/ums_user');
var psResources = require('./../ps/ps_resources');

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./ghap-oauth-promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
    .then(ghapRq.validateToken)
    .then(runCommand)
    .catch(my.reportError)

function runCommand(){
    return ghapRq.getAllUsers(oAuth.header, testerAdmin.getParentDn())
        .then(function (allUsers) {
            var ghap_user = my.findElementInArray(allUsers, 'name', userName);
            if (ghap_user) {
                log("%s user ID is '%s'\n", ghap_user.name, ghap_user.guid);
                return deleteGhapUser(ghap_user);
            } else {
                console.log("'%s' user not found.", userName);
            }
        });
}

function deleteGhapUser(ghap_user) {
    return ghapRq.getPersonalStorage(oAuth.header, ghap_user.guid)
        .then(function(ps_storage) {
            if (ps_storage) {
                return ghapRq.deletePersonalStorage(oAuth.header, ghap_user.guid)
            }
        })
        .then(function(){
            var ums_user = umsUser.makeUserFromGhapUser(ghap_user);
            return ghapRq.deleteUser(oAuth.header, ums_user)
        })
}