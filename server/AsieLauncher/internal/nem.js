var util = require("./util.js")
  , _ = require("underscore")
  , request = require("request")
  , fs = require("fs")
  , path = require("path")
  , vp = require("./versionParser.js")
  , argv = require("optimist").argv;

var supportedVersions = {
	"1.4.5": "1.4.5",
	"1.4.6": "1.4.6-1.4.7",
	"1.4.7": "1.4.6-1.4.7",
	"1.5.1": "1.5.1",
	"1.5.2": "1.5.2",
	"1.6.1": "1.6.1",
	"1.6.2": "1.6.2",
	"1.6.4": "1.6.4",
	"1.7.2": "1.7.2"
} , urlPrefix = "http://bot.notenoughmods.com/";

function getNEMList(version, cb) {
	// Check if Minecraft version is supported
	if(!_.contains(_.keys(supportedVersions), version)) {
		cb("Minecraft version not supported!");
		return;
	}
	// Attempt downloading
	request({url: urlPrefix + supportedVersions[version] + ".json", json: true}, function(err, r, info) {
		if(err) { cb(err); return; }
		cb(null, info);
	});
}

function checkModUpdates(version, modDB, cb) {
	getNEMList(version, function(err, list) {
		if(err) { cb("<Download> " + err); return; }
		// Generate list of closest mod IDs/names
		var nemList = {};
		_.each(list, function(nemMod) {
			if(nemMod.version == "dev-only") nemMod.version = nemMod.dev;
			nemList[nemMod.modid || nemMod.name] = nemMod;
		});
		var nemMods = _.keys(nemList);
		// Check per mod
		_.each(modDB, function(mod) {
			if(_.contains(nemMods, mod.id)) {
				// Mod found!
				var nemMod = nemList[mod.id];
				var oldVersion = vp.unify(mod.version);
				var newVersion = vp.unify(nemMod.version);
				if(oldVersion == newVersion) return; // The version is the same
				if(vp.compare(oldVersion, newVersion) > 0) {
					util.say("info", "[NEM] An updated version of " + mod.name + " has been found! ("
						+ mod.version + " -> " + nemMod.version + ")");
				} else {
					if(_.isString(nemMod.dev) && nemMod.dev.length > 0 && vp.compare(nemMod.version, nemMod.dev) > 0) {
						nemMod.version = nemMod.dev;
						var newVersion = vp.unify(nemMod.version);
						if(vp.compare(oldVersion, newVersion) > 0) {
							util.say("info", "[NEM] An updated DEV version of " + mod.name + " has been found! ("
								+ mod.version + " -> " + nemMod.version + ")");
						}
					}
					util.say("debug", "NEM check: " + mod.name + " " + mod.version + " -> " + nemMod.version);
				}
				if(argv.show_outdated && vp.compare(oldVersion, newVersion) < 0) {
					util.say("error", "[NEM] NotEnoughMods' listing of " + mod.name + " is outdated! (NEM: " + nemMod.version + ", here: " + mod.version + ")");
				}
				//util.say("debug", oldVersion + " -> " + newVersion);
			}
		});
	});
}

exports.checkModUpdates = checkModUpdates;
