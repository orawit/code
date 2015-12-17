package de.uni_koblenz.west.cidre.common.utils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.ReaderRIOT;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.lang.PipedQuadsStream;
import org.apache.jena.riot.lang.PipedRDFIterator;
import org.apache.jena.riot.lang.PipedRDFStream;
import org.apache.jena.riot.lang.PipedTriplesStream;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;

/**
 * Iterates over all triples contained in one graph file or in any graph file
 * contained in a folder. Blank nodes get an id unique to the computer on which
 * the graph file is read first. (Probably, blank nodes with the same label in
 * different files will receive different ids.)
 * 
 * @author Daniel Janke &lt;danijankATuni-koblenz.de&gt;
 *
 */
public class RDFFileIterator implements Iterable<Node[]>, Iterator<Node[]>,
		Closeable, AutoCloseable {

	private final Logger logger;

	private final File[] rdfFiles;

	private int currentFile;

	private Future<?> parserFuture;

	private GraphReaderRunnable readerRunner;

	private int skippedLineNumbers;

	private PipedRDFIterator<?> iterator;

	private boolean isQuad;

	private final ExecutorService executor;

	private final boolean deleteReadFiles;

	public RDFFileIterator(File file, boolean deleteFiles, Logger logger) {
		this.logger = logger;
		deleteReadFiles = deleteFiles;
		GraphFileFilter filter = new GraphFileFilter();
		if (file.exists() && file.isFile() && filter.accept(file)) {
			rdfFiles = new File[] { file };
		} else if (file.exists() && file.isDirectory()) {
			rdfFiles = file.listFiles(filter);
		} else {
			rdfFiles = new File[0];
		}
		executor = Executors.newSingleThreadExecutor();
		getNextIterator();
	}

	private boolean isCurrentFileSkippable() {
		Lang lang = RDFLanguages
				.filenameToLang(rdfFiles[currentFile].getName());
		return lang == Lang.CSV || lang == Lang.N3 || lang == Lang.NQ
				|| lang == Lang.NQUADS || lang == Lang.NT
				|| lang == Lang.NTRIPLES;
	}

	private void handleParseError(RiotException e) {
		currentFile--;
		if (!isCurrentFileSkippable()) {
			if (logger != null) {
				logger.finer("Skipping rest of file "
						+ rdfFiles[currentFile - 1].getAbsolutePath()
						+ " because of the following error:");
				logger.throwing(e.getStackTrace()[0].getClassName(),
						e.getStackTrace()[0].getMethodName(), e);
			}
			currentFile++;
			getNextIterator();
			return;
		}
		String message = e.getMessage();
		int lineWithError = skippedLineNumbers
				+ Integer.parseInt(message.substring(7, message.indexOf(',')));
		if (logger != null) {
			String prefix = message.substring(0, 7);
			String suffix = message.substring(message.indexOf(','));
			logger.finer(
					"Skipping rest of line because of the following error: "
							+ prefix + lineWithError + suffix);
		}
		if (message.contains("(newline)")) {
			lineWithError--;
		}
		String baseIRI = rdfFiles[currentFile].getAbsolutePath();
		TypedInputStream in = RDFDataMgr.open(baseIRI);
		skipErroneousLine(in, baseIRI, lineWithError);
		createIterator(baseIRI, in);
	}

	private void skipErroneousLine(TypedInputStream in, String baseIRI,
			int lineWithError) {
		int currentLine = 1;
		int nextChar = -1;
		try {
			do {
				nextChar = in.read();
				if (nextChar == '\n') {
					currentLine++;
				}
			} while (currentLine <= lineWithError && nextChar != -1);
			skippedLineNumbers = currentLine - 1;
		} catch (IOException e1) {
			if (logger != null) {
				logger.finer("Skipping rest of file "
						+ rdfFiles[currentFile - 1].getAbsolutePath()
						+ " because of the following error:");
				logger.throwing(e1.getStackTrace()[0].getClassName(),
						e1.getStackTrace()[0].getMethodName(), e1);
			}
			e1.printStackTrace();
		}
	}

	private void getNextIterator() {
		if (deleteReadFiles && currentFile > 0
				&& currentFile <= rdfFiles.length) {
			rdfFiles[currentFile - 1].delete();
		}
		if (currentFile >= rdfFiles.length) {
			iterator = null;
			return;
		}
		skippedLineNumbers = 0;
		String baseIRI = rdfFiles[currentFile].getAbsolutePath();
		TypedInputStream in = RDFDataMgr.open(baseIRI);

		createIterator(baseIRI, in);
	}

	private void createIterator(String baseIRI, TypedInputStream in) {
		iterator = new PipedRDFIterator<>();
		Lang lang = RDFLanguages
				.filenameToLang(rdfFiles[currentFile].getName());
		isQuad = RDFLanguages.isQuads(lang);
		@SuppressWarnings("unchecked")
		PipedRDFStream<?> outputStream = isQuad
				? new PipedQuadsStream((PipedRDFIterator<Quad>) iterator)
				: new PipedTriplesStream((PipedRDFIterator<Triple>) iterator);

		if (readerRunner != null) {
			readerRunner.close();
		}
		readerRunner = new GraphReaderRunnable(in, lang, baseIRI, outputStream);
		currentFile++;

		parserFuture = executor.submit(readerRunner);
	}

	@Override
	public boolean hasNext() {
		boolean hasNext = iterator != null && iterator.hasNext();
		if (!hasNext && readerRunner != null) {
			if (parserFuture != null) {
				try {
					parserFuture.get();
				} catch (InterruptedException | ExecutionException e) {
				}
				parserFuture = null;
			}
			if (readerRunner.hasException()) {
				handleParseError(readerRunner.getException());
			} else {
				getNextIterator();
			}
			if (iterator != null) {
				hasNext = hasNext();
			}
		}
		return hasNext;
	}

	@Override
	public Node[] next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		Node[] next = null;
		if (isQuad) {
			Quad quad = (Quad) iterator.next();
			if (Quad.isDefaultGraphGenerated(quad.getGraph())) {
				next = new Node[3];
			} else {
				next = new Node[4];
				next[3] = quad.getGraph();
			}
			next[0] = quad.getSubject();
			next[1] = quad.getPredicate();
			next[2] = quad.getObject();
		} else {
			Triple triple = (Triple) iterator.next();
			next = new Node[3];
			next[0] = triple.getSubject();
			next[1] = triple.getPredicate();
			next[2] = triple.getObject();
		}
		if (!hasNext()) {
			close();
		}
		return next;
	}

	@Override
	public void close() {
		executor.shutdown();
		readerRunner.close();
		if (deleteReadFiles) {
			rdfFiles[rdfFiles.length - 1].delete();
		}
	}

	@Override
	public Iterator<Node[]> iterator() {
		return this;
	}

}

class GraphReaderRunnable implements Runnable {

	private final ReaderRIOT reader;

	private final TypedInputStream in;

	private final ContentType contentType;

	private final String baseIRI;

	private final StreamRDF outputStream;

	private volatile boolean isFinished;

	private volatile RiotException exception;

	public GraphReaderRunnable(TypedInputStream in, Lang lang, String baseIRI,
			StreamRDF outputStream) {
		this.in = in;
		this.baseIRI = baseIRI;
		this.outputStream = outputStream;
		contentType = WebContent.determineCT(in.getContentType(), lang,
				baseIRI);
		reader = RDFDataMgr.createReader(lang);
		reader.setErrorHandler(ErrorHandlerFactory.errorHandlerWarn);
		reader.setParserProfile(RiotLib.profile(baseIRI, false, false,
				ErrorHandlerFactory.errorHandlerWarn));
		isFinished = false;
	}

	@Override
	public void run() {
		try {
			outputStream.start();
			reader.read(in, baseIRI, contentType, outputStream, null);
		} catch (RiotException e) {
			exception = e;
		} finally {
			isFinished = true;
			outputStream.finish();
			close();
		}
	}

	public boolean isFinished() {
		return isFinished;
	}

	public boolean hasException() {
		return exception != null;
	}

	public RiotException getException() {
		return exception;
	}

	public void close() {
		IO.close(in);
	}

}
