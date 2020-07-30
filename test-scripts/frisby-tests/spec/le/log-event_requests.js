/**
 * Created by Vlad on 27.05.2016.
 */

var Q = require('q');
var AWS = require('aws-sdk');
//var httpAwsEs = require('http-aws-es');
// http-aws-es package.json use
//   "scripts": {
//       "prepublish": "babel ./connector-es6.js > ./connector.js",
// and it looks that feature does not work on our jenkins CI instance
// npm install babel does not help....
// so possible reason is old version op npm on jenkins server
// As a temporarily decision I use compiled version of connector-es6.js
var httpAwsEs = require('./connector');

var my = require('../Common/ghap-lib');
var cfg = require('../Common/ghap-config');

function getAWSCredentialsObj () {
    var deferred = Q.defer();

    // GQ: node.js using AWS.CredentialProviderChain example
    // http://www.codegur2.com/35038838/determine-if-aws-sdk-is-has-global-credentials-configured
    //
    my.describe("Get AWS Credentials Object", function () {

        my.it(" - handle request to AWS.SharedIniFileCredentials", function () {
            var isRequestHandled = false;
            runs(function () {
                var creds = new AWS.SharedIniFileCredentials({
                    // http://stackoverflow.com/questions/3133243/how-to-get-path-to-current-script-with-node-js
                    filename: __dirname + '/.aws/credentials',
                    profile: cfg.environment
                });
                // creds.accessKeyId can be null so I do not use !==
                if ((creds.accessKeyId == null)) {
                    deferred.reject(new Error("AWS credentials for '"+cfg.environment+"' environment are not provided."));
                } else{
                    console.log("AWS credentials for '%s' environment are found. Profile is loaded.", cfg.environment);
                    AWS.config.credentials = creds;
                    deferred.resolve(creds)
                }

                /*
                AWS.config.credentialProvider.resolve(function (err, credentials) {
                    if (err)
                        deferred.reject(err);
                    else if (credentials == null)
                        deferred.reject(new Error("AWS Credentials are not provided."));
                    else {
                        console.log("Credentials are provided by '%s'", credentials.constructor.name);
                        if (credentials instanceof AWS.SharedIniFileCredentials) {
                            var creds = new AWS.SharedIniFileCredentials({profile: cfg.environment});
                            if ((creds.accessKeyId != null)) {
                                console.log("Credentials for '%s' environment are found. Profile will be changed.", cfg.environment)
                                AWS.config.credentials = creds;
                                credentials = creds;
                            }
                        }
                        deferred.resolve(credentials)
                    } */

                isRequestHandled = true;
                // lets promise to be handled
                waits(300);

            });

            waitsFor(function () {
                return isRequestHandled;
            }, "Response from CredentialProviderChain not received in time", 3 * 1000)
        })

    });

    return deferred.promise;
}

function getEsClient(aws_credentials) {

    // es.service.url
    // var my_host = "https://search-log-events-gqoyntjuhapanyx7j5kszghjoy.us-east-1.es.amazonaws.com";
    var es = require('elasticsearch').Client({
        hosts: cfg.awsEsUrl,
        connectionClass: httpAwsEs,
        amazonES: {
            region: "us-east-1",
            credentials: aws_credentials
        }
    });
    
    return Q(es);
}

module.exports.checkAccess = function(es_client) {

    var deferred = Q.defer();
    my.describe("Try access to ES", function () {

        my.it(" - ping host", function () {
            var isPingFinished = false;
            runs(function () {
                es_client.ping({
                    // ping usually has a 3000ms timeout
                    requestTimeout: Infinity
                }, function (error) {
                    if (error) {
                        console.log("PING failed.");
                        deferred.reject(error);
                    } else {
                        console.log("PING is complete");
                        deferred.resolve(es_client);
                    }
                    isPingFinished = true;
                    // lets promise to be handled
                    waits(300);
                });

            });

            waitsFor(function () {
                return isPingFinished;
            }, "PING response is not received in time", 3 * 1000)

        })
    });

    return deferred.promise;
};

module.exports.getDsLogs = function(client, start, end) {
    return module.exports.getLogs(client, 'ghap', 'dataset-download', start, end)
};

module.exports.getUwsLogs = function(client, start, end) {
    return module.exports.getLogs(client, 'ghap', 'user-workspace-download', start, end)
};

module.exports.getLogs = function(client, indexName, typeName, start, end) {
    var deferred = Q.defer();

    var start_date = new Date(start);
    var end_date;
    if (end)
        end_date = new Date(end);
    else
        end_date = new Date();
    
    my.describe("Get Dataset Downloads Log", function () {

        my.it(" - make search", function () {
            console.log("Timestamp range: ['%s' - '%s']", start_date.toISOString(), end_date.toISOString());
            var isRequestProcessed = false;
            runs(function () {
                // https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-range-query.html
                client.search({
                    "index": indexName,
                    "type": typeName,
                    "body": {
                        "query": {
                            "range": {
                                "timestamp": {
                                    "gte": start_date.toISOString(),
                                    "lt": end_date.toISOString()
                                }
                            }
                        }
                    }
                })
                    .then(function (response) {
                        console.log("%d documents received",response.hits.total);
                        deferred.resolve(response);
                        isRequestProcessed = true;
                    })
                    .catch(function (err) {
                        deferred.reject(err);
                        isRequestProcessed = true;
                    })

            });

            waitsFor(function () {
                return isRequestProcessed;
            }, this.getFullName() + " timeout.", 5*1000)

        })

    });

    return deferred.promise;

};

module.exports.getAwsEsClient = function () {
    return getAWSCredentialsObj()
        .then(getEsClient)
};

module.exports.flush = function (client) {
    var deferred = Q.defer();

    my.describe("Flush ES index", function () {

        my.it(" - make request", function () {
            var isRequestProcessed = false;
            runs(function () {
                // https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-range-query.html
                client.indices.flush({
                    "index": 'ghap'
                })
                    .then(function (response) {
                        my.logJSON(response);
                        deferred.resolve(response);
                        isRequestProcessed = true;
                    })
                    .catch(function (err) {
                        deferred.reject(err);
                        isRequestProcessed = true;
                    })

            });

            waitsFor(function () {
                return isRequestProcessed;
            }, this.getFullName() + " timeout.", 5*1000)

        })

    });

    return deferred.promise;
};

module.exports.validateLog = function (response, username, filename) {
    var deferred = Q.defer();
    //my.logJSON(response);
    my.describe("Validate Download Log", function () {
        my.it(" - response should have docs", function () {
            expect(response.hits.total).toBeGreaterThan(0);
        });
        my.it(" - record for file '" + filename + "' and user '" + username + "' should be present", function () {
            var isRecord_found = false;
            response.hits.hits.forEach(function (rec) {
                if ((rec._source.fileName === filename) && (rec._source.username === username)) {
                    console.log("Record found.");
                    isRecord_found = true;
                }
            });
            expect(isRecord_found).toBe(true);
            if (!isRecord_found) console.error("Record not found");
            deferred.resolve();
            waits(200);
        })
    });
    return deferred.promise;
};