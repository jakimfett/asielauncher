var updater = require("./AsieLauncher/internal/updater.js")
  , argv = require("optimist").argv
  , util = require("./AsieLauncher/internal/util.js")
  , _ = require("underscore")
  , request = require("request");

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
	// TODO
	run();
} else run();
