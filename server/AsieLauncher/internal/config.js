var fs = require("fs")
  , _ = require("underscore");

var config = fs.existsSync("./also-config.json") ? require("../../also-config.json") : require("../config.json")
  , LATEST_CONFIG = 2;

var saveConfig = function() {
  if(fs.existsSync("./AsieLauncher/config.json")) fs.unlinkSync("./AsieLauncher/config.json");
  fs.writeFileSync("./also-config.json", JSON.stringify(config, undefined, 2));
}

var configUpdaters = {
	1: function() {
		var newServerList = [];
		_.each(config.serverList, function(ip, name) {
			newServerList.push({
				"ip": ip, "name": name,
				"description": ""
			});
		});
		config.serverList = newServerList;
		config.version = 2;
	},
	0: function() {
		var newConfig = {
			"version": 1,
			"serverList": config.servers,
			"webServer": { "port": config.port },
			"modpack": {
				"directories": {
					"file": config.loggedDirs,
					"zip": config.zippedDirs
				},
				"nonOverwrittenFiles": config.noOverwrite,
				"blacklistedFiles": config.blacklist,
				"jarPatchesOrder": config.jarPatchOrder,
				"optionalComponents": config.options
			},
			"launcher": {
				"minecraftVersion": config.mcVersion,
				"onlineMode": config.onlineMode,
				"javaArguments": config.jvmArguments
			},
			"updatesURL": "http://asie.pl/launcher",
			"heartbeat": { // New in v1
				"url": "http://asie.pl/launcher/heartbeat.todo",
				"enabled": true
			}
		};
		config = newConfig;
	}
}

if(!config.version) { config.version = 0; }

while(config.version < LATEST_CONFIG) {
	configUpdaters[config.version]();
}
saveConfig();

exports.get = function() {
	return config;
}
exports.set = function(c) {
	config = c;
	saveConfig();
}
exports.save = saveConfig;
