
var readline = require('readline')
  , util = require('./util.js')
  , nem = require('./nem.js')
  , _ = require('underscore');

var rl = readline.createInterface({
	input: process.stdin,
 	output: process.stdout
});
rl.setPrompt("> ");

var commands = {}
  , config = {}
  , serverInfo = {};

exports.init = function(_config, _serverInfo) {
	config = _config;
	serverInfo = _serverInfo;
}

commands["check-updates"] = {
	"description": "Checks your modpack for updates.",
	"function": function(params, cb) {
		util.say("info", "Checking for updates...");
		nem.checkModUpdates(config.launcher.minecraftVersion, serverInfo.mods, cb);
	}
}

commands["help"] = {
	"description": "Prints the list of commands.",
	"function": function(params, cb) {
		_.each(_.keys(commands), function(cmdname) {
			util.say("info", cmdname + ": " + commands[cmdname].description);
		});
		cb();
	}
}

rl.prompt();
rl.on("line", function(line) {
	rl.pause();
	var arguments = line.trim().split(" ");
	var cmdname = arguments.shift().toLowerCase();
	if(_.contains(_.keys(commands), cmdname)) {
		commands[cmdname].function(arguments, function() {
			setTimeout(function() { rl.prompt(); }, 50);
		});
	} else {
		util.say("error", "Command not found: " + cmdname + "!");
		rl.prompt();
	}
});
