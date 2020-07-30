/**
 * Created by Vlad on 20.08.2015.
 */

var my = require('../Common/ghap-lib');
my.stepPrefix = 'LaunchSSH';
my.logModuleName(module.filename);

var umsRequests = require('../ums/ums_requests');

var psRequests = require('./ps_requests');
var psResources = require('./ps_resources');
var Tester = require('./ps_tester').make();
var testerVPGs = [];

var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(Tester.getName(), Tester.getPassword())
	.then(umsRequests.validateToken)
    .then(function(){return umsRequests.pullUser(oAuth, Tester)})
	.then(runSuite)
	.catch(my.reportError);

function runSuite() {
	return psRequests.multiVpgGetStacks4User(oAuth.header, Tester, testerVPGs)
		.then(findLinuxVPGaddress)
		.then(validateLinuxVPG)
}

function findLinuxVPGaddress(){
	return psRequests.multiVpgGetStatuses4User(oAuth.header, Tester, testerVPGs)
		.then(function(){
			var linuxVPG_ip = null;
			var linuxVPG_is_found = testerVPGs.some(function(vpg){
				if (vpg.computeResources.length)
					return vpg.computeResources.some(function(comp_res){
						if (comp_res.status != 'running') return false;
						if (comp_res.instanceOsType.toUpperCase() != 'LINUX') return false;
						linuxVPG_ip = comp_res.address;
						console.log("Linux instance found with IP '%s'", linuxVPG_ip);
						return true;
					});
				else
					return false;
			});
			if (linuxVPG_is_found) return linuxVPG_ip;
			throw new Error('Running linux VPG not found.')
		})
}

function validateLinuxVPG(linuxVPG_ip) {

	var SSH = require('simple-ssh');

	// Temporarily decision of keyboard-interactive authentication issue
	SSH.prototype.start = function() {
		var self = this;

		self._c.on('ready', function() {
			if (self._commands.length > 0) {
				self._queueCommand(0);
			} else {
				self._c.end();
			}
		});

		self._c.on('keyboard-interactive', function(name, instructions, instructionsLang, prompts, finish) {
			console.log('Connection :: keyboard-interactive');
			finish([self.pass]);
		});

		self._c.connect({
			host: self.host,
			port: self.port || 22,
			username: self.user,
			password: self.pass,
			readyTimeout: self.timeout,
			tryKeyboard: true
		});
	};

	var domain = 'PROD\\';
	var username = Tester.getName();
	var user_password = Tester.getPassword();

	var test_record_str = "Test record date: "+ new Date();
	var ssh = new SSH({
		host: linuxVPG_ip,
		user: domain + username,
		pass: user_password
	});

	console.log(ssh.user);

	var execResults = [];

	var connection_is_ready = false;
	var error_happens = false;
	var socket_was_closed = false;

	describe('Launch SSH connection.',function(){
		it('Expect READY status.', function(){
			runs(function(){

				console.log(this.getFullName());

				ssh.exec('echo $PATH', {exit: handleExit })
					.on('error', function(err) {
						console.log(err);
						error_happens = true;
						ssh.end();})
					.on('ready', function() {
						console.log("Connection is READY.");
						connection_is_ready = true;
					})
					.on('close', function(hadErr) {
						console.log("Connection closed %s", hadErr ? 'due to error.' : 'without error.');
						socket_was_closed = true;
						if (hadErr) error_happens = true;
					})
					.start();
			});

			waitsFor(function(){return connection_is_ready},'awaiting READY status.',ssh.timeout+100);

			runs(function(){
				expect(connection_is_ready).toBe(true);
				expect(error_happens).not.toBe(true);

				describe('Execute linux commands remotely.', function(){
					it('Expect that connection will be finally closed without errors.', function(){
						runs(function(){
							console.log(this.getFullName());

							ssh.exec('echo ${PWD##*/}', {exit: handleExit })
								.exec('cd /torquefs/', {exit: function(code, std_out, std_err){
									if (code === 0) ssh.baseDir = '/torquefs';
									handleExit(code, std_out, std_err);
								}	})
								.exec('rm -f -r T1', {exit: handleExit })
								.exec('mkdir T1', {exit: handleExit })
								.exec('cd T1', {exit: function(code, std_out, std_err){
									if (code === 0) ssh.baseDir += '/T1';
									handleExit(code, std_out, std_err);
								}	})
								.exec('echo "'+test_record_str+'" > test.txt', {exit: handleExit })
								.exec('cat test.txt', {exit: handleExit })
						})
					});

					waitsFor(function(){return socket_was_closed},' all commands are executed.', 10000);

					runs(function(){
						expect(socket_was_closed).toBe(true);
						expect(error_happens).toBe(false);

						describe('Check commands results.', function(){

							it('All exit codes should be zero.', function(){
								console.log(this.getFullName());
								execResults.forEach(function(exec_result){
									expect(exec_result[0]).toBe(0);
								})
							});

							it('Stdout of last executed command should match to the test string.', function(){
								console.log(this.getFullName());
								var last_stdout = execResults.pop()[1];
								expect(last_stdout).toBe(test_record_str+"\n");
							});

						})
					})

				})
			})

		})
	});

	function handleExit(code, std_out, std_err){
		var current_command_index = ssh._c._curChan;
		var current_command = ssh._commands[current_command_index].cmd;
		execResults.push([code, std_out, std_err]);
		console.log("Command '%s' exit code %d", current_command, code);
		if (std_out) console.log('stdOut:', std_out);
		if (std_err) console.log('stdErr:', std_err);
	}

}

// Q: node.js ssh2 authentication failure
// http://stackoverflow.com/questions/17459165/unable-to-authenticate-using-nodejs-with-ssh2-module-even-though-manual-ssh-wor