/**
 * Created by Vlad on 11.06.2015.
 */
var util = require('util');
var log_stdout = process.stdout;
var consoleLogIsOn = true;

console.log = function() {
	if (consoleLogIsOn)
		log_stdout.write(util.format.apply(this,arguments) + '\n');
};

console.put = function() {
	if (consoleLogIsOn)
		log_stdout.write(util.format.apply(this,arguments));
};

module.exports.setConsoleLogOn = function(){
	consoleLogIsOn = true;
	console.log('Console.log switched ON.')
};

module.exports.setConsoleLogOff = function(){
	consoleLogIsOn = true;
	console.put("\nConsole.log switched OFF.");
	consoleLogIsOn = false;
};
