/**
 * Created by Vlad on 12.06.2015.
 */
var
	fs = require('fs'),
	path = require('path'),
	util = require('util'),
	cp = require('child_process');

var cfg = require('./../Common/ghap-config');

var log4js = require('log4js');
log4js.configure({
	appenders: [
		{ type: 'console', category: 'stdout' },
		{ type: 'file', filename: 'ghap-stash-test.log', category: ['stdout','filelog'] }
	]
});
var logger = log4js.getLogger('stdout');
var fileLogger = log4js.getLogger('filelog');

var ctrlFlow = require('../Common/control_flow');

var args = process.argv.slice(2);
if (args.length < 1){
	console.log("Use: node %s [environment] [--logcmds] path", process.argv[1]);
	process.exit(1);
}

var isLogcmdsKeyApplied = false;

var arg_num = 0;
while (args[arg_num].substr(0,2) === '--') {
	var option_name = args[arg_num].substr(2);
	if (option_name === 'logcmds') {
		isLogcmdsKeyApplied = true;
		console.log('AutoTetsers folders will be deleted before clone.')
	} else {
		if (!cfg.setConfig(option_name)) {
			console.log("Configuration for '%s' environment not found. ", option_name);
			process.exit(1);
		}
	}
	arg_num++;
}

var data_path = args[arg_num++];
var base_path = path.resolve('.');
if (!path.isAbsolute(data_path))
	data_path = path.join(base_path, data_path);

(function runTest(){
	if (fs.existsSync(data_path) && fs.statSync(data_path).isDirectory()) {
		logger.info('chdir '+ data_path);
		process.chdir(data_path);

		deleteAutoTestersFolders();

		var autoTesters = require('./ghap-autotesters');
		autoTesters.getAutoTesters( function(){
			if (makeAutoTesterFolders(autoTesters))
				testStash(autoTesters);
			else
				final();
		});
	} else {
		logger.error("Invalid path '%s'",data_path);
		final();
	}
})();

function deleteAutoTestersFolders(){
	var dirs = getDirectories(data_path);
	dirs.forEach(function(dir_name){
		var pattern = new RegExp(/^AutoTester\d+$/);
		if (dir_name.match(pattern))
			runCommandSync('rmdir /s /q '+dir_name);
	})
}

function makeAutoTesterFolders(auto_testers){
	try {
		auto_testers.usersMap.forEach(function(userMap){
			fs.mkdirSync(userMap.name)
		});
		return true;
	} catch (ex) {
		console.error(ex.message);
		return false;
	}
}

function testStash(auto_testers){
	var prop = require("./prop");
	var clone_calls = [];
	clone_calls.push(function(next){prop.start();next()});
	auto_testers.usersMap.forEach(function(tester_map){
		clone_calls.push(
			function(next) {
				var start = new Date();
				var cmd_str = util.format("git clone https://%s:%s@git.%sghap.io/stash/scm/%s/%s.git %s",
				  tester_map.umsUser.getName(),
					tester_map.umsUser.getPassword(),
					(cfg.environment === 'prod' ? '' : cfg.environment+'.'),
					'common',
					'common',
					tester_map.umsUser.getName()
				);
				logger.info("'%s' command started.", cmd_str);
				if (isLogcmdsKeyApplied)
					cmd_str = 'echo '+cmd_str+' >> cmds_log.txt';
				var exec_options = { timeout: 330000 };
				cp.exec( cmd_str, exec_options, function(error, stdout, stderr){
					if (error)
						logger.error(error.message);
					if (stdout)
						logger.info(stdout);
					var exec_time_ms = new Date() - start;
					logger.info("Command for %s finished within %dms", tester_map.name, exec_time_ms);
					next();
				})
			}
		)
	});
	ctrlFlow.fullParallel(clone_calls, function(){
		prop.stop();
		final();
	});
}

function final(){
	var final_calls = [];

	ctrlFlow.series(final_calls, function(){
		console.log('Script finished.');
		log4js.shutdown(function(){process.exit(0)});
	});
}

function runCommandSync(cmd_str){
	logger.info(cmd_str);
	try {
		var res = cp.execSync(cmd_str + ' 2>&1');
		if (res.length > 0)
			logger.info(res.toString())
	} catch (error) {
		logger.error(error.message);
	}
}

function getDirectories(srcpath) {
	try {
		return fs.readdirSync(srcpath).filter(function (file) {
			return fs.statSync(path.join(srcpath, file)).isDirectory();
		});
	} catch (ex) {
		console.error(ex.message);
		return [];
	}
}
