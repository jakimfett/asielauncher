var util = require("./util.js")
  , _ = require("underscore")
  , request = require("request")
  , fs = require("fs")
  , path = require("path")
  , vp = require("./versionParser.js")
  , argv = require("optimist").argv;

var urlPrefix = "http://servers.asie.pl/json/modList/";

function getList(version, cb) {
	request({url: urlPrefix + version, json: true}, function(err, r, info) {
		if(err) { cb(err); return; }
		cb(null, info);
	});
}

function checkModUpdates(version, modDB, cb) {
	getList(version, function(err, list) {
		if(err) { cb("<Download> " + err); return; }
		var modIDs = _.keys(list);
		var result = {};
		_.each(modDB, function(mod) {
			if(_.contains(modIDs, mod.id)) {
				// Mod found!
				var oldVersion = vp.unify(mod.version);
				var newVersion = vp.unify(list[mod.id]);
				if(oldVersion == newVersion) return; // The version is the same
				util.say("debug", "AL version check: " + mod.name + " " + mod.version + " -> " + list[mod.id]);
				if(vp.compare(oldVersion, newVersion) > 0) {
					result[mod.id] = {
						"current": mod.version,
						"name": mod.name,
						"new": list[mod.id],
						"type": "regular",
						"source": "alsl"
					}
				}
				if(argv.show_outdated && vp.compare(oldVersion, newVersion) < 0) {
					util.say("error", "[ALSL] AsieLauncher serverlist's listing of " + mod.name + " is outdated! (NEM: " + list[mod.id] + ", here: " + mod.version + ")");
				}
			}
		});
		cb(result);
	});
}

exports.checkModUpdates = checkModUpdates;
