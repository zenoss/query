var zenoss = zenoss || {};

(function(){
	"use strict";

	zenoss.utils = {
		tzOffset: tzOffset,
		eventEmitter: eventEmitter,
		merge: merge,
		randomId: randomId
	};

	// returns timezone offset in minutes
	function tzOffset(){
		return (function(){
			var nowOffset = new Date().getTimezoneOffset(),
				janOffset = new Date(2014, 0).getTimezoneOffset(),
				junOffset = new Date(2014, 5).getTimezoneOffset(),
				isNorth,
				DSTOffset = 0;

			// if jan offset is greater than june offset,
			// this is the northern hemisphere
			// TODO - if equal, DST is not a thing
			isNorth = janOffset > junOffset;

			// if northern hemisphere and current offset is same as june
			// or if southern hemisphere and current offset is same as january
			// then this are is currently under DST
			if(isNorth && nowOffset === junOffset || !isNorth && nowOffset === janOffset){
				// getTimezoneOffset deals in minutes, so 60 = +1hr
				DSTOffset = 60;
			}

			return nowOffset + DSTOffset;
		})();
	}

	// mixin that adds on, off, and emit methods to object. usage:
	// eventEmitter.call(myObj);
	// this will extend eventEmitter's stuff onto `myObj`.
	function eventEmitter(){

		var events = {},
			cids = 0;

		// add listener for `event` that executes `callback`
		var on = function(event, callback){
			// give the callback a uuid that can later be used for removing events
			callback.cid = callback.cid || cids++;

			// if this event hasn't been registered yet, make a new array for it
			if(!(event in events)){
				events[event] = [];
			}

			// add this callback to the list for this event
			events[event].push(callback);
		};

		// lookup the `callback` by id and remove from `event`'s queue
		var off = function(event, callback){
			var callbackId = callback.cid;

			if(callbackId){
				for(var i in events[event]){
					if(events[event][i].cid === callbackId){
						events[event].splice(i, 1);
						break;
					}
				}
			}
		};

		// trigger all callbacks for `event`, and pass any supplied args in
		var emit = function(event){
			var args = Array.prototype.slice.call(arguments, 1),
				callbackList = events[event] || [];

			// call each callback with the passed in args
			for(var i in callbackList){
				callbackList[i].apply(this, args);
			}

		};
		
		var EventEmitter = function(){
			this.on = on;
			this.off = off;
			this._emit = emit;
		};

		EventEmitter.call(this);

		return this;
	}

	// shallow copies properties of each argument onto the argument
	// to the left, effectively merging together multiple objects
	function merge(){
		var obj = {};
		var args = Array.prototype.slice.call(arguments);
		args.forEach(function(arg){
			for(var i in arg){
				if(arg[i] !== undefined) obj[i] = arg[i];
			}
		});
		return obj;
	}

	// generates a random, 6 digit, numeric id
	function randomId(){
		return 'xxxxxx'.replace(/[xy]/g, function(c) {
		    return Math.floor(Math.random()*9);
		});
	}

})();