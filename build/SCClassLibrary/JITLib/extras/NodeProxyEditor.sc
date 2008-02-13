NodeProxyEditor { 		classvar <>menuHeight=18;	var <>w, <zone, <proxy, <edits, <sinks, <pxKey, <currentSettings, 		<editNameView, <>nSliders, <editKeys, <>ignoreKeys=#[], oldNodeMap, skipjack;	var <monitor;	var <skin, <font;	var <prevSettings; 		*new { arg proxy, pxKey, nSliders=16, win, comp, 		extras=[\scope, \doc, \reset], monitor=true, sinks=true, morph=false; 		^super.new.nSliders_(nSliders).init(win, comp, extras, monitor, sinks, morph)			.pxKey_(pxKey).proxy_(proxy);	}	proxy_ { arg px; 		if (px.isKindOf(NodeProxy), { 			proxy = px;			editNameView.object_(px.source);			editNameView.string_(pxKey.asString);			oldNodeMap = proxy.nodeMap;			if (monitor.notNil) { monitor.proxy_(proxy) };			this.fullUpdate;		})	}	pxKey_ { arg key; 		if (key.notNil, {  			pxKey = key; 			{ editNameView.string_(pxKey.asString);  }.defer;		})	}		clear {		proxy = nil; 		pxKey = nil; 		{ editNameView.object_(nil).string_(""); }.defer;		this.fullUpdate;	}		init { arg win, comp, extras, monitor, sinks, morph;						var bounds;						skin = GUI.skin; 				font = GUI.font.new(*GUI.skin.fontSpecs);				bounds = Rect(0, 0, 370, nSliders * skin.buttonHeight + 30);				win = win ?? { "making internal win".postln; GUI.window.new(this.class.name, bounds) };		w = win; 		zone = comp ?? { "making internal zone".postln;  GUI.compositeView.new(w, bounds); };		zone.decorator = zone.decorator ??  { FlowLayout(zone.bounds).gap_(2@0) };		w.view.background = skin.background;		zone.background = skin.foreground;		w.front;				this.makeTopLine(extras);		if (monitor, { this.makePxMon; zone.decorator.nextLine.shift(0, 4); });		if (morph, { this.makeMorph; zone.decorator.nextLine.shift(0, 4); });		this.makeSinksSliders(sinks);		this.makeSkipJack; 	}	makePxMon { 		zone.decorator.shift(0, 5);		monitor = ProxyMonitorGui(proxy, zone, skin.buttonHeight, true, false);	}	makeMorph { 		zone.decorator.shift(0, 2);		GUI.staticText.new(zone, Rect(0,0, 330, 1)).background_(Color.gray(0.2));		zone.decorator.shift(0, 2);		PxPreset(proxy, zone) 	}		makeTopLine { |extras|		editNameView = GUI.dragSource.new(zone, Rect(0,0,90, skin.buttonHeight))			.font_(font).align_(\center)			.background_(Color.white);				GUI.button.new(zone, Rect(0,0,40, menuHeight)).states_([				["watch",  skin.fontcolor, Color.clear ],				["wait",  skin.fontcolor, Color.clear ]			])			.action_({ |b| [ { this.stopUpdate }, { this.runUpdate } ][b.value].value })			.font_(font);		GUI.button.new(zone, Rect(0, 0, 20, menuHeight))				.font_(font)				.states_([ ["^", skin.fontcolor, Color.clear] ])				.action_({  this.class.new(proxy, pxKey, nSliders); });						if (extras.includes(\scope)) {			GUI.button.new(zone, Rect(0,0,40, menuHeight))				.font_(font)				.states_([["scope", skin.fontcolor, Color.clear]])				.action_({ arg btn; if(proxy.notNil, { proxy.scope }) });		};								if (extras.includes(\reset)) {			GUI.button.new(zone, Rect(0,0,40, menuHeight))				.states_([["reset",  skin.fontcolor, Color.clear ]])				.action_({ 					if(proxy.notNil) { proxy.nodeMap = ProxyNodeMap.new; this.fullUpdate; } 				})				.font_(font);		};		if (extras.includes(\doc)) {			GUI.button.new(zone, Rect(0,0,40, menuHeight))				.states_([["doc",  skin.fontcolor, Color.clear ]])				.action_({ 					if(proxy.notNil) { 						currentEnvironment.document(proxy.key)							.title_("<" + proxy.key.asString + ">") 					} 				})				.font_(font);		};						zone.decorator.nextLine.shift(0,4);	}			makeSinksSliders { |bigSinks|		var sinkWidth; 		sinkWidth = if (bigSinks, 40, 0); 	// invisibly small		#edits, sinks = Array.fill(nSliders, { arg i;			var ez, sink;			zone.decorator.nextLine; 			sink = GUI.dragBoth.new(zone, Rect(0,0, sinkWidth, skin.buttonHeight))				.string_("-").align_(\center).visible_(false);							sink.action_({ arg sink; var px;				if (sink.object.notNil) { 					px = currentEnvironment[sink.object.asSymbol];					if(px.isKindOf(NodeProxy)) {						proxy.map(editKeys[i], px);						this.fullUpdate;					}				}			}); 			ez = GUI.ezSlider.new(zone, 290 @  skin.buttonHeight, "", nil.asSpec, 				labelWidth: 60, numberWidth: 42);			ez.labelView.font_(font).align_(\center);			[ez, sink]		}).flop;	}		makeSkipJack {		skipjack = SkipJack(			{ this.checkUpdate }, 			0.2, 			{ w.isClosed }, 			this.class.name		); 		w.onClose_({ skipjack.stop; });		this.runUpdate;	}	getCurrentKeysValues {  		if (proxy.isNil, {^[] });		currentSettings = proxy.getKeysValues(except: ignoreKeys);		editKeys = currentSettings.collect({ |list| list.first.asSymbol }); 				if(editKeys.size > nSliders) { 			if( (skipjack.dt / 10).coin) { 				"warning! too many arguments to fit in the edit window".postln 			}		};	}	checkUpdate { 		var oldKeys; 		oldKeys = editKeys; 		this.getCurrentKeysValues; 		if (monitor.notNil) { monitor.updateAll }; 		if ( (editKeys != oldKeys), { this.updateAllEdits }, {  this.updateVals });	}		fullUpdate { 		this.getCurrentKeysValues; 		this.updateAllEdits;	}		updateVals {		var sl, val, mapKey;		if (currentSettings == prevSettings) {
		 	// "no change.".postln;
			^this;
		};		editKeys.do { arg key, i;			sl = edits[i];			// editKeys and currentSettings are in sync.			val = currentSettings[i][1];
			if (val != try { prevSettings[i][1] }) {	 				// when in doubt, use this:				//	val = (currentSettings.detect { |set| set[0] == key } ? [])[1];				if(sl.numberView.hasFocus.not) {					if (val.isKindOf(SimpleNumber), { 												sl.value_(val.value);						sl.labelView.string = key;						sinks[i].string_("-");					}, { 						if (val.isKindOf(BusPlug), { 							mapKey = currentEnvironment.findKeyForValue(val) ? "";							sinks[i].object_(mapKey).string_(mapKey);							sl.labelView.string = "->" + key;						});					});				};
			};		};
		prevSettings = currentSettings; 	}		updateAllEdits {		var keyPressed = false;		edits.do { arg sl, i;			var key, val, mappx, spec, slider, number, sink, mapKey;			key = editKeys[i];			sink = sinks[i];						if(key.notNil) { 				// editKeys and currentSettings are in sync.				val = currentSettings[i][1];								spec = key.asSpec;				sl.labelView.string = key.asString;				sl.sliderView.enabled = spec.notNil;				sl.sliderView.visible = spec.notNil;				sl.numberView.enabled = true;				sl.numberView.visible = true;				sink.visible = true;										sl.sliderView.keyDownAction = { keyPressed = true }; // doesn't work yet.				sl.sliderView.keyUpAction = {  proxy.xset(key, sl.value); keyPressed = false };				sl.action_({ arg nu; 								if(keyPressed.not) {									proxy.set(key, nu.value) 								}; 				});				if(spec.notNil) { sl.controlSpec = spec } {					sl.controlSpec = ControlSpec(-1e8, 1e8);				};								mappx = val.value ? 0; 				if(mappx.isNumber) {					sl.value_(mappx ? 0);					sink.object_(nil).string_("-");				} {		// assume mappx is a proxy:					if (val.isKindOf(BusPlug), { 						mapKey = currentEnvironment.findKeyForValue(mappx) ? "???";						sink.object_(mapKey).string_(mapKey);						sl.labelView.string = "->" + key;					});								}			}  {				sl.labelView.string = "";				sl.sliderView.visible = false;				sl.numberView.visible = false;				sink.object_(nil).string_("-").visible = false;			};		};		// this.adjustWindowSize;	}		runUpdate {  skipjack.start }	stopUpdate { skipjack.stop }}RecordProxyMixer {	var <>proxymixer, bounds;	var <recorder, <recType=\mix;	var <>recHeaderFormat, <>recSampleFormat, <preparedForRecording=false;	var skipjack, <display;		*new { arg proxymixer, bounds; 		^super.newCopyArgs(proxymixer, bounds).initDefaults.makeWindow	}	initDefaults {		recHeaderFormat = recHeaderFormat ?? { this.server.recHeaderFormat };		recSampleFormat = recSampleFormat ?? { this.server.recSampleFormat };	}		prepareForRecord {		var numChannels=0, path, proxies, func;		proxies = this.selectedKeys.collect { arg key; this.at(key) };		if(proxies.isEmpty) { "no proxies".postln; ^nil };						if(recType == \multichannel)  {			proxies.do {  |el| numChannels = numChannels + el.numChannels };			func = { proxies.collect(_.ar).flat }; // no vol for now!		} {			proxies.do {  |el| numChannels = max(numChannels, el.numChannels) };			func = { var sum=0;				proxies.do { |el| sum = sum +.s el.ar };				sum 			}		};				path = thisProcess.platform.recordingsDir +/+ "SC_PX_" ++ numChannels ++ "_" ++ Date.localtime.stamp ++ "." ++ recHeaderFormat;		recorder = RecNodeProxy.audio(this.server, numChannels);		recorder.source = func;		recorder.open(path, recHeaderFormat, recSampleFormat);		preparedForRecording = true;		^numChannels	}		font { ^this.skin.font }	skin { ^GUI.skin }		server { ^proxymixer.proxyspace.server }		removeRecorder {		recorder !? {			recorder.close;			recorder.clear;			recorder = nil;		};		preparedForRecording = false;	}		record { arg paused=false;		if(recorder.notNil) {recorder.record(paused) };	}		pauseRecorder { recorder !? {recorder.pause} }	unpauseRecorder { 		recorder !? { if(recorder.isRecording) { recorder.unpause } { "not recording".postln } } 	}		selectedKeys {  ^proxymixer.selectedKeys }	at { arg key; ^proxymixer.proxyspace.envir.at(key) }	selectedKeysValues {		^this.selectedKeys.collect { |key| 					var proxy = this.at(key);					[ key, proxy.numChannels ]		}.flat;	}		makeWindow {		var rw, recbut, pbut, numChannels, recTypeChoice;		var font = this.font;				rw = GUI.window.new("recording:" + proxymixer.title, bounds);		rw.view.decorator = FlowLayout(rw.view.bounds.insetBy(20, 20));		rw.onClose = { this.removeRecorder; this.stopUpdate; };		rw.view.background = this.skin.background;						recbut = GUI.button.new(rw, Rect(0, 0, 80, 20)).states_([			["prepare rec", this.skin.fontcolor, Color.clear],			["record >", Color.red, Color.gray(0.1)],			["stop []", this.skin.fontcolor, Color.red]		]).action_({|b|			var list;			switch(b.value,				1, { 					numChannels = this.prepareForRecord;					if(numChannels.isNil) { b.value = 0; pbut.value = 0 } 				},				2, { this.record(pbut.value == 1) },				0, { this.removeRecorder  }			);			if(b.value == 1 and: { numChannels.notNil }) {				list = this.selectedKeysValues;				this.displayString = format("recording % channels: %", numChannels, list.join(" "));			};					}).font_(font);				pbut = GUI.button.new(rw, Rect(0, 0, 80, 20)).states_([			["pause", this.skin.fontcolor, Color.clear],			[">", Color.red, Color.gray(0.1)]		]).action_({|b|			if(b.value == 1) {  this.pauseRecorder } { this.unpauseRecorder }				}).font_(font);				recTypeChoice = GUI.popUpMenu.new(rw, Rect(0, 0, 110, 20))				.items_([ \mix, \multichannel ])				.action_({ arg view; 					recType = view.items[view.value]; 					if(recbut.value != 0) { recbut.valueAction = 0 } 				})				.font_(font);		recTypeChoice.value = 1;		GUI.button.new(rw, Rect(0, 0, 60, 20))				.states_([["cancel", this.skin.fontcolor, Color.clear]])				.action_({ if(recbut.value != 0) { recbut.valueAction = 0 } })				.font_(font);			rw.view.decorator.nextLine;		display = GUI.staticText.new(rw, Rect(30, 40, 300, 20)).font_(font);		rw.front;		this.makeSkipJack;		this.runUpdate;				^rw	}	displayString_ { arg str;		display.string = str;	}		updateZones {		if(preparedForRecording.not) {			this.displayString = format("proxies: %", this.selectedKeysValues.join(" "));		}	}	title {  ^"recorder for" + proxymixer.title }	runUpdate { skipjack.start; }	stopUpdate { skipjack.stop }	makeSkipJack {				// stoptest should check window.		skipjack = SkipJack({ this.updateZones }, 0.5, name: this.title);	}}