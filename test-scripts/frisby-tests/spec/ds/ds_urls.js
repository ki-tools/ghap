/**
 * Created by Vlad on 26.05.2016.
 */

var cfg = require('./../Common/ghap-config');

var
    service_path = cfg.dataSbmtService,
    userdata_path = '/rest/v1/DataSubmission';

exports.submitData_url = function() {
    return service_path + userdata_path + '/submit/';
};

exports.downloadData_url = function(token, file) {
    return service_path + userdata_path + '/submission/' + file + '?token=' + token;
};

exports.deleteFile_url = function(file) {
    return service_path + userdata_path + '/delete/' + file;
};

