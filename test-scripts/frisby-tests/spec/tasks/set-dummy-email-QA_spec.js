/**
 * Created by Vlad on 11.01.2016.
 */

var Q = require('q');
var ctrlFlow = require('../Common/control_flow');

var my = require('./../Common/ghap-lib');
my.stepPrefix = 'SetDummyEmail';
my.logModuleName(module.filename);

var cfg = require('./../Common/ghap-config');
if (cfg.environment !== 'qa') {
    console.error('This script can be executed in QA environment only.');
    process.exit();
}

var umsUser = require('./../ums/ums_user');
var umsRequests = require('./../ums/ums_requests');
var dummyEmail = 'success@simulator.amazonses.com';

var qaUsernames = require('./QAUsernames.json');
var allGhapUsers =[];

// uncomment line below this to run the test
// var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
    .then(umsRequests.validateToken)
    .then(function(){
        return umsRequests.getAllUsers(oAuth.header, testerAdmin.getParentDn(),allGhapUsers)
    })
    .then(setDummyEmails)
    .then(my.finalTest)
    .catch(my.reportError);

function setDummyEmails() {
    var deferred = Q.defer();

    var createCalls = [];

    allGhapUsers.forEach(function(ghap_user) {
        var ums_user = umsUser.makeUserFromGhapUser(ghap_user);
        createCalls.push(
            function (next) {
                var user_name = ums_user.getName();
                if (my.findElementInArray(qaUsernames, user_name)) {
                    console.log("Account of user '%s' skipped.", user_name);
                    next();
                } else {
                    console.log("Email of '%s' account changed to the dummy value.", user_name);
                    umsRequests.updateUser(oAuth.header, ums_user, {email: dummyEmail}, next);
                    //next();
                }
            })
    });

    ctrlFlow.series(createCalls, deferred.resolve);

    return deferred.promise
}