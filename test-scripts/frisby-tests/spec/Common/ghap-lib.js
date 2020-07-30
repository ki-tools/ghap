var path = require('path');
var util = require("util");
var Q = require('q');
var fs = require('fs');

exports = module.exports = {};

exports.endOfLine = require('os').EOL;

exports.admin_username = 'Administrator';
exports.admin_password = '';
//exports.admin_username = 'GHAPAdministrator';
//exports.admin_password = '';

exports.stepPrefix = 'noPrefix';

exports.stepNum = 1;

// return num as string padded right with 0 to 3 chars
exports.getStepNumStr = function(n){
	if (typeof n !== 'number') n = exports.stepNum++;
	var pad0 = '00'+ n.toString();
	var result = '_' + exports.stepPrefix +'_' + pad0.substr(pad0.length-3)+'_';
	//console.log(result);
    return result;
};

exports.logStepNumber = function(){
    process.stdout.write('[' + exports.stepNum + ']');
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

var longExecutionInterval = 2000;

exports.setLongExecutionInterval = function(interval_ms){
	longExecutionInterval = interval_ms;
};

exports.logExecutionTime = function(exec_or_start_time) {
	var exec_time_ms;
	if (exec_or_start_time > 1000*60*60*24)
		exec_time_ms = new Date() - exec_or_start_time;
	else
		exec_time_ms = exec_or_start_time;

	if (exec_time_ms > longExecutionInterval)
		console.info("WARNING: Execution time is too long: %dms", exec_time_ms);
};

/**
 * Copy own properties of from_object to to_object
 * @param {object} from_object
 * @param {object} to_object
 */
exports.copyProperties = function(from_object, to_object) {
	if ((typeof from_object !== 'object' ) || (typeof to_object !== 'object')) {
		console.error('Invalid parameters in copyProperties.');
		console.error("Form:", from_object);
		console.error("To:", to_object);
		throw new Error("Invalid parameters in copyProperties.");
	}
	for ( var key_name in from_object ){
		if ( from_object.hasOwnProperty( key_name ) ){
			if ( to_object.hasOwnProperty( key_name ) ){
				to_object[key_name] = from_object[key_name];
			}
			else
				console.error("Property '%s' not found in the destination object",key_name);
		}
	}
};

exports.moveArray = function(src_array, dest_array) {
	if (dest_array instanceof Array)
		dest_array.length = 0;
	else
		dest_array = [];

	if (src_array instanceof Array) {
		var element = src_array.shift();
		while(element) {
			dest_array.push( element  );
			element = src_array.shift();
		}
	}
};

exports.cloneObject = function(obj){
	// http://stackoverflow.com/questions/5055746/cloning-an-object-in-node-js
	return util._extend({},obj)
};

exports.getModuleName = function(module_path){
	return module_path.split(path.sep).pop()
};

exports.logModuleName = function(module_path){
	console.log("*************  %s started.",exports.getModuleName(module_path));
};

exports.getInspectObjStr = function(obj){
	return util.inspect(obj, {showHidden: false, depth: null})
};

exports.logJSON = function (json){
	console.log(exports.getInspectObjStr(exports.jsonParse(json)));
};

exports.log = function (obj){
	var str = typeof obj;
	switch (str) {
		case 'object':
			str = exports.getInspectObjStr(obj);
			break;
		case 'string':
			str = util.format.apply(this,arguments);;
			break;
		default :
			str = '<' + str + '>';
	}
	console.log(str);
};

exports.pauseJasmine = function(interval_ms){
	describe('Pause', function(){
		it('for '+interval_ms+' ms.',function(){
			console.log(this.getFullName());
			waits(interval_ms)
		},interval_ms+50);
	})
};

function fixDigits (input, length) {
	while (input.toString().length < length) {
		input = "0" + input;
	}
	return input;
}

exports.logTime = function (time_ms){
	var seconds = Math.floor(time_ms/1000);
	var mile_sec = time_ms - seconds * 1000;
	var hours = Math.floor(seconds/3600);
	seconds -= hours * 3600;
	var minutes = Math.floor(seconds/60);
	seconds -= minutes * 60;
	return util.format("%s:%s:%s.%s",
		fixDigits(hours,2), fixDigits(minutes,2), fixDigits(seconds,2), fixDigits(mile_sec,3));
};

exports.reportError = function(err){
	if (err instanceof Error) {
		console.error(err);
		console.error(err.stack);
		process.exitCode = 5;
	}
};

exports.finalTest = function(){
    exports.describe("\nFinal Test", function () {
        exports.it(" - process exit code should be zero or undefined", function () {
            if (exports.getJasmineFailedSuitesCount()) process.exitCode = 4;
            expect(process.exitCode).toBeFalsy();  // to generate non-zero exit code in jasmine runner
            if (process.exitCode)
                console.log('Test case finished with errors. Check console log for details.');
            else
                console.log('Test case finished successfully.');

        })
    });
};

// http://stackoverflow.com/questions/8834126/how-to-efficiently-check-if-variable-is-array-or-object-in-nodejs-v8
function isObject(elem) {
	if (typeof elem === 'object') {
		return !(elem instanceof Array);
	}
	return false;
}

/**
 *
 * @param {Array} array of objects or primitives
 * @param {string} key_name - key name for objects or value for primitives
 * @param {string} [key_value] - key value for objects. Ignored for primitives
 * @returns {*} array element or null
 */
exports.findElementInArray = function (array, key_name, key_value) {
	if (array instanceof Array) {

		//var filtered_array = array.filter(function (element) {
		//	return (element[key_name] == key_value)
		//});
		//if (filtered_array.length > 0)
		//	return filtered_array[0];

		for (var i = 0; i < array.length; i++) {
			var element = array[i];
			if (isObject(element) && element.hasOwnProperty(key_name) && element[key_name] === key_value)
				return element;
			else if (element === key_name)
				return element;
		}

	} else
		console.error('Invalid array argument in findInArray.');

	return null;
};

exports.logSpecFullName=function(){
	console.log(jasmine.getEnv().currentSpec.getFullName());
};

// http://stackoverflow.com/questions/2686855/is-there-a-javascript-function-that-can-pad-a-string-to-get-to-a-determined-leng
// var padding = Array(256).join(' ') // make a string of 255 spaces
//
exports.pad = function(pad, str, padLeft) {
	if (util.isNumber(pad))
		pad = new Array(pad).join(' ');
	if (typeof str === 'undefined')
		return pad;
	if (padLeft) {
		return (pad + str).slice(-pad.length);
	} else {
		return (str + pad).substring(0, pad.length);
	}
};

var current_test_name;
module.exports.describe = function (test_name, spec_func) {
    current_test_name = test_name;
    describe("Frisby Test"+ exports.getStepNumStr() + test_name, spec_func);
};

module.exports.it = function (descr, it_func) {
    var spec = it(descr, function(){
        var log_message = "\n" + current_test_name + spec.description + '. [' + (exports.stepNum-1) + ']';
        console.log(log_message);
        it_func.call(spec);
    })
};

module.exports.getJasmineFailedSuitesCount = function () {
	// http://stackoverflow.com/questions/11526880/display-total-number-of-tests-expectations-run-by-jasmine
    var count = 0;
    jasmine.getEnv().currentRunner().suites().forEach(function (suite) {
        count += suite.results().failedCount;
    });
    return count;
};

function dayStr(date){
	return date.toISOString()
		.substr(0,10)
		.replace(new RegExp('-', 'g'), '')
}

function timeStr(date){
	return date.toISOString()
		.substr(11,8)
		.replace(new RegExp(':', 'g'),'')
}

module.exports.dateTimeStr = function(date){
	return dayStr(date)+timeStr(date)
};

module.exports.copyFile = function (srcPath, dstPath) {
    var deferred = Q.defer();
    var srcFileName = path.basename(srcPath);
    var dstFileName = path.basename(dstPath);

    exports.describe("Copy file", function () {
        exports.it(" - save '" + srcFileName + "' as '"+ dstFileName +"'" , function () {
            var isProcessed = false;
            var isCopied = false;

            var rd = fs.createReadStream(srcPath);
            rd.on("error", function(err) {
                deferred.reject(new Error(err));
                isProcessed = true;
            });
            var wr = fs.createWriteStream(dstPath);
            wr.on("error", function(err) {
                deferred.reject(new Error(err));
                isProcessed = true;
            });
            wr.on("close", function() {
                deferred.resolve();
                isCopied = true;
                isProcessed = true;
            });

            runs(function () { rd.pipe(wr); });

            waitsFor(function () {
                return isProcessed;
            }, this.getFullName() + " timeout.", 3*1000);

            runs(function () {
                expect(isCopied).toBe(true);
                waits(200);
            });

        })
    });

    return deferred.promise;
};
