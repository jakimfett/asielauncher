
var readline = require('readline')
  , util = require('./util.js')
  , nem = require('./nem.js')
  , alsl = require('./versionAL.js')
  , vp = require('./versionParser.js')
  , _ = require('underscore');

var rl = readline.createInterface({
	input: process.stdin,
 	output: process.stdout
});
rl.setPrompt("> ");

var commands = {}
  , config = {}
  , serverInfo = {}
  , heartbeat = null
  , fileServer = null;

exports.init = function(_config, _serverInfo, _heartbeat, _fileServer) {
	config = _config;
	serverInfo = _serverInfo;
	heartbeat = _heartbeat;
	fileServer = _fileServer;
}

function combineUpdates(one, two) {
	var different = _.intersection(_.keys(one), _.keys(two));
	var result = {};
	_.each(different, function(id) {
		var a = one[id], b = two[id];
		if(a.type != b.type) result[id] = (a.type == "dev" ? b : a);
		else if(vp.compare(a.new, b.new) > 0) result[id] = b;
		else result[id] = a;
	});
	_.each(_.keys(one), function(id) { if(!_.has(result, id)) result[id] = one[id]; });
	_.each(_.keys(two), function(id) { if(!_.has(result, id)) result[id] = two[id]; });
	return result;
}

commands["check-updates"] = {
	"description": "Checks your modpack for updates.",
	"function": function(params, cb) {
		util.say("info", "Checking for updates...");
		nem.checkModUpdates(config.launcher.minecraftVersion, serverInfo.mods, function(resultNEM) {
			//alsl.checkModUpdates(config.launcher.minecraftVersion, serverInfo.mods, function(resultALSL) {
				//var result = combineUpdates(resultNEM, resultALSL);
			var result = resultNEM;
			_.each(_.values(result), function(mod) {
				util.say("info", "["+mod.source.toUpperCase()+"] Found " + (mod.type == "dev" ? "dev " : "")
					+ "update of mod " + mod.name
					+ "! (" + mod.current + " -> " + mod.new + ")");
			});
			cb();
			//});
		});
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

commands["stop"] = {
	"description": "Gracefully stop the server.",
	"function": function(params, cb) {
		rl.close();
		heartbeat.close();
		fileServer.close();
		process.exit(0);
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
		if(cmdname.length > 0) util.say("error", "Command not found: " + cmdname + "!");
		rl.prompt();
	}
});
