var _ = require('underscore')
  , fs = require('fs')
  , wrench = require('wrench')
  , util = require('./util.js')
  , Zip = require('adm-zip')
  , path = require('path');
	
var config = {
	"blacklistedFiles": [],
	"nonOverwrittenFiles": []
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
          })
          .map(function(file) {
            var fileNew = {"filename": prefix+file, "size": util.getSize(name+"/"+file), "md5": util.md5(name+"/"+file)};
						var destination = destName+file;
            if(addLocation) fileNew.location = name+"/"+file;
				    fileNew.overwrite = !(_.contains(config.nonOverwrittenFiles, file));
						if(fileNew.overwrite)
				      fileNew.overwrite = !(_.some(config.nonOverwrittenFiles, function(nameNO) {
				        return (destination.indexOf(nameNO) == 0);
				      }));
            return fileNew;
          })
          .value();          
}
exports.getDirectoryList = getDirectoryList;

function getPossibleDirectories(name) {
	var dirs = [];
	if(name.indexOf(".") === 0) dirs = [name];
	else {
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

exports.initialize = function(_config) {
	totalSize = 0;
	config = _config;
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

exports.zip = function(dir, overwrite) {
	var name = "./AsieLauncher/temp/zips/"+dir+".zip";
	if(createZip(name, [dir])) {
		var zipData = {"filename": dir+".zip", "directory": dir == "root" ? "" : dir,
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
