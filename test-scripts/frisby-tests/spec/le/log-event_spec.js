/**
 * Created by Vlad on 24.05.2016.
 */

var Q = require('q');
var AWS = require('aws-sdk');
require('jasmine-json');

var my = require('../Common/ghap-lib');
my.stepPrefix = 'LogEvent';
my.logModuleName(module.filename);

var logRequests = require("./log-event_requests");

var cfg = require('../Common/ghap-config');

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
    .then(function(){
        testerAdmin.setAuthHeader(oAuth.header);
    })
    .then(runSuite)
    .catch(my.reportError)
    .finally(my.finalTest);

function runSuite() {
    return Q()
        .then(getEsClient)
        .then(validateEsGhapIndex)
        .then(getDsLogs)
        .then(logRequests.flush)
}

function getEsClient(aws_credentials) {
    var logRequests = require("./log-event_requests");
    return logRequests.getAwsEsClient()
}

function validateEsGhapIndex(client) {
    // https://github.com/elastic/elasticsearch-js
    var deferred = Q.defer();
    var index_name = 'ghap';
    var index_map = {
        ghap: {
            mappings: {
                'dataset-download': {
                    properties: {
                        clientip: {type: 'string'},
                        email: {type: 'string'},
                        fileName: {type: 'string'},
                        fileSize: {type: 'long'},
                        fileType: {type: 'string'},
                        matchWithGitMasterRepo: {type: 'boolean'},
                        remoteip: {type: 'string'},
                        roles: {type: 'string'},
                        timestamp: {type: 'date', format: 'dateOptionalTime'},
                        username: {type: 'string'}
                    }
                },
                'user-workspace-download': {
                    properties: {
                        clientip: {type: 'string'},
                        email: {type: 'string'},
                        fileName: {type: 'string'},
                        fileSize: {type: 'long'},
                        fileType: {type: 'string'},
                        matchWithGitMasterRepo: {type: 'boolean'},
                        remoteip: {type: 'string'},
                        roles: {type: 'string'},
                        timestamp: {type: 'date', format: 'dateOptionalTime'},
                        username: {type: 'string'}
                    }
                }
            }
        }
    };

    my.describe("Get '"+index_name+"' index info", function () {
        
        my.it(" - request docs count and data mapping ", function () {
            var isRequestProcessed = false;

            runs(function () {

                client.count({index: index_name})
                    .then(function (response) {
                        console.log("Number of documents in index '%s' : %d", index_name, response.count);
                        expect(response.count).toBeGreaterThan(1);
                    })
                    .then(function () {
                        return client.indices.getMapping({ index: index_name})
                    })
                    .then(function (response) {
                        //my.logJSON(response);
                        expect(response).toEqualJson(index_map);
                        console.log("'%s' index map validated.", index_name);
                        deferred.resolve(client);
                        isRequestProcessed = true;
                    })
                    .catch(function (error) {
                        deferred.reject(error);
                        isRequestProcessed = true;
                    })

             });
            
            waitsFor(function () {
                return isRequestProcessed;
            }, this.getFullName() + " timeout.", 5*1000)
        })

    });

    return deferred.promise;

}


function getDsLogs(client) {
    var start = new Date();
    start.setDate(start.getDate()-15);
    return logRequests.getDsLogs(client, start)
        .then(function () { return client; })
}

function specTemplate() {
    var deferred = Q.defer();

    my.describe("Spec description", function () {

        my.it(" - test description ", function () {
            var isRequestProcessed = false;
            runs(function () {

            });

            waitsFor(function () {
                return isRequestProcessed;
            }, this.getFullName() + " timeout.", 5*1000)

        })

    });

    return deferred.promise;

}
