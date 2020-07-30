/**
 * Created by Vlad on 02.03.2016.
 */

var Q = require('q');
var util = require('util');
var verbose = true;
function log(){
  if (verbose) process.stdout.write(util.format.apply(this,arguments));
}

var cfg = require('./../Common/ghap-config');

var environment = cfg.environment;
if (typeof process.argv[2] === 'string'){
  environment = process.argv[2];
}

if (environment !== cfg.environment) {
  if (!cfg.setConfig(environment)) {
    console.log("Configuration for '%s' environment not found. ", environment);
    process.exit(1);
  }
}

cfg.usersS3Bucket = "userscratch-us-east-1-"+cfg.environment;

var my = require('../Common/ghap-lib');
var ghapRq = require('./ghap-requests');

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./ghap-oauth-promise');
var oAuth = oAuthService.makeOAuthClient();

// Load the AWS SDK for Node.js
var AWS = require('aws-sdk');
// http://docs.aws.amazon.com/AWSJavaScriptSDK/guide/node-configuring.html
AWS.config.loadFromPath('aws_'+cfg.environment+'_config.json');

var s3 = new AWS.S3();

getUsersS3FoldersCount()
  .then(function(){
    return oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
  })
  .then(function() {
    return ghapRq.getAllUsers(oAuth.header, testerAdmin.getParentDn())
  })
  .then(getUsersS3Folders)
  .then(final)
  .catch(my.reportError);

function getUsersS3FoldersCount() {
  console.log('Get count of users folders in S3 bucket.');
  var deferred = Q.defer();

  var s3_params = {
    Bucket: cfg.usersS3Bucket,
    Prefix: 'users/'
  };

  var requestsCount = 0;
  var totalObjectsCount = 0;

  function processListObjectsResponse(err, data) {
    if (err) {
      console.log("S3 error:", err);
      console.log(s3_params);
      deferred.reject();
      return;
    }

    requestsCount++;
    if (data) {
      if ((requestsCount === 1) && (data.Contents.length === 0)) {
        console.log("Folder '%s' not found in bucket '%s'", s3_params.Prefix, s3_params.Bucket);
        deferred.reject();
      }
      else {
        totalObjectsCount += data.Contents.length;
        if (data.IsTruncated || (requestsCount > 1)) {
          console.log("%d objects received in response for %d request.", data.Contents.length, requestsCount);
        }
        if (data.IsTruncated) {
          s3_params.Marker = data.Contents[data.Contents.length-1].Key;
          s3.listObjects(s3_params, processListObjectsResponse);
        } else {
          console.log("\n%d objects found in bucket '%s' with prefix '%s'\n", totalObjectsCount, s3_params.Bucket, s3_params.Prefix);
          deferred.resolve(totalObjectsCount);
        }
      }
    } else {
      console.log("No objects found in bucket '%s' with prefix '%s'", s3_params.Bucket, s3_params.Prefix);
      deferred.resolve(0);
    }
  }

  // http://docs.aws.amazon.com/AWSJavaScriptSDK/latest/AWS/S3.html
  s3.listObjects(s3_params, processListObjectsResponse);

  return deferred.promise;
}

function getUsersS3Folders(allUsers) {

  console.log();
  var notEmptyFolders_count = 0;
  var promises = [];

  allUsers.forEach(function(user){
    promises.push(
      getUserS3FolderObjectCount(user)
        .then(function(count){
          if (count > 1) {
            console.log("%s user has %d objects in folder '%s'", user.name, count, user.guid);
            notEmptyFolders_count++;
          }
        })
    )
  });

  return Q.all(promises)
    .then(function(){return notEmptyFolders_count});
}

function getUserS3FolderObjectCount(user) {
  var deferred = Q.defer();

  var s3_params = {
    Bucket: cfg.usersS3Bucket,
    Prefix: 'users/'+user.guid
  };

  s3.listObjects(s3_params, function(err, data){
    if (err) {
      console.log("S3 error:", err);
      console.log(s3_params);
      deferred.reject();
      return;
    }
    if (data) {
      deferred.resolve(data.Contents.length)
    } else {
      console.log("No data found for user '%s'", user.name);
      deferred.reject();
    }

  });

  return deferred.promise;
}

function final (count) {
  console.log("\n%d users have non empty folders on S3",count);
  console.log("Script is finished.")
}