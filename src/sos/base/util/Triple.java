package sos.base.util;

public class Triple<A,B,C> {

	private final A first;
	private final B second;
	private final C third;

	public Triple(A first,B second,C third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}
	
	public A first() {
		return first;
	}
	public B second() {
		return second;
	}
	public C third() {
		return third;
	}
	
	@Override
	public String toString() {
		return "["+first+","+second+","+third+"]";
	}
}
