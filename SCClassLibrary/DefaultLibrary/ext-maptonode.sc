ValueResponderToNodeControl {
	var <responder;
	var <node, <cname, <inSpec, <outSpec;
	var <>translate;

	*new { |resp, node, cname, inSpec, outSpec|
		^super.newCopyArgs(resp, node, cname, inSpec.asSpec, outSpec.asSpec).init
	}

	inSpec_ { |spec| inSpec = spec.asSpec }
	outSpec_ { |spec| outSpec = spec.asSpec }

	init {
		responder.addDependant(this);
		translate = { |in| in };
		this.forwardValue(responder.currentValue);
	}

	// need all responders to broadcast \didFree
	// they don't currently, so do it manually
	free {
		responder.removeDependant(this);
	}

	update { |obj, what, value|
		switch(what)
		{ \value } {
			this.forwardValue(value)
		}
		{ \didFree } {
			this.free
		}
	}

	// subclasses override
	forwardValue { |value|
		value = translate.(value);
		node.set(cname, this.mapValue(value));
	}

	mapValue { |value|
		^outSpec.map(inSpec.unmap(value))
	}
}

+ View {
	mapToNodeControl { |node, cname, inSpec, outSpec|
		^ValueResponderToNodeControl(this, node, cname, inSpec, outSpec)
	}
}
