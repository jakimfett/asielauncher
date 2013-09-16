var tag = "[MCMod] "
  , util = require("./util.js")
  , modNames = require("./modNameDB.json")
  , Zip = require("adm-zip")
  , _ = require("underscore")
  , fs = require("fs")
  , wrench = require("wrench")

function getName(oldName) {
	if(_.contains(modNames, oldName)) return modNames[oldName];
	return oldName;
}

exports.getModList = function(fileHandler, directories) {
	util.say("info", tag + "Scanning through mod files...");
	var mods = [];
	var dirs = [];
	_.each(directories, function(dir) {
		_.each(fileHandler.getPossibleDirectories(dir), function(item) {
			dirs.push(item);
		});
	});
	_.each(dirs, function(dir) {
		_.each(util.onlyExtension(wrench.readdirSyncRecursive(dir), [".zip", ".jar", ".litemod"]),
		function(name) {
			var zip = new Zip(dir + "/" + name);
			if(zip.getEntry("litemod.json")) {
				var data = JSON.parse(zip.readAsText("litemod.json"));
				util.say("debug", "Read mod: " + data.name + " ("+dir+"/"+name+")");
				mods.push({
					type: "LiteLoader",
					id: data.name,
					name: getName(data.name),
					authors: [data.author || "Unknown"],
					description: data.description || "",
					version: data.version
				});
			}
			if(zip.getEntry("mcmod.info")) {
				var data = JSON.parse(zip.readAsText("mcmod.info"));
				_.each(data, function(mod) {
					if(!mod) return;
					util.say("debug", "Read mod: " + mod.name + " ("+dir+"/"+name+")");
					mods.push({
						type: "Forge",
						id: mod.modid,
						name: mod.name,
						version: mod.version,
						description: mod.description || "",
						authors: mod.authors || [mod.author || "Unknown"],
						url: mod.url || ""
					});
				});
			}
		});
	});
	return mods;
}
