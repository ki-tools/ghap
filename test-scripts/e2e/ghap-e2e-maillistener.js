/**
 * Created by vruzov on 22.10.2015.
 */

var cfg = require('./ghap-e2e-config');

var MailListener = require("mail-listener2");

var mailListener = new MailListener({
    username: cfg.userEmail,
    password: cfg.emailPassword,
    host: cfg.mailServer,
    port: cfg.mailServerPort, // imap port
    tls: true,
    tlsOptions: { rejectUnauthorized: false },
    mailbox: "INBOX", // mailbox to monitor
    //searchFilter: ["UNSEEN", "FLAGGED"], // the search filter being used after an IDLE notification has been retrieved
    markSeen: true, // all fetched email willbe marked as seen and not fetched next time
    fetchUnreadOnStart: false, // use it only if you want to get all unread email on lib start. Default is `false`,
    mailParserOptions: {streamAttachments: false}, // options to be passed to mailParser lib.
    attachments: false // download attachments as they are encountered to the project directory
    //attachmentOptions: { directory: "attachments/" } // specify a download directory for attachments
});

mailListener.on("server:connected", function(){
    console.log("Mail listener initialized.");
});

mailListener.on("server:disconnected", function(){
    console.log("IMAP server disconnected.");
});

mailListener.on("error", function(err){
    console.log(err);
});

module.exports = mailListener;
