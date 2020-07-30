/**
 * Created by Vlad on 15.12.2015.
 */

var my = require('./../Common/ghap-lib');

var Q = require('q');
var fs = require('fs');
var module_path = require('path');
var FormData = require('form-data');

var ghapFrisby = require('./../Common/ghap-frisby');

var udsUrls = require('./uds_urls');

/**
 * Create folder in user workspace on the specified path
 * @param authHeader
 * @param ums_user
 * @param folder_path path to thr folder
 * @returns {Promise} resolved with 200 if folder is created, with 409 if folder already exists, or rejected on error
 */
module.exports.createFolder = function(authHeader, ums_user, folder_path){
    return ghapFrisby.create(authHeader, ' Create Folder')
        .put(udsUrls.createFolder_url(ums_user.getGuid(),folder_path))
        .setLogMessage("Create folder '%s' in the workspace for user '%s'", folder_path, ums_user.getName())
        .onSuccess(function () {
            return 200;
        })
        .onError(function (response_status) {
            return response_status == 409;
        })
        .returnPromise();
};

module.exports.dir = function(authHeader, ums_user, path){
    return ghapFrisby.create(authHeader, ' Dir Workspace At Path')
        .get(udsUrls.dir_url(ums_user.getGuid(), path))
        .setLogMessage("List workspace data at path '%s' for user '%s'", path, ums_user.getName())
        .onSuccess(function (body) {
            return my.jsonParse(body);
        })
        .returnPromise();
};

module.exports.deleteFile = function(authHeader, ums_user, path, file){
    return ghapFrisby.create(authHeader, ' Delete File')
        .delete(udsUrls.deleteFile_url(ums_user.getGuid(), path, file))
        .setLogMessage("Delete file '%s' in the workspace for user '%s' in path '%s'",file, ums_user.getName(), path)
        .returnPromise();
};

module.exports.uploadFile = function(authHeader, ums_user, path, full_file_name){

    var deferred = Q.defer();
    var err_msg;


    fs.stat(full_file_name, function(err, stat) {
        if (err === null) {
            // file exists
            if (stat.isFile()) {

                // based on the following example ( GQ 'frisby upload' --> https://github.com/vlucas/frisby/issues/84 )
                // https://github.com/kreutter/frisby/blob/master/examples/httpbin_multipart_spec.js

                var form = new FormData();
                form.append('file', fs.createReadStream(full_file_name), {
                    knownLength: fs.statSync(full_file_name).size // we need to set the knownLength so we can call form.getLengthSync()
                });

                var file_name = module_path.basename(full_file_name);
                ghapFrisby.create(authHeader, ' Upload File')
                    .put(udsUrls.submitData_url(ums_user.getGuid(), path), form)
                    .addHeader('content-type', 'multipart/form-data; boundary=' + form.getBoundary())
                    .addHeader('content-length', form.getLengthSync())
                    .setLogMessage("Upload file '%s' to the workspace for user '%s' in path '%s'", file_name, ums_user.getName(), path)
                    .onSuccess(function () {
                        deferred.resolve(200)
                    })
                    .onError(function (response_status) {
                        if (response_status == 409) {
                            deferred.resolve(response_status);
                            return true;
                        }
                        else {
                            deferred.reject(response_status);
                            return false;
                        }
                    })
                    .returnPromise()

            } else {
                err_msg = "'" + full_file_name +"' is not a file. Upload cancelled.";
                deferred.reject(new Error(err_msg));
            }
        } else {
            if (err.code === 'ENOENT') {
                err_msg = "File '" + full_file_name +"' does not exist. Upload cancelled.";
                deferred.reject(new Error(err_msg));
            }
            else
                deferred.reject(err)
        }

    });

    return deferred.promise;

};

module.exports.downloadFile = function(oAuth, ums_user, path, file){
    // https://github.com/request/request
    // see description of 'encoding' option
    // (Note: if you expect binary data, you should set encoding: null.)
    return ghapFrisby.create(oAuth.header, ' Download File')
        .get(udsUrls.downloadData_url(ums_user.getGuid(), oAuth.access_token, path, file),{encoding:null})
        .setLogMessage("Download file '%s' from the workspace of user '%s' in path '%s'",file, ums_user.getName(), path)
        .onSuccess(function (body) {
            return body
        })
        .returnPromise();
};