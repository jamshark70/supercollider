// TODO: init needs to query for existing IDs and order

// originally conceived for LinkClock (Ableton Link support)
// but this may in theory apply to any clock that answers to
// baseBarBeat_ (e.g. MIDISyncClocks, or BeaconClock)

MeterSync {
	classvar ids;  // to know the order of MeterSyncs coming online
	var <clock, <id, <ports;
	var <repeats = 4, <delta = 0.01, lastReceived;
	var addrs, meterChangeResp, meterQueryResp;

	// normally clock.setMeterAtBeat notifies \meter
	// and this watcher should broadcast the change.
	// but the watcher may need to change baseBarBeat internally.
	// this should not broadcast. flag is off in that case
	var broadcastMeter = true;

	*initClass {
		StartUp.add {
			var remove = { |id|
				ids.removeAllSuchThat(_ == id);
			};
			var lastReceived;  // redundancy killer
			ids = Array.new;
			OSCFunc({ |msg|
				if(msg != lastReceived) {
					msg.debug;
					remove.value(msg[1]);
					ids = ids.add(msg[1]).debug("add id");  // latest id comes in at the end
					lastReceived = msg;
				};
			}, '/SC_LinkClock/addId').fix;
			OSCFunc({ |msg|
				if(msg != lastReceived) {
					msg.debug;
					remove.value(msg[1]);
					ids.debug("removed id");
					lastReceived = msg;
				};
			}, '/SC_LinkClock/removeId').fix;
		}
	}

	*new { |clock, id, ports|
		^super.new.init(clock, id, ports)
	}

	init { |argClock, argId, argPorts|
		// these should not be changed after the fact
		// create a new object instead
		clock = argClock;
		id = argId ?? { 0x7FFFFFFF.rand };
		ports = (argPorts ?? { (57120 .. 57127) }).asArray;
		addrs = ports.collect { |port| NetAddr("255.255.255.255", port) };
		meterQueryResp = OSCFunc({ |msg|
			// because of redundancy ('repeats'), we may receive this message
			// multiple times. Respond only once.
			if(msg != lastReceived) {
				msg.debug;
				lastReceived = msg;
				this.prBroadcast(
					'/SC_LinkClock/meterReply', id, msg[1], this.enabled.asInteger,
					clock.beats, clock.beatsPerBar, clock.baseBarBeat, clock.beatInBar,
					*ids
				);
			};
		}, '/SC_LinkClock/queryMeter');
		clock.addDependant(this);
		this.enabled = true;  // assume enabled when creating
		this.prBroadcast('/SC_LinkClock/addId', id.debug("MeterSync id created"));
	}

	free {
		this.prBroadcast('/SC_LinkClock/removeId', id);
		clock.removeDependant(this);
		meterQueryResp.free;
		meterChangeResp.free;
	}

	enabled { ^meterChangeResp.notNil }

	enabled_ { |bool|
		if(bool) {
			if(clock.numPeers == 0) {
				SystemClock.sched(0.25, {
					this.resyncMeter(verbose: clock.numPeers > 0);
				});
			} {
				this.resyncMeter;
			};
			meterChangeResp = OSCFunc({ |msg|
				if(msg[1] != id and: { msg != lastReceived }) {  // ignore message that I sent
					msg.debug;
					lastReceived = msg;
					// [2] = beatsPerBar, [3] = remote clock's real time, [4] = remote clock's barline
					// also, 5/8 means maybe setting the barline to a half-beat
					// but we have to round the barline because OSC receipt time is inexact
					// -- get the rounding factor from baseBarBeat.asFraction
					// *and* use my utility method because we do not want to broadcast here
					this.prSetMeterAtBeat(msg[2],
						(clock.beats + msg[4] - msg[3]).round(msg[4].asFraction[1].reciprocal)
					);
				};
			}, '/SC_LinkClock/changeMeter');
		} {
			meterChangeResp.free;
			meterChangeResp = nil;
		};
	}

	repeats_ { |value|
		if(value < 1) {
			Error("Invalid number of repeats '%'".format(value)).throw;
		};
		repeats = value
	}

	delta_ { |value|
		if(value <= 0) {
			Error("Invalid messaging delta '%'".format(value)).throw;
		};
		delta = value
	}

	prSetMeterAtBeat { |newBeatsPerBar, beats|
		var saveFlag = broadcastMeter;
		protect {
			broadcastMeter = false;
			clock.setMeterAtBeat(newBeatsPerBar, beats);
		} { broadcastMeter = saveFlag };
	}

	// must always call action after timeout!
	// resyncMeter depends on this
	queryMeter { |action, timeout = 0.2|
		var replies, resp;
		if(timeout.isKindOf(SimpleNumber).not or: { timeout <= 0 }) {
			Error("Invalid timeout '%' provided".format(timeout)).throw;
		};
		if(clock.numPeers > 0) {
			replies = Dictionary.new;
			// no need to check lastReceived here;
			// Dictionary already collapses replies per id
			resp = OSCFunc({ |msg, time, addr|
				if(msg[1] != id) {
					msg.debug;
					replies.put(msg[1], (
						id: msg[1],
						queriedAtBeat: msg[2],
						syncMeter: msg[3].asBoolean,
						beats: msg[4],
						beatsPerBar: msg[5],
						baseBarBeat: msg[6],
						beatInBar: msg[7],
						ids: msg[8..]
					));
				};
			}, '/SC_LinkClock/meterReply');
			{
				protect {
					this.prBroadcast('/SC_LinkClock/queryMeter', clock.beats);
					(timeout + (repeats * delta)).wait;
				} { resp.free };
				action.value(replies.values);
			}.fork(SystemClock);
		} {
			// no peers, don't bother asking
			// sched is needed to make sure Conditions waiting for this method still work
			SystemClock.sched(0, { action.value(List.new) });
		};
	}

	resyncMeter { |round, verbose = true|
		var replies, cond = Condition.new, newBeatsPerBar, newBase;
		var idFold, idSet;
		fork {
			// queryMeter should never hang (based on default timeout)
			this.queryMeter { |val|
				replies = val;
				cond.unhang;
			};
			cond.hang;
			replies = replies.select { |reply| reply[\syncMeter] };
			if(replies.size > 0) {
				// recovery: if I crashed and I'm rejoining, I don't know others' IDs
				// but the replies will pass it over
				idFold = replies.collectAs(_[\ids], Array).debug("reply ids").flop.flat;
				idSet = Set.new;
				idFold = idFold.select { |item|
					var keep = idSet.includes(item).not;
					idSet.add(item);
					keep
				};
				if(ids.debug("old ids").size < idFold.debug("idFold").size) { ids = idFold };
				#newBeatsPerBar, newBase = this.prGetMeter(replies, round);
				if(verbose and: { newBeatsPerBar != clock.beatsPerBar }) {
					"syncing meter to %, base = %\n".postf(newBeatsPerBar, newBase)
				};
				this.prSetMeterAtBeat(newBeatsPerBar, newBase);  // local only
				clock.changed(\resynced, true);
			} {
				if(verbose) {
					"(%) Found no SC Link peers synchronizing meter; cannot resync".format(id).warn;
				};
				clock.changed(\resynced, false);  // esp. for unit test
			}
		}
	}

	adjustMeterBase { |localBeats, remoteBeats, round = 1|
		// 'this.prSetMeterAtBeat' to avoid \meter notification
		this.prSetMeterAtBeat(clock.beatsPerBar,
			clock.baseBarBeat + ((localBeats - remoteBeats) % clock.beatsPerBar).round(round)
		);
	}

	update { |obj, what|
		switch(what)
		{ \meter } {
			if(broadcastMeter) {
				this.prBroadcast(
					'/SC_LinkClock/changeMeter',
					id, clock.beatsPerBar, clock.beats, clock.baseBarBeat
				);
			};
		}
		// if the 'model' clock is stopped, no need to keep monitoring
		{ \stop } { this.free }
		{ \disableMeterSync } { this.free };
	}

	prBroadcast { |... msg|
		var saveFlag;
		{
			repeats.do {
				// it is not safe to modify NetAddr.broadcastFlag
				// for this entire loop because we can't block other threads
				// from using it. So, do it atomically for each repeat.
				saveFlag = NetAddr.broadcastFlag;
				protect {
					NetAddr.broadcastFlag = true;
					addrs.do { |addr|
						addr.sendMsg(*msg);
					};
				} { NetAddr.broadcastFlag = saveFlag };
				delta.wait;
			};
		}.fork(SystemClock);
	}

	prGetMeter { |replies, round|
		var lookup = IdentityDictionary.new, reply, denom;
		ids.do { |id, i| lookup[id] = i };
		reply = replies.select { |reply| reply[\syncMeter] }
		.minItem { |reply| lookup[reply[\id]] };
		// .sort { |a, b| lookup[a[\id]] < lookup[b[\id]] }
		// .at(0);
		if(reply.notNil) {
			denom = reply[\beatsPerBar].asFraction[1].reciprocal;
			^[
				reply[\beatsPerBar],
				(reply[\queriedAtBeat] - reply[\beatInBar]).round(denom) % reply[\beatsPerBar]
			]
		} {
			^[clock.beatsPerBar, clock.baseBarBeat]
		}
	}

	clock_ { ^this.shouldNotImplement(thisMethod) }
}
