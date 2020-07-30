/**
 * Created by Vlad on 09.12.2015.
 */

var Q = require('q');
var my = require('../Common/ghap-lib');
var psRequests = require('./ps_requests');
var psResources = require('./ps_resources');

exports.terminateEnvironments = function(oauth_header, ums_user){
    var vpg_array = [];

    return psRequests.multiVpgGetStacks4User(oauth_header, ums_user, vpg_array)
        .then(function(){
            if (vpg_array.length ===0) return;

            var deferred = Q.defer();

            var calls = [];
            vpg_array.forEach( function(vpg){
                calls.push(	psRequests.multiVpgTerminate(oauth_header, vpg.userId, vpg.activityId) )
            });

            Q.all(calls)
                .then(function(){ deferred.resolve(); })
                .catch(function() {	deferred.reject(new Error("Error happens on terminate environment request.")); });

            return deferred.promise.then(function(){
                waitDecomissionOfEnvironments(oauth_header, ums_user)
            });
        });

};

function waitDecomissionOfEnvironments(oauth_header, ums_user){
    var deferred = Q.defer();
    const DELAY_BETWEEN_ATTEMPTS = 3*1000;
    const MAX_EXEC_TIME_MS =  10*1000;
    var start = new Date();
    var exec_time_ms;
    var vpg_array = [];

    function checkVPGs() {
        psRequests.multiVpgGetStacks4User(oauth_header, ums_user, vpg_array)
            .then(function(){
                if (vpg_array.length === 0)
                    deferred.resolve( my.pauseJasmine(3000) );
                else {
                    exec_time_ms = new Date() - start;
                    if (exec_time_ms < MAX_EXEC_TIME_MS) {
                        my.pauseJasmine(DELAY_BETWEEN_ATTEMPTS);
                        checkVPGs();
                    }
                    else {
                        console.error("\nAwaiting time for decommissions of all environments for user '%s' exceeded %d seconds.",
                            ums_user.getName(), MAX_EXEC_TIME_MS/1000);
                        deferred.reject(new Error("waitDecomissionOfEnvironments timeout."));
                    }
                }
            })
    }

    checkVPGs();

    return deferred.promise;
}