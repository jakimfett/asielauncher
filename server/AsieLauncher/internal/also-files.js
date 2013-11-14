var _ = require('underscore')
  , fs = require('fs')
  , wrench = require('wrench')
  , util = require('./util.js')
  , Zip = require('adm-zip')
  , path = require('path')
  , express = require('express');
	
var config = {
	"blacklistedFiles": [],
	"nonOverwrittenFiles": []
}

var configOut = null;

var app = null;

exports.setExpress = function(_app) { app = _app; }

exports.addFile = function(file, location) {
	util.say("debug", "Adding file " + file + " <- " + location);

	if(configOut.mode == "local") {
		util.copyFileSync(location, configOut.local.location+"/"+file);
	}
	if(configOut.mode == "http") {
		app.use("/" + file, function(req,res) { res.sendfile(location); });
	}
}


exports.addDirectory = function (dirName, realDir) {
	util.say("debug", "Adding directory: " + dirName + " <- " + realDir);

	if(configOut.mode == "local") {
		wrench.copyDirSyncRecursive(realDir+"/", configOut.local.location+"/"+dirName+"/", {
 			excludeHiddenUnix: false
		});
	}
	if(configOut.mode == "http") {
		app.use("/" + dirName, express.static(realDir));
	}
}

function getDirectoryList(name, destName, addLocation, prefix) {
	if(!_.isString(prefix)) prefix = "";
	if(!fs.existsSync(name)) return [];
	return _.chain(wrench.readdirSyncRecursive(name))
		.filter(function(file) {
			var location = name+"/"+file;
			var blacklisted = false;
			_.each(config.blacklistedFiles, function(test) {
				if(location.indexOf(test) >= 0) {
					blacklisted = true;
					util.say("info", "Blacklisted "+file);
				}
			});
			if(blacklisted) return false;
			else return !(fs.lstatSync(name+"/"+file).isDirectory());
		}).map(function(file) {
			var fileNew = {"filename": prefix+file, "size": util.getSize(name+"/"+file), "md5": util.md5(name+"/"+file)};
			var destination = destName+file;
			if(addLocation) fileNew.location = name+"/"+file;
			fileNew.overwrite = !(_.contains(config.nonOverwrittenFiles, file));
			if(fileNew.overwrite)
				fileNew.overwrite = !(_.some(config.nonOverwrittenFiles, function(nameNO) {
					return (destination.indexOf(nameNO) == 0);
				}));
			return fileNew;
		}).value();
}
exports.getDirectoryList = getDirectoryList;

function getPossibleDirectories(name) {
	var dirs = [];
	if(name.indexOf(".") === 0 || name.indexOf("/") === 0) dirs = [name, name+"-client"];
	else {
		// TODO: Figure out if we need this or want this.
		_.each(config.lookupDirectories, function(_d) {
			var d = _d;
			if(!(/\/$/.test(d))) d = d + "/";
			dirs.push(path.resolve(d+name)); dirs.push(path.resolve(d+name+"-client"));
		});
	}
	return _.filter(dirs, fs.existsSync);
}
exports.getPossibleDirectories = getPossibleDirectories;

function getDirectoriesList(name, addLocation) {
	var destName = name+"/";
	if(name == "root") destName = ""; // Special case
	var dirs = [];
	_.each(getPossibleDirectories(name), function(dir) {
		dirs = _.union(dirs, getDirectoryList(dir, destName, addLocation));
	});
	return dirs;
}

var totalSize = 0;

exports.initialize = function(_config, _modpackConfig) {
	totalSize = 0;
	config = _modpackConfig;
	configOut = _config.output;
}

exports.file = function(dir) {
	var list = getDirectoriesList(dir, false);
	return _.map(list, function(file) {
		file.filename = dir+"/"+file.filename;
		totalSize += file.size;
		return file;
	});
}


function createZip(name, dirs) {
	var zip = new Zip();
	var found = false;
	_.each(dirs, function(dir) {
		if(!fs.existsSync(dir)) return;
		found = true;
		zip.addLocalFolder(dir);
	});
	if(found) zip.writeZip(name); // No empty zips!
	return found;
}
exports.createZip = createZip;

exports.zip = function(dir, overwrite, id, location) {
	location = location || "";
 	id = id || "null";
	var name = "./AsieLauncher/temp/zips/"+id+"-"+dir+".zip";
	if(createZip(name, getPossibleDirectories(location+dir))) {
		var zipData = {"filename": id+"-"+dir+".zip", "directory": dir == "root" ? "" : dir,
			"size": util.getSize(name),
			"md5": util.md5(name), "overwrite": overwrite || true};
		totalSize += zipData.size;
		return zipData;
	} else return null;
}

function getFiles(dir) {
	var list = getDirectoryList(dir, "", false);
	var size = util.getFilesSize(list);
	return {"files": list, "size": size};
}
exports.getFiles = getFiles;

exports.platforms = function() {
	var p = {};
	_.each(fs.readdirSync("./AsieLauncher/platform"), function(platform) {
		p[platform] = getFiles("./AsieLauncher/platform/"+platform);
	});
	return p;
}

exports.getTotalSize = function() { return totalSize; }
