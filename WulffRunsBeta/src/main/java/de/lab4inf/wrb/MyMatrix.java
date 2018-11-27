package de.lab4inf.wrb;

import java.util.ArrayList;

import de.lab4inf.wrb.DemoParser.ExpressionContext;

public class MyMatrix {

	static int threadNumber = 32;
	protected DemoParser.ExpressionContext[][] matrix;
	protected Double[][] dmatrix;
	protected ArrayList<int[]> varFields = new ArrayList<int[]>();
	protected DemoParser.MatrixDefinitionContext matrixRoot;
	protected MyVisitor parent;
	protected int height;
	protected int width;

	public MyMatrix(MyVisitor parent, DemoParser.MatrixDefinitionContext matrix, int width, int height) {
		this.parent = parent;
		this.matrixRoot = matrix;
		this.height = height;
		this.width = width;
		this.dmatrix = new Double[width][height];
		this.matrix = new ExpressionContext[this.dmatrix.length][this.dmatrix[0].length];
	}
	
	public MyMatrix(Double[][] matrix) {
		this.dmatrix = matrix;
		this.height = matrix.length;
		this.width = matrix[0].length;
	}

	public void refreshNumbers() {
		for (int[] i : this.varFields) {
			ExpressionContext test = this.matrix[i[0]][i[1]];//matrix[i[0]][i[1]];
			this.dmatrix[i[0]][i[1]] = this.parent.visit(test); // this.parent.visit(this.matrix[i[0]][i[1]]);
		}
	}

	public Double[][] addition(Double[][] otherMatrix) {
		// check if sizes fit
		if (otherMatrix.length != this.dmatrix.length || otherMatrix[0].length != this.dmatrix[0].length) {
			throw new IllegalArgumentException("Size of Matrixes differs.");
		}
//		// Make sure our numbers are good
//		this.refreshNumbers();

		// Mathemagic
		Double[][] res = new Double[this.dmatrix.length][this.dmatrix[0].length];
		for (int y = 0; y < this.dmatrix.length; y++) {
			for (int x = 0; x < this.dmatrix[0].length; x++) {
				res[x][y] = this.dmatrix[x][y] + otherMatrix[x][y];
			}
		}
		return res;
	}

	public void multiplyParallelAndSeriell(MyMatrix otherMatrixObjekt, Double[][] solutionMatrix, int yStart, int yEnd) {
		// Make sure our numbers are good
		this.refreshNumbers();
		otherMatrixObjekt.refreshNumbers();
		
		Double[][] otherMatrix = otherMatrixObjekt.dmatrix;
		
		for (int i = yStart; i < yEnd; i++) {
			for (int j = 0; j < solutionMatrix[0].length; j++) {
				// initialize res
				solutionMatrix[i][j] = 0.0;
				for (int k = 0; k < otherMatrix.length; k++) {
					solutionMatrix[i][j] += this.dmatrix[i][k] * otherMatrix[k][j];
				}
			}
		}
		return;
	}
	
//	public MyMatrix multiplication(MyMatrix otherMatrix) {
//		return multiplication(otherMatrix, 0 , otherMatrix.getWidth());
//	}
	
	public MyMatrix multiplication(MyMatrix otherMatrix) {
		// Check if columns of first = rows of second
		if (this.getWidth() != otherMatrix.getHeight()) {
			throw new IllegalArgumentException("Incorrect size.");
		}
		// Make sure our numbers are good
		this.refreshNumbers();
		otherMatrix.refreshNumbers();

		// Mathemagic
		Double[][] res = new Double[this.getHeight()][otherMatrix.getWidth()];
		
		for (int i = 0; i < this.getHeight(); i++) {
			for (int j = 0; j < otherMatrix.getWidth(); j++) {
				res[i][j] = 0.0;
				for (int k = 0; k < otherMatrix.getHeight(); k++) {
					res[i][j] += this.dmatrix[i][k] * otherMatrix.getDmatrix()[k][j];
				}
			}
		}
		
		return new MyMatrix(res);
	}

	public ArrayList<MyMatrix> getSplitColMatrix(MyMatrix otherMatrix, int pieces) {
		// Mathemagic the size of the individual Pieces
		if(otherMatrix.width < pieces) {
			pieces = otherMatrix.width;
		}
		int[] size = new int[pieces];
		for (int i = 0; i < pieces; i++) {
			size[i] = (otherMatrix.getWidth() - (otherMatrix.getWidth() % pieces)) / pieces;
		}
		size[pieces - 1] += otherMatrix.getWidth() % pieces;

		int globalX = 0;
		ArrayList<MyMatrix> ret = new ArrayList<MyMatrix>();

		// stuff all the stuff into the other stuff
		for (int i = 0; i < pieces; i++) {
			Double[][] res = new Double[otherMatrix.getHeight()][size[i]];

			for (int x = 0; x < size[i]; x++) {
				for (int y = 0; y < otherMatrix.getHeight(); y++) {
					res[y][x] = otherMatrix.getDmatrix()[y][globalX];
				}
				globalX++;
			}

			ret.add(new MyMatrix(res));
		}
		return ret;
	}
	
	public MyMatrix multiplyParrallel(MyMatrix otherMatrix) {
		return multiplyParrallel(otherMatrix, MyMatrix.threadNumber);
	}

	public MyMatrix multiplyParrallel(MyMatrix otherMatrix, int piece) {
		ArrayList<MyMatrix> pieces = getSplitColMatrix(otherMatrix, piece);
		MatrixWorker[] t = new MatrixWorker[piece];
		Thread tr[] = new Thread[piece];
		
		int i = 0;

		// Thread stuffs
		for (MyMatrix p : pieces) {
			t[i] = new MatrixWorker(p, this);
			tr[i] = new Thread(t[i]);
			tr[i].start();
			// p = multiplication(p);

			i++;
		}

		// Wait for all threads to finish
		i = 0;
		Boolean dead = false;
		while (!dead) {
			if (i >= pieces.size()) {
				dead = true;
			} else {
				if (!tr[i].isAlive()) {
					i++;
				}
			}
		}
		i = 0;
		Double[][] ret = new Double[height][otherMatrix.getWidth()];
		int globalX = 0;

		// Synchronizing
		for (i = 0; i < pieces.size(); i++) {
			for (int x = 0; x < t[i].getMatrixGoal().getWidth(); x++) {
				for (int y = 0; y < height; y++) {
					ret[y][globalX] = t[i].getMatrixGoal().getDmatrix()[y][x];
				}
				globalX++;
			}
		}

		return new MyMatrix(ret);
	}

	static boolean compare(Double[][] m1, Double[][] m2) {
		if (m1.length != m2.length || m1[0].length != m2[0].length) {
			return false;
		}
		for (int x = 0; x < m1.length; x++) {
			for (int y = 0; y < m1[0].length; y++) {
				if (m1[x][y] != m2[x][y]) {
					return false;
				}
			}
		}
		return true;
	}

	static String print(Double[][] m1) {
		String s = "";
		for (int x = 0; x < m1.length; x++) {
			s += "[";
			for (int y = 0; y < m1[0].length; y++) {
				s += m1[x][y];
				if (y < m1[0].length - 1) {
					s += ", ";
				}
			}
			s += "]";
		}
		return s;
	}

	public MyVisitor getParent() {
		return parent;
	}

	public void setParent(MyVisitor parent) {
		this.parent = parent;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public DemoParser.ExpressionContext[][] getMatrix() {
		return matrix;
	}

	public ArrayList<int[]> getVarFields() {
		return varFields;
	}

	public void setVarFields(ArrayList<int[]> varFields) {
		this.varFields = varFields;
	}
	
	public void addVarField(int y, int x, DemoParser.ExpressionContext ctx) {
		int[] i = {y, x};
		this.varFields.add(i);
		this.matrix[y][x] = ctx;
	}

	public Double[][] getDmatrix() {
		return dmatrix;
	}

	public DemoParser.MatrixDefinitionContext getMatrixRoot() {
		return matrixRoot;
	}

}