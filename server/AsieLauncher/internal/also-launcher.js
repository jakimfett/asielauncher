var Zip = require('adm-zip')
        , archiver = require('archiver')
        , fs = require('fs')
        , util = require('./util.js')
        , _ = require('underscore');

exports.create = function(config, target, callback) {
    util.say("info", "[Launcher] Generating launcher JAR...");
    util.deleteFile("./AsieLauncher/temp/launcher.jar");
    var archive = archiver("zip");
    var launcher = new Zip("./AsieLauncher/temp/AsieLauncher-latest.jar");
    var outputStream = fs.createWriteStream(target);
    archive.pipe(outputStream);
    archive.on('error', function(err) {
        throw err;
    });
    _.each(launcher.getEntries(), function(entry) {
        if (!(/\/$/.test(entry.entryName))) {
            util.say("debug", "Packing " + entry.entryName);
            archive.append(entry.getData(), {name: entry.entryName});
        }
    });
    _.each(fs.readdirSync("./AsieLauncher/launcherConfig"), function(fn) {
        if (fn === "config.json"){
            return; // deprecated since 0.4.1
        }
        archive.append(fs.createReadStream("./AsieLauncher/launcherConfig/" + fn),
                {name: "resources/" + fn});
    });
    archive.append(JSON.stringify({"serverUrl": config.launcher.serverUrl, "directoryName": config.launcher.directoryName}),
            {name: "resources/config.json"});
    archive.finalize();
    outputStream.on("close", function() {
        util.say("info", "[Launcher] Launcher ready!");
        callback(target);
    });
}
