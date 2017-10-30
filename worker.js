console.log("In worker.js");

importScripts("worker/target/scala-2.12/simulation-worker-fastopt.js");
fi.iki.santtu.energysimworker.SimulationWorker().main();
