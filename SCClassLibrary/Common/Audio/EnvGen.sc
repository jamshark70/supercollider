Done : UGen {
	*kr { arg src;
		^this.multiNew('control', src)
	}
}

FreeSelf : UGen {
	*kr { arg in;
		this.multiNew('control', in);
		^in
	}
}

PauseSelf : UGen {
	*kr { arg in;
		this.multiNew('control', in);
		^in
	}
}

FreeSelfWhenDone : UGen {
	*kr { arg src;
		^this.multiNew('control', src)
	}
}

PauseSelfWhenDone : UGen {
	*kr { arg src;
		^this.multiNew('control', src)
	}
}

Pause : UGen {
	*kr { arg gate, id;
		^this.multiNew('control', gate, id)
	}
}

Free : UGen {
	*kr { arg trig, id;
		^this.multiNew('control', trig, id)
	}
}

EnvGen : UGen { // envelope generator
	classvar <doneActions;

	*initClass {
		doneActions = IdentityDictionary[
			\none -> 0,
			\pause -> 1,
			\free -> 2,
			\freeSelf -> 2,
			\freeSelfAndPrev -> 3,
			\freeSelfAndNext -> 4,
			\freeSelfAndPrevGroup -> 5,
			\freeSelfAndNextGroup -> 6,
			\freeSelfAndEarlier -> 7,
			\freeSelfAndELater -> 8,
			\freeSelfPausePrev -> 9,
			\freeSelfPauseNext -> 10,
			\freeSelfAndPrevGroupDeep -> 11,
			\freeSelfAndNextGroupDeep -> 12,
			\freeAllInGroup -> 13,
			\freeParentGroup -> 14
		];
	}

	*ar { arg envelope, gate = 1.0, levelScale = 1.0, levelBias = 0.0, timeScale = 1.0, doneAction = 0;
		envelope = this.convertEnv(envelope);
		^this.multiNewList(['audio', gate, levelScale, levelBias, timeScale,
			doneAction.asDoneAction, envelope])
	}
	*kr { arg envelope, gate = 1.0, levelScale = 1.0, levelBias = 0.0, timeScale = 1.0, doneAction = 0;
		envelope = this.convertEnv(envelope);
		^this.multiNewList(['control', gate, levelScale, levelBias, timeScale,
			doneAction.asDoneAction, envelope])
	}
	*convertEnv { arg env;
		if(env.isSequenceableCollection) {
			if (env.shape.size == 1) {
				^env.reference
			} {
				// multi-channel envelope
				^env.collect(_.reference)
			};
		};
		^env.asMultichannelArray.collect(_.reference).unbubble
	}
	*new1 { arg rate, gate, levelScale, levelBias, timeScale, doneAction, envArray;
		^super.new.rate_(rate).addToSynth.init([gate, levelScale, levelBias, timeScale, doneAction]
			++ envArray.dereference);
	}
	init { arg theInputs;
		// store the inputs as an array
		inputs = theInputs;
	}
	argNamesInputsOffset { ^2 }
}

Linen : UGen {
	*kr { arg gate = 1.0, attackTime = 0.01, susLevel = 1.0, releaseTime = 1.0, doneAction = 0;
		^this.multiNew('control', gate, attackTime, susLevel, releaseTime, doneAction.asDoneAction)
	}
}

+ Nil {
	asDoneAction { ^0 }
}

+ SimpleNumber {
	asDoneAction {}  // maybe validate?
}

+ Symbol {
	asDoneAction {
		var result = EnvGen.doneActions[this];
		if(result.notNil) { ^result } {
			Error("Invalid doneAction symbol %".format(this.asCompileString)).throw;
		}
	}
}

+ UGen {
	asDoneAction {}  // can't validate
}
