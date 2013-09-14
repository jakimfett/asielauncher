var tag = "[Updater] "
  , urlPrefix = "http://asie.pl/launcher/"
  , url = require("url")
  , util = require("./util.js")
  , Zip = require("adm-zip")
  , _ = require("underscore");

function downloadAndUnpack(urlS, target, cb) {
	var filename = url.parse(urlS).pathname.split("/").pop();
	var tempFile = "./AsieLauncher/temp/" + filename;
	util.say("info", tag + "Downloading "+filename+"...");
	util.deleteFile(tempFile);
	util.mkdir(["./AsieLauncher/temp", target]);
	util.download(urlS, tempFile, function() {
		util.say("info", tag + "Downloaded "+filename+"! Unpacking...");
		var zip = new Zip(tempFile);
		zip.extractAllTo(path.resolve(target), true);
		cb();
	});
}

exports.updateClient = function(cb) {
	downloadAndUnpack(urlPrefix + "AsieLauncher-latest.jar", "./AsieLauncher/internal/launcher", cb);
}
exports.updateServer = function(cb) {
	downloadAndUnpack(urlPrefix + "AsieLauncher-latest-server.jar", "./AsieLauncher/internal", cb);
}
