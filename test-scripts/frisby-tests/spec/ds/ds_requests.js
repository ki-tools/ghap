/**
 * Created by Vlad on 26.05.2016.
 */

var my = require('./../Common/ghap-lib');

var Q = require('q');
var fs = require('fs');
var module_path = require('path');
var FormData = require('form-data');

var ghapFrisby = require('./../Common/ghap-frisby');

var dsUrls = require('./ds_urls');

module.exports.uploadFile = function(authHeader, full_file_name){

    var deferred = Q.defer();
    var err_msg;

    fs.stat(full_file_name, function(err, stat) {
        if (err === null) {
            // file exists
            if (stat.isFile()) {

                // based on the following example ( GQ 'frisby upload' --> https://github.com/vlucas/frisby/issues/84 )
                // https://github.com/kreutter/frisby/blob/master/examples/httpbin_multipart_spec.js

                var file_name = module_path.basename(full_file_name);
                var form = new FormData();
                form.append('file', fs.createReadStream(full_file_name), {
                    knownLength: fs.statSync(full_file_name).size, // we need to set the knownLength so we can call form.getLengthSync()
                });

                ghapFrisby.create(authHeader, ' Upload Dataset File')
                    .put(dsUrls.submitData_url(), form)
                    .addHeader('content-type', 'multipart/form-data; boundary=' + form.getBoundary())
                    .addHeader('content-length', form.getLengthSync())
                    .setLogMessage("Upload file '%s' to the dataset storage.", file_name)
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

module.exports.downloadFile = function(oAuth, file){
    // https://github.com/request/request
    // see description of 'encoding' option
    // (Note: if you expect binary data, you should set encoding: null.)
    return ghapFrisby.create(oAuth.header, ' Download Dataset File')
        .get(dsUrls.downloadData_url(oAuth.access_token, encodeURIComponent(file)),{encoding:null})
        .setLogMessage("Download dataset file '%s'",file)
        .onSuccess(function (body) {
            return body
        })
        .returnPromise();
};

module.exports.deleteFile = function(authHeader, file){
    return ghapFrisby.create(authHeader, ' Delete Dataset File')
        .delete(dsUrls.deleteFile_url(encodeURIComponent(file)))
        .setLogMessage("Delete dataset file '%s'",file)
        .returnPromise();
};
