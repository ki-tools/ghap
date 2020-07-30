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