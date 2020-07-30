/**
 * Created by Vlad on 07.08.2015.
 */

var Q = require('q');

var my = require('../Common/ghap-lib');
my.stepPrefix = 'StopMultiEnv';
my.logModuleName(module.filename);

var umsRequests = require('../ums/ums_requests');

var psTester = require('./ps_tester').make();
var psTesterVPGs = [];

var psRequests = require('./ps_requests');
var psResources = require('./ps_resources');

var asRequests = require('../as/as_requests');
var allActivities = [];

var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(psTester.getName(), psTester.getPassword())
    .then(umsRequests.validateToken)
    .then(function(){return umsRequests.pullUser(oAuth, psTester)})
    .then(getAllActivities)
    .then(runSuite)
    .catch(my.reportError)
    .finally(my.finalTest);

function getAllActivities(){
    return asRequests.getAllActivities(oAuth.header, allActivities)
}

function runSuite(){

    return psRequests.multiVpgGetStatuses4User(oAuth.header, psTester, psTesterVPGs)
        .then(validatePrereqEnvStatuses)
        .then(stopEnvironments)
        .then(psRequests.waitStatuses.bind(this, oAuth.header, psTester, 'stopped'))
        .then(startEnvironments)
        .then(psRequests.waitStatuses.bind(this, oAuth.header, psTester, 'running', 5 * 60 *1000))
}

function validatePrereqEnvStatuses(){
    var deferred = Q.defer();

    describe('Validate pre-required VPGs statuses:', function(){

        it('PSTester should have as minimum 2 compute resources', function(){
            console.log(this.getFullName());
            expect(psTesterVPGs.length).toBeGreaterThan(1);
       });

        it('all resources should have running node(s)', function(){
            console.log(this.getFullName());
            psTesterVPGs.forEach(function(vpg){
                expect(vpg.computeResources.length).toBeGreaterThan(0);
                vpg.computeResources.forEach(function(comp_res){
                    console.log("%s node have status '%s'", comp_res.instanceOsType, comp_res.status);
                    expect(comp_res.status).toEqual('running');
                });
                deferred.resolve(waits(300)); // make pause to allow promise to be fulfilled

            })
        })

    });

    return deferred.promise;
}

function stopEnvironments(){
    var promises = [];

    psTesterVPGs.forEach(function (vpg) {
        var ghap_activity = my.findElementInArray(allActivities, 'id', vpg.activityId);
        promises.push(psRequests.multiVpgPause(oAuth.header, ghap_activity, psTester));
    });

    return Q.allSettled(promises);
}

function startEnvironments(){
    var promises = [];

    psTesterVPGs.forEach(function (vpg) {
        var ghap_activity = my.findElementInArray(allActivities, 'id', vpg.activityId);
        promises.push(psRequests.multiVpgResume(oAuth.header, ghap_activity, psTester));
    });

    return Q.allSettled(promises);
}