// Mikiti Takada control flow library
// http://book.mixu.net/node/ch7.html

module.exports.series = function (callbacks, last) {
	var results = [];
	function next() {
		var callback = callbacks.shift();
		if(callback) {
			callback(function() {
				results.push(Array.prototype.slice.call(arguments));
				next();
			});
		} else {
			last(results);
		}
	}
	next();
};

module.exports.fullParallel = function (callbacks, last) {
	var results = [];
	var result_count = 0;
	callbacks.forEach(function(callback, index) {
		callback( function() {
			results[index] = Array.prototype.slice.call(arguments);
			result_count++;
			if(result_count == callbacks.length) {
				last(results);
			}
		});
	});
};