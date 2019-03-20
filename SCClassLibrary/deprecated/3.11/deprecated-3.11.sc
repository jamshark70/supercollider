+ AbstractFunction {
	asInt {
		this.deprecated(thisMethod, this.class.findMethod(\asInteger));
		^this.composeUnaryOp('asInteger');
	}
}

+ Object {
	asInt {
		this.deprecated(thisMethod, this.class.findMethod(\asInteger));
		^this.asInteger;
	}
}

+ Document {
	getText { |action, start = 0, range -1|
		var result;
		this.deprecated(thisMethod, this.class.findMethod(\getString));
		result = this.getString(start, range);
		action.value(result);
	}
}
