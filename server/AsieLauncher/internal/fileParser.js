var tag = "[FileParser] "
  , util = require("./util.js")
  , modDB = require("./modDB.json")
  , Zip = require("adm-zip")
  , _ = require("underscore")
  , fs = require("fs")
  , wrench = require("wrench")
  , yaml = require('js-yaml')
  , path = require('path');

function getName(oldName) {
	if(_.contains(_.keys(modDB.names), oldName)) return modDB.names[oldName];
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

function extendModInfo(filename, info) {
	var filename = path.basename(filename);
	_.each(modDB.filenames, function(template, regexStr) {
		var regex = new RegExp(regexStr, 'i');
		if(regex.test(filename)) {
			var matches = filename.match(regex);
			_.each(template, function(v, k) {
				info[k] = _.isNumber(v) ? matches[v] : v;
			});
			return info;
		}
	});
	return info;
}

function getModInfo(filename) {
	var mods = [];
	var zip = new Zip(filename);
	if(zip.getEntry("litemod.json")) {
		try {
			var data = JSON.parse(zip.readAsText("litemod.json"));
		} catch(e) {
			util.say("error", "Error parsing "+filename+"!");
			var result = extendModInfo(filename, {});
			if(_.keys(result).length > 0) mods.push(result);
			return;
		}
		util.say("debug", "Read LiteLoader mod: " + data.name + " ("+filename+")");
		mods.push(extendModInfo(filename, {
			type: "LiteLoader",
			id: data.name,
			name: getName(data.name),
			authors: [data.author || "Unknown"],
			description: data.description || "",
			version: data.version
		}));
	} else if(zip.getEntry("mcmod.info")) {
		try {
			// Replace newlines with spaces (EnderStorage)
			var data = JSON.parse(zip.readAsText("mcmod.info").replace(/(\r|\n)/g, " "));
		} catch(e) {
			util.say("error", "Error parsing "+filename+"!");
			var result = extendModInfo(filename, {});
			if(_.keys(result).length > 0) mods.push(result);
			return;
		}
		if(data.modinfoversion >= 2) {
			data = data.modlist;
		}
		_.each(data, function(mod) {
			if(!mod) return;
			util.say("debug", "Read Forge mod: " + mod.name + " ("+filename+")");
			mods.push(extendModInfo(filename, {
				type: "Forge",
				id: mod.modid,
				name: getName(mod.name),
				version: mod.version,
				description: mod.description || "",
				authors: mod.authors || [mod.author || "Unknown"],
				url: mod.url || ""
			}));
		});
	} else {
		var result = extendModInfo(filename, {});
		if(_.keys(result).length > 0) mods.push(result);
	}
	return mods;
}

exports.getModInfo = getModInfo;

exports.getModList = function(fileHandler, directories) {
	var mods = [];
	runList(fileHandler, directories, [".zip", ".jar", ".litemod"], function(dir, name) {
		_.each(getModInfo(dir+"/"+name), function(mod) {
			mods.push(mod);
		});
	});
	return mods;
}

function getPluginInfo(filename) {
	var zip = new Zip(filename);
	var name = path.basename(filename);
	if(zip.getEntry("plugin.yml")) {
		try {
			var data = yaml.safeLoad(zip.readAsText("plugin.yml"), {
				filename: name + ":plugin.yml"
			});
		} catch(e) { util.say("error", "Error parsing "+name+"!"); return; }
		return [{
			type: "Bukkit",
			id: data.name,
			name: getName(data.name),
			version: data.version,
			authors: data.authors || [data.author || "Unknown"],
			url: data.website || "",
			description: data.description || ""
		}];
	}
}

exports.getPluginInfo = getPluginInfo;

exports.getPluginList = function(fileHandler, directories) {
	var plugins = [];
	runList(fileHandler, directories, [".jar"], function(dir, name) {
		_.each(getPluginInfo(dir+"/"+name), function(plugin) {
			plugins.push(plugin);
		});
	});
	return plugins;
}

exports.getServerProperties = function(filename) {
	if(!fs.existsSync(filename)) return;
	var lines = fs.readFileSync(filename, "utf8");
	lines = lines.split('\n');
	var props = {};
	_.each(lines, function(line) {
		if(line.indexOf("#") === 0 || line.length < 2) return; // Comment;
		var info = line.match(/(\S+)=(\S*)/);
		if(info) {
			var key = info[1];
			var value = info[2];
			if(value == "true" || value == "false") { // Boolean
				value = (value == "true") ? true : false;
			} else if(/^([0-9]+)$/.test(value)) { // Number
				value = Number(value);
			} // String
			props[key] = value;
		}
	});
	return props;
}
