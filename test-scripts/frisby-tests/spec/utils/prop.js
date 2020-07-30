/**
 * Created by Vlad on 13.06.2015.
 */

var prop_chars = {
	"|" : "/",
	"/" : "-",
	"-" : "\\",
	"\\" : "|"
};
var next_char = "|";
var intervalId = null;

module.exports.start = function (call, interval_ms) {
	if (isNaN(interval_ms)) interval_ms = 500;
	// http://stackoverflow.com/questions/10987468/cursor-blinking-removal-in-terminal-how-to
	// hide cursor
	var isTTY = false;
	if (process.stdout.isTTY) {
		isTTY = true;
		process.stdout.write("\033[?25l");
	}
	// nodej move cursor to beginning of line bash
	// http://stackoverflow.com/questions/10585683/how-do-you-edit-existing-text-and-move-the-cursor-around-in-the-terminal
	intervalId = setInterval(function () {
		var out_str = next_char;
		if (call) out_str += call();
		if (isTTY)
			out_str = "\033[s" + out_str + "\033[u";
		else
			out_str = "\r" + out_str;
		process.stdout.write(out_str);
		next_char = prop_chars[next_char];
	}, interval_ms);
};

module.exports.stop = function(){
	if (intervalId !== null){
		clearInterval(intervalId);
		intervalId = null;
		// show cursor
		if (process.stdout.isTTY)
			process.stdout.write("\033[?25h");
	}
};
