package uk.ac.bham.cs.commdet.graphchi.program;

import uk.ac.bham.cs.commdet.graphchi.behaviours.UpdateBehaviour;


public class GCProgramFactory {

	public static GCProgram getGCProgram(String weight, String direction, UpdateBehaviour updateBehaviour) {
		if (weight.equals("Unweighted") && direction.equals("Directed")) {
			return new GCProgramUnweightedDirected(updateBehaviour);
		} else {
			throw new IllegalArgumentException("Invalid GCProgram type");
		}
	}
	
}
