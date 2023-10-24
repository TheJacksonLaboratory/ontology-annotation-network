package org.jax.oan.exception;

public class OntologyAnnotationNetworkException extends Exception {
	public OntologyAnnotationNetworkException() { super(); }
	public OntologyAnnotationNetworkException(String message) { super(message);}
	public OntologyAnnotationNetworkException(String message, Exception e) { super(message,e);}
	public OntologyAnnotationNetworkException(String message, Throwable cause) {
		super(message, cause);
	}

	public OntologyAnnotationNetworkException(Throwable cause) {
		super(cause);
	}

	protected OntologyAnnotationNetworkException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
