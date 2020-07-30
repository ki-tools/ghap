/**
 * Created by Vlad on 23.09.2015.
 */

exports = module.exports = {};

var e2eMailListener = require('./ghap-e2e-maillistener');

function GhapEmailListener(){
	this.deferred = protractor.promise.defer();
}

GhapEmailListener.prototype.startListening = function ( check_mail_callback ) {
	var start = new Date();
	var self = this;

	var myEmailListener = function(mail){

		console.log("Mail from '%s' with subject '%s' received.", mail.headers.from, mail.subject);
		//consoleLog(mail);
		var delay = mail.date - start;
		// let allow 3 sec. for time async
		if (delay < -3000) {
			console.log("Mail ignored as stale. Mail date %s", mail.date);
			console.log("delay %d", delay);
			return;
		}
		//if (mail.headers.from != 'ghap@certara.com') {
		//	console.log("Mail ignored since it is received not from Certara.");
		//	return;
		//}

		check_mail_callback (mail, self.deferred);
		if (!self.deferred.isPending())
			e2eMailListener.removeListener('mail', myEmailListener);
	};

	e2eMailListener.on("mail", myEmailListener);

};

exports.createGhapEmailListener = function() { return new GhapEmailListener(); };

exports.startMailListener = function() {
	e2eMailListener.start();
	console.log('Mail listener started.');
};

exports.stopMailListener = function() {
	e2eMailListener.stop();
	console.log('Mail listener stopped.');
};