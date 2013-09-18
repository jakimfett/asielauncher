var tag = "[FileParser] "
  , util = require("./util.js")
  , modNames = require("./modNameDB.json")
  , Zip = require("adm-zip")
  , _ = require("underscore")
  , fs = require("fs")
  , wrench = require("wrench")
  , yaml = require('js-yaml');

// Prior to 0.4.0-rc1, this was called mcmod.js. Clean up!
if(fs.existsSync("./mcmod.js")) fs.unlinkSync("./mcmod.js");

function getName(oldName) {
	if(_.contains(_.keys(modNames), oldName)) return modNames[oldName];
	else return oldName;
}

var runList = function(fileHandler, directories, extensions, fn) {
	var dirs = [];
	_.each(directories, function(dir) {
		_.each(fileHandler.getPossibleDirectories(dir), function(item) {
			dirs.push(item);
		});
	});
	_.each(dirs, function(dir) {
		_.each(util.onlyExtension(wrench.readdirSyncRecursive(dir), extensions), function(name) {
			fn(dir, name);
		});
	});
}

exports.getModList = function(fileHandler, directories) {
	util.say("info", tag + "Scanning through mod files...");
	var mods = [];
	runList(fileHandler, directories, [".zip", ".jar", ".litemod"],
		function(dir, name) {
			var zip = new Zip(dir + "/" + name);
			if(zip.getEntry("litemod.json")) {
				var data = JSON.parse(zip.readAsText("litemod.json"));
				util.say("debug", "Read LiteLoader mod: " + data.name + " ("+dir+"/"+name+")");
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
					util.say("debug", "Read Forge mod: " + mod.name + " ("+dir+"/"+name+")");
					mods.push({
						type: "Forge",
						id: mod.modid,
						name: getName(mod.name),
						version: mod.version,
						description: mod.description || "",
						authors: mod.authors || [mod.author || "Unknown"],
						url: mod.url || ""
					});
				});
			}
		});
	return mods;
}

exports.getPluginList = function(fileHandler, directories) {
	util.say("info", tag + "Scanning through plugin files...");
	var plugins = [];
	runList(fileHandler, directories, [".jar"],
		function(dir, name) {
			var zip = new Zip(dir + "/" + name);
			if(zip.getEntry("plugin.yml")) {
				var data = yaml.safeLoad(zip.readAsText("plugin.yml"), {
					filename: name + ":plugin.yml"
				});
				plugins.push({
					type: "Bukkit",
					id: data.name,
					name: getName(data.name),
					version: data.version,
					authors: data.authors || [data.author || "Unknown"],
					url: data.website || "",
					description: data.description || ""
				});
			}
		});
	return plugins;
}

exports.getServerProperties = function(filename) {
	var lines = fs.readFileSync(filename).split('\n');
	var props = {};
	_.each(lines, function(line) {
		if(line.indexOf("#") === 0 || line.length < 2) return; // Comment;
		var info = /(\S+)=(\S*)/.match(line);
		if(info) {
			var key = info[0];
			var value = info[1];
			if(value == "true" || value == "false") { // Boolean
				value = (value == "true") ? true : false;
			} else if(/^([0-9]+)$/.test(value)) { // Number
				value = Number(value);
			} // String
			props[key] = value;
		}
	});
	console.log(props);
	return props;
}
