package de.lab4inf.wrb;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Set;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class WRBScript implements Script {

	// LinkedList<String> varName = new LinkedList<String>();
	// LinkedList<Double> varValue = new LinkedList<Double>();
	MyVisitor visitor = new MyVisitor();

	@Override
	public double parse(String definition) throws IllegalArgumentException{
		CharStream input = new ANTLRInputStream(definition);
		return parse(input);
	}

	@Override
	public double parse(InputStream defStream) throws IOException {
		CharStream input = new ANTLRInputStream(defStream);
		return parse(input);
	}
	
	
	public double[][] parseMatrix(CharStream input) throws IllegalArgumentException {
		try {
			DemoLexer lexer = new DemoLexer(input);
			lexer.removeErrorListeners();
			lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
			
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			
			DemoParser parser = new DemoParser(tokens);
			parser.removeErrorListeners();
			parser.addErrorListener(ThrowingErrorListener.INSTANCE);
			
			ParseTree tree = parser.root();
			visitor.visit(tree);
			
			return visitor.getMatrixErgebnis();
		} catch (Exception e) {
//			System.out.println("Not parsable. ");
			throw new IllegalArgumentException("Not Parsable:" + e.getMessage());
		}
	}
	
	public double parse(CharStream input) throws IllegalArgumentException {
		try {
			DemoLexer lexer = new DemoLexer(input);
			lexer.removeErrorListeners();
			lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
			
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			
			DemoParser parser = new DemoParser(tokens);
			parser.removeErrorListeners();
			parser.addErrorListener(ThrowingErrorListener.INSTANCE);
			
			ParseTree tree = parser.root();
			visitor.visit(tree);
			
			return visitor.getErgebnis();
		} catch (Exception e) {
//			System.out.println("Not parsable. ");
			throw new IllegalArgumentException("Not Parsable:" + e.getMessage());
		}
	}

	@Override
	public Set<String> getFunctionNames() {
		return visitor.getFuncMap().keySet();
	}

	@Override
	public Set<String> getVariableNames() {
		return visitor.getVarMap().keySet();
	}

	@Override
	public void setFunction(String name, Function fct) {
		visitor.getFuncMap().put(name, fct);
	}

	@Override
	public Function getFunction(String name) throws IllegalArgumentException {
		if (!visitor.getFuncMap().containsKey(name)) {
			throw new IllegalArgumentException("Error 404: Function '" + name + "' not found.");
		}
		return visitor.getFuncMap().get(name);
	}

	@Override
	public double getVariable(String name) throws IllegalArgumentException {
		return visitor.getVariable(name).getValue();
		// int index = varName.indexOf(name);
		// if (index != -1)
		// return varValue.get(index);
		// throw new IllegalArgumentException("Variable " + name + " not found");
	}

	@Override
	public void setVariable(String name, double value) {
		visitor.setVariable(name, value);
		// if(!varName.contains(name)) {
		// varName.add(name);
		// varValue.add(value);
		// }else {
		// varValue.add(varName.indexOf(name), value);
		// }
	}
	
	public MyMatrix getMatrix(String name) throws IllegalArgumentException {
		return visitor.getMatrix(name);
	}
	
	public double[][] getMatrixSolution(String name) throws IllegalArgumentException {
		return visitor.getMatrixSolution(name);
	}
	
	/**
     * @param that another script with variables and functions to add
     * @return the new build script.
     */
	@Override
    public Script concat(Script that) {
		Script ret = new WRBScript();
		
		for (String varName : this.getVariableNames()) {
            double var = this.getVariable(varName);
            ret.setVariable(varName, var);
        }
        this.getFunctionNames().forEach((fctName) -> {
        	ret.setFunction(fctName, this.getFunction(fctName)); //this may be problematic, since it just gets a pointer
        });
		
        for (String varName : that.getVariableNames()) {
            double var = that.getVariable(varName);
            ret.setVariable(varName, var);
        }
        that.getFunctionNames().forEach((fctName) -> {
        	ret.setFunction(fctName, that.getFunction(fctName)); //this may be problematic, since it just gets a pointer
        });
        
        return ret;
    }

}
