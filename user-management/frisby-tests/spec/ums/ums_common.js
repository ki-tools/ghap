exports = module.exports = {};

exports.endOfLine = require('os').EOL;

exports.admin_username = 'Administrator';
exports.admin_password = '';
//exports.admin_username = 'GHAPAdministrator';
//exports.admin_password = '';

exports.stepPrefix = 'A';

var stepNum = 1;
exports.stepNum = stepNum;

// return num as string padded right with 0 to 3 chars
exports.getStepNumStr = function(n){
	if (typeof n !== 'number') n = stepNum++;
	var pad0 = '00'+ n.toString();
	return exports.stepPrefix  + pad0.substr(pad0.length-3);
};

//
// Parse body as JSON, ensuring not to re-parse when body is already an object (thanks @dcaylor)
//
exports.jsonParse = function (body) {
	var json = "";
	try {
		json = (typeof body === "object") ? body : JSON.parse(body);
	} catch(e) {
		throw new Error("Error parsing JSON string: " + e.message + "\n\tGiven: " + body);
	}
	return json;
};

exports.resultsHaveErrors = function(results) {
	return  results.every( function(element){	return (element[0] !== 0)	});
};

exports.logExecutionTime = function(start) {
	var exec_time_ms = new Date() - start;
	if (exec_time_ms > 2000)
		console.info("WARNING: Execution time is too long: %dms", exec_time_ms);
};

exports.copyProperties = function(from_object, to_object) {
	if ((typeof from_object !== 'object' ) || (typeof to_object !== 'object')) {
		console.error('Invalid parameters for copyProperties.')
		return;
	}
	for ( var key_name in from_object ){
		if ( from_object.hasOwnProperty( key_name ) ){
			if ( to_object.hasOwnProperty( key_name ) ){
				to_object[key_name] = from_object[key_name];
			}
			else
				console.error('Unknown property '+key_name+' in destination object')
		}
	}
};
