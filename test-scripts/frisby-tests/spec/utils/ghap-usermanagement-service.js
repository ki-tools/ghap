/**
 * Created by Vlad on 12.06.2015.
 */
var Qs = require('qs');
var request = require('request');
var util = require('util');

var my = require('../Common/ghap-lib');
var umsUrls = require('../ums/ums_urls');

var headers = {};

module.exports.getAllUsers = function (authHeader, parentDn_str, allGhapUsers_array, callback) {
	const EXPECTED_STATUS = 200;
	headers['content-type'] = 'application/json';
	headers[authHeader.Name] = authHeader.Value;
	request({
		url: umsUrls.getFindAllUsers_Url(parentDn_str),
		method: 'GET',
		headers: headers
	}, function (error, response, body) {
		var result = {
			is_successful: false,
			message: util.format("Get All Users"),
			status_code: 0
		};
		if (error) {
			result.message += ' error.';
			console.log(result.message);
			console.error(error)
		} else {
			result.status_code = response.statusCode;
			result.message += util.format(". Response status code %d", response.statusCode);
			console.log(result.message);
			if (response.statusCode == EXPECTED_STATUS) {
				result.is_successful = true;
				my.moveArray(my.jsonParse(body), allGhapUsers_array);
			} else
				if (body) console.log(body);
		}
		callback(result);
	});
};
