/**
 * Created by Vlad on 28.11.2015.
 */

var ini = require('ini');
var fs = require('fs');
var path = require('path');

var iniDirName = path.resolve(__dirname, '.ghap-e2e');

function getIniFileName(env_name) {
    return path.resolve(iniDirName, env_name.toUpperCase(), 'config');
}

function createIniDirIfNotExists(env_name) {
    var stats;
    try {
        stats = fs.statSync(iniDirName);
    } catch (e) {
        stats = null;
        fs.mkdirSync(iniDirName);
    }
    if (stats && !stats.isDirectory()) {
        throw new Error(iniDirName + ' is not a directory.');
    }

    var iniEnvDirName = path.resolve(iniDirName, env_name.toUpperCase());

    try {
        stats = fs.statSync(iniEnvDirName);
    } catch (e) {
        stats = null;
        fs.mkdirSync(iniEnvDirName);
    }
    if (stats && !stats.isDirectory()) {
        throw new Error(iniEnvDirName + ' is not a directory.');
    }
}

exports.get = function(env_name) {
    var stats;
    var ini_file_name = getIniFileName(env_name);
    try {
        stats = fs.statSync(ini_file_name);
    } catch (e) {
        stats = null;
    }
    var e2e_map;
    if (stats) {
        if (stats.isFile()) {
            e2e_map = ini.parse(fs.readFileSync(ini_file_name, 'utf-8'));
        } else {
            throw new Error(ini_file_name + ' is not a file.')
        }
    } else {
        e2e_map = {user:{password:''}};
    }    return e2e_map;
};

/**
 * Save ini_map object to .ghap/<env_name>/config file as ini-formatted
 * @param {object} ini_map
 * @param {string} env_name
 */
exports.save = function(ini_map, env_name) {
    createIniDirIfNotExists(env_name);
    var ini_file_name = getIniFileName(env_name);
    fs.writeFileSync(ini_file_name, ini.stringify(ini_map));
};