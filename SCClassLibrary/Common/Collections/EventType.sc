EventType {
	classvar all;
	var <>name, <>func, <parent;

	*initClass {
		all = IdentityDictionary.new;
	}

	*new { |name, func, parent, ownerEvent(Event.default.parent)|
		^super.newCopyArgs(name, func, parent).init(ownerEvent)
	}

	init { |ownerEvent|
		this.setParent(parent, ownerEvent);  // here, to set parent's parent
		this.class.put(this, ownerEvent);
	}

	setParent { |parentEvent, ownerEvent(Event.default.parent)|
		if(parentEvent.notNil and: { parentEvent.parent.isNil }) {
			parentEvent.parent = ownerEvent
		};
		parent = parentEvent;
	}
	parent_ { |parentEvent|
		this.setParent(parentEvent, Event.default.parent)
	}

	*at { |name, ownerEvent(Event.default.parent)|
		^all[ownerEvent].tryPerform(\at, name)
	}

	*put { |eventType, ownerEvent(Event.default.parent)|
		var collection = all[ownerEvent];
		if(collection.isNil) {
			collection = IdentityDictionary.new;
			all.put(ownerEvent, collection);
		};
		collection.put(eventType.name, eventType)
	}

	*allFor { |ownerEvent(Event.default.parent)|
		var collection = all[ownerEvent];
		if(collection.isNil) {
			^IdentityDictionary.new
		} {
			^collection
		}
	}

	*parentTypesFor { |ownerEvent(Event.default.parent)|
		^this.allFor(ownerEvent).collect(_.parent)
	}

	*functionsFor { |ownerEvent(Event.default.parent)|
		^this.allFor(ownerEvent).collect(_.func)
	}
}
