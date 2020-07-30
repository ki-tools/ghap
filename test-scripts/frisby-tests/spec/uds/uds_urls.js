/**
 * Created by Vlad on 15.12.2015.
 */

var cfg = require('./../Common/ghap-config');

var
    service_path = cfg.userdataService,
    userdata_path = '/rest/v1/UserData';

exports.createFolder_url = function(user_id, path) {
    return service_path + userdata_path + '/folder/' + user_id + '?path=' + path;
};

exports.dir_url = function(user_id, path) {
    return service_path + userdata_path + '/data-location/' + user_id + '?path=' + path;
};

exports.deleteFile_url = function(user_id, path, file) {
    return service_path + userdata_path + '/delete/' + user_id + '?path=' + path + '&file=' + file;
};

exports.submitData_url = function(user_id, path) {
    return service_path + userdata_path + '/submit-location/' + user_id + '?path=' + path;
};

exports.downloadData_url = function(user_id, token, path, file) {
    return service_path + userdata_path + '/zip/' + user_id + '?token=' + token + '&path=' + path + '&file=' + file;
};
