/**
 * Created by Vlad on 09.12.2015.
 */

var Q = require('q');

var my = require('../Common/ghap-lib');
my.stepPrefix = 'TerminateEnvs';
my.logModuleName(module.filename);

var psUtil = require('./ps-terminate-envs');
var psTester = require('./ps_tester').make();

var umsRequests = require('../ums/ums_requests');
var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(psTester.getName(), psTester.getPassword())
    .then(umsRequests.validateToken)
    .then(function(){return umsRequests.pullUser(oAuth, psTester)})
    .then(startSuite)
    .catch(my.reportError);

function startSuite(){
    return psUtil.terminateEnvironments(oAuth.header, psTester)
}
