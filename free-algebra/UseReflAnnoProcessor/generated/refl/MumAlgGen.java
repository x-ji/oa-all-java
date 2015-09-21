package refl;

import test.MumAlg;

public final class MumAlgGen implements MumAlg<MumAlgGen.TypeE ,MumAlgGen.TypeP> {
	public interface TypeE {
		<E,P> E accept(MumAlg<E,P> alg);
	}

	public interface TypeP {
		<E,P> P accept(MumAlg<E,P> alg);
	}

	public TypeE booleanNode(boolean p0) {
		return new TypeE() {
			public <E,P> E accept(MumAlg<E,P> alg) {
					return alg.booleanNode(p0);
			}
		};
	}
	public TypeE defineNode(java.lang.String p0, TypeE p1) {
		return new TypeE() {
			public <E,P> E accept(MumAlg<E,P> alg) {
					return alg.defineNode(p0, p1.accept(alg));
			}
		};
	}
	public TypeE ifNode(TypeE p0, TypeE p1, TypeE p2) {
		return new TypeE() {
			public <E,P> E accept(MumAlg<E,P> alg) {
					return alg.ifNode(p0.accept(alg), p1.accept(alg), p2.accept(alg));
			}
		};
	}
	public TypeE mumblerSymbol(java.lang.String p0) {
		return new TypeE() {
			public <E,P> E accept(MumAlg<E,P> alg) {
					return alg.mumblerSymbol(p0);
			}
		};
	}
	public TypeE quoteNode(TypeE p0) {
		return new TypeE() {
			public <E,P> E accept(MumAlg<E,P> alg) {
					return alg.quoteNode(p0.accept(alg));
			}
		};
	}
	public TypeP start(TypeE p0) {
		return new TypeP() {
			public <E,P> P accept(MumAlg<E,P> alg) {
					return alg.start(p0.accept(alg));
			}
		};
	}
	public TypeE stringNode(java.lang.String p0) {
		return new TypeE() {
			public <E,P> E accept(MumAlg<E,P> alg) {
					return alg.stringNode(p0);
			}
		};
	}
}