package org.geospatialweb.arqext;


import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterNullIterator;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArg;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArgType;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionEval;
import com.hp.hpl.jena.sparql.util.NodeFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Map1Iterator;

/**
 * 
 * @author Taylor Cowan
 */
public class nearby extends PropertyFunctionEval {

	private static Node plat = Node.createURI("http://www.w3.org/2003/01/geo/wgs84_pos#lat");
	private static Node plon = Node.createURI("http://www.w3.org/2003/01/geo/wgs84_pos#long");
	
	public nearby() {
		super(PropFuncArgType.PF_ARG_EITHER, PropFuncArgType.PF_ARG_EITHER);
	}

	@Override
	public QueryIterator execEvaluated(Binding binding, PropFuncArg subject,
			Node arg2, PropFuncArg object, ExecutionContext ctx) {
		if ( object.isNode())
			return nearbyNode(binding, subject, object.getArg(), ctx, 100);
		else if (object.getArgListSize() == 2) {
			return nearbyNode(binding, subject, object.getArg(0), ctx, asInteger(object.getArg(1)));
		}
		Node nlat = object.getArg(0);
		Node nlon = object.getArg(1);
		Node limit = object.getArg(2);
		return find(binding, subject.getArg(), ctx, asFloat(nlat), asFloat(nlon), asInteger(limit));
	}

	private QueryIterator nearbyNode(Binding binding, PropFuncArg argSubject, Node n,
			ExecutionContext ctx, int limit) {
		double lat = Double.MIN_VALUE;
		double lon = Double.MIN_VALUE;
		Graph g = ctx.getActiveGraph();
		ExtendedIterator it = g.find(n, plat, Node.ANY);
		if ( it.hasNext()) {
			Triple t = (Triple)it.next();
			lat = asFloat(t.getObject());
			it.close();
		} else return new QueryIterNullIterator(ctx);

		it = g.find(n, plon, Node.ANY);
		if ( it.hasNext()) {
			Triple t = (Triple)it.next();
			lon = asFloat(t.getObject());
			it.close();
		} else return new QueryIterNullIterator(ctx);
		return find(binding, argSubject.getArg(), ctx, lat, lon, limit);			
	}

	private QueryIterator find(Binding binding, Node match,
			ExecutionContext ctx, double lat, double lon, int limit) {
		Indexer idx = (Indexer) ctx.getContext().get(Geo.indexKey);
		List<String> l = idx.getNearby(lat, lon, limit);
		if (! match.isVariable())
			throw new QueryExecException("nearby() must come first in your WHERE clause.");
		HitConverter cnv = new HitConverter(binding, Var.alloc(match));
		Iterator<Binding> iter = new Map1Iterator(cnv, l.iterator());
		return new QueryIterPlainWrapper(iter, ctx);
	}

	static private double asFloat(Node n) {
		if (n == null)
			return Float.MIN_VALUE;
		NodeValue nv = NodeValue.makeNode(n);
		if (nv.isFloat())
			return nv.getFloat();
		else if ( nv.isDouble())
			return nv.getDouble();
		else if (nv.isString()) 
		try {
			return Double.parseDouble(nv.getString());
		} catch (NumberFormatException e) {
			
		}
		return Float.MIN_VALUE;
	}

	static private int asInteger(Node n) {
		return (n == null) ? Integer.MIN_VALUE:NodeFactory.nodeToInt(n);
	}
}
