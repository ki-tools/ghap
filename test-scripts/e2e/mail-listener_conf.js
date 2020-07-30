/**
 * Created by Vlad on 02.09.2015.
 */

exports.config = {
	seleniumAddress: 'http://localhost:4444/wd/hub',

	specs: ['mail-listener_spec.js'],

	multiCapabilities: [
		//{ browserName: 'chrome'}
		{ browserName: 'ie'}
		//{
		//	'browserName': 'phantomjs',
		//
		//	/*
		//	 * Can be used to specify the phantomjs binary path.
		//	 * This can generally be ommitted if you installed phantomjs globally.
		//	 */
		//	'phantomjs.binary.path': require('phantomjs').path,
		//
		//	/*
		//	 * Command line args to pass to ghostdriver, phantomjs's browser driver.
		//	 * See https://github.com/detro/ghostdriver#faq
		//	 */
		//	'phantomjs.ghostdriver.cli.args': ['--loglevel=DEBUG']
		//}
	],

	onPrepare: function () {
		var MailListener = require("mail-listener2");

		// here goes your email connection configuration
		var mailListener = new MailListener({
			username: "ghap.tester@gmail.com",
			password: "",
			host: "imap.gmail.com",
			port: 993, // imap port
			tls: true,
			tlsOptions: { rejectUnauthorized: false },
			mailbox: "INBOX", // mailbox to monitor
			//searchFilter: ["UNSEEN", "FLAGGED"], // the search filter being used after an IDLE notification has been retrieved
			markSeen: true, // all fetched email willbe marked as seen and not fetched next time
			fetchUnreadOnStart: true // use it only if you want to get all unread email on lib start. Default is `false`,
			//mailParserOptions: {streamAttachments: false}, // options to be passed to mailParser lib.
			//attachments: false // download attachments as they are encountered to the project directory
			//attachmentOptions: { directory: "attachments/" } // specify a download directory for attachments
		});

		mailListener.on("server:connected", function(){
			console.log("Mail listener connected to IMAP server.");
		});

		mailListener.on("server:disconnected", function(){
			console.log("Mail listener disconnected from IMAP server.");
		});

		mailListener.on("error", function(err){
			console.log('Mail listener error:', err);
		});

		global.mailListener = mailListener;
	},

	onCleanUp: function () {
		mailListener.stop();
	}

};
