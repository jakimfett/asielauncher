var updater = require("./AsieLauncher/internal/updater.js")
  , argv = require("optimist").argv
  , util = require("./AsieLauncher/internal/util.js")
  , _ = require("underscore")
  , request = require("request")
  , async = require("async");

function run() {
	// Wipe cache
	_.each(require.cache, function(v, k) {
		require.cache[k] = null;
		delete require.cache[k];
	});
	// Launch
	var also = require("./AsieLauncher/internal/also.js");
	also.run(process.cwd());
}

if(!(argv.s || argv.skip)) {
	util.say("info", "Checking for updates...");
	var oldInfo = updater.getLocalInfo();
	updater.getOnlineInfo(function(newInfo) {
		if(newInfo == null) run();
		else async.parallel([
			function(cb) {
				if(newInfo.client_revision > oldInfo.client_revision)
					updater.updateClient(cb);
				else cb();
			},
			function(cb) {
				if(newInfo.server_revision > oldInfo.server_revision)
					updater.updateServer(cb);
				else cb();
			}
		], function() {
			updater.saveInfo(newInfo);
			run();
		});
	});
} else run();
