/**
 * Created by vruzov on 14.09.2015.
 */

var my = require('./../Common/ghap-lib');
my.stepPrefix = 'GetCurrentUser';
my.logModuleName(module.filename);

var umsUrls = require('./ums_urls');
var umsUser = require('./ums_user');
var umsRequests = require('./ums_requests');

var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
var testerAdmin = require('./tester_admin');

oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
    .then(umsRequests.validateToken)
    .then(runCase)
    .catch(my.reportError);

function runCase(){
    umsRequests.pullUserData(oAuth.header, testerAdmin, finished);
}

function finished(){
    console.log('\nFinished.')
}