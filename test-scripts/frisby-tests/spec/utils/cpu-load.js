/**
 * Created by Vlad on 30.03.2016.
 */

// https://gist.github.com/bag-man/5570809

var os = require("os");
var prop = require("./prop");

// Utility:
// http://stackoverflow.com/questions/149055/how-can-i-format-numbers-as-money-in-javascript
/**
 * Get number as a string with separated 3 digits sections and fixed decimal part
 * @param {number} [n]: length of decimal
 * @param {number} [x]: length of sections
 */
Number.prototype.format = function(n, x) {
  var re = '\\d(?=(\\d{' + (x || 3) + '})+' + (n > 0 ? '\\.' : '$') + ')';
  return this.toFixed(Math.max(0, ~~n)).replace(new RegExp(re, 'g'), '$&,');
};

function CpuLoader(load_time_ms){

  this.updateInterval = 500;

  if (isNaN(load_time_ms))
    load_time_ms = 400;

  if (load_time_ms < 100)
    load_time_ms = 100;
  if ((load_time_ms + 50) > this.updateInterval)
    load_time_ms = this.updateInterval - 50;

  this.loadTime = load_time_ms;

}

//Create function to get CPU information
CpuLoader.prototype.cpuAverage= function () {

  //Initialise sum of idle and time of cores and fetch CPU info
  var totalIdle = 0, totalTick = 0;
  var cpus = os.cpus();

  //Loop through CPU cores
  for(var i = 0, len = cpus.length; i < len; i++) {

    //Select CPU core
    var cpu = cpus[i];

    //Total up the time in the cores tick
    for(type in cpu.times) {
      totalTick += cpu.times[type];
    }

    //Total up the idle time of the core
    totalIdle += cpu.times.idle;
  }

  //Return the average Idle and Tick times
  return {idle: totalIdle / cpus.length,  total: totalTick / cpus.length};
};

CpuLoader.prototype.measure = function () {
  
  var numOps = loadCpuFor(this.loadTime);

  //Grab second Measure
  var endMeasure = this.cpuAverage();

  //Calculate the difference in idle and total time between the measures
  var idleDifference = endMeasure.idle - this.startMeasure.idle;
  var totalDifference = endMeasure.total - this.startMeasure.total;

  //Calculate the average percentage CPU usage
  var percentageCPU = 100 - ~~(100 * idleDifference / totalDifference);

  //Output result
  var k = 1000/this.updateInterval;
  return " " + percentageCPU + "% CPU Usage. Loading: " + (numOps*k).format() + " multiplications of random numbers per second.";
};

function loadCpuFor(ms) {
  var now = new Date().getTime();
  var numOfMultiplication = 0;
  var result = 0;
  while(true) {
    for (var i=1; i < 10000; i++) {
      result += Math.random() * Math.random();
      numOfMultiplication++;
    }
    if (new Date().getTime() > now +ms)
      return numOfMultiplication;
  }
}

CpuLoader.prototype.start = function () {
  //Grab first CPU Measure
  this.startMeasure = this.cpuAverage();
  prop.start(this.measure.bind(this), this.updateInterval);
};

CpuLoader.prototype.stop = function () {
  prop.stop()
};

// Q: node.js load several cpu

// main

(function () {

  var load_time_ms;
  if (typeof process.argv[2] === 'string'){
    load_time_ms = Number(process.argv[2]);
  }
  if (isNaN(load_time_ms))
    load_time_ms = 400;

  var cpu_loader = new CpuLoader(load_time_ms);

  // http://stackoverflow.com/questions/5006821/nodejs-how-to-read-keystrokes-from-stdin
  var stdin = process.stdin;

  // without this, we would only get streams once enter is pressed
  if (stdin.isTTY)
    stdin.setRawMode(true);

  // resume stdin in the parent process (node app won't quit all by itself
  // unless an error or process.exit() happens)
  stdin.resume();

  // i don't want binary, do you?
  stdin.setEncoding('utf8');

  // on any data into stdin
  stdin.on('data', function (key) {
    // ctrl-c ( end of text )
    if (key === '\u0003') {
      cpu_loader.stop();
      console.log();
      process.exit();
    }
  });

  console.log("Use: node cpu-load [100..450]");
  console.log("%d CPU detected. Load time parameter is %d", os.cpus().length, cpu_loader.loadTime);
  console.log("Press ctr+C to cancel job;");
  cpu_loader.start();

})();
